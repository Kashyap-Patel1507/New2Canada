package com.new2canada.search;

import com.new2canada.autocomplete.AutocompleteSystem;
import com.new2canada.config.RunMode;
import com.new2canada.database.ListingRepository;
import com.new2canada.database.SearchHistoryRepository;
import com.new2canada.indexing.InvertedIndex;
import com.new2canada.models.Apartment;
import com.new2canada.models.SearchResult;
import com.new2canada.ranking.PageRanker;
import com.new2canada.ranking.ResultSorter;
import com.new2canada.spellcheck.SpellChecker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Facade over every search-related component.
 *
 * <p>The HTTP handlers only know about {@code SearchEngine}; they never
 * reach into the inverted index or the ranker directly. This keeps the API
 * surface small and makes it easy to add new query types later (e.g. price
 * filters) without touching the server code.
 *
 * <p>Holds:
 * <ul>
 *   <li>One {@link InvertedIndex} per category — searches stay scoped,</li>
 *   <li>One global {@link AutocompleteSystem} over all categories,</li>
 *   <li>One {@link SpellChecker} preloaded with the dictionary and the
 *       vocabulary harvested at crawl time,</li>
 *   <li>An anonymous {@link SearchTracker} for DEMO mode and a
 *       per-user {@link SearchHistoryRepository} for FULL mode.</li>
 * </ul>
 */
public class SearchEngine {

    private final RunMode mode;
    private final ListingRepository listingRepo;
    private final SearchHistoryRepository historyRepo;
    private final SpellChecker spellChecker;
    private final AutocompleteSystem autocomplete = new AutocompleteSystem();

    // One index + one ranker per category.
    private final Map<String, InvertedIndex> indexes = new LinkedHashMap<>();
    private final Map<String, PageRanker> rankers = new LinkedHashMap<>();

    // In-memory document store, keyed by docId.
    private final Map<String, SearchResult> docs = new LinkedHashMap<>();

    // Anonymous tracker (one per process) for DEMO mode + /api/history fallback.
    private final SearchTracker localTracker = new SearchTracker();

    public SearchEngine(RunMode mode, ListingRepository listingRepo,
                        SearchHistoryRepository historyRepo) {
        this.mode = mode;
        this.listingRepo = listingRepo;
        this.historyRepo = historyRepo;
        this.spellChecker = new SpellChecker();
        for (String t : new String[]{"apartments"}) {
            indexes.put(t, new InvertedIndex());
            rankers.put(t, new PageRanker(indexes.get(t)));
        }
    }

    /* ----------------- ingestion ---------------------------------- */
    // Each ingestX persists to Firestore (when in FULL mode) and then
    // indexes in memory. The indexX counterpart skips persistence — used
    // by hydrateFromCache() to avoid round-tripping cached data straight
    // back to the network.

    public synchronized void ingestApartment(Apartment a) {
        if (a == null) return;
        if (listingRepo != null) listingRepo.upsertApartment(a);
        indexApartment(a);
    }

    private void indexApartment(Apartment a) {
        String prov = a.getProvince() == null || a.getProvince().isEmpty()
                ? "" : " (" + a.getProvince() + ")";
        registerDoc("apartments", a.getId(), a.getTitle(),
                a.getBedrooms() + "br · " + a.getCity() + prov + " · $" + a.getMonthlyRent(),
                a.getUrl(), a.getSource(), a.toIndexText())
                .addField("price", a.getMonthlyRent())
                .addField("bedrooms", a.getBedrooms())
                .addField("address", a.getAddress())
                .addField("city", a.getCity())
                .addField("province", a.getProvince());
    }

    /**
     * Pulls every cached listing out of Firestore and re-indexes it in
     * memory <em>without</em> writing back. Called at boot so the UI
     * shows real data within milliseconds — the live crawler then runs
     * in the background and overwrites with fresh data.
     *
     * <p>Returns the number of rows successfully indexed.
     */
    public synchronized int hydrateFromCache() {
        if (listingRepo == null) return 0;
        int n = 0;
        for (Map<String, Object> row : listingRepo.readAll()) {
            String type = String.valueOf(row.getOrDefault("type", ""));
            try {
                switch (type) {
                    case "apartment" -> { indexApartment(rowToApartment(row)); n++; }
                    default          -> { /* unknown type — skip silently */ }
                }
            } catch (RuntimeException e) {
                System.err.println("hydrate: skipping malformed row "
                        + row.get("id") + " — " + e.getMessage());
            }
        }
        rebuildIndex();
        return n;
    }

    /* ----------------- row → model helpers (Firestore types) ----------- */

    private static Apartment rowToApartment(Map<String, Object> r) {
        return new Apartment(
                str(r, "id"), str(r, "title"), str(r, "address"),
                str(r, "city"), str(r, "province"),
                intOf(r, "bedrooms"), dbl(r, "monthlyRent"),
                str(r, "source"), str(r, "url"), str(r, "description"));
    }

    // Firestore stores numbers as Long / Double, never Integer / int —
    // these helpers paper over that so direct casts can't ClassCastException.
    private static String str(Map<String, Object> r, String k) {
        Object v = r.get(k);
        return v == null ? "" : String.valueOf(v);
    }
    private static int intOf(Map<String, Object> r, String k) {
        Object v = r.get(k);
        if (v instanceof Number n) return n.intValue();
        try { return v == null ? 0 : Integer.parseInt(String.valueOf(v)); }
        catch (NumberFormatException e) { return 0; }
    }
    private static double dbl(Map<String, Object> r, String k) {
        Object v = r.get(k);
        if (v instanceof Number n) return n.doubleValue();
        try { return v == null ? 0 : Double.parseDouble(String.valueOf(v)); }
        catch (NumberFormatException e) { return 0; }
    }

    private SearchResult registerDoc(String type, String id, String title, String subtitle,
                                     String url, String source, String indexText) {
        SearchResult sr = new SearchResult(id, type, title, subtitle, url, source);
        docs.put(id, sr);
        indexes.get(type).addDocument(id, indexText);
        autocomplete.indexDocument(indexText);
        spellChecker.learn(title);
        for (String tok : indexText.toLowerCase().split("[^a-z0-9]+")) spellChecker.learn(tok);
        return sr;
    }

    /** Called by the scheduler after a crawl run completes. */
    public synchronized void rebuildIndex() {
        // The per-doc additions already keep the index in sync; this is a
        // hook for future cache invalidation, e.g. dropping stale listings.
    }

    /* ----------------- query ------------------------------------- */

    public synchronized List<SearchResult> search(String type, String query, String uid) {
        return search(type, query, null, null, uid);
    }

    /**
     * Search with optional facet filters. {@code cityFilter} or
     * {@code provinceFilter} may be {@code null}/empty to skip that filter.
     * Filters are case-insensitive equality matches on the corresponding
     * field stored on each {@link SearchResult}.
     */
    public synchronized List<SearchResult> search(String type, String query,
                                                  String cityFilter,
                                                  String provinceFilter,
                                                  String uid) {
        InvertedIndex idx = indexes.get(type);
        PageRanker rank   = rankers.get(type);
        if (idx == null) return new ArrayList<>();

        Map<String, Double> scores = rank.rank(query);
        List<SearchResult> hits = new ArrayList<>();
        for (Map.Entry<String, Double> e : scores.entrySet()) {
            SearchResult doc = docs.get(e.getKey());
            if (doc == null) continue;
            if (!fieldMatches(doc, "city", cityFilter))         continue;
            if (!fieldMatches(doc, "province", provinceFilter)) continue;
            doc.setScore(e.getValue());
            hits.add(doc);
        }
        List<SearchResult> ranked = ResultSorter.sortDesc(hits);

        localTracker.record(query, type, ranked.size());
        if (mode == RunMode.FULL && historyRepo != null && uid != null) {
            historyRepo.record(uid, query, type, ranked.size());
        }
        return ranked;
    }

    /** Top picks by type with optional facet filters. */
    public synchronized List<SearchResult> topByType(String type, int limit,
                                                     String cityFilter, String provinceFilter) {
        InvertedIndex idx = indexes.get(type);
        if (idx == null) return new ArrayList<>();
        List<SearchResult> out = new ArrayList<>();
        for (SearchResult sr : docs.values()) {
            if (!type.equals(sr.getType())) continue;
            if (!fieldMatches(sr, "city", cityFilter))         continue;
            if (!fieldMatches(sr, "province", provinceFilter)) continue;
            sr.setScore(1.0 + Math.random() * 0.001);
            out.add(sr);
            if (out.size() >= limit) break;
        }
        return ResultSorter.sortDesc(out);
    }

    private static boolean fieldMatches(SearchResult sr, String field, String wanted) {
        if (wanted == null || wanted.isBlank() || "any".equalsIgnoreCase(wanted)) return true;
        Object v = sr.getFields().get(field);
        return v != null && wanted.equalsIgnoreCase(String.valueOf(v));
    }

    /**
     * Returns the distinct cities and provinces present in the index for a
     * given type. Used by the front-end to populate filter dropdowns.
     */
    public synchronized Map<String, Object> facets(String type) {
        java.util.TreeSet<String> cities    = new java.util.TreeSet<>();
        java.util.TreeSet<String> provinces = new java.util.TreeSet<>();
        Map<String, java.util.TreeSet<String>> citiesByProvince = new java.util.TreeMap<>();
        for (SearchResult sr : docs.values()) {
            if (!type.equals(sr.getType())) continue;
            Object c = sr.getFields().get("city");
            Object p = sr.getFields().get("province");
            String city = c != null ? c.toString().trim() : "";
            String prov = p != null ? p.toString().trim() : "";
            if (!city.isEmpty()) cities.add(city);
            if (!prov.isEmpty()) provinces.add(prov);
            if (!prov.isEmpty() && !city.isEmpty()) {
                citiesByProvince.computeIfAbsent(prov, k -> new java.util.TreeSet<>()).add(city);
            }
        }
        Map<String, List<String>> citiesByProvinceOut = new LinkedHashMap<>();
        for (Map.Entry<String, java.util.TreeSet<String>> e : citiesByProvince.entrySet()) {
            citiesByProvinceOut.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("cities", new ArrayList<>(cities));
        out.put("provinces", new ArrayList<>(provinces));
        out.put("citiesByProvince", citiesByProvinceOut);
        return out;
    }

    /**
     * Returns up to {@code limit} documents of the given type with no query —
     * used by the /api/pagerank "Top picks" endpoint. Documents are scored by
     * the inverted-index size of each (a rough popularity proxy: pages with
     * more unique terms tend to be richer and more useful to surface).
     */
    public synchronized List<SearchResult> topByType(String type, int limit) {
        return topByType(type, limit, null, null);
    }

    public List<String> autocomplete(String prefix, int limit) {
        return autocomplete.suggest(prefix, limit);
    }

    public List<String> spellSuggest(String word, int limit) {
        return spellChecker.suggest(word, limit);
    }

    public boolean isInDictionary(String word) {
        return spellChecker.isValid(word);
    }

    /* ----------------- introspection (for /api/debug) ------------- */

    public Map<String, Object> stats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("mode", mode.name());
        s.put("totalDocs", docs.size());
        s.put("trieSize", autocomplete.trieSize());
        s.put("dictionarySize", spellChecker.dictionarySize());
        Map<String, Integer> perType = new LinkedHashMap<>();
        for (Map.Entry<String, InvertedIndex> e : indexes.entrySet()) {
            perType.put(e.getKey(), e.getValue().totalDocuments());
        }
        s.put("documentsByType", perType);
        s.put("uniqueQueries", localTracker.uniqueQueries());
        s.put("totalQueries", localTracker.totalQueries());
        return s;
    }

    public SearchTracker localTracker() { return localTracker; }
}
