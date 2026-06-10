package com.new2canada.parser.rentals;

import com.new2canada.models.Apartment;
import com.new2canada.utils.Location;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.new2canada.parser.rentals.RentalExtractorUtils.*;

/**
 * Rentola Halifax search ({@code https://rentola.ca/for-rent?location=halifax}).
 *
 * <p>Each card links to {@code /listings/<slug>-p<id>} and contains a title
 * paragraph, a "street, city, province postal, country" address paragraph,
 * and a "C$X,XXX / month" price paragraph.
 */
public class RentolaExtractor implements RentalExtractor {

    private static final Pattern LISTING_HREF = Pattern.compile("^/listings/");

    @Override
    public boolean supports(String url) {
        return url != null && url.toLowerCase().contains("rentola.ca");
    }

    @Override
    public List<Apartment> extract(Document doc, String url) {
        List<Apartment> out = new ArrayList<>();
        for (Element card : cardsByAnchor(doc, LISTING_HREF, 1)) {
            String text = card.text();
            String href = firstAttr(card, "a[href]", "href", doc.baseUri());
            String title = firstTextChain(card, "Apartment listing", "p[class*=font-medium]", "h2, h3, h4");
            String rawAddress = firstTextChain(card, "Halifax, NS", "p[class*=grey-400]", "p[class*=truncate]");
            double rent = priceFromText(text);
            int bedrooms = inferBedrooms(text);

            Location loc = Location.parse(rawAddress);
            String city = loc.city.isEmpty() || loc.city.equals("Canada") ? "Halifax" : loc.city;
            String province = loc.province.isEmpty() ? "NS" : loc.province;
            String address = streetAddress(rawAddress);
            if (address.isEmpty()) address = rawAddress;

            out.add(new Apartment(idFromUrl("rentola", href, title), title, address,
                    city, province, bedrooms, rent, "rentola.ca", absUrl(href, doc.baseUri()), ""));
        }
        return out;
    }
}
