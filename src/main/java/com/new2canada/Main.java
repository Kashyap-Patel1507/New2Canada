package com.new2canada;

import com.new2canada.auth.AuthMiddleware;
import com.new2canada.auth.FirebaseAuthVerifier;
import com.new2canada.config.AppConfig;
import com.new2canada.config.RunMode;
import com.new2canada.crawler.CrawlScheduler;
import com.new2canada.database.FirestoreClient;
import com.new2canada.database.ListingRepository;
import com.new2canada.database.SearchHistoryRepository;
import com.new2canada.database.UserRepository;
import com.new2canada.search.SearchEngine;
import com.new2canada.server.ApiHandler;
import com.new2canada.server.StaticFileHandler;
import com.new2canada.server.WebServer;

import java.io.File;
import java.io.IOException;

/**
 * Application entry point.
 *
 * <p>The boot sequence is short and explicit so it is easy to talk through
 * in viva:
 *
 * <ol>
 *   <li>Detect whether the Firebase service-account key is present →
 *       enter {@code FULL} or {@code DEMO} mode.</li>
 *   <li>If {@code FULL}: initialise Firebase Admin → Firestore.</li>
 *   <li>Construct the {@link SearchEngine} (always in-memory; Firestore is
 *       a side-cache, not the primary store).</li>
 *   <li>Start the {@link CrawlScheduler} — it runs once at boot and every
 *       6h thereafter.</li>
 *   <li>Start the {@link WebServer} on port {@code 8080}.</li>
 * </ol>
 */
public class Main {

    public static void main(String[] args) throws IOException {
        System.out.println("=== New2Canada Search Engine ===");

        RunMode mode = decideMode();
        ListingRepository listingRepo = null;
        UserRepository userRepo = null;
        SearchHistoryRepository historyRepo = null;
        FirebaseAuthVerifier verifier = null;

        if (mode == RunMode.FULL) {
            try {
                FirestoreClient.init(AppConfig.SERVICE_ACCOUNT_KEY_PATH);
                listingRepo = new ListingRepository();
                userRepo = new UserRepository();
                historyRepo = new SearchHistoryRepository();
                verifier = new FirebaseAuthVerifier();
                System.out.println("FULL mode — Firebase + Firestore initialised.");
            } catch (IOException e) {
                System.err.println("Firebase init failed (" + e.getMessage()
                        + ") — falling back to DEMO mode.");
                mode = RunMode.DEMO;
            }
        }
        if (mode == RunMode.DEMO) {
            System.out.println("DEMO mode — no serviceAccountKey.json found.");
            System.out.println("    Auth is disabled, Firestore writes are skipped.");
            System.out.println("    Add a serviceAccountKey.json next to pom.xml to enable FULL mode.");
        }

        SearchEngine engine = new SearchEngine(mode, listingRepo, historyRepo);

        // Hydrate the in-memory index from Firestore *before* starting the
        // crawler. This is the difference between "blank page for 2 min while
        // we re-scrape everything" and "real listings in milliseconds". The
        // background crawl then refreshes the index — no UX wait.
        if (mode == RunMode.FULL) {
            long t = System.currentTimeMillis();
            int n = engine.hydrateFromCache();
            System.out.printf("Cache hydrated: %d listings in %d ms%n",
                    n, System.currentTimeMillis() - t);
        }

        // Kick off the live crawl in the background. start() returns
        // immediately (scheduleAtFixedRate is non-blocking).
        CrawlScheduler scheduler = new CrawlScheduler(engine);
        scheduler.start();

        AuthMiddleware auth = new AuthMiddleware(mode, verifier);
        ApiHandler api = new ApiHandler(engine, auth, userRepo, historyRepo, scheduler);
        StaticFileHandler statics = new StaticFileHandler();

        WebServer server = new WebServer(api, statics);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down…");
            scheduler.shutdown();
            server.stop();
        }, "shutdown-hook"));

        System.out.println("Open http://localhost:" + AppConfig.HTTP_PORT + " in Chrome.");
    }

    private static RunMode decideMode() {
        File key = new File(AppConfig.SERVICE_ACCOUNT_KEY_PATH);
        return (key.exists() && key.isFile()) ? RunMode.FULL : RunMode.DEMO;
    }
}
