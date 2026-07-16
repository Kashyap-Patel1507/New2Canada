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
 *
 * <p>The card markup is Tailwind utility classes only, so there is nothing
 * stable to select on; the {@code /buildings/} detail links are the reliable
 * hook. Note the listing grid only renders once the page's WebGL map
 * initialises — see the SwiftShader flags in {@link
 * com.new2canada.crawler.PoliteFetcher}.
 */
public class PadMapperExtractor implements RentalExtractor {

    /** Listing-detail links, e.g. /buildings/p217502/forest-ridge-apartments-at-2380-baseline-rd. */
    private static final Pattern LISTING_HREF = Pattern.compile("^/buildings/");

    /** Ancestor levels from the detail link up to the element holding the whole card. */
    private static final int CARD_DEPTH = 6;

    @Override
    public boolean supports(String url) {
        return url != null && url.toLowerCase().contains("padmapper.com");
    }

    @Override
    public String readySelector() { return "a[href^='/buildings/']"; }

    @Override
    public List<Apartment> extract(Document doc, String url) {
        List<Apartment> out = new ArrayList<>();
        for (Element card : cardsByAnchor(doc, LISTING_HREF, CARD_DEPTH)) {
            String text = card.text();
            String href = firstAttr(card, "a[href^=/buildings/]", "href", doc.baseUri());
            String title = firstTextChain(card, "Apartment listing",
                    "a[href^=/buildings/]", "h2, h3, h4, [class*=title]");
            // "$1,552–$2,599" for a range — priceFromText takes the low end.
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
