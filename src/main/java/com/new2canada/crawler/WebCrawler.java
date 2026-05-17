package com.new2canada.crawler;

import com.new2canada.parser.HTMLParser;
import org.jsoup.nodes.Document;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Abstract base for all category-specific crawlers.
 *
 * <p>Implements the classic <b>breadth-first search</b> walk over web pages:
 * a {@link Queue} (FIFO) is seeded with the configured URLs; each pop calls
 * {@link PoliteFetcher#fetch(String)} and hands the resulting {@link Document}
 * to the subclass via {@link #handle(Document)}.
 *
 * <p>For an ACC student project we deliberately keep the BFS depth at 0 —
 * we only fetch the seed URLs themselves, no link-following. This:
 * <ul>
 *   <li>keeps the load on remote sites tiny,</li>
 *   <li>keeps the demo deterministic,</li>
 *   <li>still demonstrates the Queue + BFS pattern the course wants.</li>
 * </ul>
 *
 * <p>Subclasses provide:
 * <ul>
 *   <li>{@link #seeds()} — list of URLs to crawl,</li>
 *   <li>{@link #handle(Document)} — what to do with the parsed page.</li>
 * </ul>
 *
 * Demonstrates: <b>Queue</b>, <b>BFS</b>, inheritance / OOP.
 */
public abstract class WebCrawler {

    protected final PoliteFetcher fetcher;
    protected final HTMLParser parser = new HTMLParser();

    /** Tracks URLs we've already visited in this run to avoid duplicates. */
    private final Set<String> visited = new HashSet<>();

    protected WebCrawler(PoliteFetcher fetcher) {
        this.fetcher = fetcher;
    }

    /** Each subclass returns its own seed URLs from {@code AppConfig}. */
    protected abstract List<String> seeds();

    /** Each subclass turns a parsed page into typed objects + persists them. */
    protected abstract void handle(Document doc);

    /** Runs the BFS crawl. Returns the number of URLs successfully fetched. */
    public int crawl() {
        Queue<String> frontier = new ArrayDeque<>(seeds());
        int fetched = 0;
        while (!frontier.isEmpty()) {
            String url = frontier.poll();
            if (url == null || visited.contains(url)) continue;
            visited.add(url);
            Document doc = fetcher.fetch(url);
            if (doc == null) continue;
            handle(doc);
            fetched++;
        }
        return fetched;
    }
}
