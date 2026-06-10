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
 * Craigslist Montreal apartments search
 * ({@code https://montreal.craigslist.org/search/apa}).
 *
 * <p>Static, server-rendered results — each listing is a
 * {@code li.cl-static-search-result}.
 */
public class CraigslistExtractor implements RentalExtractor {

    @Override
    public boolean supports(String url) {
        return url != null && url.toLowerCase().contains("craigslist.org");
    }

    @Override
    public List<Apartment> extract(Document doc, String url) {
        List<Apartment> out = new ArrayList<>();
        Elements cards = doc.select("li.cl-static-search-result");

        for (Element card : cards) {
            String title = firstTextChain(card, "Apartment listing", "div.title", ".title");
            String href = firstAttr(card, "a", "href", doc.baseUri());
            String priceText = firstTextChain(card, "", "div.price", ".price");
            double rent = priceFromText(priceText);
            String rawLocation = firstTextChain(card, "Montreal, QC", "div.location", ".location");
            Location loc = Location.parse(rawLocation);
            String city = loc.city.isEmpty() || loc.city.equals("Canada") ? "Montreal" : loc.city;
            String province = loc.province.isEmpty() ? "QC" : loc.province;
            String address = streetAddress(card.text());
            if (address.isEmpty()) address = rawLocation;
            int bedrooms = inferBedrooms(card.attr("title") + " " + title);
            String id = idFromUrl("cl", href, title);

            out.add(new Apartment(id, title, address, city, province, bedrooms, rent,
                    "craigslist.org", absUrl(href, doc.baseUri()), ""));
        }
        return out;
    }
}
