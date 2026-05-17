package com.new2canada.server;

import com.new2canada.config.AppConfig;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Thin wrapper around the JDK's {@link HttpServer}.
 *
 * <p>One server, two handlers: {@code /api/*} → {@link ApiHandler},
 * everything else → {@link StaticFileHandler}.
 */
public class WebServer {

    private final HttpServer server;

    public WebServer(HttpHandler apiHandler, HttpHandler staticHandler) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(AppConfig.HTTP_PORT), 0);
        server.createContext("/api/", apiHandler);
        server.createContext("/", staticHandler);
        // A small thread pool keeps simultaneous browser requests responsive.
        server.setExecutor(Executors.newFixedThreadPool(8));
    }

    public void start() {
        server.start();
        System.out.println("Server started on http://localhost:" + AppConfig.HTTP_PORT);
    }

    public void stop() { server.stop(0); }
}
