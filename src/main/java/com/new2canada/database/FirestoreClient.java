package com.new2canada.database;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Bootstraps the Firebase Admin SDK and exposes a single {@link Firestore}
 * handle to the rest of the app.
 *
 * <p>Initialised by {@code Main} only when the project is running in
 * {@code FULL} mode. In {@code DEMO} mode this class is never touched and
 * its dependencies stay dormant on the classpath.
 */
public final class FirestoreClient {

    private static Firestore firestore;

    private FirestoreClient() {}

    /** Initialises FirebaseApp + Firestore using the given service-account key. */
    public static void init(String serviceAccountKeyPath) throws IOException {
        try (FileInputStream creds = new FileInputStream(serviceAccountKeyPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(creds))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            firestore = com.google.firebase.cloud.FirestoreClient.getFirestore();
        }
    }

    public static boolean isInitialised() { return firestore != null; }

    public static Firestore get() {
        if (firestore == null) {
            throw new IllegalStateException(
                    "Firestore is not initialised (running in DEMO mode?).");
        }
        return firestore;
    }
}
