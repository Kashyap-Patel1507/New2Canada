package com.new2canada.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tokenises raw strings into a normalised list of lowercase word tokens.
 *
 * <p>Used by every component that compares text:
 * {@link com.new2canada.indexing.InvertedIndex},
 * {@link com.new2canada.autocomplete.AutocompleteSystem},
 * {@link com.new2canada.spellcheck.SpellChecker}.
 *
 * <p>Steps:
 * <ol>
 *   <li>Lowercase the input.</li>
 *   <li>Split on anything that is not a letter or digit.</li>
 *   <li>Drop empty tokens and a small list of stop-words.</li>
 * </ol>
 *
 * Complexity: O(N) over the length of the input string.
 *
 * Demonstrates: <b>Regular Expressions</b>.
 */
public final class TextNormalizer {

    private TextNormalizer() {}

    /** Single split-by-non-word pattern, reused. */
    private static final Pattern SPLIT = Pattern.compile("[^a-z0-9]+");

    /** Tiny English stop-word list — enough for a student project. */
    private static final List<String> STOP_WORDS = List.of(
            "the", "a", "an", "and", "or", "of", "to", "in", "on", "for",
            "with", "is", "are", "be", "this", "that", "it", "as", "by",
            "at", "from"
    );

    public static List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        String[] parts = SPLIT.split(text.toLowerCase());
        for (String p : parts) {
            if (p.isEmpty() || STOP_WORDS.contains(p)) continue;
            out.add(p);
        }
        return out;
    }

    /** Lowercase, trim, collapse whitespace — used for autocomplete prefixes. */
    public static String normalizePrefix(String raw) {
        if (raw == null) return "";
        return raw.toLowerCase().trim().replaceAll("\\s+", " ");
    }
}
