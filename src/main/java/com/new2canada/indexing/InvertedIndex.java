package com.new2canada.indexing;

import com.new2canada.utils.TextNormalizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Term-to-documents map: the classic full-text-search building block.
 *
 * <p>For every unique token seen across all indexed documents, the inverted
 * index stores the list of documents (and term frequencies) that contain it.
 * A query for {@code "toronto apartment"} becomes two O(1) HashMap lookups
 * and a set-intersection on the result lists.
 *
 * <p>Conceptually:
 * <pre>
 *   "toronto"  → [(doc=apt-12, tf=3), (doc=job-7,  tf=1), …]
 *   "apartment"→ [(doc=apt-12, tf=2), (doc=apt-09, tf=1), …]
 * </pre>
 *
 * <p><b>Time complexity</b>
 * <ul>
 *   <li>Build: O(N · L) where N = #docs, L = average tokens per doc.</li>
 *   <li>Lookup: O(1) average per term + O(min(list-sizes)) for AND-merge.</li>
 * </ul>
 *
 * Demonstrates: <b>HashMap</b>, <b>Inverted Index</b>.
 */
public class InvertedIndex {

    /** A single (document, term-frequency) entry. */
    public static class Posting {
        public final String docId;
        public final int termFrequency;
        public Posting(String docId, int tf) { this.docId = docId; this.termFrequency = tf; }
    }

    private final Map<String, List<Posting>> index = new HashMap<>();
    private final Set<String> docIds = new HashSet<>();

    /** Adds one document's tokens to the index. Idempotent w.r.t. docId. */
    public void addDocument(String docId, String text) {
        if (docId == null || docId.isEmpty() || text == null) return;
        docIds.add(docId);

        // Per-doc term frequency, then push one Posting per unique term.
        WordFrequencyCounter c = new WordFrequencyCounter();
        c.countText(text);
        for (Map.Entry<String, Integer> e : c.snapshot().entrySet()) {
            index.computeIfAbsent(e.getKey(), k -> new ArrayList<>())
                 .add(new Posting(docId, e.getValue()));
        }
    }

    /** Postings for one term, or empty list. */
    public List<Posting> postings(String term) {
        return index.getOrDefault(term, List.of());
    }

    /**
     * AND-search: returns ids of documents that contain *every* token in the
     * query. Token order doesn't matter, but a query of zero tokens returns
     * an empty set so we never accidentally return everything.
     */
    public Set<String> search(String rawQuery) {
        List<String> tokens = TextNormalizer.tokenize(rawQuery);
        if (tokens.isEmpty()) return new LinkedHashSet<>();

        Set<String> result = null;
        for (String t : tokens) {
            Set<String> hits = new HashSet<>();
            for (Posting p : postings(t)) hits.add(p.docId);
            if (result == null) result = hits;
            else result.retainAll(hits);
            if (result.isEmpty()) break;
        }
        return result == null ? new HashSet<>() : result;
    }

    public int totalTerms()     { return index.size(); }
    public int totalDocuments() { return docIds.size(); }
}
