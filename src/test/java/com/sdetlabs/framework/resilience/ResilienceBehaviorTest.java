package com.sdetlabs.framework.resilience;

import com.sdetlabs.framework.mocking.WireMockTestServer;
import com.sdetlabs.framework.http.RestClient;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.awaitility.Awaitility.await;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ResilienceBehaviorTest {
    @Test(groups = {"resilience", "positive", "regression"})
    public void retriesTemporaryServiceErrorsAndRecovers() {
        AtomicInteger calls = new AtomicInteger();
        RetryPolicy policy = new RetryPolicy(4, Duration.ZERO, 2.0, Duration.ofSeconds(1));
        RetryExecutor executor = new RetryExecutor();

        int status = executor.execute(
                () -> calls.incrementAndGet() < 3 ? 503 : 200,
                result -> result >= 500,
                exception -> false,
                policy);

        assertEquals(status, 200);
        assertEquals(calls.get(), 3, "The operation should recover on its third attempt");
    }

    @Test(groups = {"resilience", "positive", "regression"})
    public void retriesSelectedRuntimeFailures() {
        AtomicInteger calls = new AtomicInteger();
        String result = new RetryExecutor().execute(() -> {
            if (calls.incrementAndGet() == 1) throw new IllegalStateException("temporary failure");
            return "recovered";
        }, ignored -> false, exception -> exception instanceof IllegalStateException,
                new RetryPolicy(2, Duration.ZERO, 1.0, Duration.ZERO));

        assertEquals(result, "recovered");
        assertEquals(calls.get(), 2);
    }

    @Test(groups = {"resilience", "async", "positive", "regression"})
    public void pollsUntilAnAsynchronousJobCompletes() {
        try (WireMockTestServer mock = new WireMockTestServer()) {
            mock.wireMock().stubFor(get(urlPathEqualTo("/jobs/job-42"))
                    .inScenario("async-job")
                    .whenScenarioStateIs(STARTED)
                    .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                            .withBody("{\"jobId\":\"job-42\",\"status\":\"processing\"}"))
                    .willSetStateTo("complete"));
            mock.wireMock().stubFor(get(urlPathEqualTo("/jobs/job-42"))
                    .inScenario("async-job")
                    .whenScenarioStateIs("complete")
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody("{\"jobId\":\"job-42\",\"status\":\"complete\"}")));

            RestClient client = mock.client();
            await().alias("job completion")
                    .pollInterval(Duration.ofMillis(20))
                    .atMost(Duration.ofSeconds(2))
                    .untilAsserted(() -> {
                        Response response = client.get("/jobs/job-42");
                        assertEquals(response.statusCode(), 200);
                        assertEquals(response.jsonPath().getString("status"), "complete");
                    });
            mock.wireMock().verify(2, getRequestedFor(urlPathEqualTo("/jobs/job-42")));
        }
    }

    @Test(groups = {"resilience", "idempotency", "positive", "regression"})
    public void repeatedPaymentWithSameIdempotencyKeyCreatesSingleCharge() {
        try (WireMockTestServer mock = new WireMockTestServer()) {
            String key = "order-42-payment";
            mock.wireMock().stubFor(post(urlPathEqualTo("/payments"))
                    .inScenario("idempotent-payment")
                    .whenScenarioStateIs(STARTED)
                    .withHeader("Idempotency-Key", equalTo(key))
                    .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                            .withBody("{\"transactionId\":\"txn-42\",\"status\":\"captured\",\"chargeCount\":1}"))
                    .willSetStateTo("charged"));
            mock.wireMock().stubFor(post(urlPathEqualTo("/payments"))
                    .inScenario("idempotent-payment")
                    .whenScenarioStateIs("charged")
                    .withHeader("Idempotency-Key", equalTo(key))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody("{\"transactionId\":\"txn-42\",\"status\":\"captured\",\"chargeCount\":1}")));

            RestClient client = mock.client();
            Response first = charge(client, key);
            Response replay = charge(client, key);

            assertEquals(first.statusCode(), 201);
            assertEquals(replay.statusCode(), 200);
            assertEquals(first.jsonPath().getString("transactionId"), replay.jsonPath().getString("transactionId"));
            assertEquals(replay.jsonPath().getInt("chargeCount"), 1);
            mock.wireMock().verify(2, postRequestedFor(urlPathEqualTo("/payments"))
                    .withHeader("Idempotency-Key", equalTo(key)));
        }
    }

    @Test(groups = {"resilience", "timeout", "positive", "regression"})
    public void timedOutRequestCanRecoverThroughHealthyEndpoint() throws Exception {
        try (WireMockTestServer mock = new WireMockTestServer()) {
            mock.wireMock().stubFor(get(urlEqualTo("/slow"))
                    .willReturn(aResponse().withFixedDelay(400).withStatus(200).withBody("late")));
            mock.wireMock().stubFor(get(urlEqualTo("/recovered"))
                    .willReturn(aResponse().withStatus(200).withBody("healthy")));

            HttpRequest request = HttpRequest.newBuilder(URI.create(mock.baseUrl() + "/slow"))
                    .timeout(Duration.ofMillis(50)).GET().build();
            boolean timedOut = false;
            try {
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            } catch (HttpTimeoutException expected) {
                timedOut = true;
            }
            assertTrue(timedOut, "The delayed response should exceed the client deadline");
            assertEquals(mock.client().get("/recovered").statusCode(), 200,
                    "A timeout against one endpoint should not block a healthy request");
        }
    }

    private static Response charge(RestClient client, String idempotencyKey) {
        return client.post("/payments", client.newRequest()
                .header("Idempotency-Key", idempotencyKey)
                .body(Map.of("orderId", "order-42", "amount", 29.99)));
    }
}
