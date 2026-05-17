package com.new2canada.crawler;

import com.new2canada.config.AppConfig;
import com.new2canada.models.MobilePlan;
import com.new2canada.parser.DataExtractor;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Crawls Canadian mobile-carrier "Plans" pages (Freedom Mobile,
 * Public Mobile).
 *
 * Demonstrates: <b>OOP inheritance</b>.
 */
public class MobileCrawler extends WebCrawler {

    private final DataExtractor extractor = new DataExtractor();
    private final Consumer<MobilePlan> sink;

    public MobileCrawler(PoliteFetcher fetcher, Consumer<MobilePlan> sink) {
        super(fetcher);
        this.sink = sink;
    }

    @Override
    protected List<String> seeds() {
        return new ArrayList<>(AppConfig.MOBILE_SEEDS);
    }

    @Override
    protected void handle(Document doc) {
        String carrier = carrierOf(doc.baseUri());
        String src     = sourceOf(doc.baseUri());
        for (MobilePlan p : extractor.extractMobilePlans(doc, src, carrier)) sink.accept(p);
    }

    private static String carrierOf(String url) {
        if (url == null) return "Carrier";
        String u = url.toLowerCase();
        if (u.contains("freedom_mobile") || u.contains("freedommobile")) return "Freedom Mobile";
        if (u.contains("public_mobile")  || u.contains("publicmobile"))  return "Public Mobile";
        if (u.contains("rogers"))                                         return "Rogers";
        if (u.contains("bell_mobility")  || u.contains("bell.ca"))        return "Bell";
        if (u.contains("telus"))                                          return "Telus";
        if (u.contains("koodo"))                                          return "Koodo";
        if (u.contains("virgin"))                                         return "Virgin Plus";
        if (u.contains("fido"))                                           return "Fido";
        if (u.contains("chatr"))                                          return "Chatr";
        if (u.contains("lucky"))                                          return "Lucky Mobile";
        if (u.contains("sasktel"))                                        return "SaskTel";
        if (u.contains("eastlink"))                                       return "Eastlink";
        if (u.contains("videotron"))                                      return "Videotron";
        if (u.contains("tbaytel"))                                        return "TBayTel";
        if (u.contains("ice_wireless"))                                   return "Ice Wireless";
        return "Carrier";
    }

    private static String sourceOf(String url) {
        if (url == null) return "unknown";
        if (url.contains("freedommobile"))    return "freedommobile.ca";
        if (url.contains("publicmobile"))     return "publicmobile.ca";
        if (url.contains("wikipedia"))        return "wikipedia.org";
        if (url.contains("rogers.com"))       return "rogers.com";
        if (url.contains("koodomobile"))      return "koodomobile.com";
        if (url.contains("virginplus"))       return "virginplus.ca";
        if (url.contains("fido.ca"))          return "fido.ca";
        if (url.contains("sasktel.com"))      return "sasktel.com";
        if (url.contains("eastlink.ca"))      return "eastlink.ca";
        return url;
    }
}
