package com.new2canada.models;

/**
 * A single job posting.
 *
 * <p>Used by the {@code JobCrawler}, {@code DataExtractor}, the inverted index,
 * and the search-result JSON envelope.
 */
public class Job {

    private final String id;
    private final String title;
    private final String employer;
    private final String city;
    private final String province;
    private final double hourlyRate;
    private final String source;
    private final String url;
    private final String description;

    public Job(String id, String title, String employer, String city, String province,
               double hourlyRate, String source, String url, String description) {
        this.id = id;
        this.title = title;
        this.employer = employer;
        this.city = city;
        this.province = province == null ? "" : province;
        this.hourlyRate = hourlyRate;
        this.source = source;
        this.url = url;
        this.description = description;
    }

    public String getId()         { return id; }
    public String getTitle()      { return title; }
    public String getEmployer()   { return employer; }
    public String getCity()       { return city; }
    public String getProvince()   { return province; }
    public double getHourlyRate() { return hourlyRate; }
    public String getSource()     { return source; }
    public String getUrl()        { return url; }
    public String getDescription(){ return description; }

    public String toIndexText() {
        return title + " " + employer + " " + city + " " + province + " "
                + source + " " + description;
    }
}
