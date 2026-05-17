package com.new2canada.config;

/**
 * Two operating modes:
 *
 * <ul>
 *   <li>{@link #FULL} — Firebase service-account key is present, Firestore +
 *       Google Sign-In are active.</li>
 *   <li>{@link #DEMO} — no credentials yet, the app runs in-memory only,
 *       authentication is bypassed, and a banner is shown in the UI.</li>
 * </ul>
 *
 * The choice is made once in {@code Main} at startup.
 */
public enum RunMode {
    FULL,
    DEMO
}
