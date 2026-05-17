package com.new2canada.models;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic envelope returned by the {@link com.new2canada.search.SearchEngine}.
 *
 * <p>Carries a normalised set of fields that the multi-page UI can render the
 * same way regardless of whether the underlying document is an
 * {@link Apartment}, {@link Job}, {@link BankPlan}, or {@link MobilePlan}.
 *
 * <p>Score is assigned by {@link com.new2canada.ranking.PageRanker}; results
 * are then sorted by {@link com.new2canada.ranking.ResultSorter} (Merge Sort).
 */
public class SearchResult implements Comparable<SearchResult> {

    private final String id;
    private final String type;       // "apartment" | "job" | "bank" | "mobile"
    private final String title;
    private final String subtitle;
    private final String url;
    private final String source;
    private double score;
    private final Map<String, Object> fields = new LinkedHashMap<>();

    public SearchResult(String id, String type, String title, String subtitle,
                        String url, String source) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.url = url;
        this.source = source;
    }

    public SearchResult addField(String key, Object value) {
        fields.put(key, value);
        return this;
    }

    public String getId()              { return id; }
    public String getType()            { return type; }
    public String getTitle()           { return title; }
    public String getSubtitle()        { return subtitle; }
    public String getUrl()             { return url; }
    public String getSource()          { return source; }
    public double getScore()           { return score; }
    public Map<String,Object> getFields() { return fields; }

    public void setScore(double score) { this.score = score; }

    /** Descending by score so higher-ranked items sort first. */
    @Override
    public int compareTo(SearchResult o) {
        return Double.compare(o.score, this.score);
    }
}
