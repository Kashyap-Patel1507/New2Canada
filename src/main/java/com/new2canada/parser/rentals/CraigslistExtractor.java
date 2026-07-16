package com.new2canada.parser.rentals;

import com.new2canada.models.Apartment;
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
 * <p>Craigslist ships two different markups: {@code li.cl-static-search-result}
 * when JavaScript is off, and a {@code div.cl-search-result} grid once its JS
 * runs. We fetch through headless Chrome, so we get the latter — but accept
 * both so the extractor still works against a plain HTTP fetch.
 */
public class CraigslistExtractor implements RentalExtractor {

    @Override
    public boolean supports(String url) {
        return url != null && url.toLowerCase().contains("craigslist.org");
    }

    @Override
    public List<Apartment> extract(Document doc, String url) {
        List<Apartment> out = new ArrayList<>();
        Elements cards = doc.select("div.cl-search-result, li.cl-static-search-result");

        for (Element card : cards) {
            // JS grid puts the title in a.posting-title (and on the card's own
            // title attr); the static markup uses div.title.
            String title = firstTextChain(card, "", "a.posting-title .label", "a.posting-title", "div.title", ".title");
            if (title.isEmpty()) title = card.attr("title");
            if (title.isEmpty()) title = "Apartment listing";
            String href = firstAttr(card, "a.posting-title[href], a.main[href], a[href]", "href", doc.baseUri());
            String priceText = firstTextChain(card, "", "span.priceinfo", "div.price", ".price");
            double rent = priceFromText(priceText);
            // Craigslist's location field is a neighbourhood ("Côte Des Neiges"),
            // not a city, and the French pages spell it "Montréal". Parsing it as
            // the city split one seed across a dozen city values and made
            // search-by-city miss rows, so the seed's own city is authoritative
            // here — the neighbourhood is kept below as the address.
            String rawLocation = firstTextChain(card, "Montreal, QC", "span.result-location", "div.location", ".location");
            String city = "Montreal";
            String province = "QC";
            String address = streetAddress(card.text());
            if (address.isEmpty()) address = rawLocation;
            int bedrooms = inferBedrooms(firstTextChain(card, "", "span.post-bedrooms")
                    + " " + card.attr("title") + " " + title);
            String id = idFromUrl("cl", href, title);

            out.add(new Apartment(id, title, address, city, province, bedrooms, rent,
                    "craigslist.org", absUrl(href, doc.baseUri()), ""));
        }
        return out;
    }
}
