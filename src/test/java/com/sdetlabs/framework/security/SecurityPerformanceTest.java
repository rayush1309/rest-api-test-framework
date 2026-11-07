package com.sdetlabs.framework.security;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.sdetlabs.framework.http.RestClient;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/** Deterministic local checks for security boundaries and controlled service load. */
public class SecurityPerformanceTest {

    @Test(groups = {"security", "authentication", "negative", "regression"})
    public void expiredMissingAndInsufficientCredentialsAreRejected() {
        try (MockApi mock = new MockApi()) {
            mock.server.stubFor(get(urlEqualTo("/account"))
                    .withHeader("Authorization", equalTo("Bearer expired-token"))
                    .atPriority(1)
                    .willReturn(aResponse().withStatus(401).withHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"")
                            .withBody("{\"error\":\"TOKEN_EXPIRED\"}")));
            mock.server.stubFor(get(urlEqualTo("/account"))
                    .atPriority(10)
                    .willReturn(aResponse().withStatus(401).withBody("{\"error\":\"AUTHENTICATION_REQUIRED\"}")));
            mock.server.stubFor(get(urlEqualTo("/admin"))
                    .willReturn(aResponse().withStatus(403).withBody("{\"error\":\"FORBIDDEN\"}")));

            assertEquals(mock.client.get("/account").statusCode(), 401);
            assertEquals(mock.client.get("/account", mock.client.newRequest()
                    .header("Authorization", "Bearer malformed-token")).statusCode(), 401);
            Response expired = mock.client.get("/account", mock.client.newRequest()
                    .header("Authorization", "Bearer expired-token"));
            assertEquals(expired.statusCode(), 401);
            assertEquals(expired.jsonPath().getString("error"), "TOKEN_EXPIRED");
            assertEquals(mock.client.get("/admin", mock.client.newRequest()
                    .header("Authorization", "Bearer customer-token")).statusCode(), 403);
            assertEquals(mock.client.get("/admin", mock.client.newRequest()
                    .header("Authorization", "Bearer customer-token")
                    .header("X-Role", "admin")).statusCode(), 403,
                    "A caller-controlled role header must not grant admin access");
        }
    }

    @Test(groups = {"security", "authorization", "regression"})
    public void bearerCredentialsKeepCustomerProfilesIsolated() {
        try (MockApi mock = new MockApi()) {
            mock.server.stubFor(get(urlEqualTo("/profile"))
                    .withHeader("Authorization", equalTo("Bearer customer-a"))
                    .atPriority(1)
                    .willReturn(aResponse().withStatus(200).withBody("{\"customerId\":\"A\"}")));
            mock.server.stubFor(get(urlEqualTo("/profile"))
                    .withHeader("Authorization", equalTo("Bearer customer-b"))
                    .atPriority(1)
                    .willReturn(aResponse().withStatus(200).withBody("{\"customerId\":\"B\"}")));
            mock.server.stubFor(get(urlEqualTo("/profile"))
                    .atPriority(10)
                    .willReturn(aResponse().withStatus(401)));

            assertEquals(mock.client.get("/profile", mock.client.newRequest()
                    .header("Authorization", "Bearer customer-a")).jsonPath().getString("customerId"), "A");
            assertEquals(mock.client.get("/profile", mock.client.newRequest()
                    .header("Authorization", "Bearer customer-b")).jsonPath().getString("customerId"), "B");
            assertEquals(mock.client.get("/profile", mock.client.newRequest()
                    .header("Authorization", "Bearer customer-a")).jsonPath().getString("customerId"), "A");
            assertEquals(mock.client.get("/profile").statusCode(), 401);
        }
    }

    @Test(groups = {"security", "validation", "negative", "regression"})
    public void unsafeSearchInputAndUnsupportedContentTypeAreRejected() {
        try (MockApi mock = new MockApi()) {
            mock.server.stubFor(post(urlPathEqualTo("/search"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.query", matching("(?i).*(<script|--|union\\s+select).*")))
                    .atPriority(1)
                    .willReturn(aResponse().withStatus(400).withBody("{\"error\":\"INVALID_QUERY\"}")));
            mock.server.stubFor(post(urlPathEqualTo("/search"))
                    .withHeader("Content-Type", containing("application/json"))
                    .atPriority(5)
                    .willReturn(aResponse().withStatus(200).withBody("{\"results\":[]}")));
            mock.server.stubFor(post(urlPathEqualTo("/search"))
                    .atPriority(10)
                    .willReturn(aResponse().withStatus(415).withBody("{\"error\":\"UNSUPPORTED_MEDIA_TYPE\"}")));

            Response safe = mock.client.post("/search", Map.of("query", "wireless headphones"));
            Response injection = mock.client.post("/search", mock.client.newRequest()
                    .contentType("application/json").body(Map.of("query", "' UNION SELECT * --")));
            Response wrongType = mock.client.post("/search", mock.client.newRequest()
                    .contentType("text/plain").body("query=wireless"));
            assertEquals(safe.statusCode(), 200);
            assertEquals(injection.statusCode(), 400);
            assertEquals(wrongType.statusCode(), 415);
        }
    }

    @Test(groups = {"performance", "reliability", "regression"})
    public void repeatedAndConcurrentLocalRequestsStayResponsiveAndIsolated() throws Exception {
        try (MockApi mock = new MockApi()) {
            mock.server.stubFor(get(urlEqualTo("/health"))
                    .withHeader("X-Request-Id", matching("request-\\d+"))
                    .willReturn(aResponse().withStatus(200).withFixedDelay(15).withBody("{\"status\":\"ok\"}")));

            long startedAt = System.nanoTime();
            for (int i = 0; i < 10; i++) {
                assertEquals(mock.client.get("/health", mock.client.newRequest()
                        .header("X-Request-Id", "request-" + i)).statusCode(), 200);
            }
            Duration repeatedDuration = Duration.ofNanos(System.nanoTime() - startedAt);
            assertTrue(repeatedDuration.compareTo(Duration.ofSeconds(5)) < 0,
                    "Ten local responses should complete within the generous five-second ceiling");

            ExecutorService pool = Executors.newFixedThreadPool(5);
            try {
                List<Future<Integer>> responses = new ArrayList<>();
                for (int i = 0; i < 15; i++) {
                    String requestId = "request-" + (i + 10);
                    responses.add(pool.submit(() -> mock.client.get("/health", mock.client.newRequest()
                            .header("X-Request-Id", requestId)).statusCode()));
                }
                for (Future<Integer> response : responses) {
                    assertEquals(response.get(), Integer.valueOf(200));
                }
            } finally {
                pool.shutdownNow();
            }
            mock.server.verify(25, getRequestedFor(urlEqualTo("/health"))
                    .withHeader("X-Request-Id", matching("request-\\d+")));
        }
    }

    private static final class MockApi implements AutoCloseable {
        private final WireMockServer server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        private final RestClient client;

        private MockApi() {
            server.start();
            client = new RestClient(server.baseUrl());
        }

        @Override
        public void close() {
            server.stop();
        }
    }
}
