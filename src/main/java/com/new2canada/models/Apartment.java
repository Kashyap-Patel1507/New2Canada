package com.new2canada.models;

/**
 * A single apartment listing.
 *
 * <p>Plain immutable-ish POJO. Used by the housing crawler / parser pipeline,
 * by the inverted index (via {@link #toIndexText()}), and by the JSON writer
 * when results are sent to the browser.
 */
public class Apartment {

    private final String id;
    private final String title;
    private final String city;
    private final String province;
    private final int bedrooms;
    private final double monthlyRent;
    private final String source;     // e.g. "kijiji.ca"
    private final String url;
    private final String description;

    public Apartment(String id, String title, String city, String province, int bedrooms,
                     double monthlyRent, String source, String url, String description) {
        this.id = id;
        this.title = title;
        this.city = city;
        this.province = province == null ? "" : province;
        this.bedrooms = bedrooms;
        this.monthlyRent = monthlyRent;
        this.source = source;
        this.url = url;
        this.description = description;
    }

    public String getId()          { return id; }
    public String getTitle()       { return title; }
    public String getCity()        { return city; }
    public String getProvince()    { return province; }
    public int getBedrooms()       { return bedrooms; }
    public double getMonthlyRent() { return monthlyRent; }
    public String getSource()      { return source; }
    public String getUrl()         { return url; }
    public String getDescription() { return description; }

    /** Text used by the inverted index: everything searchable, lowercased downstream. */
    public String toIndexText() {
        return title + " " + city + " " + province + " " + bedrooms + "br "
                + source + " " + description;
    }
}
