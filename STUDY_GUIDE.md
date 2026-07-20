# New2Canada — Code Study Guide

Plain-English explanation of each member's code, for studying before the viva.
For each file: **what it does**, **how it works**, **key lines to remember**, and
**error handling** (what to say). Scroll to your name — everything you need is there.

> **⚠️ Two things to confirm with your team before the viva:**
> 1. This guide uses your **name → feature** mapping (one feature each): Utsav = Spell Checking,
>    Mishan = Frequency Count, Kashyap = Word Completion, Khushi = Search Frequency, Karan = Page Ranking.
>    That covers **5 of the 10 required features**. The other 5 — Web Crawler, HTML Parser,
>    Inverted Index, Regex Validation, Pattern Finding — still exist and **still need to be shown**;
>    they're in the **"Other features"** section near the bottom. Decide who presents each.
> 2. This mapping **differs from the submitted proposal** (which gave two features per member).
>    Whichever you use for grading, be consistent.

---

## How the whole project fits together (read this first)

1. **Crawl** — `CrawlScheduler` starts `HousingCrawler`, which does a BFS over 10 rental-site
   seed URLs. `PoliteFetcher` loads each page in a headless Chrome so JavaScript listings render.
2. **Parse** — `DataExtractor` hands each page to the matching site extractor in `parser/rentals/`,
   which pulls out `Apartment` objects (title, city, rent, …).
3. **Index** — each apartment's text goes into the `InvertedIndex` (word → documents), the
   autocomplete `Trie`, the word-frequency counter, and the spell-check dictionary.
4. **Search & rank** — a query goes to `SearchEngine`, which looks words up in the inverted index,
   scores results with `PageRanker` (TF·IDF), and orders them with `ResultSorter` (merge sort).
5. **Serve** — `ApiHandler` exposes everything as `/api/*` endpoints; the web page (`index.html` +
   `single.js`) calls them and draws the results.

`search/SearchEngine.java` is the **shared facade** that wires everyone's features together — it
belongs to the whole team, not one person.

---

# Utsav — Spell Checking
### 📂 Exact files to open (2):
- `src/main/java/com/new2canada/spellcheck/EditDistance.java`
- `src/main/java/com/new2canada/spellcheck/SpellChecker.java`

### `spellcheck/EditDistance.java` — how many edits between two words
- **Purpose:** Levenshtein distance — the minimum single-character edits (insert, delete, substitute)
  to turn word A into word B. "kitten" → "sitting" = 3.
- **How it works:** the classic **Wagner-Fischer** dynamic-programming table `dp[i][j]` = edits to
  turn the first `i` letters of A into the first `j` letters of B. Seed the first row/column, then
  fill each cell: matching letters are free (copy the diagonal), otherwise `1 + min(delete, insert,
  substitute)`. Answer is the bottom-right cell.
- **Key lines:**
  - `for (int i=0;i<=m;i++) dp[i][0]=i;` and `dp[0][j]=j;` — base cases (turn a word into "" = one edit per letter).
  - `if (a.charAt(i-1) == b.charAt(j-1)) dp[i][j] = dp[i-1][j-1];` — letters match → free.
  - `dp[i][j] = 1 + Math.min(subst, Math.min(del, ins));` — **the recurrence** (1 + cheapest edit).
  - `return buildTable(a,b)[a.length()][b.length()];` — answer = bottom-right cell.
- **DSA shown:** **Dynamic Programming**, 2-D table.

### `spellcheck/SpellChecker.java` — the "Did you mean…?" engine
- **Purpose:** is a word spelled right, and if not, suggest close alternatives.
- **How it works:** dictionary in a **HashSet** (O(1) "is this a word?") + a word→frequency **HashMap**.
  `suggest()` scans the dictionary, keeps candidates within edit distance 2, sorts by (closest first,
  then most frequent). `learn()` adds words seen during crawling.
- **Key lines:**
  - `dictionary.add(word); frequencies.put(word, 1);` — load dictionary into the HashSet.
  - `if (dictionary.contains(word)) return List.of(word);` — already valid, no correction.
  - `if (Math.abs(candidate.length() - word.length()) > 2) continue;` — cheap length filter (skip impossible candidates).
  - `int d = EditDistance.distance(word, candidate); if (d <= 2) …` — keep only close words.
  - `.comparingInt(distance).thenComparingInt(-frequency)` — sort: closest, then most popular.
- **DSA shown:** **HashSet / HashMap**, Edit Distance, comparator sort.

### 🛡️ Error handling — what to say in the viva
**"Pure algorithms, so error handling is input validation — I return safe defaults instead of catching exceptions that can't happen."**
- Guard clauses: null/empty checks, the length filter, a default limit.
- The one piece of I/O — loading `dictionary.txt` — goes through `ResourceLoader`, which **catches `IOException`** and falls back to an empty dictionary if the file is missing.
- *(If asked "why no try/catch in EditDistance?" → it's pure math with no I/O; there's nothing that can throw.)*

---

# Mishan — Frequency Count
### 📂 Exact file to open (1):
- `src/main/java/com/new2canada/indexing/WordFrequencyCounter.java`

*(⚠️ The `indexing/` folder also contains `InvertedIndex.java` — that's a **different** feature, see "Other features". Open **only** `WordFrequencyCounter.java` for your part.)*

### `indexing/WordFrequencyCounter.java` — count how often each word appears
- **Purpose:** count how many times each word appears inside one document. This "term frequency" is
  the TF part of the TF·IDF ranking and feeds the inverted index.
- **How it works:** a **HashMap** of word → count. `countText()` tokenizes the text and `merge`s each
  token's count by 1. `snapshot()` returns the counts sorted by frequency for the debug view.
- **Key lines:**
  - `private final Map<String, Integer> counts = new HashMap<>();` — **the counter itself**.
  - `counts.merge(token, 1, Integer::sum);` — **increment this word's count by 1** (starts at 1 if new).
  - `for (String tok : TextNormalizer.tokenize(text)) countToken(tok);` — split text into words, count each.
  - `counts.getOrDefault(token, 0)` — an unseen word safely returns 0.
- **DSA shown:** **HashMap**, frequency counting.

### 🛡️ Error handling — what to say in the viva
**"No I/O in my feature, so error handling is input validation — there's nothing that can throw to catch."**
- `countToken` skips `null`/empty tokens (`if (token == null || token.isEmpty()) return;`).
- `frequencyOf` returns 0 for a word that was never counted, instead of failing.
- *(If asked "why no try/catch?" → it's a pure in-memory HashMap; the robust form of error handling here is guarding the inputs.)*

---

# Kashyap — Word Completion (Autocomplete)
### 📂 Exact files to open (2):
- `src/main/java/com/new2canada/autocomplete/Trie.java`
- `src/main/java/com/new2canada/autocomplete/AutocompleteSystem.java`

### `autocomplete/Trie.java` — the prefix tree
- **Purpose:** store words so "all words starting with X" is fast.
- **How it works:** a tree where each node has a **HashMap** from the next character to the next node.
  `insert()` walks/creates nodes letter by letter and marks the last node as a word-end (with a
  frequency). `findByPrefix()` walks to the prefix node, then `collect()` does a depth-first walk
  gathering every word underneath.
- **Key lines:**
  - `final Map<Character, Node> children = new HashMap<>();` — each node's child map (this is what makes it a Trie).
  - `curr = curr.children.computeIfAbsent(c, k -> new Node());` — insert: walk/create a node per letter.
  - `curr.isEndOfWord = true; curr.frequency++; curr.word = word;` — mark the end of a word.
  - `curr = curr.children.get(c); if (curr == null) return null;` — findNode: stop if the path doesn't exist.
  - `if (node.isEndOfWord) out.add(node); for (Node child : node.children.values()) collect(child, out);` — DFS collect.
- **DSA shown:** **Trie**, **HashMap**, recursion (DFS).

### `autocomplete/AutocompleteSystem.java` — top-k completions
- **Purpose:** given a prefix, return the few most-popular completions (e.g. "tor" → "toronto").
- **How it works:** feeds words into the `Trie`; `suggest()` gets all words under the prefix, then uses
  a **bounded min-heap** (`PriorityQueue` by frequency): offer each word and drop the smallest when the
  heap passes k, so the k highest-frequency words survive.
- **Key lines:**
  - `PriorityQueue<Trie.Node> heap = new PriorityQueue<>(Comparator.comparingInt(n -> n.frequency));` — min-heap by frequency.
  - `heap.offer(n); if (heap.size() > k) heap.poll();` — **the top-k trick**: keep dropping the smallest, so the k biggest survive.
  - `while (!heap.isEmpty()) result.add(0, heap.poll().word);` — drain, inserting at front → highest frequency first.
- **DSA shown:** **Trie + Priority Queue (min-heap)** top-k.

### 🛡️ Error handling — what to say in the viva
**"Pure algorithms, so error handling is input validation — I guard inputs and return safe defaults, never crash."**
- `insert(null/empty)` does nothing; searching a prefix that doesn't exist returns an empty list (the walk hits a `null` child and stops).
- `suggest` with a `null`/empty prefix returns an empty list; a non-positive limit falls back to a default; it never returns `null`.
- *(If asked "why no try/catch?" → the Trie is a pure in-memory structure with no I/O to fail.)*

---

# Khushi — Search Frequency
### 📂 Exact files to open (2):
- `src/main/java/com/new2canada/search/SearchTracker.java`
- `src/main/java/com/new2canada/database/SearchHistoryRepository.java`

### `search/SearchTracker.java` — remember what was searched (in memory)
- **Purpose:** track how many times each term was searched, plus the recent-search log.
- **How it works:** a **HashMap** of query → count (the frequency view) and a **LinkedList** of the last
  50 searches (chronological). LinkedList is chosen because it gives O(1) append at the tail and O(1)
  removal at the head when the 50-item cap is exceeded.
- **Key lines:**
  - `private final Map<String,Integer> freq = …; private final LinkedList<Entry> history = …;` — the two structures.
  - `freq.merge(key, 1, Integer::sum);` — count this query.
  - `history.addLast(new Entry(…)); while (history.size() > MAX_HISTORY) history.removeFirst();` — **why a LinkedList**: O(1) add at tail + O(1) drop at head when over 50.
- **DSA shown:** **HashMap + LinkedList**.

### `database/SearchHistoryRepository.java` — save history to Firestore
- **Purpose:** make each signed-in user's search history survive a server restart.
- **How it works:** `record()` writes one Firestore document per search to `searches/{uid}/queries`;
  `frequencies()` reads them back and aggregates into term → count; `recent()` returns the newest N.
- **Key lines:**
  - `if (uid == null || !FirestoreClient.isInitialised()) return;` — the DEMO-mode / no-user guard.
  - `.collection(ROOT).document(uid).collection(SUB).document().set(doc);` — write one query to Firestore.
  - `counts.merge(q.toString(), 1, Integer::sum);` — aggregate the user's queries into per-term counts.
- **DSA shown:** HashMap aggregation, cloud persistence.

### 🛡️ Error handling — what to say in the viva
**"Firestore errors are caught and logged so search keeps working, and it's safe even with no database."**
- Every Firestore call is in `try/catch` for `InterruptedException` and `ExecutionException` — a database outage **logs and continues** instead of crashing the request.
- A guard makes it a safe no-op in **DEMO mode** (when Firestore isn't initialised).
- `SearchTracker` ignores null/blank queries and its list is capped, so memory can't grow forever.
- So: real `try/catch` for the database I/O, guard clauses for the in-memory tracker.

---

# Karan — Page Ranking
### 📂 Exact files to open (2):
- `src/main/java/com/new2canada/ranking/PageRanker.java`
- `src/main/java/com/new2canada/ranking/ResultSorter.java`

### `ranking/PageRanker.java` — score documents by relevance/importance
- **Purpose:** decide the order results appear in — most relevant/important first.
- **How it works:** two passes. **(1) TF·IDF** — a document scores higher if it contains the query word
  often (TF) and the word is rare across all documents (IDF). **(2) PageRank-style smoothing** — each
  document absorbs a small damped share of its neighbours' scores. `rank(query)` ranks search results;
  `rankAll()` runs the **same** ranker over all terms (no query) for the "Top picks" view.
- **Key lines:**
  - `double idf = Math.log(1.0 + (double) totalDocs / postings.size());` — **IDF**: rarer word = bigger weight.
  - `double tf = 1 + Math.log(p.termFrequency); scores.merge(p.docId, tf * idf, Double::sum);` — **TF·IDF** added to the doc's score.
  - `return smooth(tfidf(index.terms()), index.terms());` — `rankAll()`: importance ranking for Top Picks.
  - `heap.offer(e); if (heap.size() > k) heap.poll();` — top-k with a min-heap.
- **DSA shown:** **HashMap**, **TF·IDF**, iterative scoring, **PriorityQueue** for top-k.

### `ranking/ResultSorter.java` — order results by score (merge sort)
- **Purpose:** sort the scored results from highest to lowest.
- **How it works:** a hand-written **merge sort** — recursively split the list, sort each half, merge
  back taking the higher score first. Stable, so equal scores keep their original order.
- **Key lines:**
  - `int mid = lo + (hi-lo)/2; mergeSort(a,lo,mid); mergeSort(a,mid+1,hi); merge(…);` — **divide and conquer**.
  - `if (left.get(i).getScore() >= right.get(j).getScore()) a.set(k++, left.get(i++));` — **the merge**: pick the higher score first (descending, stable).
- **DSA shown:** **Merge Sort**, recursion, divide-and-conquer.

### 🛡️ Error handling — what to say in the viva
**"The ranking math guards against empty input and divide-by-zero, and the sorter handles trivial lists safely."**
- An empty query returns an empty score map; words not in the index are skipped.
- `Math.max(1, …)` **prevents divide-by-zero** on an empty index.
- `ResultSorter` returns a `null`/single-element list as-is, and copies the input so it never mutates the caller's list.
- *(These are pure algorithms — no I/O — so the correct error handling is guarding the inputs, not try/catch.)*

---

# Other features in the project (still built — still need presenting for the 10-feature rubric)

These 5 aren't in the name→feature mapping above, but they exist and **someone must demo each** (the
rubric wants all 10 features). Assign an owner in the blank column, then that person opens the exact files.

| Feature | Exact files to open | Owner (fill in) |
|---|---|---|
| **Web Crawler** | `crawler/WebCrawler.java`, `crawler/HousingCrawler.java`, `crawler/PoliteFetcher.java`, `crawler/CrawlScheduler.java` | ______ |
| **HTML Parser** | `parser/HTMLParser.java`, `parser/DataExtractor.java`, `parser/rentals/KijijiExtractor.java` (as the example) | ______ |
| **Inverted Index** | `indexing/InvertedIndex.java` | ______ |
| **Regex Validation** | `regex/RegexValidator.java` | ______ |
| **Pattern Finding** | `regex/PatternFinder.java` | ______ |

What each shows (one line):
- **Web Crawler** — BFS over a `Queue` + `HashSet` of visited URLs; `PoliteFetcher` drives headless
  Chrome with a per-host throttle and restarts on crash. **Real try/catch** for network/browser failures.
- **HTML Parser** — `HTMLParser` wraps Jsoup; `DataExtractor` Strategy-dispatches to the right site
  extractor in `parser/rentals/`; each extractor returns an empty list on an unrecognized page.
- **Inverted Index** — `HashMap<term, List<Posting>>`; the AND-query is `result.retainAll(hits)` (set intersection).
- **Regex Validation** — compiled `Pattern`s for postal/email/phone/SIN; null-safe (`return false` on null).
- **Pattern Finding** — extracts prices/dates/phones/emails via a `while (m.find())` loop; catches
  `NumberFormatException` on the price parse.

**Shared glue (everyone):** `search/SearchEngine.java` (the facade that connects all features — great
"how it all connects" talking point) and `server/ApiHandler.java` (the `/api/*` endpoints).

---

## Quick reference — name → feature → folder → line to point at

| Owner | Feature | Folder to open | Say this | Point at this line |
|---|---|---|---|---|
| **Utsav** | Spell Checking | `spellcheck/` | "Wagner-Fischer edit distance + dictionary HashSet" | `1 + Math.min(subst, min(del, ins))` |
| **Mishan** | Frequency Count | `indexing/WordFrequencyCounter.java` | "HashMap word → count, the TF in TF·IDF" | `counts.merge(token, 1, Integer::sum)` |
| **Kashyap** | Word Completion | `autocomplete/` | "Trie for prefix match + min-heap for top-k" | `if (heap.size() > k) heap.poll()` |
| **Khushi** | Search Frequency | `search/SearchTracker.java` + `database/SearchHistoryRepository.java` | "HashMap + LinkedList, persisted to Firestore" | `while (history.size() > MAX_HISTORY) history.removeFirst()` |
| **Karan** | Page Ranking | `ranking/` | "TF·IDF scoring, then a hand-written merge sort" | `tf * idf` merged into the score |
