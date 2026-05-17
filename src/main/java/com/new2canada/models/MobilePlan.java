package com.new2canada.models;

/**
 * A Canadian mobile / SIM plan from a carrier such as Freedom Mobile or
 * Public Mobile.
 */
public class MobilePlan {

    private final String id;
    private final String carrier;
    private final String planName;
    private final double monthlyPrice;
    private final double dataGb;
    private final boolean unlimitedTalk;
    private final String source;
    private final String url;
    private final String description;

    public MobilePlan(String id, String carrier, String planName, double monthlyPrice,
                      double dataGb, boolean unlimitedTalk, String source,
                      String url, String description) {
        this.id = id;
        this.carrier = carrier;
        this.planName = planName;
        this.monthlyPrice = monthlyPrice;
        this.dataGb = dataGb;
        this.unlimitedTalk = unlimitedTalk;
        this.source = source;
        this.url = url;
        this.description = description;
    }

    public String getId()             { return id; }
    public String getCarrier()        { return carrier; }
    public String getPlanName()       { return planName; }
    public double getMonthlyPrice()   { return monthlyPrice; }
    public double getDataGb()         { return dataGb; }
    public boolean isUnlimitedTalk()  { return unlimitedTalk; }
    public String getSource()         { return source; }
    public String getUrl()            { return url; }
    public String getDescription()    { return description; }

    public String toIndexText() {
        return carrier + " " + planName + " " + dataGb + "gb data sim mobile "
                + (unlimitedTalk ? "unlimited talk " : "")
                + source + " " + description;
    }
}
