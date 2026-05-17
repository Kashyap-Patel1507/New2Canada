package com.new2canada.ranking;

import com.new2canada.indexing.InvertedIndex;
import com.new2canada.utils.TextNormalizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Assigns a relevance score to each document for a given user query.
 *
 * <p>The score combines two ideas the course wants demonstrated:
 *
 * <ol>
 *   <li><b>TF·IDF</b> — classic information-retrieval relevance. For each
 *       query term we multiply the document's term-frequency by the log of
 *       (total docs / docs containing the term). High TF + rare term =
 *       higher score.</li>
 *   <li><b>Iterative refinement (PageRank-style)</b> — documents that share
 *       many terms with other already-high-scoring documents get a small
 *       boost. This is a simplified random-walk relaxation: a couple of
 *       passes is enough for an ACC project and the maths is easy to
 *       explain in viva.</li>
 * </ol>
 *
 * <p>Documents are surfaced from the {@link InvertedIndex} (so we never
 * score irrelevant docs) and the top-k are extracted with a bounded
 * <b>min-heap</b> ({@link PriorityQueue}).
 *
 * <p><b>Time complexity</b>: O(k · E) where k is the number of refinement
 * iterations (default 2) and E is the number of (term, doc) edges touched
 * by the query.
 *
 * Demonstrates: <b>HashMap</b>, <b>Priority Queue / Heap</b>, iterative
 * scoring (PageRank flavour), TF·IDF.
 */
public class PageRanker {

    private final InvertedIndex index;
    private final int iterations;

    public PageRanker(InvertedIndex index) {
        this(index, 2);
    }

    public PageRanker(InvertedIndex index, int iterations) {
        this.index = index;
        this.iterations = Math.max(1, iterations);
    }

    /** Returns a map of docId → score for the given query. Empty if no hits. */
    public Map<String, Double> rank(String rawQuery) {
        List<String> tokens = TextNormalizer.tokenize(rawQuery);
        Map<String, Double> scores = new HashMap<>();
        if (tokens.isEmpty()) return scores;

        int totalDocs = Math.max(1, index.totalDocuments());

        // -------- pass 1: TF·IDF --------
        for (String t : tokens) {
            List<InvertedIndex.Posting> postings = index.postings(t);
            if (postings.isEmpty()) continue;
            double idf = Math.log(1.0 + (double) totalDocs / postings.size());
            for (InvertedIndex.Posting p : postings) {
                double tf = 1 + Math.log(p.termFrequency);
                scores.merge(p.docId, tf * idf, Double::sum);
            }
        }

        // -------- pass 2+: PageRank-style smoothing --------
        // Each doc absorbs a damped share of its neighbours' scores via the
        // shared-term edges in the inverted index. Simple, but enough to
        // make popular co-occurring docs rise.
        double damping = 0.15;
        for (int it = 1; it < iterations; it++) {
            final Map<String, Double> current = scores;
            Map<String, Double> next = new HashMap<>();
            for (Map.Entry<String, Double> e : current.entrySet()) {
                next.merge(e.getKey(), e.getValue() * (1 - damping), Double::sum);
            }
            for (String t : tokens) {
                List<InvertedIndex.Posting> postings = index.postings(t);
                double sumOthers = postings.stream()
                        .mapToDouble(p -> current.getOrDefault(p.docId, 0.0)).sum();
                for (InvertedIndex.Posting p : postings) {
                    double share = damping * (sumOthers - current.getOrDefault(p.docId, 0.0))
                            / Math.max(1, postings.size() - 1);
                    next.merge(p.docId, share, Double::sum);
                }
            }
            scores = next;
        }
        return scores;
    }

    /** Returns the top-k docIds, highest first, using a bounded min-heap. */
    public List<String> topK(Map<String, Double> scores, int k) {
        if (k <= 0 || scores.isEmpty()) return new ArrayList<>();
        PriorityQueue<Map.Entry<String, Double>> heap =
                new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));
        for (Map.Entry<String, Double> e : scores.entrySet()) {
            heap.offer(e);
            if (heap.size() > k) heap.poll();
        }
        List<String> out = new ArrayList<>();
        while (!heap.isEmpty()) out.add(0, heap.poll().getKey());
        return out;
    }
}
