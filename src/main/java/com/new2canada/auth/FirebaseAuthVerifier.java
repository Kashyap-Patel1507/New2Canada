package com.new2canada.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;

/**
 * Verifies the Firebase ID token sent by the browser.
 *
 * <p>The frontend (via the Firebase Auth JS SDK) signs the user in with
 * Google, gets a short-lived JWT ID token, and attaches it to every
 * protected API call as {@code Authorization: Bearer <token>}. Here on the
 * server side we hand that token to the Firebase Admin SDK, which:
 *
 * <ol>
 *   <li>Verifies the JWT signature against Google's public keys,</li>
 *   <li>Checks the {@code aud} (audience), {@code iss} (issuer), and
 *       expiry,</li>
 *   <li>Returns the decoded {@link FirebaseToken} with the verified UID.</li>
 * </ol>
 *
 * <p>If anything fails — bad signature, expired token, project mismatch —
 * we return {@code null} and {@link AuthMiddleware} sends a 401.
 */
public class FirebaseAuthVerifier {

    /** Returns a verified {@link User} or {@code null} if verification failed. */
    public User verify(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) return null;
        String token = bearerToken.startsWith("Bearer ") ? bearerToken.substring(7) : bearerToken;
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid    = decoded.getUid();
            String email  = decoded.getEmail();
            String name   = decoded.getName();
            String photo  = decoded.getPicture();

            // Fetch the full record only if the token didn't carry a name yet
            // (e.g. brand-new accounts whose profile hasn't been refreshed).
            if (name == null || name.isBlank()) {
                try {
                    UserRecord rec = FirebaseAuth.getInstance().getUser(uid);
                    name  = rec.getDisplayName();
                    photo = rec.getPhotoUrl();
                } catch (FirebaseAuthException ignored) { /* keep nulls */ }
            }
            return new User(uid, email, name, photo);
        } catch (FirebaseAuthException e) {
            System.err.println("FirebaseAuthVerifier: token rejected — " + e.getMessage());
            return null;
        }
    }
}
