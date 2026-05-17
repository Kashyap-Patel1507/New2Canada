package com.new2canada.crawler;

import com.new2canada.config.AppConfig;
import com.new2canada.models.BankPlan;
import com.new2canada.parser.DataExtractor;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Crawls Canadian-bank student-account pages (RBC, Scotia, CIBC).
 *
 * Demonstrates: <b>OOP inheritance</b>.
 */
public class BankCrawler extends WebCrawler {

    private final DataExtractor extractor = new DataExtractor();
    private final Consumer<BankPlan> sink;

    public BankCrawler(PoliteFetcher fetcher, Consumer<BankPlan> sink) {
        super(fetcher);
        this.sink = sink;
    }

    @Override
    protected List<String> seeds() {
        return new ArrayList<>(AppConfig.BANK_SEEDS);
    }

    @Override
    protected void handle(Document doc) {
        String url   = doc.baseUri();
        String bank  = bankOf(url);
        String src   = sourceOf(url);
        for (BankPlan p : extractor.extractBankPlans(doc, src, bank)) sink.accept(p);
    }

    private static String bankOf(String url) {
        if (url == null) return "Bank";
        String u = url.toLowerCase();
        if (u.contains("rbc") || u.contains("royal_bank"))           return "RBC";
        if (u.contains("scotia") || u.contains("bank_of_nova"))      return "Scotiabank";
        if (u.contains("cibc") || u.contains("canadian_imperial"))   return "CIBC";
        if (u.contains("td.com") || u.contains("toronto-dominion") || u.contains("/td_")) return "TD";
        if (u.contains("bmo") || u.contains("bank_of_montreal"))     return "BMO";
        if (u.contains("national_bank") || u.contains("/nbc"))       return "National Bank";
        if (u.contains("tangerine"))                                  return "Tangerine";
        if (u.contains("simplii"))                                    return "Simplii";
        if (u.contains("hsbc"))                                       return "HSBC";
        if (u.contains("desjardins"))                                 return "Desjardins";
        if (u.contains("eq_bank") || u.contains("eqbank"))            return "EQ Bank";
        return "Bank";
    }

    private static String sourceOf(String url) {
        if (url == null) return "unknown";
        if (url.contains("rbcroyalbank")) return "rbc.com";
        if (url.contains("scotiabank"))   return "scotiabank.com";
        if (url.contains("cibc"))         return "cibc.com";
        if (url.contains("td.com"))       return "td.com";
        if (url.contains("bmo.com"))      return "bmo.com";
        if (url.contains("tangerine"))    return "tangerine.ca";
        if (url.contains("simplii"))      return "simplii.com";
        if (url.contains("hsbc"))         return "hsbc.ca";
        if (url.contains("wikipedia"))    return "wikipedia.org";
        return url;
    }
}
