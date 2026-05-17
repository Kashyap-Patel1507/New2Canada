package com.new2canada.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Serves files packaged under {@code src/main/resources/static/} to the
 * browser.
 *
 * <p>Mapping rules:
 * <ul>
 *   <li>{@code GET /}            → {@code static/index.html}</li>
 *   <li>{@code GET /anything.html} → {@code static/anything.html}</li>
 *   <li>{@code GET /css/x.css}    → {@code static/css/x.css}</li>
 * </ul>
 *
 * <p>Path traversal is blocked by stripping every {@code "../"} segment
 * before we touch the classpath.
 */
public class StaticFileHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if ("/".equals(path)) path = "/index.html";
        String safe = ("static" + path).replace("../", "");
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        try (InputStream in = cl.getResourceAsStream(safe)) {
            if (in == null) { notFound(ex, path); return; }
            byte[] body = in.readAllBytes();
            ex.getResponseHeaders().add("Content-Type", contentTypeFor(path));
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        }
    }

    private static void notFound(HttpExchange ex, String path) throws IOException {
        byte[] body = ("404 — " + path).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(404, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }

    private static String contentTypeFor(String path) {
        String p = path.toLowerCase();
        if (p.endsWith(".html")) return "text/html; charset=utf-8";
        if (p.endsWith(".css"))  return "text/css; charset=utf-8";
        if (p.endsWith(".js"))   return "application/javascript; charset=utf-8";
        if (p.endsWith(".json")) return "application/json; charset=utf-8";
        if (p.endsWith(".png"))  return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".svg"))  return "image/svg+xml";
        return "application/octet-stream";
    }
}
