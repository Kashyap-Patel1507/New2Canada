package com.new2canada.parser.rentals;

import com.new2canada.models.Apartment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

import static com.new2canada.parser.rentals.RentalExtractorUtils.*;

/**
 * 4Rent.ca Edmonton apartments search
 * ({@code https://4rent.ca/apartments-for-rent/ab/edmonton/3352}).
 *
 * <p>Vue-rendered results grid with no documented stable card class — fall
 * back to {@link RentalExtractorUtils#genericPriceCards} over likely
 * selectors.
 */
public class FourRentExtractor implements RentalExtractor {

    @Override
    public boolean supports(String url) {
        return url != null && url.toLowerCase().contains("4rent.ca");
    }

    @Override
    public List<Apartment> extract(Document doc, String url) {
        List<Apartment> out = new ArrayList<>();
        List<Element> cards = genericPriceCards(doc,
                "[class*=listing-card]", "[class*=property-card]", "[class*=result-card]",
                "[class*=listing-item]", "[class*=rental-item]", "li[class*=result]", "article");

        for (Element card : cards) {
            String text = card.text();
            String href = firstAttr(card, "a[href]", "href", doc.baseUri());
            String title = firstTextChain(card, "Apartment listing", "h2, h3, h4, [class*=title], [class*=address]");
            double rent = priceFromText(text);
            int bedrooms = inferBedrooms(text);
            String address = streetAddress(text);
            if (address.isEmpty()) address = title.equals("Apartment listing") ? "Edmonton, AB" : title;

            out.add(new Apartment(idFromUrl("4rent", href, title), title, address,
                    "Edmonton", "AB", bedrooms, rent, "4rent.ca", absUrl(href, doc.baseUri()), ""));
        }
        return out;
    }
}
