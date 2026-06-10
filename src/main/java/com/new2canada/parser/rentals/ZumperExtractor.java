package com.new2canada.parser.rentals;

import com.new2canada.models.Apartment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.new2canada.parser.rentals.RentalExtractorUtils.*;

/**
 * Zumper Calgary apartments search
 * ({@code https://www.zumper.com/apartments-for-rent/calgary-ab}).
 *
 * <p>Zumper is a JS-rendered React app with no stable card class name, so we
 * find listing-detail links (a deeper path under {@code /apartments-for-rent/})
 * and scrape the surrounding card text for address/price/beds.
 */
public class ZumperExtractor implements RentalExtractor {

    private static final Pattern LISTING_HREF =
            Pattern.compile("^/apartments-for-rent/[^/]+/[^/]+/[^/]+");

    @Override
    public boolean supports(String url) {
        return url != null && url.toLowerCase().contains("zumper.com");
    }

    @Override
    public List<Apartment> extract(Document doc, String url) {
        List<Apartment> out = new ArrayList<>();
        for (Element card : cardsByAnchor(doc, LISTING_HREF, 3)) {
            String text = card.text();
            String href = firstAttr(card, "a[href]", "href", doc.baseUri());
            String title = firstTextChain(card, "Apartment listing", "h2, h3, h4, [class*=title]");
            double rent = priceFromText(text);
            int bedrooms = inferBedrooms(text);
            String address = streetAddress(text);
            if (address.isEmpty()) address = title.equals("Apartment listing") ? "Calgary, AB" : title;

            out.add(new Apartment(idFromUrl("zumper", href, title), title, address,
                    "Calgary", "AB", bedrooms, rent, "zumper.com", absUrl(href, doc.baseUri()), ""));
        }
        return out;
    }
}
