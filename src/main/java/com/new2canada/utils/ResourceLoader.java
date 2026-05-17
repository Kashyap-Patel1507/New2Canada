package com.new2canada.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Small classpath I/O helper.
 *
 * <p>Loads files packaged inside {@code src/main/resources/} — currently the
 * dictionary and any static web assets the server may need to read from Java
 * (the static handler streams them directly).
 */
public final class ResourceLoader {

    private ResourceLoader() {}

    /** Reads a resource line-by-line. Returns empty list if missing. */
    public static List<String> readLines(String resourcePath) {
        List<String> lines = new ArrayList<>();
        try (InputStream in = open(resourcePath)) {
            if (in == null) return lines;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String l;
                while ((l = r.readLine()) != null) {
                    String trimmed = l.trim();
                    if (!trimmed.isEmpty()) lines.add(trimmed);
                }
            }
        } catch (IOException e) {
            System.err.println("ResourceLoader: failed to read " + resourcePath + " — " + e.getMessage());
        }
        return lines;
    }

    /** Opens a resource as a raw stream. Caller closes it. */
    public static InputStream open(String resourcePath) {
        return ResourceLoader.class.getClassLoader().getResourceAsStream(resourcePath);
    }
}
