package com.new2canada.parser.rentals;

import com.new2canada.models.Apartment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.new2canada.parser.rentals.RentalExtractorUtils.*;

/**
 * liv.rent Vancouver listings
 * ({@code https://liv.rent/rental-listings/city/vancouver}).
 *
 * <p>liv.rent embeds a {@code schema.org/CollectionPage} JSON-LD block with
 * one {@code Product}/{@code RealEstateListing} entry per building, each
 * carrying a structured {@code PostalAddress} and an {@code Offer} price —
 * far more reliable than scraping the rendered card markup, and present even
 * in the pre-render HTML.
 */
public class LivRentExtractor implements RentalExtractor {

    // Each listing entry starts with this marker inside the JSON-LD blob.
    private static final String ITEM_MARKER = "\"@type\":[\"Product\",\"RealEstateListing\"]";

    private static final Pattern NAME      = Pattern.compile("\"name\":\"([^\"]*)\"");
    private static final Pattern URL       = Pattern.compile("\"url\":\"(https?:[^\"]*)\"");
    private static final Pattern STREET    = Pattern.compile("\"streetAddress\":\"([^\"]*)\"");
    private static final Pattern LOCALITY  = Pattern.compile("\"addressLocality\":\"([^\"]*)\"");
    private static final Pattern REGION    = Pattern.compile("\"addressRegion\":\"([^\"]*)\"");
    private static final Pattern PRICE     = Pattern.compile("\"price\":(\\d+(?:\\.\\d+)?)");

    @Override
    public boolean supports(String url) {
        return url != null && url.toLowerCase().contains("liv.rent");
    }

    @Override
    public List<Apartment> extract(Document doc, String url) {
        List<Apartment> out = new ArrayList<>();

        for (Element script : doc.select("script[type=application/ld+json]")) {
            String json = script.data();
            if (!json.contains("RealEstateListing")) continue;

            int from = 0;
            while (true) {
                int idx = json.indexOf(ITEM_MARKER, from);
                if (idx < 0) break;
                // Scan a generous window after the marker — enough to cover
                // name/address/offer for a single listing without spilling
                // into the next one in the array.
                int end = json.indexOf(ITEM_MARKER, idx + ITEM_MARKER.length());
                String chunk = json.substring(idx, end < 0 ? json.length() : end);
                from = idx + ITEM_MARKER.length();

                String name    = group(NAME, chunk, "Apartment listing");
                String href    = group(URL, chunk, doc.baseUri());
                String street  = group(STREET, chunk, "");
                String locality = group(LOCALITY, chunk, "Vancouver");
                String region  = group(REGION, chunk, "BC");
                double price   = parseDouble(group(PRICE, chunk, "0"));

                String address = street.isEmpty() ? name : street;
                int bedrooms = inferBedrooms(name);

                out.add(new Apartment(idFromUrl("livrent", href, name), name, address,
                        locality, region, bedrooms, price, "liv.rent", absUrl(href, doc.baseUri()), ""));
            }
        }
        return out;
    }

    private static String group(Pattern p, String text, String fallback) {
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1) : fallback;
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return 0; }
    }
}
