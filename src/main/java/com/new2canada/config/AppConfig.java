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
     * Seed URLs for each crawler — picked because they return static,
     * server-rendered HTML. Kijiji location IDs were verified by hitting
     * each URL and inspecting which city the listings actually report.
     */
    public static final List<String> APARTMENT_SEEDS = List.of(
            // Aggregator URLs — give us cross-Canada and Ontario-wide listings
            "https://www.kijiji.ca/b-apartments-condos/canada/c37l0",
            "https://www.kijiji.ca/b-apartments-condos/ontario/c37l9004",
            // Ontario — Greater Toronto & Southwestern
            "https://www.kijiji.ca/b-apartments-condos/city-of-toronto/c37l1700273",
            "https://www.kijiji.ca/b-apartments-condos/north-york/c37l1700274",
            "https://www.kijiji.ca/b-apartments-condos/brampton/c37l1700276",
            "https://www.kijiji.ca/b-apartments-condos/oakville-halton/c37l1700277",
            "https://www.kijiji.ca/b-apartments-condos/pickering/c37l1700275",
            "https://www.kijiji.ca/b-apartments-condos/windsor/c37l1700220",
            "https://www.kijiji.ca/b-apartments-condos/london/c37l1700214",
            "https://www.kijiji.ca/b-apartments-condos/cambridge/c37l1700210",
            "https://www.kijiji.ca/b-apartments-condos/waterloo/c37l1700212",
            "https://www.kijiji.ca/b-apartments-condos/woodstock/c37l1700241",
            "https://www.kijiji.ca/b-apartments-condos/guelph/c37l1700242",
            "https://www.kijiji.ca/b-apartments-condos/stratford/c37l1700213",
            "https://www.kijiji.ca/b-apartments-condos/lindsay/c37l1700219",
            // Ontario — Eastern & Northern
            "https://www.kijiji.ca/b-apartments-condos/ottawa/c37l1700185",
            "https://www.kijiji.ca/b-apartments-condos/brockville/c37l1700247",
            "https://www.kijiji.ca/b-apartments-condos/sudbury/c37l1700245",
            "https://www.kijiji.ca/b-apartments-condos/north-bay/c37l1700243",
            "https://www.kijiji.ca/b-apartments-condos/sault-ste-marie/c37l1700244",
            "https://www.kijiji.ca/b-apartments-condos/timmins/c37l1700238",
            // British Columbia
            "https://www.kijiji.ca/b-apartments-condos/vancouver/c37l1700287",
            "https://www.kijiji.ca/b-apartments-condos/north-vancouver/c37l1700289",
            "https://www.kijiji.ca/b-apartments-condos/richmond-bc/c37l1700288",
            "https://www.kijiji.ca/b-apartments-condos/surrey/c37l1700285",
            "https://www.kijiji.ca/b-apartments-condos/maple-ridge/c37l1700290",
            "https://www.kijiji.ca/b-apartments-condos/victoria/c37l1700173",
            // Alberta
            "https://www.kijiji.ca/b-apartments-condos/calgary/c37l1700199",
            "https://www.kijiji.ca/b-apartments-condos/edmonton/c37l1700203",
            "https://www.kijiji.ca/b-apartments-condos/medicine-hat/c37l1700231",
            "https://www.kijiji.ca/b-apartments-condos/lethbridge/c37l1700230",
            "https://www.kijiji.ca/b-apartments-condos/grande-prairie/c37l1700233",
            // Saskatchewan / Manitoba
            "https://www.kijiji.ca/b-apartments-condos/saskatoon/c37l1700197",
            // Maritimes / Atlantic
            "https://www.kijiji.ca/b-apartments-condos/halifax/c37l1700321",
            "https://www.kijiji.ca/b-apartments-condos/charlottetown/c37l1700118",
            "https://www.kijiji.ca/b-apartments-condos/st-johns/c37l1700113",
            // Quebec
            "https://www.kijiji.ca/b-apartments-condos/montreal/c37l1700281",
            "https://www.kijiji.ca/b-apartments-condos/saguenay/c37l1700178",
            "https://www.kijiji.ca/b-apartments-condos/drummondville/c37l1700121",
            // Rooms for rent — extra Windsor source
            "https://www.kijiji.ca/b-rooms-for-rent-shared-accommodation/windsor/c36l1700220",
            // ---- Craigslist (different DOM, handled by DataExtractor) ----
            "https://toronto.craigslist.org/search/apa",
            "https://vancouver.craigslist.org/search/apa",
            "https://montreal.craigslist.org/search/apa",
            "https://victoria.craigslist.org/search/apa",
            "https://calgary.craigslist.org/search/apa",
            "https://ottawa.craigslist.org/search/apa",
            "https://edmonton.craigslist.org/search/apa",
            "https://winnipeg.craigslist.org/search/apa",
            "https://halifax.craigslist.org/search/apa",
            "https://hamilton.craigslist.org/search/apa",
            "https://windsor.craigslist.org/search/apa",
            "https://kingston.craigslist.org/search/apa",
            "https://london.craigslist.org/search/apa"
    );

    public static final List<String> JOB_SEEDS = List.of(
            // Ontario — multiple cities + job types
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=student&locationstring=Windsor%2C+ON",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=part+time&locationstring=Windsor%2C+ON",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=cashier&locationstring=Windsor%2C+ON",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=part+time&locationstring=Toronto%2C+ON",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=cashier&locationstring=Mississauga%2C+ON",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=warehouse&locationstring=Brampton%2C+ON",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=server&locationstring=Ottawa%2C+ON",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=retail&locationstring=Hamilton%2C+ON",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=barista&locationstring=London%2C+ON",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=part+time&locationstring=Sudbury%2C+ON",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=part+time&locationstring=Kingston%2C+ON",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=part+time&locationstring=Barrie%2C+ON",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=part+time&locationstring=Kitchener%2C+ON",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=part+time&locationstring=Niagara+Falls%2C+ON",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=student&locationstring=Thunder+Bay%2C+ON",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=part+time&locationstring=Oshawa%2C+ON",
            // BC
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=student&locationstring=Vancouver%2C+BC",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=part+time&locationstring=Victoria%2C+BC",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=student&locationstring=Surrey%2C+BC",
            // AB
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=student&locationstring=Calgary%2C+AB",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=part+time&locationstring=Edmonton%2C+AB",
            // MB / SK
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=student&locationstring=Winnipeg%2C+MB",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=part+time&locationstring=Saskatoon%2C+SK",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=student&locationstring=Regina%2C+SK",
            // QC
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=student&locationstring=Montreal%2C+QC",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=part+time&locationstring=Quebec+City%2C+QC",
            // Maritimes / Atlantic
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=student&locationstring=Halifax%2C+NS",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=student&locationstring=Fredericton%2C+NB",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=student&locationstring=Charlottetown%2C+PE",
            "https://www.jobbank.gc.ca/jobsearch/jobsearch?searchstring=student&locationstring=St.+John%27s%2C+NL"
            // NOTE: Eluta.ca and Indeed both reject our Java HTTP client at the
            // TLS handshake level (TLS-fingerprint anti-bot). Their listings can
            // only be fetched from a real browser — see VIVA.md.
    );

    // Only original bank corporate sites — Wikipedia entries were producing
    // garbage data (paragraph H1 + first $ amount on the page parsed as a
    // monthly fee, e.g. CIBC at $1800/mo). Real student-account fees come
    // from the curated list in CuratedBankPlans.
    public static final List<String> BANK_SEEDS = List.of(
            "https://www.rbcroyalbank.com/personal.html",
            "https://www.rbcroyalbank.com/students/index.html",
            "https://www.scotiabank.com/ca/en/personal.html",
            "https://www.scotiabank.com/ca/en/personal/bank-accounts/students.html",
            "https://www.cibc.com/en/personal-banking.html",
            "https://www.cibc.com/en/personal-banking/bank-accounts.html",
            "https://www.td.com/ca/en/personal-banking",
            "https://www.td.com/ca/en/personal-banking/products/bank-accounts/student-bank-accounts",
            "https://www.bmo.com/main/personal/",
            "https://www.bmo.com/main/personal/bank-accounts/student/",
            "https://www.nbc.ca/personal.html",
            "https://www.tangerine.ca/en/personal",
            "https://www.simplii.com/en/home.html",
            "https://www.eqbank.ca/",
            "https://www.desjardins.com/ca/personal/index.jsp"
    );

    // Only original carrier sites — Wikipedia carrier articles produced
    // useless "0 GB · $0/mo" entries from their history paragraphs. Real
    // plan data comes from the curated list in CuratedMobilePlans.
    public static final List<String> MOBILE_SEEDS = List.of(
            "https://www.freedommobile.ca/en-CA",
            "https://www.freedommobile.ca/en-CA/plans",
            "https://www.rogers.com/plans",
            "https://www.koodomobile.com/en/plans",
            "https://www.virginplus.ca/en/plans",
            "https://www.fido.ca/plans",
            "https://www.bell.ca/Mobility/Cell_phone_plans",
            "https://www.telus.com/en/mobility/plans",
            "https://www.publicmobile.ca/en/on/plans",
            "https://www.chatrwireless.com/plans",
            "https://www.luckymobile.ca/plans",
            "https://www.sasktel.com/personal/mobility/plans",
            "https://www.eastlink.ca/wireless/plans",
            "https://videotron.com/en/mobile/plans",
            "https://tbaytel.net/personal/wireless/"
    );

    /**
     * Scholarship & financial-aid seeds. Only original gov / sector-body /
     * foundation sites — Wikipedia award articles are excluded so we never
     * mix encyclopedia content into the search index.
     */
    public static final List<String> SCHOLARSHIP_SEEDS = List.of(
            // Government of Canada — primary source
            "https://www.scholarships.gc.ca/scholarships-bourses/index-eng.aspx",
            "https://www.scholarships.gc.ca/scholarships-bourses/non_can-eng.aspx",
            "https://www.scholarships.gc.ca/scholarships-bourses/can-eng.aspx",
            "https://www.canada.ca/en/services/benefits/education.html",
            "https://www.canada.ca/en/employment-social-development/services/education/grants.html",
            "https://www.canada.ca/en/services/benefits/education/student-aid.html",
            "https://www.canada.ca/en/services/finance/educationfunding.html",
            // Universities Canada — sector body listing of awards
            "https://www.univcan.ca/programs-and-scholarships/",
            // Major Canadian foundations & award programmes — originating sites
            "https://vanier.gc.ca/en/home-accueil.html",
            "https://banting.fellowships-bourses.gc.ca/en/home-accueil.html",
            "https://www.rhodeshouse.ox.ac.uk/scholarships/the-rhodes-scholarship/",
            "https://loranscholar.ca/",
            "https://schulichleaders.com/",
            "https://www.fondationtrudeau.ca/en",
            "https://www.killamlaureates.ca/",
            "https://osap.gov.on.ca/",
            "https://studentaid.alberta.ca/"
    );
}
