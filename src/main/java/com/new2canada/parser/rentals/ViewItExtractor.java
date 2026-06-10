package com.new2canada.parser.rentals;

import com.new2canada.models.Apartment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

import static com.new2canada.parser.rentals.RentalExtractorUtils.*;

/**
 * ViewIt Toronto rentals search
 * ({@code https://www.viewit.ca/rentals/toronto?cid=14}).
 *
 * <p>Angular SPA with no documented stable card class — fall back to
 * {@link RentalExtractorUtils#genericPriceCards} over a few likely
 * selectors.
 */
public class ViewItExtractor implements RentalExtractor {

    @Override
    public boolean supports(String url) {
        return url != null && url.toLowerCase().contains("viewit.ca");
    }

    @Override
    public List<Apartment> extract(Document doc, String url) {
        List<Apartment> out = new ArrayList<>();
        List<Element> cards = genericPriceCards(doc,
                "[class*=listing-card]", "[class*=property-card]", "[class*=rental-card]",
                "app-listing-card", "app-property-card", "[class*=listing-item]", "li[class*=result]");

        for (Element card : cards) {
            String text = card.text();
            String href = firstAttr(card, "a[href]", "href", doc.baseUri());
            String title = firstTextChain(card, "Apartment listing", "h2, h3, h4, [class*=title], [class*=address]");
            double rent = priceFromText(text);
            int bedrooms = inferBedrooms(text);
            String address = streetAddress(text);
            if (address.isEmpty()) address = title.equals("Apartment listing") ? "Toronto, ON" : title;

            out.add(new Apartment(idFromUrl("viewit", href, title), title, address,
                    "Toronto", "ON", bedrooms, rent, "viewit.ca", absUrl(href, doc.baseUri()), ""));
        }
        return out;
    }
}
