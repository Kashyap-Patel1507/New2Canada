package com.new2canada.parser.rentals;

import com.new2canada.models.Apartment;
import org.jsoup.nodes.Document;

import java.util.List;

/**
 * Knows how to pull {@link Apartment} listings out of one rental site's
 * search-results page.
 *
 * <p>Each implementation targets exactly one of the 10 configured
 * {@code AppConfig.APARTMENT_SEEDS} URLs, so it can hardcode that seed's
 * city/province and tune its CSS selectors to that site's DOM.
 *
 * Demonstrates: <b>interfaces / polymorphism</b> — {@link RentalExtractorRegistry}
 * dispatches to whichever implementation matches the page URL.
 */
public interface RentalExtractor {

    /** True if this extractor knows how to parse the page at {@code url}. */
    boolean supports(String url);

    /** Pulls all apartment listings out of {@code doc}. Never returns null. */
    List<Apartment> extract(Document doc, String url);

    /**
     * CSS selector that only appears once this site's listings have rendered,
     * or {@code null} if the seed page is server-rendered and needs no wait.
     *
     * <p>The JS-rendered sites populate their grid well after {@code load}
     * fires, and they don't all take the same amount of time — a single fixed
     * sleep in the fetcher is either too short (we scrape an empty grid) or
     * wastes seconds on every other site. Each extractor knows its own DOM, so
     * it names the element the fetcher should wait for.
     */
    default String readySelector() { return null; }
}
