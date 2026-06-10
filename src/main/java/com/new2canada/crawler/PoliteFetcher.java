package com.new2canada.crawler;

import com.new2canada.config.AppConfig;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Single-threaded, polite headless-browser fetcher.
 *
 * <p>Many of the rental sites we scrape (Zumper, PadMapper, Liv.rent, ...)
 * render their listings with JavaScript, so a plain HTTP GET returns an
 * empty shell. This fetcher drives a headless Chrome instance and hands
 * Jsoup the fully-rendered page source instead.
 *
 * <p>Adds three guarantees that distinguish a student project from a hostile
 * scraper:
 *
 * <ul>
 *   <li>A descriptive User-Agent identifying the project and course.</li>
 *   <li>A {@link AppConfig#FETCH_DELAY_MS} delay between hits to the
 *       <i>same host</i>, so a burst of crawler activity never floods a
 *       single site.</li>
 *   <li>A short page-load timeout — if a remote site is slow we fail fast and
 *       let the Firestore cache take over, instead of blocking a request
 *       thread.</li>
 * </ul>
 *
 * Demonstrates: respectful real-world crawling, simple per-host throttling
 * via a {@link HashMap}.
 */
public class PoliteFetcher implements AutoCloseable {

    /** host → epoch-millis of the last successful fetch. */
    private final Map<String, Long> lastFetchPerHost = new HashMap<>();

    private WebDriver driver;

    public PoliteFetcher() {
        WebDriverManager.chromedriver().setup();
        this.driver = newDriver();
    }

    private static WebDriver newDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--headless=new",
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--window-size=1920,1080",
                "user-agent=" + AppConfig.USER_AGENT);
        WebDriver d = new ChromeDriver(options);
        d.manage().timeouts().pageLoadTimeout(java.time.Duration.ofSeconds(20));
        return d;
    }

    /** Fetches the URL as a parsed Jsoup {@link Document}, or {@code null} on failure. */
    public synchronized Document fetch(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            throttleSameHost(url);
            driver.get(url);
            // Give client-side rendered listings (Zumper, PadMapper, Liv.rent, ...)
            // a moment to populate the DOM after the initial page load.
            Thread.sleep(2000);
            String html = driver.getPageSource();
            recordFetch(url);
            return Jsoup.parse(html, url);
        } catch (Exception e) {
            System.err.println("PoliteFetcher: " + url + " -> " + e.getMessage());
            // A page-load timeout or crash can leave the Chrome session dead
            // for every subsequent fetch — recreate it so later seeds in the
            // same crawl still get a chance.
            if (!isSessionAlive()) {
                try { driver.quit(); } catch (Exception ignored) {}
                try { driver = newDriver(); }
                catch (Exception ex) { System.err.println("PoliteFetcher: failed to restart Chrome -> " + ex.getMessage()); }
            }
            return null;
        }
    }

    private boolean isSessionAlive() {
        try { driver.getCurrentUrl(); return true; }
        catch (Exception e) { return false; }
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

    /** Shuts down the underlying headless Chrome process. */
    @Override
    public void close() {
        try { driver.quit(); }
        catch (Exception ignored) {}
    }
}
