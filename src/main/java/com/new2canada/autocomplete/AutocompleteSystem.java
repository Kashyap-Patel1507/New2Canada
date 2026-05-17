package com.new2canada.autocomplete;

import com.new2canada.config.AppConfig;
import com.new2canada.utils.TextNormalizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Returns the top-k most frequent words that start with a user-supplied prefix.
 *
 * <p>Combines a {@link Trie} (for fast prefix matching) with a bounded
 * <b>min-heap</b> ({@link PriorityQueue}) that keeps the top-k candidates
 * by frequency.
 *
 * <p>Example: type "tor" → ["toronto", "toronto-east", …]
 *
 * <p><b>Time complexity</b>: O(L + N + k log k) where L is the prefix length
 * and N is the number of words under the matched node.
 *
 * Demonstrates: <b>Trie</b>, <b>Priority Queue / Heap</b>, <b>HashMap</b>.
 */
public class AutocompleteSystem {

    private final Trie trie = new Trie();

    /** Feeds one word into the trie. Call repeatedly while crawling. */
    public void index(String word) {
        if (word == null || word.isBlank()) return;
        trie.insert(word.toLowerCase());
    }

    /** Convenience: tokenise a whole document and feed every token. */
    public void indexDocument(String text) {
        for (String tok : TextNormalizer.tokenize(text)) index(tok);
    }

    /** Top-k completions for {@code rawPrefix}; never returns null. */
    public List<String> suggest(String rawPrefix, int limit) {
        int k = limit <= 0 ? AppConfig.AUTOCOMPLETE_LIMIT : limit;
        String prefix = TextNormalizer.normalizePrefix(rawPrefix);
        if (prefix.isEmpty()) return new ArrayList<>();

        List<Trie.Node> matches = trie.findByPrefix(prefix);
        // Min-heap ordered by frequency ascending — when it overflows we drop
        // the smallest, leaving the top-k by frequency at the end.
        PriorityQueue<Trie.Node> heap =
                new PriorityQueue<>(Comparator.comparingInt(n -> n.frequency));

        for (Trie.Node n : matches) {
            heap.offer(n);
            if (heap.size() > k) heap.poll();
        }

        // Drain & reverse so the highest-frequency word comes first.
        List<String> result = new ArrayList<>();
        while (!heap.isEmpty()) result.add(0, heap.poll().word);
        return result;
    }

    public int trieSize() { return trie.size(); }
}
