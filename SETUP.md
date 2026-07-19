# SETUP — New2Canada (team members)

Quick setup guide for our team. Firebase is **already configured** in this
repo — you don't need to create your own project. Just install the tools,
drop in the key file, build, and run.

Should take ~5 minutes.

---

## 1. Install the prerequisites

You need three things on your machine. Verify each with the command on the right.

| Tool         | Min version | Check it works   |
|--------------|-------------|------------------|
| JDK          | 17+         | `java -version`  |
| Apache Maven | 3.8+        | `mvn -version`   |
| Google Chrome | recent stable | open it — **required**, see note below |

> **Chrome is required, not optional.** The crawler drives a **headless Chrome** (via Selenium) so JavaScript-rendered rental sites populate their listings before we scrape. WebDriverManager auto-downloads the matching ChromeDriver on first run, but you must have Chrome itself installed. It's also the browser you'll use to view the UI.

**If you don't have JDK 17 yet:**

```bash
# macOS (Homebrew)
brew install openjdk@17

# Windows: download Temurin 17 from https://adoptium.net
# Linux:   sudo apt install openjdk-17-jdk
```

**If you don't have Maven yet:**

```bash
# macOS
brew install maven

# Windows: https://maven.apache.org/download.cgi  (extract & add bin/ to PATH)
# Linux:   sudo apt install maven
```

Everything else (Jsoup, Selenium, WebDriverManager, Firebase Admin SDK,
SLF4J) is declared in `pom.xml` and Maven downloads it automatically on the
first build — **you do not install Java libraries by hand**.

---

## 2. Get the code

```bash
git clone <repo-url>
cd New2Canada
```

…or download the ZIP from GitHub and extract it.

Open the folder in **VS Code** (`File → Open Folder…`). Accept the prompt
to install the "Extension Pack for Java" if it appears.

---

## 3. Drop in the Firebase service-account key

`serviceAccountKey.json` is intentionally `.gitignored` (it contains a
private key), so it's **not** in the GitHub repo. Get it from the team
lead via Slack / Drive / 1Password and place it here:

```
New2Canada/
├── pom.xml
├── README.md
├── SETUP.md                    ← this file
├── serviceAccountKey.json      ← drop the key HERE (next to pom.xml)
└── src/ …
```

The file must be named **exactly** `serviceAccountKey.json` (lowercase
`s`, mixed-case rest). If you rename it, the app will boot in DEMO mode.

> The Firebase web config (`firebaseConfig` in `src/main/resources/static/js/auth.js`)
> is already filled in — you don't need to touch it.

---

## 4. Build and run

From the project root:

```bash
mvn clean package
java -jar target/new2canada.jar
```

…or just use the launcher (macOS / Linux):

```bash
./run.sh
```

On a clean machine the first build downloads dependencies (~1–2 min). After that,
restarts are instant.

You should see:

```
=== New2Canada Search Engine ===
FULL mode — Firebase + Firestore initialised.
[crawler] starting refresh…
Server started on http://localhost:8080
```

Open **<http://localhost:8080>**. Top-right shows a "Sign in with Google"
button. Sign in with any Google account, run a few searches, then hit
`/api/history` (or re-open the app) to confirm Firestore is recording them.

If it says **DEMO mode** instead, your `serviceAccountKey.json` is missing
or named wrong — re-check step 3.

---

## 5. Folder map

```
New2Canada/
├── pom.xml                         Maven build + dependency list
├── README.md                       project overview
├── SETUP.md                        ← this file
├── run.sh                          one-line launcher
├── serviceAccountKey.json          [YOU ADD — get from team lead]
├── target/                         build output (ignored by git)
└── src/main/
    ├── java/com/new2canada/        Java backend
    │   ├── Main.java
    │   ├── config/                 AppConfig (seed URLs), RunMode
    │   ├── crawler/                WebCrawler, HousingCrawler, PoliteFetcher (Selenium), Scheduler
    │   ├── parser/                 HTMLParser, DataExtractor, rentals/ (per-site extractors)
    │   ├── indexing/               InvertedIndex, WordFrequencyCounter
    │   ├── ranking/                PageRanker, ResultSorter (merge sort)
    │   ├── spellcheck/             SpellChecker, EditDistance (Wagner-Fischer)
    │   ├── autocomplete/           Trie + min-heap top-k
    │   ├── regex/                  RegexValidator, PatternFinder
    │   ├── search/                 SearchEngine, SearchTracker
    │   ├── auth/                   Firebase ID-token verifier
    │   ├── database/               FirestoreClient + repositories
    │   ├── models/                 Apartment, SearchResult
    │   ├── server/                 WebServer, ApiHandler, StaticFileHandler
    │   └── utils/                  ResourceLoader, Location, TextNormalizer
    └── resources/
        ├── dictionary.txt          spell-check seed words
        └── static/                 the website (HTML + CSS + JS)
            ├── index.html          single-page app (all features)
            ├── login.html          Google Sign-In page
            ├── css/                styles.css · stitch.css
            └── js/                 app.js · auth.js · pages/single.js
```

---

## 6. Library list (auto-installed by Maven)

You don't install these by hand. They live in `pom.xml`:

| Library                              | Version | Purpose                                                          |
|--------------------------------------|---------|------------------------------------------------------------------|
| `org.jsoup:jsoup`                    | 1.17.2  | HTML parsing (CSS-selector extraction of rendered pages)         |
| `org.seleniumhq.selenium:selenium-java` | 4.18.1 | Drives headless Chrome so JS-rendered listings populate         |
| `io.github.bonigarcia:webdrivermanager` | 5.7.0 | Auto-downloads the ChromeDriver matching the installed Chrome    |
| `com.google.firebase:firebase-admin` | 9.2.0   | Verifies Google ID tokens · talks to Firestore from Java         |
| `org.slf4j:slf4j-simple`             | 2.0.13  | Logging backend required transitively by firebase-admin          |

The fat JAR `target/new2canada.jar` bundles all of the above (≈90 MB).

---

## 7. Troubleshooting

| Symptom                                                       | Fix                                                                                                                       |
|---------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| `java -jar` says "no main manifest attribute"                 | Run `mvn clean package` first — the shade plugin builds the runnable JAR.                                                 |
| Server boots in **DEMO mode** even though you added the JSON  | File must be named **exactly** `serviceAccountKey.json` and sit in the same folder as `pom.xml`.                          |
| `FirebaseApp with name [DEFAULT] doesn't exist`               | Your `serviceAccountKey.json` is malformed. Ask the team lead for a fresh copy.                                           |
| "Sign in" button does nothing                                 | Hard-refresh the page (Cmd-Shift-R / Ctrl-Shift-R) — your browser cached the old `auth.js`.                               |
| Browser shows empty pages for ~10 seconds after start         | The live crawler hasn't finished its first pass yet. Wait and refresh.                                                    |
| Crawler logs `403` / `429`                                    | That target site is rate-limiting. The polite fetcher already throttles 1.5 s per host — wait or run during off-peak.     |
| `mvn` fails to download dependencies                          | Network issue. Check internet & retry. On a corporate network you may need a Maven proxy in `~/.m2/settings.xml`.         |

---

## 8. "I'm done setting up" checklist

- [ ] `java -version` shows 17 or higher
- [ ] `mvn -version` shows 3.8 or higher
- [ ] `serviceAccountKey.json` sits next to `pom.xml`
- [ ] `mvn clean package` succeeds
- [ ] `java -jar target/new2canada.jar` prints **FULL mode — Firebase + Firestore initialised.**
- [ ] <http://localhost:8080> loads and "Sign in with Google" works

All boxes ticked → you're done. Ping the team channel if you got stuck.
