# New2Canada — Project Report

**Course:** COMP 8547 — Advanced Computing Concepts
**Project:** International Student Assistance Search Engine
**Submission date:** _to be filled_

---

## 1. Introduction

Every year tens of thousands of international students arrive in Canada and immediately need to make four practical decisions:

1. Where will I live? (apartment / shared room)
2. How will I pay for it? (part-time job)
3. Where will I bank? (student chequing account)
4. How will I stay connected? (mobile / SIM plan)

Information is scattered across many sites — Rentals.ca, Indeed, every major bank's website, every mobile carrier's plans page. Comparing them is tedious and the language barrier makes spell-correction and autocomplete genuinely useful.

**New2Canada** is a single localhost web app that crawls these sources, indexes them, and lets a student search and compare them with the polish of a modern search engine: instant autocomplete, "Did you mean…?" spell correction, ranked results, Google Sign-In, and a per-user search history.

## 2. Objectives

| # | Objective | Met by |
|---|---|---|
| 1 | Demonstrate every required course DSA in a single application | The catalogue of Java classes (§5–§6) |
| 2 | Build a working, original web crawler | `crawler/` package, live Jsoup scrape |
| 3 | Provide intelligent search assistance (spell-check, autocomplete) | `spellcheck/`, `autocomplete/` |
| 4 | Use page-ranking with iterative refinement | `ranking/PageRanker.java` |
| 5 | Make the system usable from the browser | Multi-page UI on `localhost:8080` |
| 6 | Add authentication and cloud persistence | Firebase Auth + Cloud Firestore |
| 7 | Stay resilient under network failures | Live → Firestore-cache fallback |

## 3. System architecture

The system is a single Java process. Inside it:

```
Chrome (localhost:8080)
   │ HTTP (with Bearer token in FULL mode)
   ▼
HttpServer  ──►  ApiHandler  ──►  AuthMiddleware  ─→  FirebaseAuthVerifier
                       │
                       ▼
                  SearchEngine
                  ├─ InvertedIndex   (HashMap)
                  ├─ PageRanker      (PriorityQueue + iter)
                  ├─ AutocompleteSystem (Trie)
                  ├─ SpellChecker    (Edit Distance)
                  └─ SearchTracker   (HashMap + LinkedList)
                       │
                       ▼  rebuild()                  refresh()
                  CrawlScheduler  ◄──  ScheduledExecutorService
                       │
            ┌──────────┼─────────┐
            ▼          ▼         ▼
       HousingCrawler  JobCrawler  ... (BFS Queue + PoliteFetcher)
            │
            ▼  Jsoup live fetch
       rentals.ca · indeed.ca · rbc.com · …
                       │ writes
                       ▼
                  Firestore (listings/, users/, searches/{uid}/queries/)
```

Boot sequence (`Main.main`):

1. Detect `serviceAccountKey.json` → enter `FULL` or `DEMO`.
2. (FULL) Initialise Firebase Admin SDK and obtain a Firestore handle.
3. Construct `SearchEngine` (always in-memory; Firestore is a cache).
4. Start `CrawlScheduler` — first crawl runs immediately, repeats every 6 h.
5. Start `WebServer` on port 8080.

## 4. Algorithms used

| Algorithm | Where | Complexity | Why this algorithm |
|---|---|---|---|
| **Breadth-first search** | `WebCrawler` over a `Queue<String>` frontier | O(N) URLs | Classic, predictable crawl order; trivial to demonstrate. |
| **Trie traversal** | `Trie.findByPrefix` / `collect` | O(L + N) | O(L) prefix lookup is the gold standard for autocomplete. |
| **Wagner-Fischer DP (Levenshtein)** | `EditDistance.buildTable` | O(m·n) | Standard, correct, easy to walk through in viva. |
| **Bounded min-heap selection** | `AutocompleteSystem`, `PageRanker.topK` | O(N log k) | Faster than a full sort when only the top-k are needed. |
| **Inverted-index AND-merge** | `InvertedIndex.search` | O(min posting length) | Each token is O(1) lookup; intersection bounded by smallest list. |
| **TF·IDF + PageRank-style smoothing** | `PageRanker.rank` | O(k · E) | TF·IDF for query relevance, smoothing for popularity. |
| **Merge sort** | `ResultSorter.sortDesc` | O(N log N), stable | Stable sort preserves crawl order among ties. |
| **Regex match / find** | `RegexValidator`, `PatternFinder` | O(N) per pattern | Pre-compiled `Pattern` objects, reused across requests. |

## 5. Data structures used

| Data structure | Where it lives | Role |
|---|---|---|
| `HashMap` | `InvertedIndex`, `WordFrequencyCounter`, `SearchTracker`, `SpellChecker` frequencies | O(1) average-case key lookup |
| `HashSet` | `SpellChecker` dictionary, `WebCrawler.visited` | Membership in O(1) |
| `Trie` | `autocomplete/Trie.java` | Prefix lookup in O(L) |
| `PriorityQueue` (min-heap) | `AutocompleteSystem`, `PageRanker.topK` | Bounded top-k extraction |
| `Queue` (`ArrayDeque`) | `WebCrawler` frontier | BFS over URLs |
| `LinkedList` | `SearchTracker.history` | O(1) append + O(1) head-drop when capped |
| `ArrayList` | Almost everywhere | Default ordered list |
| `Comparable` + custom `compareTo` | `SearchResult` | Descending sort by score |

## 6. Feature explanations

### 6.1 Web crawler
`crawler/WebCrawler.java` defines an abstract BFS engine. Each subclass (`HousingCrawler`, `JobCrawler`, `BankCrawler`, `MobileCrawler`) provides:

- a list of **seed URLs** read from `AppConfig`,
- a `handle(Document)` callback that parses the page and writes typed POJOs to the `SearchEngine`.

`PoliteFetcher` adds a User-Agent, a 1.5 s per-host delay, and an 8-second timeout — so a stalled remote site never blocks an HTTP handler thread.

### 6.2 HTML parser
`parser/HTMLParser` is a thin Jsoup wrapper. `parser/DataExtractor` then does the messy real-world work: it tries several CSS selectors, falls back to regex via `PatternFinder` for prices, and infers fields like `city`, `bedrooms`, and `dataGb` from heuristics. If a target site changes its HTML structure the extractor simply returns an empty list — the Firestore cache picks up the slack.

### 6.3 Spell checking
`spellcheck/EditDistance` is a textbook Wagner-Fischer implementation. `spellcheck/SpellChecker` loads `dictionary.txt` into a `HashSet`, augments it with words harvested at crawl time, and on misspelt input returns the top-k candidates within edit distance ≤ 2, ordered by (distance, then corpus frequency).

### 6.4 Autocomplete
`autocomplete/Trie` is built character-by-character at index time. `AutocompleteSystem.suggest` finds the subtree, collects all word-end nodes, and uses a bounded min-heap to return the top-k by stored frequency.

### 6.5 Frequency count
`indexing/WordFrequencyCounter` is a HashMap-backed counter exposed via the `/api/debug` endpoint for demo purposes and used by the inverted index when building term-frequency postings.

### 6.6 Search frequency tracking
`search/SearchTracker` keeps a HashMap of query → count plus a bounded `LinkedList` of the last 50 entries. In FULL mode each query is also pushed to `searches/{uid}/queries/` in Firestore so it survives a restart.

### 6.7 Page ranking
`ranking/PageRanker.rank` does two passes:
1. **TF·IDF** — sum of `(1 + log TF) · log(1 + N/DF)` over query terms.
2. **PageRank-style smoothing** — each document absorbs a damped share of its co-listed neighbours' scores. Two iterations is enough.

`topK` then drains a bounded min-heap to return the top-k results.

### 6.8 Inverted indexing
`indexing/InvertedIndex` maintains `HashMap<term, List<Posting>>`. Each `Posting` carries `(docId, termFrequency)`. AND queries intersect the posting lists.

### 6.9 Regex validation & pattern finding
`regex/RegexValidator` exposes `isPostalCode`, `isEmail`, `isPhone`, `isSin`. `regex/PatternFinder` extracts prices, dates, phones, and emails from arbitrary text.

## 7. Firestore data model

```
listings/{id}
  type, source, url, title, … type-specific fields …
  scrapedAt
users/{uid}
  email, displayName, photoUrl, createdAt, lastLoginAt
searches/{uid}/queries/{autoId}
  q, type, resultsCount, timestamp
```

Security rules ensure that `listings/*` are server-write, public-read, and that a user can only read/write their own `users/{uid}` and `searches/{uid}/**` subtree.

## 8. Authentication flow

1. Frontend (`js/auth.js`) loads the Firebase Auth JS SDK and renders a **Sign in with Google** button.
2. Sign-in triggers Google's OAuth popup; on success the SDK exposes a short-lived **ID token** (JWT).
3. Every `/api/*` call attaches `Authorization: Bearer <idToken>`.
4. Server-side `AuthMiddleware` calls `FirebaseAuth.getInstance().verifyIdToken(token)`, which checks the JWT signature against Google's public keys, the audience (`aud`), the issuer (`iss`), and the expiry.
5. On success the decoded UID + email + display name become a `User` object the handler can read. On failure the handler returns 401.

## 9. Screenshots

> Insert the screenshots listed in `README.md` here, one per page, captioned.

## 10. References

- T. Cormen, C. Leiserson, R. Rivest, C. Stein — *Introduction to Algorithms*, 4 e (MIT Press).
- D. Manning, P. Raghavan, H. Schütze — *Introduction to Information Retrieval* (Cambridge, free PDF).
- R. Wagner, M. Fischer — *The String-to-String Correction Problem*, JACM 1974.
- Jsoup user guide — <https://jsoup.org/cookbook/>
- Firebase Admin SDK — <https://firebase.google.com/docs/admin/setup>
- Firebase Authentication — <https://firebase.google.com/docs/auth>
- Cloud Firestore — <https://firebase.google.com/docs/firestore>

## 11. Conclusion

New2Canada turns a list of textbook DSA primitives into something a real student would use. The architecture stays simple: one in-memory `SearchEngine` composed of small specialised classes, an embedded HTTP server, and a thin frontend that talks to it. Adding cloud persistence (Firestore) and authentication (Google Sign-In) did not require any redesign — the engine was already a pure facade.

The biggest engineering lesson is the value of graceful degradation. By making Firebase optional and routing every cache write through a single repository, the app boots successfully on an empty laptop, and the same code is production-ready once a service account is added.

Future work: replace the in-memory inverted index with Firestore-backed sharded indexes for horizontal scale, add live re-ranking based on per-user click signals, and turn the localhost UI into a deployable web app behind App Engine.
