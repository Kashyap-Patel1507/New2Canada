package com.new2canada.crawler;

import com.new2canada.config.AppConfig;
import com.new2canada.models.Apartment;
import com.new2canada.parser.DataExtractor;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Crawls Canadian rental sites (rentals.ca, realtor.ca-style pages) and
 * yields {@link Apartment} POJOs.
 *
 * <p>Why a dedicated subclass? Each domain has its own DOM structure, so a
 * one-size-fits-all extractor would be brittle. Concrete crawlers stay small
 * — they just plug a {@link DataExtractor} into the BFS engine inherited
 * from {@link WebCrawler}.
 *
 * Demonstrates: <b>OOP inheritance</b>, single-responsibility.
 */
public class HousingCrawler extends WebCrawler {

    private final DataExtractor extractor = new DataExtractor();
    private final Consumer<Apartment> sink;

    public HousingCrawler(PoliteFetcher fetcher, Consumer<Apartment> sink) {
        super(fetcher);
        this.sink = sink;
    }

    @Override
    protected List<String> seeds() {
        return new ArrayList<>(AppConfig.APARTMENT_SEEDS);
    }

    @Override
    protected void handle(Document doc) {
        String source = sourceOf(doc.baseUri());
        int count = 0;
        for (Apartment a : extractor.extractApartments(doc, source)) {
            sink.accept(a);
            if (++count >= AppConfig.MAX_ITEMS_PER_SEED) break;
        }
    }

    private static String sourceOf(String url) {
        if (url == null) return "unknown";
        if (url.contains("kijiji.ca"))      return "kijiji.ca";
        if (url.contains("craigslist.org")) return "craigslist.org";
        if (url.contains("rentals.ca"))     return "rentals.ca";
        if (url.contains("realtor.ca"))     return "realtor.ca";
        return url;
    }
}
