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
 * Realtor.ca Waterloo rentals search
 * ({@code https://www.realtor.ca/on/waterloo/rentals}).
 *
 * <p>Server-rendered cards: {@code div.listingCard.card} with
 * {@code .listingCardAddress}, {@code .listingCardPrice} and an icon strip
 * ({@code .listingCardIconCon}) where one icon's label is "Bedrooms".
 */
public class RealtorExtractor implements RentalExtractor {

    @Override
    public boolean supports(String url) {
        return url != null && url.toLowerCase().contains("realtor.ca");
    }

    @Override
    public List<Apartment> extract(Document doc, String url) {
        List<Apartment> out = new ArrayList<>();
        Elements cards = doc.select("div.listingCard.card");

        for (Element card : cards) {
            String href = firstAttr(card, "a.listingDetailsLink, a", "href", doc.baseUri());
            String address = firstText(card, ".listingCardAddress", "Waterloo, Ontario");
            String priceText = firstText(card, ".listingCardPrice", "");
            double rent = priceFromText(priceText);
            int bedrooms = bedroomsFromIcons(card);
            Location loc = Location.parse(address);
            String city = loc.city.isEmpty() || loc.city.equals("Canada") ? "Waterloo" : loc.city;
            String province = loc.province.isEmpty() ? "ON" : loc.province;
            String title = address;

            out.add(new Apartment(idFromUrl("realtor", href, address), title, address,
                    city, province, bedrooms, rent, "realtor.ca", absUrl(href, doc.baseUri()), ""));
        }
        return out;
    }

    /** Finds the icon group labelled "Bedrooms" and reads its number. */
    private static int bedroomsFromIcons(Element card) {
        for (Element icon : card.select(".listingCardIconCon")) {
            String label = firstText(icon, ".listingCardIconText", "");
            if (label.toLowerCase().contains("bedroom")) {
                String num = firstText(icon, ".listingCardIconNum", "1");
                try { return (int) Double.parseDouble(num.trim()); }
                catch (NumberFormatException ignored) {}
            }
        }
        return inferBedrooms(card.text());
    }
}
