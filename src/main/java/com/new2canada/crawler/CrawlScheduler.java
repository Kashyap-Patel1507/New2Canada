package com.new2canada.crawler;

import com.new2canada.config.AppConfig;
import com.new2canada.search.SearchEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs every crawler at boot and again every
 * {@link AppConfig#CRAWL_INTERVAL_MIN} minutes.
 *
 * <p>Pipeline per run:
 * <ol>
 *   <li>Seed curated bank + mobile plans synchronously — these land in the
 *       index in &lt;100 ms so /banks.html and /mobile.html have data the
 *       moment the user opens them.</li>
 *   <li>Run all five live crawlers concurrently on a small thread pool.
 *       Each {@link PoliteFetcher} per-host throttle still applies, but
 *       different categories hit different hosts so we get a roughly
 *       category-count speed-up.</li>
 *   <li>Once the last live crawl returns, call
 *       {@link SearchEngine#rebuildIndex()}.</li>
 * </ol>
 *
 * <p>Wrapped in try/catch so a remote-site failure on one category never
 * blocks the others.
 *
 * Demonstrates: <b>ExecutorService</b>, scheduled tasks, concurrent crawling.
 */
public class CrawlScheduler {

    private final SearchEngine engine;

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "crawl-scheduler");
        t.setDaemon(true);
        return t;
    });

    /** Pool that runs the five live crawlers concurrently. */
    private final ExecutorService crawlPool = Executors.newFixedThreadPool(5, r -> {
        Thread t = new Thread(r, "crawl-worker");
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
        crawlPool.shutdownNow();
    }

    private void runOnce() {
        System.out.println("[crawler] starting refresh…");
        long started = System.currentTimeMillis();

        // 1. Curated data first — instant, no network. The user opening
        //    /banks.html or /mobile.html within the first few seconds will
        //    immediately see real plan data instead of a blank page.
        int curated = 0;
        try { curated += CuratedBankPlans.seed(engine::ingestBank); }
        catch (Exception e) { System.err.println("CuratedBankPlans seed failed: " + e.getMessage()); }
        try { curated += CuratedMobilePlans.seed(engine::ingestMobile); }
        catch (Exception e) { System.err.println("CuratedMobilePlans seed failed: " + e.getMessage()); }
        engine.rebuildIndex();
        System.out.printf("[crawler] curated seed: %d entries (%.1fs)%n",
                curated, (System.currentTimeMillis() - started) / 1000.0);

        // 2. Live crawlers in parallel — each gets its own PoliteFetcher
        //    so the per-host throttle is independent. PoliteFetcher.fetch is
        //    synchronized; sharing one instance would force all crawlers to
        //    queue up at that lock, defeating the parallelism.
        List<Callable<Integer>> tasks = new ArrayList<>();
        tasks.add(named("HousingCrawler",      () -> new HousingCrawler(new PoliteFetcher(), engine::ingestApartment).crawl()));
        tasks.add(named("JobCrawler",          () -> new JobCrawler(new PoliteFetcher(), engine::ingestJob).crawl()));
        tasks.add(named("BankCrawler",         () -> new BankCrawler(new PoliteFetcher(), engine::ingestBank).crawl()));
        tasks.add(named("MobileCrawler",       () -> new MobileCrawler(new PoliteFetcher(), engine::ingestMobile).crawl()));
        tasks.add(named("ScholarshipCrawler",  () -> new ScholarshipCrawler(new PoliteFetcher(), engine::ingestScholarship).crawl()));

        int total = 0;
        try {
            List<Future<Integer>> futures = crawlPool.invokeAll(tasks);
            for (Future<Integer> f : futures) {
                try { total += f.get(); }
                catch (Exception e) { System.err.println("crawl task failed: " + e.getMessage()); }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[crawler] interrupted: " + e.getMessage());
        }

        engine.rebuildIndex();

        long secs = (System.currentTimeMillis() - started) / 1000;
        System.out.printf("[crawler] done in %ds — %d curated + %d scraped pages%n",
                secs, curated, total);
    }

    /** Wraps a Callable with a try/catch so one failure can't kill invokeAll. */
    private static Callable<Integer> named(String name, Callable<Integer> body) {
        return () -> {
            try { return body.call(); }
            catch (Exception e) { System.err.println(name + " failed: " + e.getMessage()); return 0; }
        };
    }
}
