package com.new2canada.search;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Per-user search-history tracker.
 *
 * <p>Two structures, one purpose:
 * <ul>
 *   <li>{@link HashMap} {@code freq} — how many times the user issued each
 *       distinct query, for the popular-search ranking.</li>
 *   <li>{@link LinkedList} {@code history} — chronological log, bounded to
 *       the most recent {@code MAX_HISTORY} entries. LinkedList is used here
 *       because we need O(1) append at the tail <i>and</i> O(1) removal at
 *       the head when the bound is exceeded.</li>
 * </ul>
 *
 * <p>When Firestore is enabled, the {@code SearchEngine} also pushes a copy
 * of every query into {@code SearchHistoryRepository} so the user's history
 * survives a server restart.
 *
 * <p><b>Time complexity</b>: O(1) amortised per operation.
 *
 * Demonstrates: <b>HashMap</b>, <b>LinkedList</b>.
 */
public class SearchTracker {

    private static final int MAX_HISTORY = 50;

    private final Map<String, Integer> freq = new HashMap<>();
    private final LinkedList<Entry> history = new LinkedList<>();

    public static class Entry {
        public final String query;
        public final String type;
        public final long timestamp;
        public final int resultsCount;
        public Entry(String q, String t, int r) {
            this.query = q; this.type = t; this.timestamp = System.currentTimeMillis();
            this.resultsCount = r;
        }
    }

    public void record(String query, String type, int resultsCount) {
        if (query == null || query.isBlank()) return;
        String key = query.toLowerCase().trim();
        freq.merge(key, 1, Integer::sum);
        history.addLast(new Entry(query, type, resultsCount));
        while (history.size() > MAX_HISTORY) history.removeFirst();
    }

    public Map<String, Integer> frequencies() { return new HashMap<>(freq); }

    /** Most recent first. */
    public List<Entry> recent() {
        LinkedList<Entry> reversed = new LinkedList<>();
        for (Entry e : history) reversed.addFirst(e);
        return reversed;
    }

    public int totalQueries()  { return history.size(); }
    public int uniqueQueries() { return freq.size(); }

    public void clear() {
        freq.clear();
        history.clear();
    }
}
