package com.new2canada.server;

import com.new2canada.auth.AuthMiddleware;
import com.new2canada.auth.User;
import com.new2canada.crawler.CrawlScheduler;
import com.new2canada.database.SearchHistoryRepository;
import com.new2canada.database.UserRepository;
import com.new2canada.models.SearchResult;
import com.new2canada.regex.RegexValidator;
import com.new2canada.search.SearchEngine;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Routes every {@code /api/*} request to the right backend component.
 *
 * <p>This is intentionally a single-class router rather than a framework —
 * the project shouldn't need Spring just to expose six endpoints. The
 * routing table sits at the top of {@link #handle(HttpExchange)} so the
 * whole API surface is readable in one screen.
 */
public class ApiHandler implements HttpHandler {

    private final SearchEngine engine;
    private final AuthMiddleware auth;
    private final UserRepository userRepo;
    private final SearchHistoryRepository historyRepo;
    private final CrawlScheduler scheduler;

    public ApiHandler(SearchEngine engine, AuthMiddleware auth,
                      UserRepository userRepo, SearchHistoryRepository historyRepo,
                      CrawlScheduler scheduler) {
        this.engine = engine;
        this.auth = auth;
        this.userRepo = userRepo;
        this.historyRepo = historyRepo;
        this.scheduler = scheduler;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path   = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        try {
            switch (path) {
                case "/api/search"        -> handleSearch(ex);
                case "/api/autocomplete"  -> handleAutocomplete(ex);
                case "/api/spellcheck"    -> handleSpellcheck(ex);
                case "/api/validate"      -> handleValidate(ex);
                case "/api/pagerank"      -> handlePagerank(ex);
                case "/api/facets"        -> handleFacets(ex);
                case "/api/history"       -> handleHistory(ex);
                case "/api/me"            -> handleMe(ex);
                case "/api/refresh-crawl" -> handleRefresh(ex);
                case "/api/debug"         -> handleDebug(ex);
                default                   -> json(ex, 404, Map.of("error", "no such endpoint: " + path));
            }
        } catch (Exception e) {
            json(ex, 500, Map.of("error", method + " " + path + " failed", "message",
                    String.valueOf(e.getMessage())));
        }
    }

    /* ----------------- /api/search -------------------------------------- */
    private void handleSearch(HttpExchange ex) throws IOException {
        Map<String, String> q = parseQuery(ex.getRequestURI());
        String type     = q.getOrDefault("type", "apartments");
        String query    = q.getOrDefault("q", "");
        String city     = q.getOrDefault("city", "");
        String province = q.getOrDefault("province", "");

        User user = auth.resolve(ex); // may be null in FULL mode if unauth'd — search is still public
        String uid = user != null ? user.uid() : null;

        List<SearchResult> hits = engine.search(type, query, city, province, uid);
        List<Map<String, Object>> out = new ArrayList<>();
        for (SearchResult r : hits.subList(0, Math.min(50, hits.size()))) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", r.getId());
            row.put("type", r.getType());
            row.put("title", r.getTitle());
            row.put("subtitle", r.getSubtitle());
            row.put("url", r.getUrl());
            row.put("source", r.getSource());
            row.put("score", r.getScore());
            row.put("fields", r.getFields());
            out.add(row);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        body.put("type", type);
        body.put("count", out.size());
        body.put("results", out);

        // Add a spell suggestion if there were no hits.
        if (out.isEmpty() && !query.isBlank()) {
            List<String> suggestions = engine.spellSuggest(query, 1);
            if (!suggestions.isEmpty()) body.put("didYouMean", suggestions.get(0));
        }
        json(ex, 200, body);
    }

    /* ----------------- /api/autocomplete -------------------------------- */
    private void handleAutocomplete(HttpExchange ex) throws IOException {
        Map<String, String> q = parseQuery(ex.getRequestURI());
        String prefix = q.getOrDefault("q", "");
        int limit     = parseInt(q.get("limit"), 10);
        json(ex, 200, Map.of(
                "prefix", prefix,
                "suggestions", engine.autocomplete(prefix, limit)));
    }

    /* ----------------- /api/spellcheck ---------------------------------- */
    private void handleSpellcheck(HttpExchange ex) throws IOException {
        Map<String, String> q = parseQuery(ex.getRequestURI());
        String word = q.getOrDefault("word", "");
        List<String> suggestions = engine.spellSuggest(word, 5);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("word", word);
        body.put("valid", engine.isInDictionary(word));
        body.put("suggestions", suggestions);
        body.put("corrected", suggestions.isEmpty() ? word : suggestions.get(0));
        json(ex, 200, body);
    }

    /* ----------------- /api/validate ------------------------------------ */
    private void handleValidate(HttpExchange ex) throws IOException {
        Map<String, String> form = ex.getRequestMethod().equalsIgnoreCase("POST")
                ? parseFormBody(ex)
                : parseQuery(ex.getRequestURI());
        String type  = form.getOrDefault("type", "postal");
        String value = form.getOrDefault("value", "");
        boolean ok   = RegexValidator.validate(type, value);
        json(ex, 200, Map.of(
                "type", type,
                "value", value,
                "valid", ok,
                "message", ok ? "Looks good." : "Invalid " + type + " format."));
    }

    /* ----------------- /api/pagerank ------------------------------------ */
    private void handlePagerank(HttpExchange ex) throws IOException {
        Map<String, String> q = parseQuery(ex.getRequestURI());
        String type     = q.getOrDefault("type", "apartments");
        String city     = q.getOrDefault("city", "");
        String province = q.getOrDefault("province", "");
        List<SearchResult> hits = engine.topByType(type, 50, city, province);
        List<Map<String, Object>> out = new ArrayList<>();
        for (SearchResult r : hits) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", r.getId());
            row.put("type", r.getType());
            row.put("title", r.getTitle());
            row.put("subtitle", r.getSubtitle());
            row.put("url", r.getUrl());
            row.put("source", r.getSource());
            row.put("score", r.getScore());
            row.put("fields", r.getFields());
            out.add(row);
        }
        json(ex, 200, Map.of("type", type, "top", out));
    }

    /* ----------------- /api/facets -------------------------------------- */
    private void handleFacets(HttpExchange ex) throws IOException {
        Map<String, String> q = parseQuery(ex.getRequestURI());
        String type = q.getOrDefault("type", "apartments");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type);
        body.putAll(engine.facets(type));
        json(ex, 200, body);
    }

    /* ----------------- /api/history (protected) ------------------------- */
    private void handleHistory(HttpExchange ex) throws IOException {
        User u = auth.resolve(ex);
        if (u == null) { json(ex, 401, Map.of("error", "sign in to view history")); return; }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("uid", u.uid());
        body.put("demoMode", auth.isDemo());
        List<Map<String, Object>> recent;
        if (!auth.isDemo() && historyRepo != null) {
            // FULL mode: frequencies + recent both come from Firestore, so they
            // are per-user, persistent across restarts, and consistent.
            body.put("frequencies", historyRepo.frequencies(u.uid(), 500));
            recent = historyRepo.recent(u.uid(), 25);
        } else {
            // DEMO mode: no Firestore, so serve the in-memory tracker.
            body.put("frequencies", engine.localTracker().frequencies());
            recent = new ArrayList<>();
            for (var entry : engine.localTracker().recent()) {
                recent.add(Map.of(
                        "q", entry.query, "type", entry.type,
                        "timestamp", entry.timestamp, "resultsCount", entry.resultsCount));
            }
        }
        body.put("recent", recent);
        json(ex, 200, body);
    }

    /* ----------------- /api/me ----------------------------------------- */
    private void handleMe(HttpExchange ex) throws IOException {
        User u = auth.resolve(ex);
        if (u == null) { json(ex, 401, Map.of("error", "not signed in")); return; }
        if (!auth.isDemo() && userRepo != null) userRepo.upsertOnLogin(u);
        json(ex, 200, Map.of(
                "uid", u.uid(),
                "email", u.email() == null ? "" : u.email(),
                "displayName", u.displayName() == null ? "" : u.displayName(),
                "photoUrl", u.photoUrl() == null ? "" : u.photoUrl(),
                "demoMode", auth.isDemo()));
    }

    /* ----------------- /api/refresh-crawl (protected) ------------------ */
    private void handleRefresh(HttpExchange ex) throws IOException {
        User u = auth.resolve(ex);
        if (u == null) { json(ex, 401, Map.of("error", "sign in to trigger a crawl")); return; }
        if (scheduler != null) scheduler.triggerNow();
        json(ex, 202, Map.of("ok", true, "message", "Crawl triggered."));
    }

    /* ----------------- /api/debug -------------------------------------- */
    private void handleDebug(HttpExchange ex) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>(engine.stats());
        body.put("demoMode", auth.isDemo());
        json(ex, 200, body);
    }

    /* ----------------- helpers ----------------------------------------- */

    private static void json(HttpExchange ex, int status, Object body) throws IOException {
        byte[] payload = JsonWriter.write(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(status, payload.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(payload); }
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> out = new HashMap<>();
        String q = uri.getRawQuery();
        if (q == null || q.isEmpty()) return out;
        for (String pair : q.split("&")) {
            int idx = pair.indexOf('=');
            String k = idx < 0 ? pair : pair.substring(0, idx);
            String v = idx < 0 ? "" : pair.substring(idx + 1);
            out.put(URLDecoder.decode(k, StandardCharsets.UTF_8),
                    URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return out;
    }

    private static Map<String, String> parseFormBody(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> out = new HashMap<>();
        if (body.isEmpty()) return out;
        for (String pair : body.split("&")) {
            int idx = pair.indexOf('=');
            String k = idx < 0 ? pair : pair.substring(0, idx);
            String v = idx < 0 ? "" : pair.substring(idx + 1);
            out.put(URLDecoder.decode(k, StandardCharsets.UTF_8),
                    URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return out;
    }

    private static int parseInt(String s, int fallback) {
        if (s == null) return fallback;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return fallback; }
    }
}
