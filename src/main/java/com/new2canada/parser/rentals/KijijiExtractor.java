package com.new2canada.parser.rentals;

import com.new2canada.models.Apartment;
import com.new2canada.utils.Location;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import static com.new2canada.parser.rentals.RentalExtractorUtils.*;

/**
 * Kijiji apartments &amp; condos — national listing
 * ({@code https://www.kijiji.ca/b-apartments-condos/canada/c37l0}).
 *
 * <p>Cards expose a {@code data-testid} based markup; older Kijiji pages used
 * {@code li.regular-ad}/{@code li.top-ad}, kept here as a fallback.
 */
public class KijijiExtractor implements RentalExtractor {

    @Override
    public boolean supports(String url) {
        return url != null && url.toLowerCase().contains("kijiji.ca");
    }

    @Override
    public List<Apartment> extract(Document doc, String url) {
        List<Apartment> out = new ArrayList<>();

        Elements cards = doc.select("section[data-testid=listing-card]");
        if (cards.isEmpty()) cards = doc.select("li.regular-ad, li.top-ad, [data-listing-id]");

        for (Element card : cards) {
            String id = card.attr("data-listingid");
            if (id.isEmpty()) id = card.attr("data-listing-id");
            if (id.isEmpty()) id = "apt-kijiji-" + Math.abs(card.text().hashCode() % 10_000_000);
            else id = "apt-kijiji-" + id;

            String title = firstTextChain(card, "Apartment listing",
                    "[data-testid=listing-title] a[data-testid=listing-link]",
                    "[data-testid=listing-title]", "h3 a", "h3", ".title a", ".title");
            String href = firstAttr(card, "[data-testid=listing-link], a.title, a", "href", doc.baseUri());
            String rawLocation = firstTextChain(card, "Canada",
                    "[data-testid=listing-location]", ".location");
            Location loc = Location.parse(rawLocation);
            String address = streetAddress(card.text());
            if (address.isEmpty()) address = rawLocation;
            String desc = firstTextChain(card, "", "[data-testid=listing-description]", ".description");
            double rent = priceFromText(firstTextChain(card, card.text(),
                    "[data-testid=listing-price]", ".price"));
            int bedrooms = inferBedrooms(card.text());

            out.add(new Apartment(id, title, address, loc.city, loc.province, bedrooms, rent,
                    "kijiji.ca", absUrl(href, doc.baseUri()), desc));
        }
        return out;
    }
}
