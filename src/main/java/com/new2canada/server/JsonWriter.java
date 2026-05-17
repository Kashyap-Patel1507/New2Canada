package com.new2canada.server;

import java.util.Collection;
import java.util.Map;

/**
 * Minimal hand-rolled JSON serialiser.
 *
 * <p>We avoid pulling in Jackson / Gson so the only Maven dependencies are
 * Jsoup and firebase-admin. The serialiser supports exactly what the API
 * needs: nested {@link Map}s, {@link Collection}s, strings, numbers,
 * booleans, and null. Strings are properly escaped.
 *
 * <p>It is <i>not</i> a general-purpose JSON library — don't use it outside
 * this project.
 */
public final class JsonWriter {

    private JsonWriter() {}

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object v) {
        if (v == null)                  { sb.append("null"); return; }
        if (v instanceof Boolean)       { sb.append(v); return; }
        if (v instanceof Number)        { sb.append(v); return; }
        if (v instanceof Map<?, ?> m)   { writeMap(sb, m); return; }
        if (v instanceof Collection<?> c){ writeArray(sb, c); return; }
        if (v.getClass().isArray()) {
            sb.append('[');
            int len = java.lang.reflect.Array.getLength(v);
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(',');
                writeValue(sb, java.lang.reflect.Array.get(v, i));
            }
            sb.append(']');
            return;
        }
        // Fallback: string
        writeString(sb, v.toString());
    }

    private static void writeMap(StringBuilder sb, Map<?, ?> m) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> e : m.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            writeString(sb, String.valueOf(e.getKey()));
            sb.append(':');
            writeValue(sb, e.getValue());
        }
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, Collection<?> c) {
        sb.append('[');
        boolean first = true;
        for (Object item : c) {
            if (!first) sb.append(',');
            first = false;
            writeValue(sb, item);
        }
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"'  -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }
}
