package com.new2canada.parser.rentals;

import com.new2canada.models.Apartment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import static com.new2canada.parser.rentals.RentalExtractorUtils.*;

/**
 * RentSeeker Winnipeg apartments search
 * ({@code https://www.rentseeker.ca/rentals/apartments/manitoba/winnipeg}).
 *
 * <p>Listing cards render with a {@code .listing-card} class (Vue/Nuxt app);
 * fall back to a generic price-card scan if that markup isn't present.
 */
public class RentSeekerExtractor implements RentalExtractor {

    @Override
    public boolean supports(String url) {
        return url != null && url.toLowerCase().contains("rentseeker.ca");
    }

    @Override
    public List<Apartment> extract(Document doc, String url) {
        List<Apartment> out = new ArrayList<>();
        Elements cards = doc.select(".listing-card");
        List<Element> cardList = cards.isEmpty()
                ? genericPriceCards(doc, "[class*=listing-card]", "[class*=property-card]",
                        "[class*=rental-card]", "[class*=listing-item]", "article")
                : new ArrayList<>(cards);

        for (Element card : cardList) {
            String text = card.text();
            String href = firstAttr(card, "a[href]", "href", doc.baseUri());
            String title = firstTextChain(card, "Apartment listing",
                    "h2, h3, h4, [class*=title], [class*=name], [class*=address]");
            double rent = priceFromText(text);
            int bedrooms = inferBedrooms(text);
            String address = streetAddress(text);
            if (address.isEmpty()) address = title.equals("Apartment listing") ? "Winnipeg, MB" : title;

            out.add(new Apartment(idFromUrl("rentseeker", href, title), title, address,
                    "Winnipeg", "MB", bedrooms, rent, "rentseeker.ca", absUrl(href, doc.baseUri()), ""));
        }
        return out;
    }
}
