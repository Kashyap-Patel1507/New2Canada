package com.new2canada.parser.rentals;

import com.new2canada.models.Apartment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

import static com.new2canada.parser.rentals.RentalExtractorUtils.*;

/**
 * Zumper Calgary apartments search
 * ({@code https://www.zumper.com/apartments-for-rent/calgary-ab}).
 *
 * <p>Zumper is a JS-rendered React app. Its CSS-module class names carry a
 * per-build hash suffix ({@code ListingCardContentSection_longTermPrice__DM1kY}),
 * so we anchor on the stable {@code data-testid=listing-card} hook and
 * prefix-match the inner class names.
 *
 * <p>Deliberately <i>not</i> keyed off {@code /address/} links — those are the
 * SEO "nearby addresses" accordion in the page footer, which carries no price.
 */
public class ZumperExtractor implements RentalExtractor {

    @Override
    public boolean supports(String url) {
        return url != null && url.toLowerCase().contains("zumper.com");
    }

    @Override
    public String readySelector() { return "[data-testid=listing-card]"; }

    @Override
    public List<Apartment> extract(Document doc, String url) {
        List<Apartment> out = new ArrayList<>();
        for (Element card : doc.select("[data-testid=listing-card]")) {
            String href = firstAttr(card, "a[href]", "href", doc.baseUri());
            String title = firstTextChain(card, "Apartment listing",
                    "[class*=detailLinkText]", "h2, h3, h4");
            // "$1,610–$2,200" for a range — priceFromText takes the low end.
            double rent = priceFromText(firstTextChain(card, "", "[class*=longTermPrice]", "[class*=price]"));
            int bedrooms = inferBedrooms(firstTextChain(card, "", "[class*=bedsRangeText]"));
            String address = firstTextChain(card, "", "[class*=fullAddress]", "[class*=addressText]");
            if (address.isEmpty()) address = title.equals("Apartment listing") ? "Calgary, AB" : title;

            out.add(new Apartment(idFromUrl("zumper", href, title), title, address,
                    "Calgary", "AB", bedrooms, rent, "zumper.com", absUrl(href, doc.baseUri()), ""));
        }
        return out;
    }
}
