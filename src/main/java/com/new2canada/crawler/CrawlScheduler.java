package com.new2canada.crawler;

import com.new2canada.config.AppConfig;
import com.new2canada.search.SearchEngine;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs the apartment crawler at boot and again every
 * {@link AppConfig#CRAWL_INTERVAL_MIN} minutes.
 *
 * <p>Wrapped in try/catch so a remote-site failure never crashes the
 * scheduler thread.
 *
 * Demonstrates: scheduled tasks, polite crawling.
 */
public class CrawlScheduler {

    private final SearchEngine engine;

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "crawl-scheduler");
        t.setDaemon(true);
        return t;
    });

    public CrawlScheduler(SearchEngine engine) {
        this.engine = engine;
    }

    /** Kicks off the very first crawl immediately + recurring crawls thereafter. */
    public void start() {
        exec.scheduleAtFixedRate(this::runOnce, 0, AppConfig.CRAWL_INTERVAL_MIN, TimeUnit.MINUTES);
    }

    /** Triggers a one-off crawl right now (used by /api/refresh-crawl). */
    public void triggerNow() {
        exec.submit(this::runOnce);
    }

    public void shutdown() {
        exec.shutdownNow();
    }

    private void runOnce() {
        System.out.println("[crawler] starting refresh…");
        long started = System.currentTimeMillis();

        int total = 0;
        try (PoliteFetcher fetcher = new PoliteFetcher()) {
            total = new HousingCrawler(fetcher, engine::ingestApartment).crawl();
        } catch (Exception e) {
            System.err.println("HousingCrawler failed: " + e.getMessage());
        }

        engine.rebuildIndex();

        long secs = (System.currentTimeMillis() - started) / 1000;
        System.out.printf("[crawler] done in %ds — %d scraped pages%n", secs, total);
    }
}
