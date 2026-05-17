package com.new2canada.database;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteResult;
import com.new2canada.models.Apartment;
import com.new2canada.models.BankPlan;
import com.new2canada.models.Job;
import com.new2canada.models.MobilePlan;
import com.new2canada.models.Scholarship;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * CRUD-style access to the {@code listings/} Firestore collection.
 *
 * <p>Used as a fall-back cache: every successful live crawl writes its
 * results here; if a later crawl fails, the search engine reads from here
 * instead. Documents are flat — Firestore can't store inheritance trees, so
 * we store every field as a primitive and tag with a {@code type} string.
 *
 * <p>All writes are synchronous (we call {@code .get()} on the
 * {@link ApiFuture}) so the demo can demonstrate ordering during the viva.
 */
public class ListingRepository {

    private static final String COLLECTION = "listings";

    /* ----------------- writes ------------------------------------------------ */

    public void upsertApartment(Apartment a) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("type", "apartment");
        doc.put("title", a.getTitle());
        doc.put("city", a.getCity());
        doc.put("province", a.getProvince());
        doc.put("bedrooms", a.getBedrooms());
        doc.put("monthlyRent", a.getMonthlyRent());
        doc.put("source", a.getSource());
        doc.put("url", a.getUrl());
        doc.put("description", a.getDescription());
        doc.put("scrapedAt", System.currentTimeMillis());
        write(a.getId(), doc);
    }

    public void upsertJob(Job j) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("type", "job");
        doc.put("title", j.getTitle());
        doc.put("employer", j.getEmployer());
        doc.put("city", j.getCity());
        doc.put("province", j.getProvince());
        doc.put("hourlyRate", j.getHourlyRate());
        doc.put("source", j.getSource());
        doc.put("url", j.getUrl());
        doc.put("description", j.getDescription());
        doc.put("scrapedAt", System.currentTimeMillis());
        write(j.getId(), doc);
    }

    public void upsertBank(BankPlan b) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("type", "bank");
        doc.put("bankName", b.getBankName());
        doc.put("planName", b.getPlanName());
        doc.put("monthlyFee", b.getMonthlyFee());
        doc.put("freeTransactions", b.getFreeTransactions());
        doc.put("studentEligible", b.isStudentEligible());
        doc.put("source", b.getSource());
        doc.put("url", b.getUrl());
        doc.put("description", b.getDescription());
        doc.put("scrapedAt", System.currentTimeMillis());
        write(b.getId(), doc);
    }

    public void upsertScholarship(Scholarship s) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("type", "scholarship");
        doc.put("name", s.getName());
        doc.put("provider", s.getProvider());
        doc.put("amount", s.getAmount());
        doc.put("level", s.getLevel());
        doc.put("internationalEligible", s.isInternationalEligible());
        doc.put("city", s.getCity());
        doc.put("province", s.getProvince());
        doc.put("source", s.getSource());
        doc.put("url", s.getUrl());
        doc.put("description", s.getDescription());
        doc.put("scrapedAt", System.currentTimeMillis());
        write(s.getId(), doc);
    }

    public void upsertMobile(MobilePlan m) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("type", "mobile");
        doc.put("carrier", m.getCarrier());
        doc.put("planName", m.getPlanName());
        doc.put("monthlyPrice", m.getMonthlyPrice());
        doc.put("dataGb", m.getDataGb());
        doc.put("unlimitedTalk", m.isUnlimitedTalk());
        doc.put("source", m.getSource());
        doc.put("url", m.getUrl());
        doc.put("description", m.getDescription());
        doc.put("scrapedAt", System.currentTimeMillis());
        write(m.getId(), doc);
    }

    private void write(String id, Map<String, Object> doc) {
        if (!FirestoreClient.isInitialised()) return; // DEMO mode no-op
        try {
            ApiFuture<WriteResult> f = FirestoreClient.get()
                    .collection(COLLECTION).document(id).set(doc);
            f.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.err.println("Firestore write failed for " + id + ": " + e.getMessage());
        }
    }

    /* ----------------- reads ------------------------------------------------ */

    /** Returns every cached listing as a flat Firestore-style map. */
    public List<Map<String, Object>> readAll() {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!FirestoreClient.isInitialised()) return out;
        try {
            ApiFuture<com.google.cloud.firestore.QuerySnapshot> f =
                    FirestoreClient.get().collection(COLLECTION).get();
            for (QueryDocumentSnapshot d : f.get().getDocuments()) {
                Map<String, Object> data = d.getData();
                data.put("id", d.getId());
                out.add(data);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.err.println("Firestore read failed: " + e.getMessage());
        }
        return out;
    }

    /** Convenience for /api/debug: total count. */
    public int countCached() {
        if (!FirestoreClient.isInitialised()) return 0;
        try {
            DocumentSnapshot meta = FirestoreClient.get().collection("meta").document("listings").get().get();
            if (meta.exists() && meta.contains("total")) return ((Long) meta.get("total")).intValue();
            return readAll().size();
        } catch (InterruptedException | ExecutionException e) {
            return 0;
        }
    }
}
