package com.new2canada.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cleans up raw "location" strings scraped from Kijiji and JobBank into a
 * normalised {city, province} pair.
 *
 * <p>Real-world inputs look like:
 * <ul>
 *   <li>{@code "Etobicoke (ON)"} → {@code {city="Etobicoke", province="ON"}}</li>
 *   <li>{@code "Toronto, Ontario"} → {@code {city="Toronto", province="ON"}}</li>
 *   <li>{@code "St Lawrence-East Bayfront-The Islands, Toronto"} →
 *       {@code {city="Toronto", province="ON"}}</li>
 *   <li>{@code "Mississauga"} → {@code {city="Mississauga", province="ON"}}
 *       (looked up via a small Canadian city → province map)</li>
 * </ul>
 *
 * Demonstrates: <b>Regex</b>, <b>HashMap lookup</b>.
 */
public final class Location {

    public final String city;
    public final String province;

    private Location(String city, String province) {
        this.city = city;
        this.province = province;
    }

    public static Location of(String city, String province) {
        return new Location(city, province);
    }

    /**
     * Match "(ON)" or ", ON" or " ON" — but ONLY the real Canadian province
     * codes. The previous loose `[ABCMNQYS][A-Z]` mis-matched things like
     * "St" inside "St. John's" → fake province "ST".
     */
    private static final Pattern PROV_CODE =
            Pattern.compile(
                "(?:\\(|,\\s*|\\s+)(AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT)\\b",
                Pattern.CASE_INSENSITIVE);

    /** Map of full-name → 2-letter code. */
    private static final Map<String, String> FULL_NAME = Map.ofEntries(
            Map.entry("alberta", "AB"),
            Map.entry("british columbia", "BC"),
            Map.entry("manitoba", "MB"),
            Map.entry("new brunswick", "NB"),
            Map.entry("newfoundland", "NL"),
            Map.entry("newfoundland and labrador", "NL"),
            Map.entry("nova scotia", "NS"),
            Map.entry("northwest territories", "NT"),
            Map.entry("nunavut", "NU"),
            Map.entry("ontario", "ON"),
            Map.entry("prince edward island", "PE"),
            Map.entry("quebec", "QC"),
            Map.entry("saskatchewan", "SK"),
            Map.entry("yukon", "YT")
    );

    /** Known Canadian cities → 2-letter province code. */
    private static final Map<String, String> CITY_TO_PROV = buildCityMap();

    private static Map<String, String> buildCityMap() {
        Map<String, String> m = new java.util.HashMap<>();

        // ----- Ontario -----------------------------------------------------
        String[] on = {
            "toronto","mississauga","brampton","hamilton","ottawa","london",
            "kitchener","waterloo","cambridge","windsor","oshawa","barrie",
            "guelph","st. catharines","kingston","niagara falls","sudbury",
            "thunder bay","peterborough","sarnia","oakville","richmond hill",
            "burlington","markham","vaughan","ajax","pickering","milton",
            "newmarket","aurora","whitby","scarborough","etobicoke","north york",
            "east york","woodbridge","stoney creek","ancaster","dundas",
            "midland","orillia","brockville","cornwall","belleville","welland",
            "chatham","timmins","north bay","kemptville","morrisburg",
            "bracebridge","city of toronto"
        };
        for (String c : on) m.put(c, "ON");

        // ----- British Columbia -------------------------------------------
        String[] bc = {
            "vancouver","victoria","surrey","burnaby","richmond","kelowna",
            "abbotsford","kamloops","nanaimo","langley","saanich","delta",
            "coquitlam","new westminster","north vancouver","west vancouver",
            "maple ridge","port coquitlam","chilliwack","prince george",
            "white rock","salmo","mcbride","saanichton"
        };
        for (String c : bc) m.put(c, "BC");

        // ----- Alberta ----------------------------------------------------
        String[] ab = {
            "calgary","edmonton","red deer","lethbridge","st. albert","medicine hat",
            "grande prairie","airdrie","spruce grove","leduc","okotoks",
            "fort mcmurray","coaldale"
        };
        for (String c : ab) m.put(c, "AB");

        // ----- Manitoba ---------------------------------------------------
        String[] mb = {"winnipeg","brandon","steinbach","portage la prairie","selkirk","thompson"};
        for (String c : mb) m.put(c, "MB");

        // ----- Saskatchewan ----------------------------------------------
        String[] sk = {"saskatoon","regina","prince albert","moose jaw","weyburn","yorkton"};
        for (String c : sk) m.put(c, "SK");

        // ----- Quebec -----------------------------------------------------
        String[] qc = {
            "montreal","montréal","quebec city","québec","laval","gatineau",
            "sherbrooke","longueuil","trois-rivières","saguenay","chicoutimi",
            "joliette","montréal-nord","levis","drummondville"
        };
        for (String c : qc) m.put(c, "QC");

        // ----- New Brunswick ----------------------------------------------
        String[] nb = {"fredericton","saint john","moncton","dieppe","miramichi","charlo"};
        for (String c : nb) m.put(c, "NB");

        // ----- Nova Scotia ------------------------------------------------
        String[] ns = {"halifax","sydney","dartmouth","truro","new glasgow","antigonish"};
        for (String c : ns) m.put(c, "NS");

        // ----- Prince Edward Island ---------------------------------------
        for (String c : new String[]{"charlottetown","summerside","stratford"}) m.put(c, "PE");

        // ----- Newfoundland and Labrador ----------------------------------
        String[] nl = {"st. john's","st johns","saint john's","corner brook","mount pearl","clarke's beach","clarkes beach"};
        for (String c : nl) m.put(c, "NL");

        // ----- Territories ------------------------------------------------
        m.put("whitehorse",     "YT");
        m.put("yellowknife",    "NT");
        m.put("iqaluit",        "NU");

        return java.util.Collections.unmodifiableMap(m);
    }

    /**
     * Parse a raw location string. Never returns null; falls back to
     * {@code city="Canada", province=""} when nothing useful can be extracted.
     */
    public static Location parse(String raw) {
        if (raw == null) return new Location("Canada", "");
        String s = raw.replaceAll("\\s+", " ").trim();
        if (s.isEmpty()) return new Location("Canada", "");

        // 1. Province code via regex: "(ON)" or ", ON" or " ON"
        String province = "";
        Matcher m = PROV_CODE.matcher(s);
        if (m.find()) province = m.group(1).toUpperCase();

        // 2. Province via full name
        if (province.isEmpty()) {
            String lower = s.toLowerCase();
            for (Map.Entry<String, String> e : FULL_NAME.entrySet()) {
                if (lower.contains(e.getKey())) { province = e.getValue(); break; }
            }
        }

        // 3. Strip the province part from the string to leave the city portion.
        //    Use the strict province-code regex so "St." in "St. John's" is preserved.
        String cityPart = s
                .replaceAll("(?i)\\((AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT)\\)", "")
                .replaceAll("(?i),\\s*(AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT)\\b", "")
                .replaceAll("(?i)\\s+(AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT)\\s*$", "")
                .replaceAll("(?i)\\b(ontario|alberta|quebec|manitoba|saskatchewan|nova scotia|new brunswick|prince edward island|newfoundland(?: and labrador)?|british columbia|yukon|nunavut|northwest territories)\\b", "")
                .trim();

        // 4. Kijiji often gives "Neighborhood, City" — keep the last segment.
        String city = pickCityFromSegments(cityPart);

        // 5. If still no province, look up via city
        if (province.isEmpty()) {
            String prov = CITY_TO_PROV.get(city.toLowerCase());
            if (prov != null) province = prov;
        }

        if (city.isEmpty()) city = "Canada";
        return new Location(city, province);
    }

    /**
     * Given "St Lawrence-East Bayfront-The Islands, Toronto", returns
     * "Toronto". For "Toronto", returns "Toronto". For "City of Toronto",
     * normalises to "Toronto".
     */
    private static String pickCityFromSegments(String s) {
        if (s == null || s.isBlank()) return "";
        String[] parts = s.split(",");
        // Walk from the rightmost segment looking for a known city.
        for (int i = parts.length - 1; i >= 0; i--) {
            String seg = parts[i].trim()
                    .replaceAll("(?i)^city of\\s+", "");
            if (seg.isEmpty()) continue;
            if (CITY_TO_PROV.containsKey(seg.toLowerCase())) return capitalise(seg);
        }
        // None of the segments is a known city — just keep the last one.
        String last = parts[parts.length - 1].trim()
                .replaceAll("(?i)^city of\\s+", "");
        return capitalise(last);
    }

    private static String capitalise(String w) {
        if (w == null || w.isEmpty()) return w;
        StringBuilder out = new StringBuilder();
        boolean newWord = true;
        for (char c : w.toCharArray()) {
            if (Character.isWhitespace(c) || c == '-') { newWord = true; out.append(c); }
            else {
                out.append(newWord ? Character.toUpperCase(c) : Character.toLowerCase(c));
                newWord = false;
            }
        }
        return out.toString();
    }
}
