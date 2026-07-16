package com.new2canada.parser.rentals;

import com.new2canada.models.Apartment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

import static com.new2canada.parser.rentals.RentalExtractorUtils.*;

/**
 * ViewIt Toronto rentals search
 * ({@code https://www.viewit.ca/Listings?cid=14}).
 *
 * <p>Note the seed is {@code /Listings}, not {@code /rentals/toronto} — the
 * latter is a city landing page whose only listings are five "featured" promo
 * tiles. {@code /Listings?cid=14} is the real search-results page, where each
 * hit is an {@code article.resultListing}.
 *
 * <p>ViewIt is ASP.NET WebForms and shows 5 results per page, with pagination
 * driven by {@code __doPostBack} rather than URLs — so there is no page-2 URL
 * to seed, and we take the first page only.
 */
public class ViewItExtractor implements RentalExtractor {

    @Override
    public boolean supports(String url) {
        return url != null && url.toLowerCase().contains("viewit.ca");
    }

    @Override
    public String readySelector() { return "article.resultListing"; }

    @Override
    public List<Apartment> extract(Document doc, String url) {
        List<Apartment> out = new ArrayList<>();
        for (Element card : doc.select("article.resultListing")) {
            String text = card.text();
            String href = firstAttr(card, "a[href]", "href", doc.baseUri());
            // The h5 is the street address, which doubles as the listing title.
            String title = firstTextChain(card, "Apartment listing", "h5", "h2, h3, h4");
            // "$1850 and up" — priceFromText takes the starting rent.
            double rent = priceFromText(firstTextChain(card, "", "div.resultListing-price"));
            int bedrooms = inferBedrooms(text);
            String address = streetAddress(text);
            if (address.isEmpty()) address = title.equals("Apartment listing") ? "Toronto, ON" : title;

            out.add(new Apartment(idFromUrl("viewit", href, title), title, address,
                    "Toronto", "ON", bedrooms, rent, "viewit.ca", absUrl(href, doc.baseUri()), ""));
        }
        return out;
    }
}
