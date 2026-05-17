package com.new2canada.indexing;

import com.new2canada.utils.TextNormalizer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Counts how often each token appears inside a single document.
 *
 * <p>Wraps a {@link HashMap} from token → frequency. Used by:
 * <ul>
 *   <li>{@link InvertedIndex} when computing per-document term frequencies,</li>
 *   <li>{@link com.new2canada.ranking.PageRanker} for the TF component of
 *       TF·IDF,</li>
 *   <li>the {@code /api/debug} endpoint to show statistics during the demo.</li>
 * </ul>
 *
 * <p><b>Time complexity</b>: O(N) over the input text. Lookups are O(1)
 * amortised.
 *
 * Demonstrates: <b>HashMap</b>, <b>frequency counting</b>.
 */
public class WordFrequencyCounter {

    private final Map<String, Integer> counts = new HashMap<>();
    private int totalTokens = 0;

    /** Tokenises {@code text} via {@link TextNormalizer} and accumulates counts. */
    public void countText(String text) {
        for (String tok : TextNormalizer.tokenize(text)) countToken(tok);
    }

    public void countToken(String token) {
        if (token == null || token.isEmpty()) return;
        counts.merge(token, 1, Integer::sum);
        totalTokens++;
    }

    public int frequencyOf(String token) {
        return counts.getOrDefault(token, 0);
    }

    public int uniqueTokens() { return counts.size(); }
    public int totalTokens()  { return totalTokens; }

    /** Snapshot of internal state — handy for /api/debug. */
    public Map<String, Integer> snapshot() {
        // Sort descending by frequency so the demo output is readable.
        Map<String, Integer> sorted = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    /** Demo helper: count one batch of texts and return the counter. */
    public static WordFrequencyCounter of(List<String> texts) {
        WordFrequencyCounter c = new WordFrequencyCounter();
        for (String t : texts) c.countText(t);
        return c;
    }
}
