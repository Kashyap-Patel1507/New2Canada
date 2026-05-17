# New2Canada — Presentation Outline (7–10 minutes)

Target: 14 slides, ~30–40 seconds each. Assumes a 4-person team; speaking parts shown in brackets. Adjust to your team size.

---

## Slide 1 — Title
**[Member 1 · ~20s]**

> Hi, we're {names}. We built **New2Canada — an International Student Assistance Search Engine** for COMP 8547. It crawls real Canadian rental, job, bank, and mobile-plan sites, indexes them, and lets a student search and compare them from one localhost website with Google Sign-In and cloud persistence.

## Slide 2 — Problem
**[Member 1 · ~40s]**

- New international students must immediately compare apartments, part-time jobs, student bank accounts, and mobile plans.
- Information is scattered across 8+ sites; language barriers make spell-correction valuable.
- We solve it with one search box, ranked results, and offline-resilient caching.

## Slide 3 — Architecture diagram
**[Member 1 · ~50s]**

Show the diagram from `REPORT.md` §3. Talk through the boot order:
1. Detect Firebase → FULL or DEMO mode.
2. Crawl scheduler kicks off live BFS over seed URLs.
3. HTTP server starts on port 8080.

## Slide 4 — Tech stack
**[Member 2 · ~30s]**

- Java 17, Maven (one external runtime dep: Jsoup + firebase-admin).
- JDK built-in `com.sun.net.httpserver.HttpServer` — no Spring needed.
- Frontend: vanilla HTML/CSS/JS, no framework.
- Cloud: Firebase Auth (Google Sign-In) + Cloud Firestore.

## Slide 5 — Data structures
**[Member 2 · ~60s]**

Single slide listing each DS and where it lives:
- `HashMap` — inverted index, frequency counters, search tracker
- `Trie` — autocomplete (`autocomplete/Trie.java`)
- `PriorityQueue` (min-heap) — top-k extraction
- `Queue` (`ArrayDeque`) — BFS frontier in WebCrawler
- `LinkedList` — bounded chronological history

Highlight one: "We chose a Trie for autocomplete because we get O(L) prefix lookup regardless of dictionary size — HashMap would force us to scan every key."

## Slide 6 — Algorithms (1/2)
**[Member 2 · ~50s]**

- **BFS** in the crawler over a Queue frontier.
- **Wagner-Fischer DP** for Levenshtein edit distance — walk through the `dp[i][j]` recurrence with a 4×4 example.

## Slide 7 — Algorithms (2/2)
**[Member 2 · ~50s]**

- **Merge sort** of the result list — O(N log N), stable, deterministic.
- **TF·IDF + iterative refinement** for page-rank-style scoring.
- **Pre-compiled `Pattern`** objects for regex validation (postal code / email / phone / SIN).

## Slide 8 — Live crawler
**[Member 3 · ~40s]**

- BFS over seed URLs (Rentals.ca, Indeed, RBC/Scotia/CIBC, Freedom/Public).
- `PoliteFetcher` — User-Agent + 1.5 s per-host throttle + 8 s timeout.
- Runs once at boot, then every 6 h via `ScheduledExecutorService`.
- On success → upserts into Firestore. On failure → the engine keeps serving the previous cache.

## Slide 9 — Cloud integration
**[Member 3 · ~50s]**

- Firebase Admin SDK on the server; Firebase Auth JS SDK in the browser.
- Sign-in: Google OAuth popup → ID token (JWT) → backend `verifyIdToken` against Google's public keys.
- Firestore collections: `listings/`, `users/`, `searches/{uid}/queries/`.
- Demo-mode fallback: with no `serviceAccountKey.json` the app still runs, in-memory only, with a banner shown in the UI.

## Slide 10 — Web UI walkthrough
**[Member 3 · ~30s]**

Screenshots of: home, apartments search with autocomplete, "Did you mean?" banner, history page populated from Firestore.

## Slide 11 — Live demo (1/2): search + autocomplete + spell-check
**[Member 4 · ~90s]**

- Open `http://localhost:8080/apartments.html`.
- Type "tor" → autocomplete dropdown appears.
- Submit "torronto" → "Did you mean: toronto?" banner → click it → ranked results.
- Show `/api/debug` JSON in another tab: Trie size, dictionary size, top-10 by score.

## Slide 12 — Live demo (2/2): auth + history + resilience
**[Member 4 · ~60s]**

- Click **Sign in with Google** → sign in → `Hi, {name}` appears in the topbar.
- Run a couple of searches; visit `/history.html` → frequency table + recent searches are now from Firestore.
- **Resilience demo:** turn WiFi off, click "Refresh crawl" — engine still serves cached listings.

## Slide 13 — Course-requirement checklist
**[Member 4 · ~40s]**

Show the table from `REPORT.md` §5 mapping every required feature & DS to a class. Emphasise "every box ticked".

## Slide 14 — Q&A
**[All]**

- Thank you.
- Hand-prepared answers from `VIVA.md`.

---

### Speaker tips

- The whole pipeline boots in <5 seconds. Pre-warm the crawler at least 60 seconds before the demo so listings are ready.
- If WiFi is bad on demo day, demo in DEMO mode and call it out as a design feature.
- Keep "complexity" claims concrete: `O(L)` for prefix lookup, `O(m·n)` for edit distance, `O(N log N)` for merge sort.
