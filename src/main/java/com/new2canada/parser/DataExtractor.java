package com.new2canada.parser;

import com.new2canada.models.Apartment;
import com.new2canada.parser.rentals.RentalExtractorRegistry;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Pulls {@link Apartment} POJOs out of a Jsoup {@link Document}.
 *
 * <p>Delegates to {@link RentalExtractorRegistry} — one
 * {@link com.new2canada.parser.rentals.RentalExtractor} per configured
 * rental site.
 *
 * <p>If the upstream site changes its DOM the extractor returns an empty list
 * and the caller falls back to the Firestore cache, so the UI keeps working.
 *
 * Demonstrates: <b>CSS selectors</b>, <b>polymorphism / strategy pattern</b>.
 */
public class DataExtractor {

    private final RentalExtractorRegistry rentalExtractors = new RentalExtractorRegistry();

    public List<Apartment> extractApartments(Document doc, String source) {
        if (doc == null) return new ArrayList<>();
        return rentalExtractors.extract(doc, doc.baseUri());
    }
}
