package com.new2canada.regex;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts structured fragments (prices, dates, currencies, phone numbers,
 * email addresses) from a blob of free-form text.
 *
 * <p>This is the counterpart of {@link RegexValidator}. Where the validator
 * answers "is this string a valid X?", the finder answers "find every X in
 * this string". Used by the parser to pull prices out of scraped pages and
 * by the demo endpoint to show pattern extraction.
 *
 * Demonstrates: <b>Regular Expressions</b> (extraction).
 */
public final class PatternFinder {

    private PatternFinder() {}

    // Match either a comma-grouped amount ("$1,250", "$1,250,000.00") OR a
    // run of digits ("$1850", "$185"). The previous pattern "\d{1,3}[,\d]{0,3}"
    // capped the post-dollar match at 4 chars, so "$1,850" matched "$1,85" and
    // got parsed as 185 — losing the trailing 0 on every 4-figure dollar value.
    private static final Pattern PRICE_CAD =
            Pattern.compile("(?:CAD\\s*)?\\$\\s*(?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\.\\d{1,2})?");

    private static final Pattern PERCENT =
            Pattern.compile("\\d+(?:\\.\\d+)?\\s*%");

    private static final Pattern DATE_ISO =
            Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b");

    private static final Pattern DATE_LONG =
            Pattern.compile("\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2},?\\s+\\d{4}\\b",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern PHONE =
            Pattern.compile("\\+?1?[\\s\\-.]?\\(?\\d{3}\\)?[\\s\\-.]?\\d{3}[\\s\\-.]?\\d{4}");

    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    /** All Canadian-dollar prices in the text, in source order. */
    public static List<String> findPrices(String text)   { return findAll(text, PRICE_CAD); }
    public static List<String> findPercents(String text) { return findAll(text, PERCENT); }
    public static List<String> findDates(String text) {
        List<String> out = findAll(text, DATE_ISO);
        out.addAll(findAll(text, DATE_LONG));
        return out;
    }
    public static List<String> findPhones(String text)   { return findAll(text, PHONE); }
    public static List<String> findEmails(String text)   { return findAll(text, EMAIL); }

    /** Try to extract the first numeric price (e.g. "$1,250.00" → 1250.00). */
    public static double firstPriceAsNumber(String text, double fallback) {
        List<String> prices = findPrices(text);
        if (prices.isEmpty()) return fallback;
        String cleaned = prices.get(0).replaceAll("[^\\d.]", "");
        if (cleaned.isEmpty()) return fallback;
        try { return Double.parseDouble(cleaned); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static List<String> findAll(String text, Pattern p) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        Matcher m = p.matcher(text);
        while (m.find()) out.add(m.group());
        return out;
    }
}
