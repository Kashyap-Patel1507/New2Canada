package com.new2canada.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Thin wrapper around Jsoup that turns raw HTML into a navigable
 * {@link Document}.
 *
 * <p>Kept as a tiny class so that all parsing entry points go through one
 * place — easier to swap the parser later (e.g. for Lagarto or HtmlUnit)
 * without rewriting the crawlers.
 *
 * Demonstrates: <b>HTML parsing</b> via the Jsoup DOM tree.
 */
public class HTMLParser {

    /** Parses an HTML string. Returns an empty {@link Document} if input is null. */
    public Document parse(String html, String baseUri) {
        if (html == null) return Jsoup.parse("", baseUri == null ? "" : baseUri);
        return Jsoup.parse(html, baseUri == null ? "" : baseUri);
    }

    public Document parse(String html) {
        return parse(html, "");
    }
}
