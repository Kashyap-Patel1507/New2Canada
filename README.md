# New2Canada — International Student Assistance Search Engine

> **COMP 8547 — Advanced Computing Concepts** · Final Project

A Java backend + localhost website that helps new international students arriving in Canada compare apartments, part-time jobs, student bank accounts, and mobile / SIM plans across major Canadian providers (Rentals.ca, Indeed.ca, RBC / Scotia / CIBC, Freedom / Public Mobile).

The project demonstrates every data structure and algorithm required by the course — Trie, HashMap, Priority Queue, Queue, LinkedList, Merge Sort, Edit Distance (Wagner-Fischer), Inverted Index, Regular Expressions, and a PageRank-style ranking pass — wrapped in a cloud-backed web app with Google Sign-In and Firestore persistence.

---

## What you get out of the box

| Feature | Tech |
|---|---|
| Live web crawler | Jsoup + a polite per-host throttler |
| HTML parser | Jsoup CSS-selector extraction |
| Spell checker | Levenshtein DP against a dictionary HashSet |
| Autocomplete | Trie + bounded min-heap (top-k by frequency) |
| Inverted index search | `HashMap<term, postings>` with AND-merge |
| Page ranking | TF·IDF + 2-pass PageRank-style smoothing |
| Result sorting | Hand-rolled merge sort |
| Regex validation | Postal code · email · phone · SIN |
| Pattern extraction | Prices · dates · phone numbers from free text |
| Auth | Google Sign-In via Firebase Authentication |
| Persistence | Google Cloud Firestore (cached listings + per-user history) |
| Web UI | Multi-page localhost site (vanilla HTML/CSS/JS) |

---

## Quick start

### 1. Prerequisites

- **JDK 17+** (check with `java -version`)
- **Apache Maven 3.8+** (check with `mvn -version`)
- **VS Code** with the Java Extension Pack (recommended)
- **Chrome / any modern browser** for the web UI

### 2. Get the code

Clone or unzip the project into a folder, then open it in VS Code. Accept the prompt to install the Java Extension Pack if you haven't already.

### 3. Build and run

```bash
mvn clean package
java -jar target/new2canada.jar
```

You should see something like:

```
=== New2Canada Search Engine ===
DEMO mode — no serviceAccountKey.json found.
[crawler] starting refresh…
Server started on http://localhost:8080
```

Open **http://localhost:8080** in Chrome.

> **Note** — on first launch the page will say "no matches yet" for a few seconds while the live crawler hits Rentals.ca / Indeed / the bank sites and builds the index. Refresh after \~15 seconds.

---

## Folder map

```
New2Canada/
├── pom.xml                            Maven build
├── README.md                          ← you are here
├── REPORT.md                          full project report
├── PRESENTATION.md                    slide-by-slide outline
├── VIVA.md                            likely viva questions + answers
├── .vscode/                           VS Code launch config
├── serviceAccountKey.json             [you add this; .gitignored]
└── src/main/
    ├── java/com/new2canada/
    │   ├── Main.java                  entry point
    │   ├── config/                    AppConfig · RunMode
    │   ├── crawler/                   WebCrawler + 4 subclasses + PoliteFetcher + Scheduler
    │   ├── parser/                    HTMLParser · DataExtractor (Jsoup)
    │   ├── indexing/                  InvertedIndex · WordFrequencyCounter
    │   ├── ranking/                   PageRanker · ResultSorter (merge sort)
    │   ├── spellcheck/                SpellChecker · EditDistance (Wagner-Fischer)
    │   ├── autocomplete/              Trie · AutocompleteSystem (Trie + PQ)
    │   ├── regex/                     RegexValidator · PatternFinder
    │   ├── search/                    SearchEngine · SearchTracker
    │   ├── auth/                      User · FirebaseAuthVerifier · AuthMiddleware
    │   ├── database/                  FirestoreClient + 3 repositories
    │   ├── models/                    Apartment · Job · BankPlan · MobilePlan · SearchResult
    │   ├── server/                    WebServer · ApiHandler · StaticFileHandler · JsonWriter
    │   └── utils/                     ResourceLoader · TextNormalizer
    └── resources/
        ├── dictionary.txt
        └── static/                    10 HTML pages + css + js
```

---

## REST API

| Method | Path | Auth | Returns |
|---|---|---|---|
| GET  | `/api/search?type=apartments|jobs|banks|mobile&q=…` | open | Ranked JSON results |
| GET  | `/api/autocomplete?q=…&limit=10` | open | Top-k completions |
| GET  | `/api/spellcheck?word=…` | open | `{ corrected, suggestions[] }` |
| POST | `/api/validate` (body: `type`, `value`) | open | `{ valid, message }` |
| GET  | `/api/pagerank?type=…` | open | Top-ranked items overall |
| GET  | `/api/history` | **required** | Caller's frequency map + recent searches |
| GET  | `/api/me` | **required** | Current user profile |
| POST | `/api/refresh-crawl` | **required** | Trigger an on-demand re-crawl |
| GET  | `/api/debug` | open | Index sizes, Trie size, dictionary size |

Auth is via `Authorization: Bearer <Firebase-ID-token>`. In DEMO mode the middleware short-circuits every caller to an anonymous user.

---

## Troubleshooting

- **`java -jar` fails with "no main manifest attribute"** — make sure you ran `mvn clean package` first; the shade plugin builds the runnable JAR.
- **Browser shows "no matches yet"** — the live crawler hasn't finished its first pass. Wait 10–20s then refresh.
- **Crawler logs `403` or `429`** — the target site is rate-limiting us. The polite fetcher already throttles per host; you may need to wait or run during off-peak hours. The Firestore cache (FULL mode) lets the UI keep serving stale data in the meantime.
- **`FirebaseApp with name [DEFAULT] doesn't exist`** — your `serviceAccountKey.json` is missing, malformed, or pointing at a wrong project. Re-download from Firebase Console.
- **"Sign in" button does nothing** — you haven't pasted your `firebaseConfig` snippet into `src/main/resources/static/js/auth.js` yet. The page log will say so.

---

## Screenshots (placeholders)

> Add screenshots under `docs/screens/` and reference them here:
> - `docs/screens/01-landing.png`
> - `docs/screens/02-apartments-search.png`
> - `docs/screens/03-did-you-mean.png`
> - `docs/screens/04-autocomplete.png`
> - `docs/screens/05-validate.png`
> - `docs/screens/06-history-firestore.png`

---
