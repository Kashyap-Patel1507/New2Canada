package com.new2canada.crawler;

import com.new2canada.config.AppConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Single-threaded, polite HTTP fetcher.
 *
 * <p>Adds three guarantees that distinguish a student project from a hostile
 * scraper:
 *
 * <ul>
 *   <li>A descriptive User-Agent identifying the project and course.</li>
 *   <li>A {@link AppConfig#FETCH_DELAY_MS} delay between hits to the
 *       <i>same host</i>, so a burst of crawler activity never floods a
 *       single site.</li>
 *   <li>A short timeout — if a remote site is slow we fail fast and let the
 *       Firestore cache take over, instead of blocking a request thread.</li>
 * </ul>
 *
 * Demonstrates: respectful real-world crawling, simple per-host throttling
 * via a {@link HashMap}.
 */
public class PoliteFetcher {

    /** host → epoch-millis of the last successful fetch. */
    private final Map<String, Long> lastFetchPerHost = new HashMap<>();

    /** Fetches the URL as a parsed Jsoup {@link Document}, or {@code null} on failure. */
    public synchronized Document fetch(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            throttleSameHost(url);
            Document doc = Jsoup.connect(url)
                    .userAgent(AppConfig.USER_AGENT)
                    .timeout(15_000)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .get();
            recordFetch(url);
            return doc;
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("PoliteFetcher: " + url + " -> " + e.getMessage());
            return null;
        }
    }

    private void throttleSameHost(String url) {
        String host = hostOf(url);
        Long last = lastFetchPerHost.get(host);
        if (last == null) return;
        long waitFor = AppConfig.FETCH_DELAY_MS - (System.currentTimeMillis() - last);
        if (waitFor > 0) {
            try { Thread.sleep(waitFor); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    private void recordFetch(String url) {
        lastFetchPerHost.put(hostOf(url), System.currentTimeMillis());
    }

    private static String hostOf(String url) {
        try { return new java.net.URI(url).getHost(); }
        catch (Exception e) { return url; }
    }
}
