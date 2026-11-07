package com.sdetlabs.framework.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;

/** Verifies that automatic rate-limit handling never repeats a state-changing request. */
public class NonIdempotentRetryTest {
    @Test(groups = {"negative", "regression", "resilience"})
    public void rateLimitedPostIsNotRetried() throws IOException {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/payments", exchange -> {
            requests.incrementAndGet();
            exchange.getResponseHeaders().set("Retry-After", "0");
            respond(exchange, 429, "{\"error\":\"rate limited\"}");
        });
        server.start();
        try {
            var response = new RestClient("http://127.0.0.1:" + server.getAddress().getPort())
                    .post("/payments", Map.of("amount", 10, "paymentToken", "token-for-test"));
            assertEquals(response.statusCode(), 429);
            assertEquals(requests.get(), 1, "A POST must not be repeated automatically after HTTP 429");
        } finally {
            server.stop(0);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
