package com.new2canada.autocomplete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classic prefix tree (Trie) used for autocomplete.
 *
 * <p>Each node holds a {@link HashMap} from the next character to the next
 * node. Storing children in a HashMap (instead of a fixed 26-slot array)
 * keeps memory small for sparse subtrees and lets us index any character —
 * including digits, which we want for queries like "2br" or "5g".
 *
 * <p>Every inserted word carries a frequency. When the system later asks
 * for completions of a prefix, the returned candidates are ordered by the
 * frequency that was inserted here.
 *
 * <p><b>Time complexity</b>
 * <ul>
 *   <li>{@link #insert} → O(L) where L = word length.</li>
 *   <li>{@link #findNode} → O(L).</li>
 *   <li>{@link #collect} → O(N) over the subtree rooted at the matched node.</li>
 * </ul>
 *
 * Demonstrates: <b>Trie</b>, <b>HashMap</b>.
 */
public class Trie {

    /** A single trie node. Package-private so the system class can read it. */
    static class Node {
        final Map<Character, Node> children = new HashMap<>();
        boolean isEndOfWord = false;
        int frequency = 0;        // how many times this exact word was inserted
        String word = null;       // the actual word (only set on end nodes)
    }

    private final Node root = new Node();
    private int size = 0;

    /** Adds (or increments the frequency of) a word. */
    public void insert(String word) {
        if (word == null || word.isEmpty()) return;
        Node curr = root;
        for (char c : word.toCharArray()) {
            curr = curr.children.computeIfAbsent(c, k -> new Node());
        }
        if (!curr.isEndOfWord) size++;
        curr.isEndOfWord = true;
        curr.frequency++;
        curr.word = word;
    }

    /** True iff the exact word was inserted at least once. */
    public boolean contains(String word) {
        Node n = findNode(word);
        return n != null && n.isEndOfWord;
    }

    /** Returns all stored words that start with {@code prefix} (any order). */
    public List<Node> findByPrefix(String prefix) {
        List<Node> out = new ArrayList<>();
        Node start = findNode(prefix);
        if (start == null) return out;
        collect(start, out);
        return out;
    }

    /** Returns the node reached by walking {@code s}, or {@code null}. */
    private Node findNode(String s) {
        Node curr = root;
        for (char c : s.toCharArray()) {
            curr = curr.children.get(c);
            if (curr == null) return null;
        }
        return curr;
    }

    /** Depth-first walk under {@code node}, gathering every word-end. */
    private void collect(Node node, List<Node> out) {
        if (node.isEndOfWord) out.add(node);
        for (Node child : node.children.values()) collect(child, out);
    }

    public int size() { return size; }
}
