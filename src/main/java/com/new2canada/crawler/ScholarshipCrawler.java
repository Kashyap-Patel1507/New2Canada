package com.new2canada.crawler;

import com.new2canada.config.AppConfig;
import com.new2canada.models.Scholarship;
import com.new2canada.parser.DataExtractor;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Crawls Canadian scholarship & financial-aid sources — the Government of
 * Canada portal at scholarships.gc.ca, ESDC student-aid pages, Universities
 * Canada, and Wikipedia articles for major named awards.
 *
 * Demonstrates: <b>OOP inheritance</b> (same hierarchy as BankCrawler /
 * MobileCrawler), <b>BFS over a Queue of seed URLs</b> (in the superclass).
 */
public class ScholarshipCrawler extends WebCrawler {

    private final DataExtractor extractor = new DataExtractor();
    private final Consumer<Scholarship> sink;

    public ScholarshipCrawler(PoliteFetcher fetcher, Consumer<Scholarship> sink) {
        super(fetcher);
        this.sink = sink;
    }

    @Override
    protected List<String> seeds() {
        return new ArrayList<>(AppConfig.SCHOLARSHIP_SEEDS);
    }

    @Override
    protected void handle(Document doc) {
        String url = doc.baseUri();
        String src = sourceOf(url);
        for (Scholarship s : extractor.extractScholarships(doc, src)) sink.accept(s);
    }

    private static String sourceOf(String url) {
        if (url == null) return "unknown";
        if (url.contains("scholarships.gc.ca")) return "scholarships.gc.ca";
        if (url.contains("canada.ca"))          return "canada.ca";
        if (url.contains("univcan.ca"))         return "univcan.ca";
        if (url.contains("wikipedia"))          return "wikipedia.org";
        return url;
    }
}
