package com.new2canada.models;

/**
 * A Canadian student-friendly bank account / plan offered by a major bank
 * (RBC, Scotia, CIBC, …).
 */
public class BankPlan {

    private final String id;
    private final String bankName;
    private final String planName;
    private final double monthlyFee;
    private final int freeTransactions;
    private final boolean studentEligible;
    private final String source;
    private final String url;
    private final String description;

    public BankPlan(String id, String bankName, String planName, double monthlyFee,
                    int freeTransactions, boolean studentEligible,
                    String source, String url, String description) {
        this.id = id;
        this.bankName = bankName;
        this.planName = planName;
        this.monthlyFee = monthlyFee;
        this.freeTransactions = freeTransactions;
        this.studentEligible = studentEligible;
        this.source = source;
        this.url = url;
        this.description = description;
    }

    public String getId()              { return id; }
    public String getBankName()        { return bankName; }
    public String getPlanName()        { return planName; }
    public double getMonthlyFee()      { return monthlyFee; }
    public int getFreeTransactions()   { return freeTransactions; }
    public boolean isStudentEligible() { return studentEligible; }
    public String getSource()          { return source; }
    public String getUrl()             { return url; }
    public String getDescription()     { return description; }

    public String toIndexText() {
        return bankName + " " + planName + " student banking account "
                + (studentEligible ? "student " : "")
                + source + " " + description;
    }
}
