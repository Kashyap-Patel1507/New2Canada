package com.new2canada.auth;

/**
 * Lightweight representation of an authenticated end-user.
 *
 * <p>Populated by {@link FirebaseAuthVerifier} after a successful
 * {@code verifyIdToken} call. The same instance is then attached to the
 * HttpExchange attributes by {@link AuthMiddleware} so handlers can read
 * {@code user.uid()} when answering per-user requests like /api/history.
 */
public class User {

    private final String uid;
    private final String email;
    private final String displayName;
    private final String photoUrl;

    public User(String uid, String email, String displayName, String photoUrl) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
    }

    public String uid()         { return uid; }
    public String email()       { return email; }
    public String displayName() { return displayName; }
    public String photoUrl()    { return photoUrl; }

    /** Anonymous user used in DEMO mode where auth is bypassed. */
    public static User anonymous() {
        return new User("anon", "anonymous@demo", "Demo User", "");
    }
}
