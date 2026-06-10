package com.new2canada.parser.rentals;

import com.new2canada.models.Apartment;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Dispatches an apartment search-results page to whichever
 * {@link RentalExtractor} knows that site's DOM.
 *
 * <p>One extractor per entry in {@code AppConfig.APARTMENT_SEEDS}.
 *
 * Demonstrates: <b>polymorphism / strategy pattern</b>.
 */
public class RentalExtractorRegistry {

    private final List<RentalExtractor> extractors = List.of(
            new KijijiExtractor(),
            new CraigslistExtractor(),
            new ZumperExtractor(),
            new PadMapperExtractor(),
            new ViewItExtractor(),
            new FourRentExtractor(),
            new RentSeekerExtractor(),
            new RealtorExtractor(),
            new LivRentExtractor(),
            new RentolaExtractor()
    );

    /** Finds the first extractor that handles {@code url} and runs it. Returns empty list if none match. */
    public List<Apartment> extract(Document doc, String url) {
        if (doc == null) return new ArrayList<>();
        for (RentalExtractor extractor : extractors) {
            if (extractor.supports(url)) return extractor.extract(doc, url);
        }
        return new ArrayList<>();
    }
}
