package com.new2canada.database;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.WriteResult;
import com.new2canada.auth.User;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Stores a tiny profile row in {@code users/{uid}} the first time someone
 * signs in, and updates {@code lastLoginAt} on every subsequent call.
 *
 * <p>Uses {@code SetOptions.merge()} so we never blow away fields the user
 * has added (favourites, etc.).
 */
public class UserRepository {

    private static final String COLLECTION = "users";

    public void upsertOnLogin(User u) {
        if (u == null || !FirestoreClient.isInitialised()) return;
        Map<String, Object> doc = new HashMap<>();
        doc.put("email", u.email());
        doc.put("displayName", u.displayName());
        doc.put("photoUrl", u.photoUrl());
        doc.put("lastLoginAt", System.currentTimeMillis());
        doc.put("createdAt", System.currentTimeMillis()); // overwritten by merge on existing docs

        try {
            ApiFuture<WriteResult> f = FirestoreClient.get()
                    .collection(COLLECTION).document(u.uid())
                    .set(doc, SetOptions.merge());
            f.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.err.println("UserRepository.upsertOnLogin: " + e.getMessage());
        }
    }
}
