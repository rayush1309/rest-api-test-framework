package com.sdetlabs.framework.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;

public class RateLimitRetryTest {
    @Test(groups = {"positive", "regression"})
    public void retriesRateLimitedRequestAndReturnsSuccessfulResponse() throws IOException {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/limited", exchange -> {
            try (exchange) {
                if (requests.incrementAndGet() == 1) {
                    exchange.getResponseHeaders().set("Retry-After", "0");
                    respond(exchange, 429, "{\"message\":\"rate limited\"}");
                } else {
                    respond(exchange, 200, "{\"ok\":true}");
                }
            }
        });
        server.start();
        try {
            Response response = new RestClient("http://127.0.0.1:" + server.getAddress().getPort())
                    .get("/limited");
            assertEquals(response.statusCode(), 200);
            assertEquals(requests.get(), 2, "A single 429 should cause one retry");
        } finally {
            server.stop(0);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
