package com.new2canada.models;

/**
 * A Canadian scholarship, bursary, or financial-aid award targeted at
 * international and domestic post-secondary students. Crawled live from
 * Government of Canada (scholarships.gc.ca, ESDC), Universities Canada,
 * and Wikipedia award articles.
 */
public class Scholarship {

    private final String id;
    private final String name;
    private final String provider;
    private final double amount;
    private final String level;        // "Undergraduate", "Graduate", "Any"
    private final boolean internationalEligible;
    private final String city;
    private final String province;
    private final String source;
    private final String url;
    private final String description;

    public Scholarship(String id, String name, String provider, double amount,
                       String level, boolean internationalEligible,
                       String city, String province,
                       String source, String url, String description) {
        this.id = id;
        this.name = name;
        this.provider = provider;
        this.amount = amount;
        this.level = level;
        this.internationalEligible = internationalEligible;
        this.city = city;
        this.province = province;
        this.source = source;
        this.url = url;
        this.description = description;
    }

    public String getId()                    { return id; }
    public String getName()                  { return name; }
    public String getProvider()              { return provider; }
    public double getAmount()                { return amount; }
    public String getLevel()                 { return level; }
    public boolean isInternationalEligible() { return internationalEligible; }
    public String getCity()                  { return city; }
    public String getProvince()              { return province; }
    public String getSource()                { return source; }
    public String getUrl()                   { return url; }
    public String getDescription()           { return description; }

    public String toIndexText() {
        return name + " " + provider + " scholarship bursary award financial aid student "
                + (internationalEligible ? "international " : "")
                + level + " " + city + " " + province + " "
                + source + " " + description;
    }
}
