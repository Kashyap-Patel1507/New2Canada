package com.new2canada.parser.rentals;

import com.new2canada.regex.PatternFinder;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared helpers for the per-site {@link RentalExtractor} implementations —
 * CSS-selector lookups, price/bedroom regex, and a generic "find the listing
 * card around this link" strategy for the JS-rendered sites whose markup
 * doesn't expose a stable card class name.
 *
 * Demonstrates: <b>CSS selectors</b>, <b>Regex pattern finding</b>.
 */
public final class RentalExtractorUtils {

    private RentalExtractorUtils() {}

    public static String firstText(Element root, String selector, String fallback) {
        Elements found = root.select(selector);
        if (found.isEmpty()) return fallback;
        String t = found.first().text().trim();
        return t.isEmpty() ? fallback : t;
    }

    /** Tries each selector in turn, returning the first non-empty match. */
    public static String firstTextChain(Element root, String fallback, String... selectors) {
        for (String sel : selectors) {
            Elements found = root.select(sel);
            if (found.isEmpty()) continue;
            String t = found.first().text().trim();
            if (!t.isEmpty()) return t;
        }
        return fallback;
    }

    public static String firstAttr(Element root, String selector, String attr, String fallback) {
        Elements found = root.select(selector);
        if (found.isEmpty()) return fallback;
        String v = found.first().attr(attr);
        return v == null || v.isEmpty() ? fallback : v;
    }

    public static String absUrl(String href, String baseUri) {
        if (href == null || href.isEmpty()) return baseUri;
        if (href.startsWith("http://") || href.startsWith("https://")) return href;
        try { return new java.net.URI(baseUri).resolve(href).toString(); }
        catch (Exception e) { return href; }
    }

    // "bd"/"bds" are ViewIt's spelling ("1 bd", "2 bds"); without them those
    // listings silently fall through to the 1-bedroom default below.
    private static final Pattern BEDROOM_PATTERN =
            Pattern.compile("(\\d+)\\s*(?:br|bd|bds|bed|beds|bedroom|bedrooms)\\b", Pattern.CASE_INSENSITIVE);

    /** Defaults to 1 bedroom; recognises "studio"/"bachelor" as 0. */
    public static int inferBedrooms(String text) {
        if (text == null) return 1;
        Matcher m = BEDROOM_PATTERN.matcher(text);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); }
            catch (NumberFormatException ignored) {}
        }
        String lower = text.toLowerCase();
        if (lower.contains("studio") || lower.contains("bachelor")) return 0;
        return 1;
    }

    /**
     * Digit-grouping separator used by French-locale pages (Craigslist Montreal
     * serves "$1 850" with a non-breaking or narrow no-break space). Java's
     * {@code \s} doesn't cover U+00A0/U+202F, so the price regex would stop at
     * the "1" and read $1 850 as $1 — we strip the separator first.
     */
    private static final Pattern GROUPING_SPACE =
            Pattern.compile("(?<=\\d)[\\u00a0\\u202f\\u2009 ](?=\\d{3}(?!\\d))");

    public static double priceFromText(String text) {
        if (text == null) return 0.0;
        return PatternFinder.firstPriceAsNumber(
                GROUPING_SPACE.matcher(text).replaceAll(""), 0.0);
    }

    /**
     * Matches a Canadian-style street address line, e.g. "1310 - 258A Sunview Street"
     * or "62 Ontario St". Short, ambiguous abbreviations (St, Dr, Rd, Ave, etc.) are only
     * matched with a tight 1-2 word prefix so we don't swallow unrelated promo text that
     * happens to contain a word like "Stay" or "Steal" — full words (Street, Avenue, ...)
     * are unambiguous so the prefix is allowed to span more (non-greedily).
     */
    private static final Pattern STREET_ADDRESS = Pattern.compile(
            "\\b\\d{1,6}[A-Za-z]?(?:\\s*-\\s*\\d{1,6}[A-Za-z]?)?\\s+(?:" +
            "(?:[A-Za-zéè.'-]+\\s+){1,2}(?:St|Ave|Rd|Dr|Blvd|Ln|Cr|Ct|Pl|Hwy|Pkwy|Cir)" +
            "|" +
            "[A-Za-zéè.' -]+?\\s+(?:Street|Avenue|Road|Drive|Boulevard|Way|Lane|Crescent|Court|Place|Terrace|Trail|Highway|Parkway|Circle)" +
            ")\\b",
            Pattern.CASE_INSENSITIVE);

    /** Pulls the first street-address-looking substring out of free text, or "" if none found. */
    public static String streetAddress(String text) {
        if (text == null) return "";
        Matcher m = STREET_ADDRESS.matcher(text.replaceAll("\\s+", " "));
        return m.find() ? m.group().trim() : "";
    }

    /**
     * For JS-rendered sites without a stable "card" class: collects every
     * {@code <a href>} whose href matches {@code hrefPattern} (deduplicated by
     * href, since a card often has both an image link and a title link to the
     * same listing), then returns the ancestor element {@code levels} steps
     * up from each matching anchor as the "card" to scrape text from.
     */
    public static List<Element> cardsByAnchor(Element root, Pattern hrefPattern, int levels) {
        List<Element> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Element a : root.select("a[href]")) {
            String href = a.attr("href");
            if (href.isEmpty() || !hrefPattern.matcher(href).find()) continue;
            if (!seen.add(href)) continue;
            Element card = a;
            for (int i = 0; i < levels && card.parent() != null; i++) card = card.parent();
            out.add(card);
        }
        return out;
    }

    /** Stable-ish id derived from the listing URL, prefixed per site. */
    public static String idFromUrl(String prefix, String url, String title) {
        return "apt-" + prefix + "-" + Math.abs((url + title).hashCode() % 10_000_000);
    }

    /**
     * For sites whose listing cards don't have a documented stable class
     * name: tries each selector in order and returns the elements from the
     * first one whose matches each contain a recognisable price. Used as a
     * best-effort fallback for JS-rendered card grids.
     */
    public static List<Element> genericPriceCards(Element root, String... candidateSelectors) {
        for (String sel : candidateSelectors) {
            List<Element> withPrice = new ArrayList<>();
            for (Element el : root.select(sel)) {
                if (priceFromText(el.text()) > 0) withPrice.add(el);
            }
            if (!withPrice.isEmpty()) return withPrice;
        }
        return new ArrayList<>();
    }
}
