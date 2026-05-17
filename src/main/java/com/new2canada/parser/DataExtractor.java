package com.new2canada.parser;

import com.new2canada.models.Apartment;
import com.new2canada.models.BankPlan;
import com.new2canada.models.Job;
import com.new2canada.models.MobilePlan;
import com.new2canada.models.Scholarship;
import com.new2canada.regex.PatternFinder;
import com.new2canada.utils.Location;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pulls typed domain objects out of a Jsoup {@link Document}.
 *
 * <p>Each {@code extract…} method knows the specific structure of the
 * upstream site it's targeting:
 * <ul>
 *   <li><b>Apartments → Kijiji</b>: {@code section[data-testid=listing-card]}</li>
 *   <li><b>Jobs → JobBank.gc.ca</b>: {@code article.action-buttons > a.resultJobItem}</li>
 *   <li><b>Banks → Wikipedia + bank corporate pages</b>: title + first paragraph</li>
 *   <li><b>Mobile → Wikipedia carrier articles</b>: title + first paragraph</li>
 * </ul>
 *
 * <p>If the upstream site changes its DOM the extractor returns an empty list
 * and the caller falls back to the Firestore cache, so the UI keeps working.
 *
 * Demonstrates: <b>CSS selectors</b>, <b>Regex pattern finding</b>.
 */
public class DataExtractor {

    /* ----------------- Apartments (Kijiji + Craigslist) ------------------- */

    public List<Apartment> extractApartments(Document doc, String source) {
        List<Apartment> out = new ArrayList<>();
        if (doc == null) return out;

        // Craigslist branches off completely — different markup.
        if (source != null && source.contains("craigslist")) {
            return extractCraigslistApartments(doc, source);
        }

        Elements cards = doc.select("section[data-testid=listing-card]");
        // Fallback: older Kijiji markup
        if (cards.isEmpty()) cards = doc.select("li.regular-ad, li.top-ad, [data-listing-id]");

        for (Element card : cards) {
            String id    = card.attr("data-listingid");
            if (id == null || id.isEmpty()) id = card.attr("data-listing-id");
            if (id == null || id.isEmpty()) id = "apt-" + UUID.randomUUID().toString().substring(0, 8);
            else id = "apt-" + id;

            String title = firstText(card,
                    "[data-testid=listing-title] a[data-testid=listing-link], [data-testid=listing-title], h3 a, h3, .title a, .title",
                    "Apartment listing");
            String url   = firstAttr(card,
                    "[data-testid=listing-link], a.title, a", "href", doc.baseUri());
            String rawCity = firstText(card,
                    "[data-testid=listing-location], .location",
                    inferCityFromUrl(doc.baseUri()));
            Location loc = Location.parse(rawCity);
            String desc  = firstText(card,
                    "[data-testid=listing-description], .description",
                    "");
            double rent  = PatternFinder.firstPriceAsNumber(
                    firstText(card, "[data-testid=listing-price], .price", card.text()),
                    0.0);
            int    bedr  = inferBedrooms(card.text());

            out.add(new Apartment(id, title, loc.city, loc.province, bedr, rent,
                    source, absUrl(url, doc.baseUri()), desc));
        }
        return out;
    }

    /** Craigslist's static-results page — different markup from Kijiji. */
    private List<Apartment> extractCraigslistApartments(Document doc, String source) {
        List<Apartment> out = new ArrayList<>();
        Elements cards = doc.select("li.cl-static-search-result");
        String inferredCity = inferCityFromUrl(doc.baseUri());
        for (Element card : cards) {
            String title = firstTextChain(card, "Apartment listing", "div.title", ".title");
            String url   = firstAttr(card, "a", "href", doc.baseUri());
            String priceText = firstTextChain(card, "", "div.price", ".price");
            double rent  = PatternFinder.firstPriceAsNumber(priceText, 0.0);
            String rawCity = firstTextChain(card, inferredCity, "div.location", ".location");
            Location loc = Location.parse(rawCity);
            // Craigslist doesn't expose a description in the index — keep blank.
            int bedr = inferBedrooms(card.attr("title") + " " + title);
            String id = "apt-cl-" + Math.abs((url + title).hashCode() % 10_000_000);
            out.add(new Apartment(id, title, loc.city, loc.province, bedr, rent,
                    source, absUrl(url, doc.baseUri()), ""));
        }
        return out;
    }

    /* ----------------- Jobs (JobBank + Eluta) ----------------------------- */

    public List<Job> extractJobs(Document doc, String source) {
        List<Job> out = new ArrayList<>();
        if (doc == null) return out;

        // Eluta branches off — different markup from JobBank.
        if (source != null && source.contains("eluta")) {
            return extractElutaJobs(doc, source);
        }

        Elements cards = doc.select("article.action-buttons");
        // Fallback for non-JobBank job sites
        if (cards.isEmpty()) cards = doc.select(".jobsearch-SerpJobCard, .result, .job_seen_beacon");

        for (Element card : cards) {
            String href     = firstAttr(card, "a.resultJobItem, a", "href", "");
            String url      = absUrl(href, doc.baseUri());
            String id       = "job-" + extractDigits(card.attr("id"));
            if (id.equals("job-")) id = "job-" + UUID.randomUUID().toString().substring(0, 8);

            // Try most-specific selectors first; firstTextChain returns on the
            // first non-empty match so we don't get parent-element bleed-through.
            String title    = firstTextChain(card, "Part-time job",
                    "span.noctitle", ".noctitle", "h3.title", "h2.title");
            String employer = firstTextChain(card, "Employer not listed",
                    "li.business", ".companyName", ".employer");
            String rawCity  = cleanLocation(firstTextChain(card, "Canada",
                    "li.location", ".location"));
            Location loc    = Location.parse(rawCity);
            String salary   = firstTextChain(card, "", "li.salary", ".salary");
            double hourly   = PatternFinder.firstPriceAsNumber(salary, 0.0);
            String desc     = firstTextChain(card, salary,
                    ".result-display", ".summary", ".job-snippet", "p");

            out.add(new Job(id, title, employer, loc.city, loc.province, hourly,
                    source, url, desc));
        }
        return out;
    }

    /** Eluta.ca search-results — `div.organic-job` per posting. */
    private List<Job> extractElutaJobs(Document doc, String source) {
        List<Job> out = new ArrayList<>();
        Elements cards = doc.select("div.organic-job");
        for (Element card : cards) {
            String title    = firstTextChain(card, "Part-time job",
                    "a.lk-job-title", "h2.title a", "h2.title");
            String employer = firstTextChain(card, "Employer not listed",
                    ".organic-company", ".company", ".employer");
            String rawCity  = firstTextChain(card, "Canada",
                    ".location", ".organic-location", ".lk-job-location");
            Location loc    = Location.parse(rawCity);
            String salary   = firstTextChain(card, "", ".position-salary", ".salary");
            double hourly   = PatternFinder.firstPriceAsNumber(salary, 0.0);
            String url      = firstAttr(card, "a.lk-job-title, a", "data-url", "");
            if (!url.isEmpty() && !url.startsWith("http")) url = "https://www.eluta.ca/" + url;
            if (url.isEmpty()) url = doc.baseUri();
            String desc     = firstTextChain(card, salary,
                    ".organic-text", ".snippet", "p");
            String id = "job-eluta-" + Math.abs((url + title).hashCode() % 10_000_000);
            out.add(new Job(id, title, employer, loc.city, loc.province, hourly,
                    source, url, desc));
        }
        return out;
    }

    /* ----------------- Banks (corporate homepages) ----------------------- */

    public List<BankPlan> extractBankPlans(Document doc, String source, String bankName) {
        List<BankPlan> out = new ArrayList<>();
        if (doc == null) return out;

        String title = firstText(doc, "h1, h2, title",
                bankName + " banking");
        String desc  = firstSubstantialParagraph(doc,
                "main p, article p, .content p, #content p, p");

        // Only attribute a monthly fee when the page explicitly says so —
        // grabbing the first "$" on a bank's marketing page produced things
        // like "CIBC $1800/mo" (credit limits, loan amounts, etc.). Default
        // to $0, which is correct for almost every Canadian student account.
        double fee = extractMonthlyFee(doc.text());
        boolean stud = doc.text().toLowerCase().contains("student");

        String id = "bank-" + bankName.toLowerCase().replaceAll("[^a-z0-9]+", "-")
                + "-" + Math.abs((source + title).hashCode() % 100_000);
        out.add(new BankPlan(id, bankName, title, fee, 30, stud, source, doc.baseUri(), desc));
        return out;
    }

    /**
     * Pulls a monthly fee out of free-form text, but only when the dollar
     * amount is clearly labelled as a recurring monthly charge. Recognises
     * "$X/mo", "$X per month", "$X monthly". Falls back to $0 if the page
     * advertises "no monthly fee" or no clear monthly figure is present —
     * defaulting to 0 is correct for the typical Canadian student account.
     */
    private static double extractMonthlyFee(String text) {
        if (text == null) return 0;
        String t = text.toLowerCase();
        if (t.contains("no monthly fee") || t.contains("$0 monthly")
                || t.contains("zero monthly fee")) return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "\\$\\s*(\\d{1,3}(?:[,.]\\d{1,3})?)\\s*(?:/\\s*(?:mo|month)\\b|per\\s+month|monthly)",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1).replace(",", ".")); }
            catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    /* ----------------- Scholarships (Gov.ca + Wikipedia) ----------------- */

    public List<Scholarship> extractScholarships(Document doc, String source) {
        List<Scholarship> out = new ArrayList<>();
        if (doc == null) return out;

        String baseUri = doc.baseUri() == null ? "" : doc.baseUri();
        boolean isGovList = baseUri.contains("scholarships.gc.ca");

        // Government listing pages render an HTML table — try that first.
        if (isGovList) {
            Elements rows = doc.select("table tbody tr, table tr");
            for (Element row : rows) {
                Elements cells = row.select("td");
                if (cells.size() < 2) continue;
                String name = cells.first().text().trim();
                if (name.isEmpty() || name.length() < 4) continue;
                String desc   = cells.size() > 1 ? cells.get(1).text().trim() : "";
                String href   = firstAttr(row, "a", "href", baseUri);
                String url    = absUrl(href, baseUri);
                double amount = PatternFinder.firstPriceAsNumber(desc, 0.0);
                String level  = inferLevel(name + " " + desc);
                boolean intl  = (name + " " + desc).toLowerCase().contains("international")
                        || baseUri.contains("non_can");
                String prov   = "";
                String id = "sch-gov-" + Math.abs((url + name).hashCode() % 10_000_000);
                out.add(new Scholarship(id, name, "Government of Canada", amount, level,
                        intl, "Canada", prov, source, url, desc));
            }
            // If we found nothing in the table, fall through to the
            // generic article extractor — the page might be prose-only.
            if (!out.isEmpty()) return out;
        }

        // Wikipedia / univcan / canada.ca prose pages: H1 + first paragraph.
        String name = firstText(doc, "h1#firstHeading, h1, h2, title",
                "Scholarship");
        String desc = firstSubstantialParagraph(doc,
                "#mw-content-text p, #bodyContent p, main p, article p, p");
        double amount = PatternFinder.firstPriceAsNumber(desc, 0.0);
        String level  = inferLevel(name + " " + desc);
        boolean intl  = doc.text().toLowerCase().contains("international student")
                || doc.text().toLowerCase().contains("non-canadian");
        String provider = providerOf(baseUri);

        String id = "sch-" + provider.toLowerCase().replaceAll("[^a-z0-9]+", "-")
                + "-" + Math.abs((source + name).hashCode() % 100_000);
        out.add(new Scholarship(id, name, provider, amount, level, intl,
                "Canada", "", source, baseUri, desc));
        return out;
    }

    private static String providerOf(String url) {
        if (url == null) return "Scholarship Canada";
        String u = url.toLowerCase();
        if (u.contains("scholarships.gc.ca"))  return "Government of Canada";
        if (u.contains("canada.ca"))           return "Government of Canada";
        if (u.contains("univcan.ca"))          return "Universities Canada";
        if (u.contains("wikipedia"))           return "Public award";
        return "Scholarship Canada";
    }

    private static String inferLevel(String text) {
        if (text == null) return "Any";
        String t = text.toLowerCase();
        boolean grad  = t.contains("graduate") || t.contains("phd") || t.contains("doctoral")
                || t.contains("master") || t.contains("postdoc");
        boolean under = t.contains("undergraduate") || t.contains("bachelor")
                || t.contains("first-year") || t.contains("high school");
        if (grad && !under)  return "Graduate";
        if (under && !grad)  return "Undergraduate";
        if (grad)            return "Graduate";
        if (under)           return "Undergraduate";
        return "Any";
    }

    /* ----------------- Mobile (Wikipedia carrier articles) --------------- */

    public List<MobilePlan> extractMobilePlans(Document doc, String source, String carrier) {
        List<MobilePlan> out = new ArrayList<>();
        if (doc == null) return out;

        String name  = firstText(doc, "h1, h2, title", carrier + " plans");
        // Scan the whole page text — carrier plan pages bury prices in
        // many small elements, not always in a paragraph.
        String fullText = doc.text();
        double data  = inferDataGb(fullText);
        double price = PatternFinder.firstPriceAsNumber(fullText, 0.0);
        // Discard prices outside the plausible Canadian mobile-plan range —
        // marketing text often mentions loan limits or contest totals ($10,000)
        // that aren't monthly prices.
        if (price < 5 || price > 500) price = 0;
        // Skip entries that have neither GB nor a sane price — they're just
        // marketing shells (or JS-rendered pages where Jsoup saw an empty body).
        if (data <= 0 && price <= 0) return out;

        boolean talk = fullText.toLowerCase().contains("unlimited talk")
                || fullText.toLowerCase().contains("unlimited canada");
        String desc  = firstSubstantialParagraph(doc, "main p, article p, p");

        String id = "mob-" + carrier.toLowerCase().replaceAll("[^a-z0-9]+", "-")
                + "-" + Math.abs((source + name).hashCode() % 100_000);
        out.add(new MobilePlan(id, carrier, name, price, data, talk, source, doc.baseUri(), desc));
        return out;
    }

    /* ----------------- Helpers ------------------------------------------- */

    private static String firstText(Element root, String selector, String fallback) {
        Elements found = root.select(selector);
        if (found.isEmpty()) return fallback;
        String t = found.first().text().trim();
        return t.isEmpty() ? fallback : t;
    }

    /**
     * Tries each selector in order; returns the text from the first one that
     * matches a non-empty element. Use when comma-separated selectors would
     * pick up unwanted parent elements.
     */
    private static String firstTextChain(Element root, String fallback, String... selectors) {
        for (String sel : selectors) {
            Elements found = root.select(sel);
            if (found.isEmpty()) continue;
            String t = found.first().text().trim();
            if (!t.isEmpty()) return t;
        }
        return fallback;
    }

    /** Strips JobBank's "Location" prefix and collapses whitespace. */
    private static String cleanLocation(String raw) {
        if (raw == null) return "Canada";
        String s = raw.replaceFirst("^\\s*Location\\s*", "").replaceAll("\\s+", " ").trim();
        return s.isEmpty() ? "Canada" : s;
    }

    private static String firstAttr(Element root, String selector, String attr, String fallback) {
        Elements found = root.select(selector);
        if (found.isEmpty()) return fallback;
        String v = found.first().attr(attr);
        return v == null || v.isEmpty() ? fallback : v;
    }

    /** Returns the first paragraph with at least 60 chars of real text. */
    private static String firstSubstantialParagraph(Element root, String selector) {
        for (Element p : root.select(selector)) {
            String t = p.text().trim();
            if (t.length() >= 60) return truncate(t, 320);
        }
        // Last resort: any paragraph
        Element any = root.selectFirst(selector);
        return any != null ? truncate(any.text().trim(), 320) : "";
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }

    private static String absUrl(String href, String baseUri) {
        if (href == null || href.isEmpty()) return baseUri;
        if (href.startsWith("http://") || href.startsWith("https://")) return href;
        try { return new java.net.URI(baseUri).resolve(href).toString(); }
        catch (Exception e) { return href; }
    }

    private static String extractDigits(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) if (Character.isDigit(c)) b.append(c);
        return b.toString();
    }

    private static String inferCityFromUrl(String url) {
        if (url == null) return "Canada";
        String u = url.toLowerCase();
        if (u.contains("windsor"))     return "Windsor";
        if (u.contains("toronto"))     return "Toronto";
        if (u.contains("mississauga")) return "Mississauga";
        if (u.contains("brampton"))    return "Brampton";
        if (u.contains("hamilton"))    return "Hamilton";
        if (u.contains("ottawa"))      return "Ottawa";
        return "Canada";
    }

    private static int inferBedrooms(String text) {
        if (text == null) return 1;
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("(\\d+)\\s*(?:br|bed|bedroom)",
                        java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); }
            catch (NumberFormatException ignored) {}
        }
        if (text.toLowerCase().contains("studio") || text.toLowerCase().contains("bachelor")) return 0;
        return 1;
    }

    private static double inferDataGb(String text) {
        if (text == null) return 0;
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*GB",
                        java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1)); }
            catch (NumberFormatException ignored) {}
        }
        return 0;
    }
}
