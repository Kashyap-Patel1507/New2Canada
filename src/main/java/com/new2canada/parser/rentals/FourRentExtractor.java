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
 * <p>Nuxt/Vue-rendered results grid. Each listing is a
 * {@code div.property-result}; the surrounding layout is Tailwind utilities
 * but the card, its {@code .result-info} body and the title link carry stable
 * names.
 *
 * <p>The grid only populates once the page's WebGL map initialises — see the
 * SwiftShader flags in {@link com.new2canada.crawler.PoliteFetcher}. Without
 * them the list sits at "Loading..." and this extractor sees nothing.
 */
public class FourRentExtractor implements RentalExtractor {

    @Override
    public boolean supports(String url) {
        return url != null && url.toLowerCase().contains("4rent.ca");
    }

    @Override
    public String readySelector() { return "div.property-result"; }

    @Override
    public List<Apartment> extract(Document doc, String url) {
        List<Apartment> out = new ArrayList<>();
        for (Element card : doc.select("div.property-result")) {
            String text = card.text();
            String href = firstAttr(card, "a[href^=/apartment-for-rent/]", "href", doc.baseUri());
            String title = firstTextChain(card, "Apartment listing",
                    "a.text-teal-blue", "a[href^=/apartment-for-rent/]", "h2, h3, h4");
            // "$1,070+" — the card's first price is the starting rent.
            double rent = priceFromText(text);
            // Bed/bath sit in their own list items ("0 Beds", "1 Bath"); read the
            // beds one directly so "1 Bath" can't be mistaken for a bedroom count.
            int bedrooms = inferBedrooms(firstTextChain(card, "", "li:contains(Bed)"));
            // The city line also carries .text-bluish-grey, so exclude the bold one.
            String address = firstTextChain(card, "", "div.text-bluish-grey:not(.font-bold)");
            if (address.isEmpty()) address = streetAddress(text);
            if (address.isEmpty()) address = title.equals("Apartment listing") ? "Edmonton, AB" : title;

            out.add(new Apartment(idFromUrl("4rent", href, title), title, address,
                    "Edmonton", "AB", bedrooms, rent, "4rent.ca", absUrl(href, doc.baseUri()), ""));
        }
        return out;
    }
}
