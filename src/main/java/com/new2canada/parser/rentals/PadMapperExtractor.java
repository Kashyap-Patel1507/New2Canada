package com.new2canada.parser.rentals;

import com.new2canada.models.Apartment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.new2canada.parser.rentals.RentalExtractorUtils.*;

/**
 * PadMapper Ottawa apartments search
 * ({@code https://www.padmapper.com/apartments/ottawa-on}).
 *
 * <p>Same JS-rendered, no-stable-card-class situation as Zumper (same parent
 * company) — find listing-detail links and scrape the surrounding card text.
 */
public class PadMapperExtractor implements RentalExtractor {

    private static final Pattern LISTING_HREF =
            Pattern.compile("^/apartments/[^/]+/[^/]+/[^/]+");

    @Override
    public boolean supports(String url) {
        return url != null && url.toLowerCase().contains("padmapper.com");
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
            if (address.isEmpty()) address = title.equals("Apartment listing") ? "Ottawa, ON" : title;

            out.add(new Apartment(idFromUrl("padmapper", href, title), title, address,
                    "Ottawa", "ON", bedrooms, rent, "padmapper.com", absUrl(href, doc.baseUri()), ""));
        }
        return out;
    }
}
