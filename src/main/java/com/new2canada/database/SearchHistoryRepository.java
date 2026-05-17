package com.new2canada.database;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Appends each authenticated user's queries to
 * {@code searches/{uid}/queries/{autoId}} so they can see their history on
 * /history.html across server restarts.
 */
public class SearchHistoryRepository {

    private static final String ROOT = "searches";
    private static final String SUB  = "queries";

    public void record(String uid, String query, String type, int resultsCount) {
        if (uid == null || uid.isBlank() || !FirestoreClient.isInitialised()) return;
        Map<String, Object> doc = new HashMap<>();
        doc.put("q", query);
        doc.put("type", type);
        doc.put("resultsCount", resultsCount);
        doc.put("timestamp", System.currentTimeMillis());
        try {
            ApiFuture<WriteResult> f = FirestoreClient.get()
                    .collection(ROOT).document(uid)
                    .collection(SUB).document().set(doc);
            f.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.err.println("SearchHistoryRepository.record: " + e.getMessage());
        }
    }

    /** Returns the user's most-recent N queries, newest first. */
    public List<Map<String, Object>> recent(String uid, int limit) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (uid == null || !FirestoreClient.isInitialised()) return out;
        try {
            ApiFuture<com.google.cloud.firestore.QuerySnapshot> f = FirestoreClient.get()
                    .collection(ROOT).document(uid)
                    .collection(SUB)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get();
            for (QueryDocumentSnapshot d : f.get().getDocuments()) {
                Map<String, Object> data = d.getData();
                out.add(data);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.err.println("SearchHistoryRepository.recent: " + e.getMessage());
        }
        return out;
    }
}
