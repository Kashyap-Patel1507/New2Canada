package com.new2canada.auth;

import com.new2canada.config.RunMode;
import com.sun.net.httpserver.HttpExchange;

/**
 * Centralised "who is the caller?" helper.
 *
 * <p>The HTTP server reads the {@code Authorization} header on every API
 * request and asks this class for the {@link User}. Behaviour:
 *
 * <ul>
 *   <li>{@link RunMode#DEMO} — auth is disabled, every caller is
 *       {@link User#anonymous()}.</li>
 *   <li>{@link RunMode#FULL} — the bearer token must validate via
 *       {@link FirebaseAuthVerifier}. If it does not, this method returns
 *       {@code null} and the calling handler returns 401.</li>
 * </ul>
 */
public class AuthMiddleware {

    private final RunMode mode;
    private final FirebaseAuthVerifier verifier;

    public AuthMiddleware(RunMode mode, FirebaseAuthVerifier verifier) {
        this.mode = mode;
        this.verifier = verifier;
    }

    /** Returns the caller's {@link User} or {@code null} if rejected. */
    public User resolve(HttpExchange exchange) {
        if (mode == RunMode.DEMO) return User.anonymous();
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null) return null;
        return verifier.verify(header);
    }

    public boolean isDemo() { return mode == RunMode.DEMO; }
}
