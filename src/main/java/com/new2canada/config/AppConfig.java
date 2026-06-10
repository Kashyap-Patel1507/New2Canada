package com.new2canada.config;

import java.util.List;

/**
 * Centralised configuration constants.
 *
 * <p>Everything here is a plain {@code public static final} value so the whole
 * codebase reads from one place. No environment-variable hot-reloading — this
 * is a final project, not production infra.
 */
public final class AppConfig {

    private AppConfig() {}

    /** HTTP port the embedded web server listens on. */
    public static final int HTTP_PORT = 8080;

    /** Path (relative to working dir) where the Firebase service-account key is expected. */
    public static final String SERVICE_ACCOUNT_KEY_PATH = "serviceAccountKey.json";

    /** Polite delay between live HTTP requests, in milliseconds. */
    public static final long FETCH_DELAY_MS = 1500L;

    /** User-Agent the crawler announces itself with. */
    public static final String USER_AGENT =
            "New2Canada-StudentProject/1.0 (+https://example.edu COMP8547)";

    /** How often the background crawler refreshes data, in minutes. */
    public static final long CRAWL_INTERVAL_MIN = 360L; // 6h

    /** Max number of listings kept per category. */
    public static final int MAX_ITEMS_PER_TYPE = 2000;

    /** Hard ceiling per seed URL (otherwise Craigslist/JobBank dominate one city). */
    public static final int MAX_ITEMS_PER_SEED = 20;

    /** Default top-k for autocomplete suggestions. */
    public static final int AUTOCOMPLETE_LIMIT = 10;

    /** Default top-k for spell-correction suggestions. */
    public static final int SPELLCHECK_LIMIT = 5;

    /**
     * Seed URLs for the apartment crawler — exactly one per supported rental
     * site, each already scoped to a specific Canadian city. Each seed has a
     * matching {@link com.new2canada.parser.rentals.RentalExtractor} that
     * knows that site's DOM and the city/province it targets.
     */
    public static final List<String> APARTMENT_SEEDS = List.of(
            "https://www.kijiji.ca/b-apartments-condos/canada/c37l0",
            "https://montreal.craigslist.org/search/apa",
            "https://www.zumper.com/apartments-for-rent/calgary-ab",
            "https://www.padmapper.com/apartments/ottawa-on",
            "https://www.viewit.ca/rentals/toronto?cid=14",
            "https://4rent.ca/apartments-for-rent/ab/edmonton/3352",
            "https://www.rentseeker.ca/rentals/apartments/manitoba/winnipeg",
            "https://www.realtor.ca/on/waterloo/rentals",
            "https://liv.rent/rental-listings/city/vancouver",
            "https://rentola.ca/for-rent?location=halifax"
    );

}
