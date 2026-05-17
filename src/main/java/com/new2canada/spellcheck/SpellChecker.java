package com.new2canada.spellcheck;

import com.new2canada.config.AppConfig;
import com.new2canada.utils.ResourceLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Did you mean…?" engine.
 *
 * <p>Loads an English dictionary from {@code resources/dictionary.txt} into a
 * {@link HashSet} for O(1) word lookup. Words harvested from crawled documents
 * are added on top, so domain words such as "rentals" or "scotiabank" become
 * valid spellings.
 *
 * <p>If a query word is not in the dictionary, the checker linearly scans the
 * dictionary, computes {@link EditDistance#distance(String, String)}, and
 * returns the {@code k} closest candidates within an edit distance of 2.
 *
 * <p><b>Time complexity</b>: O(D · m · n) where D is the dictionary size and
 * m, n are word lengths. Practical for a student-sized dictionary; a
 * production system would use a BK-tree or symspell.
 *
 * Demonstrates: <b>HashMap/HashSet</b>, <b>Edit Distance</b>.
 */
public class SpellChecker {

    private final Set<String> dictionary = new HashSet<>();
    /** Lowercased word → frequency. Used to break ties. */
    private final Map<String, Integer> frequencies = new HashMap<>();

    public SpellChecker() {
        for (String w : ResourceLoader.readLines("dictionary.txt")) {
            String word = w.toLowerCase();
            dictionary.add(word);
            frequencies.put(word, 1);
        }
    }

    /**
     * Add a word seen while crawling (or boost its frequency).
     *
     * <p>Splits on any non-alphanumeric char and adds each token separately —
     * otherwise raw titles like "Apartment!" or "apartment." each become
     * distinct dictionary entries and clutter the spell-check suggestions
     * with punctuation variants.
     */
    public void learn(String word) {
        if (word == null || word.isBlank()) return;
        for (String tok : word.toLowerCase().split("[^a-z0-9]+")) {
            if (tok.length() < 2) continue;
            dictionary.add(tok);
            frequencies.merge(tok, 1, Integer::sum);
        }
    }

    public boolean isValid(String word) {
        return word != null && dictionary.contains(word.toLowerCase());
    }

    /** Top-k corrections within edit distance 2. */
    public List<String> suggest(String input) {
        return suggest(input, AppConfig.SPELLCHECK_LIMIT);
    }

    public List<String> suggest(String input, int limit) {
        if (input == null || input.isBlank()) return Collections.emptyList();
        String word = input.toLowerCase();
        if (dictionary.contains(word)) return List.of(word);

        List<String[]> scored = new ArrayList<>();
        for (String candidate : dictionary) {
            // Cheap length filter: words that differ in length by more than 2
            // can never have edit distance ≤ 2, so skip the expensive DP.
            if (Math.abs(candidate.length() - word.length()) > 2) continue;
            int d = EditDistance.distance(word, candidate);
            if (d <= 2) {
                scored.add(new String[]{candidate, Integer.toString(d)});
            }
        }
        // Sort: shorter distance first; then higher frequency first.
        scored.sort(Comparator
                .<String[]>comparingInt(a -> Integer.parseInt(a[1]))
                .thenComparingInt(a -> -frequencies.getOrDefault(a[0], 0)));

        List<String> out = new ArrayList<>();
        for (int i = 0; i < scored.size() && i < limit; i++) out.add(scored.get(i)[0]);
        return out;
    }

    public int dictionarySize() { return dictionary.size(); }
}
