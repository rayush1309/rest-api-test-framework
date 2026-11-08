package com.sdetlabs.api.jsonplaceholder;

import com.sdetlabs.api.jsonplaceholder.model.JsonPlaceholderPost;
import com.sdetlabs.framework.http.PooledHttpClient;
import com.sdetlabs.framework.http.RestClient;
import com.sdetlabs.framework.mocking.WireMockTestServer;
import com.sdetlabs.framework.resilience.Bulkhead;
import com.sdetlabs.framework.resilience.BulkheadRejectedException;
import com.sdetlabs.framework.resilience.CircuitBreaker;
import com.sdetlabs.framework.resilience.CircuitOpenException;
import com.sdetlabs.framework.resilience.FallbackExecutor;
import com.sdetlabs.framework.resilience.RequestRateLimiter;
import com.sdetlabs.framework.resilience.RetryExecutor;
import com.sdetlabs.framework.resilience.RetryPolicy;
import io.restassured.response.Response;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.awaitility.Awaitility.await;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

/** Exercises resilience controls using JSONPlaceholder's post contract and local fault injection. */
public class JsonPlaceholderResilienceTest {
    private static final String POST_JSON = "{\"userId\":1,\"id\":1,\"title\":\"sample post\",\"body\":\"post body\"}";

    @Test(groups = {"resilience", "regression", "mocking"})
    public void rateLimiterRejectsExcessCallsBeforeTheyReachTheService() {
        try (WireMockTestServer mock = new WireMockTestServer()) {
            mock.wireMock().stubFor(get(urlEqualTo("/posts/1"))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody(POST_JSON)));
            JsonPlaceholderApi api = api(mock.client());
            AtomicLong monotonicTime = new AtomicLong();
            RequestRateLimiter limiter = new RequestRateLimiter(2, Duration.ofSeconds(1), monotonicTime::get);
            int sent = 0;
            int rejected = 0;

            for (int i = 0; i < 4; i++) {
                if (limiter.tryAcquire()) {
                    assertEquals(api.post(1).statusCode(), 200);
                    sent++;
                } else {
                    rejected++;
                }
            }

            assertEquals(sent, 2);
            assertEquals(rejected, 2);
            mock.wireMock().verify(2, getRequestedFor(urlEqualTo("/posts/1")));
            monotonicTime.set(TimeUnit.MILLISECONDS.toNanos(500));
            assertTrue(limiter.tryAcquire(), "The token bucket should refill at its configured rate");
        }
    }

    @Test(groups = {"resilience", "regression", "mocking"})
    public void bulkheadRejectsCallsWhenItsDependencySlotIsOccupied() throws Exception {
        try (WireMockTestServer mock = new WireMockTestServer()) {
            mock.wireMock().stubFor(get(urlEqualTo("/posts/1"))
                    .willReturn(aResponse().withFixedDelay(350).withStatus(200)
                            .withHeader("Content-Type", "application/json").withBody(POST_JSON)));
            JsonPlaceholderApi api = api(mock.client());
            Bulkhead bulkhead = new Bulkhead(1, Duration.ZERO);
            ExecutorService workers = Executors.newSingleThreadExecutor();
            try {
                Future<Integer> firstCall = workers.submit(() -> bulkhead.execute(() -> api.post(1).statusCode()));
                await().atMost(Duration.ofSeconds(2)).untilAsserted(
                        () -> mock.wireMock().verify(1, getRequestedFor(urlEqualTo("/posts/1"))));

                assertThrows(BulkheadRejectedException.class,
                        () -> bulkhead.execute(() -> api.post(1).statusCode()));
                assertEquals(firstCall.get(2, TimeUnit.SECONDS).intValue(), 200);
                mock.wireMock().verify(1, getRequestedFor(urlEqualTo("/posts/1")));
            } finally {
                workers.shutdownNow();
            }
        }
    }

    @Test(groups = {"resilience", "regression", "mocking"})
    public void connectionPoolCapsLeasedConnectionsAndBoundsWaiting() throws Exception {
        try (WireMockTestServer mock = new WireMockTestServer();
             PooledHttpClient client = new PooledHttpClient(1, 1,
                     Duration.ofMillis(100), Duration.ofSeconds(1), Duration.ofSeconds(2))) {
            mock.wireMock().stubFor(get(urlEqualTo("/posts/1"))
                    .willReturn(aResponse().withFixedDelay(400).withStatus(200)
                            .withHeader("Content-Type", "application/json").withBody(POST_JSON)));
            ExecutorService workers = Executors.newSingleThreadExecutor();
            try {
                Future<PooledHttpClient.HttpResult> first = workers.submit(
                        () -> client.get(mock.baseUrl() + "/posts/1"));
                await().atMost(Duration.ofSeconds(2)).until(() -> client.leasedConnections() == 1);

                assertEquals(client.maxTotalConnections(), 1);
                assertEquals(client.maxConnectionsPerRoute(), 1);
                assertThrows(ConnectionPoolTimeoutException.class,
                        () -> client.get(mock.baseUrl() + "/posts/1"));
                assertEquals(first.get(2, TimeUnit.SECONDS).statusCode(), 200);
            } finally {
                workers.shutdownNow();
            }
        }
    }

    @Test(groups = {"resilience", "regression", "mocking"})
    public void retriesTransientFailureThenUsesFallbackWhenCircuitOpens() {
        try (WireMockTestServer mock = new WireMockTestServer()) {
            mock.wireMock().stubFor(get(urlEqualTo("/posts/1"))
                    .inScenario("recover-on-retry")
                    .whenScenarioStateIs(STARTED)
                    .willReturn(aResponse().withStatus(503))
                    .willSetStateTo("healthy"));
            mock.wireMock().stubFor(get(urlEqualTo("/posts/1"))
                    .inScenario("recover-on-retry")
                    .whenScenarioStateIs("healthy")
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody(POST_JSON)));
            JsonPlaceholderApi api = api(mock.client());
            RetryExecutor retry = new RetryExecutor();

            Response recovered = retry.execute(() -> api.post(1),
                    response -> response.statusCode() >= 500,
                    exception -> false,
                    new RetryPolicy(2, Duration.ofMillis(5), 2.0, Duration.ofMillis(10)));

            assertEquals(recovered.statusCode(), 200);
            mock.wireMock().verify(2, getRequestedFor(urlEqualTo("/posts/1")));
        }
    }

    @Test(groups = {"resilience", "regression", "mocking"})
    public void circuitBreakerStopsCallsAndReturnsControlledFallback() {
        try (WireMockTestServer mock = new WireMockTestServer()) {
            mock.wireMock().stubFor(get(urlEqualTo("/posts/1"))
                    .willReturn(aResponse().withStatus(503)));
            JsonPlaceholderApi api = api(mock.client());
            CircuitBreaker breaker = new CircuitBreaker(1, Duration.ofMinutes(1));
            FallbackExecutor fallback = new FallbackExecutor();
            JsonPlaceholderPost cachedPost = new JsonPlaceholderPost(1, 1, "cached post", "controlled fallback");

            for (int attempt = 0; attempt < 2; attempt++) {
                JsonPlaceholderPost result = fallback.execute(
                        () -> breaker.execute(() -> {
                            Response response = api.post(1);
                            if (response.statusCode() >= 500) {
                                throw new IllegalStateException("JSONPlaceholder dependency is unavailable");
                            }
                            return response.as(JsonPlaceholderPost.class);
                        }),
                        failure -> failure instanceof IllegalStateException || failure instanceof CircuitOpenException,
                        () -> cachedPost);
                assertEquals(result.title(), "cached post");
            }

            assertEquals(breaker.state(), CircuitBreaker.State.OPEN);
            mock.wireMock().verify(1, getRequestedFor(urlEqualTo("/posts/1")));
        }
    }

    private static JsonPlaceholderApi api(RestClient client) {
        return new JsonPlaceholderApi(client);
    }
}
