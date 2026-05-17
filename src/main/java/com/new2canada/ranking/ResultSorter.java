package com.new2canada.ranking;

import com.new2canada.models.SearchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Sorts a list of {@link SearchResult} in <i>descending</i> order of score
 * using <b>merge sort</b>.
 *
 * <p>Why hand-roll merge sort when {@code List.sort} already exists? Because
 * the course explicitly asks for a merge sort / quick sort implementation —
 * and the deterministic O(N log N) worst case + stability of merge sort is
 * exactly what we want for ranked results.
 *
 * <p><b>Time complexity</b>: O(N log N).
 * <br><b>Space complexity</b>: O(N).
 *
 * Demonstrates: <b>Merge Sort</b>, recursion, divide-and-conquer.
 */
public final class ResultSorter {

    private ResultSorter() {}

    public static List<SearchResult> sortDesc(List<SearchResult> input) {
        if (input == null || input.size() < 2) {
            return input == null ? new ArrayList<>() : new ArrayList<>(input);
        }
        List<SearchResult> copy = new ArrayList<>(input);
        mergeSort(copy, 0, copy.size() - 1);
        return copy;
    }

    private static void mergeSort(List<SearchResult> a, int lo, int hi) {
        if (lo >= hi) return;
        int mid = lo + (hi - lo) / 2;
        mergeSort(a, lo, mid);
        mergeSort(a, mid + 1, hi);
        merge(a, lo, mid, hi);
    }

    /** In-place merge of two adjacent sorted halves [lo..mid] and [mid+1..hi]. */
    private static void merge(List<SearchResult> a, int lo, int mid, int hi) {
        List<SearchResult> left  = new ArrayList<>(a.subList(lo, mid + 1));
        List<SearchResult> right = new ArrayList<>(a.subList(mid + 1, hi + 1));

        int i = 0, j = 0, k = lo;
        while (i < left.size() && j < right.size()) {
            // Descending by score: pick the higher first.
            if (left.get(i).getScore() >= right.get(j).getScore()) {
                a.set(k++, left.get(i++));
            } else {
                a.set(k++, right.get(j++));
            }
        }
        while (i < left.size())  a.set(k++, left.get(i++));
        while (j < right.size()) a.set(k++, right.get(j++));
    }
}
