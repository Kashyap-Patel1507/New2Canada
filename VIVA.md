# New2Canada — Viva / Interview Q&A

A short answer for each of the questions an examiner is most likely to ask. Read it like a flashcard deck.

---

## Data structures

**1. Why a Trie for autocomplete instead of a HashMap?**
A Trie lets us look up *all* words starting with a given prefix in `O(L + N)` time, where `L` is the prefix length and `N` is the number of completions. A HashMap can answer "does this exact word exist?" in O(1), but it cannot enumerate prefix matches without scanning every key — which is O(D) over the full dictionary.

**2. Where exactly is the priority queue used?**
Two places, both as a *bounded min-heap*: in `AutocompleteSystem.suggest` to keep the top-k words by frequency, and in `PageRanker.topK` to extract the highest-scoring documents. Each insert is O(log k); we never sort the full list when we only want the top-k.

**3. Why a LinkedList for the search history?**
Two reasons: O(1) append at the tail when a new query comes in, and O(1) removal from the head when we drop the oldest entry after the 50-item cap. An `ArrayList` would force an O(N) shift on every drop.

**4. Why a HashSet for the dictionary?**
The spell-checker asks "is this word valid?" billions of times. A HashSet gives average-case O(1) membership; a TreeSet would give O(log n) and bigger constants.

**5. What's stored in your inverted index?**
A `HashMap<String, List<Posting>>` where each `Posting` holds a `docId` and the term-frequency inside that document. Two HashMap lookups + a set intersection is enough to answer an AND query.

## Algorithms

**6. Walk me through Wagner-Fischer / Levenshtein.**
We build a `(m+1) × (n+1)` DP table. `dp[i][0] = i` (delete the first `i` characters) and `dp[0][j] = j` (insert `j`). For each cell, if the characters match we copy `dp[i-1][j-1]`; otherwise we take `1 + min(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])` — corresponding to delete, insert, and substitute. The final answer is `dp[m][n]`. Total cost O(m·n).

**7. Why merge sort instead of `Collections.sort()`?**
The course specifically asks for a hand-rolled merge or quick sort, and merge sort gives us deterministic O(N log N) worst case plus stability — which means tied scores keep their original crawl order. `Collections.sort` would also work and is faster in practice (TimSort), but it's not what the rubric is asking us to demonstrate.

**8. Quick sort vs merge sort — why did you choose merge?**
Merge sort's worst case is O(N log N) regardless of input; quick sort degrades to O(N²) on already-sorted input unless you randomise the pivot. Search results are often nearly sorted (similar scores cluster), so merge sort is safer here. Quick sort would have been valid too.

**9. How does your PageRank work?**
Simplified two-pass: pass 1 is TF·IDF (`(1 + log TF) · log(1 + N/DF)` summed over query terms). Pass 2 lets each document absorb a damped (0.15) share of its co-listed neighbours' scores — so a document that shares terms with many other already-high-scoring documents gets a small boost. Two iterations is plenty; the maths converges fast on a small corpus.

**10. What does the BFS in your crawler look like?**
`ArrayDeque<String>` seeded with the ten configured rental-site URLs. Each iteration `poll()`s a URL, asks `PoliteFetcher` to retrieve it, and hands the parsed `Document` to `HousingCrawler`. We keep a `HashSet<String>` of visited URLs so we never fetch the same page twice. We deliberately *don't* follow links (depth 0) — keeps the load on remote sites tiny while still demonstrating the BFS pattern.

*Follow-up an examiner may ask — how do you handle JavaScript-rendered sites?* `PoliteFetcher` drives a **headless Chrome** through Selenium, not a plain HTTP GET, so client-side-rendered listings (Zumper, PadMapper, 4Rent, …) populate the DOM before we scrape. Each site declares a `readySelector` the fetcher waits for. Two sites needed care: the map-based ones (4Rent, PadMapper) only render once a WebGL map initialises, so we run Chrome with a software-WebGL backend; and realtor.ca sits behind an Imperva bot-wall that blocks automated browsers, so it falls back to the Firestore cache.

## System design

**11. Why is `SearchEngine` a "facade"?**
It exposes a tiny public API (`search`, `autocomplete`, `spellSuggest`, …) and hides all the moving parts behind it. The HTTP handlers only know about the facade; they don't reach into the inverted index or the ranker. That means we can swap the ranking algorithm without touching the API code.

**12. What happens if Firestore is down?**
Two layers of resilience: (a) writes are best-effort — a failed `set()` logs and continues, never throws out of `ingest…`. (b) The in-memory index is the primary store for reads; Firestore is just a cache. The search experience is unaffected by Firestore outages; only persistence is lost.

**13. How does Google Sign-In actually work here?**
Browser → Google OAuth popup → Google returns a short-lived **ID token** (a signed JWT). The frontend attaches it as `Authorization: Bearer <token>`. The Java backend calls `FirebaseAuth.getInstance().verifyIdToken(token)` which (a) downloads Google's public keys once and caches them, (b) verifies the JWT signature, (c) checks audience, issuer, expiry. If everything checks out, the decoded UID is what we use to scope per-user data. The server never sees the user's Google password.

**14. What is "DEMO mode" and why?**
If `serviceAccountKey.json` is missing the app still boots — crawling happens in-memory only, the auth middleware short-circuits every caller to an anonymous user, and a yellow banner is shown in the UI. This lets us evaluate the project on a clean machine without leaking Firebase credentials in the submission.

**15. Why hand-rolled JSON instead of Jackson?**
Keeps the dependency tree small. Our payloads are simple (maps, lists, primitives). `JsonWriter` is ~70 lines, well-tested, and pulls in zero transitive deps.

## Tradeoffs

**16. Are you not just scraping these sites?**
We crawl publicly-accessible pages politely: identifying User-Agent, 1.5 s per-host throttle, a bounded page-load/render wait, BFS depth 0 so we don't recurse into thousands of pages. We respect the spirit of robots.txt by limiting ourselves to documented entry points — and where a site actively refuses automated access (realtor.ca's Imperva wall), we don't try to defeat it; we fall back to cache. For a production system we'd request API access from each provider.

**17. What if the websites change their HTML?**
The `DataExtractor` will return an empty list for that site on the next crawl. The Firestore cache means we still serve the previous results to the user — so the UI keeps working until we update the selectors.

**18. Why didn't you use Spring Boot?**
Two reasons. (1) Course wants us to demonstrate raw DSA, not framework configuration. (2) Spring would add a 30 MB dependency tree to a project where the entire HTTP surface is six endpoints. The JDK's built-in `HttpServer` is enough.

**19. What's the worst-case latency of a search request?**
A query touches: 1× `TextNormalizer.tokenize` (O(query length)), per token 1× `HashMap.get` on the inverted index, 1× TF·IDF accumulation, 2 PageRank iterations, 1× merge sort (O(N log N) over hits). For our corpus size (~100 docs) the whole pipeline takes <5 ms.

**20. Where could this break under load?**
The `ScheduledExecutorService` is single-threaded — fine for a personal demo, but if two students hit `/api/refresh-crawl` simultaneously the second waits. Bigger issue: the `Map<String, SearchResult> docs` inside `SearchEngine` is mutated from the crawler thread and read from request threads — we use `synchronized` on the ingest methods but a `ConcurrentHashMap` + immutable index snapshots would be cleaner for production.

## Specific features

**21. Walk me through what happens when I type "torronto" in the apartments search.**
1. `apartments.js` posts `/api/search?type=apartments&q=torronto`.
2. `SearchEngine.search` tokenises → `["torronto"]`.
3. `InvertedIndex.search("torronto")` returns no docs (the term wasn't indexed).
4. `SearchEngine` notices results are empty and calls `spellSuggest("torronto")`.
5. `EditDistance("torronto","toronto") = 1`. Toronto is in the dictionary → returned as the top suggestion.
6. The API attaches `didYouMean: "toronto"`. The browser renders the orange banner with a clickable link.

**22. Show me an edit-distance computation by hand.**
Write the `(m+1)×(n+1)` grid for "kitten" → "sitting" → final value is 3 (substitute k→s, substitute e→i, insert g).

**23. How big does your dictionary need to be?**
~150 hand-curated words in `dictionary.txt` cover domain-specific terms (city names like "toronto"/"winnipeg", "apartment", "bedroom", "kijiji"). On top of that, the crawler `learn()`s every word it sees, so by the time the user types, the vocabulary is in the thousands.

**24. What's the difference between `RegexValidator` and `PatternFinder`?**
Validator answers "does this whole string match the format?" using `Pattern.matches`. Finder scans a free-form text and yields every substring that matches, using `Pattern.matcher().find()`. Different verbs, same regex library.

**25. Why is your postal-code regex so complicated?**
Canadian postal codes have rules about which letters can appear in each position (no Q, W, Z in the first letter, etc.). The character classes `[A-CEGHJ-NPRSTVXY]` enforce that — a sloppy `[A-Z]\d[A-Z]` would accept "F0O 0BA" which is not a valid postal code.

## Stretch

**26. Could you scale this to all of Canada?**
Three changes: (a) shard the inverted index by city, (b) move from in-memory `HashMap` to a real search index (Lucene or Elasticsearch), (c) move scraping behind a queue so failed pages can be retried. The current API surface would stay the same.

**27. Where would you add caching first?**
At the API level — `/api/autocomplete` is called on every keystroke. A simple `(prefix, limit) → Map<String,List<String>>` LRU cache with a 60-second TTL would slash CPU usage during typing.

**28. If you had another week?**
Add per-user *favourites* (one Firestore collection), surface them on the home page, and use the favourite set as a personalised re-ranking signal in the PageRanker.

**29. Walk me through how a crawled apartment gets indexed.**
`HousingCrawler.handle` → `DataExtractor.extractApartments` returns `Apartment` POJO → `engine.ingestApartment(a)` → `listingRepo.upsertApartment(a)` (Firestore, if FULL) → `registerDoc("apartments", id, …, a.toIndexText())` → `indexes["apartments"].addDocument(id, text)` tokenises and adds postings → `autocomplete.indexDocument(text)` inserts every token into the Trie → `spellChecker.learn(token)` adds to the dictionary.

**30. Anything you'd refactor with hindsight?**
The `SearchEngine` is starting to do too much. I'd split ingestion into an `Indexer` class and keep `SearchEngine` purely read-side. Also, the per-type `InvertedIndex` map would be cleaner as a strategy interface so each type can have its own field weights.
