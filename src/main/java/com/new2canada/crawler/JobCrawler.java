package com.new2canada.crawler;

import com.new2canada.config.AppConfig;
import com.new2canada.models.Job;
import com.new2canada.parser.DataExtractor;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Crawls Indeed.ca (and structurally similar Canadian job boards) for
 * part-time / student-friendly postings.
 *
 * Demonstrates: <b>OOP inheritance</b>.
 */
public class JobCrawler extends WebCrawler {

    private final DataExtractor extractor = new DataExtractor();
    private final Consumer<Job> sink;

    public JobCrawler(PoliteFetcher fetcher, Consumer<Job> sink) {
        super(fetcher);
        this.sink = sink;
    }

    @Override
    protected List<String> seeds() {
        return new ArrayList<>(AppConfig.JOB_SEEDS);
    }

    @Override
    protected void handle(Document doc) {
        String source = sourceOf(doc.baseUri());
        int count = 0;
        for (Job j : extractor.extractJobs(doc, source)) {
            sink.accept(j);
            if (++count >= AppConfig.MAX_ITEMS_PER_SEED) break;
        }
    }

    private static String sourceOf(String url) {
        if (url == null) return "unknown";
        if (url.contains("jobbank.gc.ca")) return "jobbank.gc.ca";
        if (url.contains("eluta"))         return "eluta.ca";
        if (url.contains("indeed"))        return "indeed.ca";
        if (url.contains("glassdoor"))     return "glassdoor.ca";
        return url;
    }
}
