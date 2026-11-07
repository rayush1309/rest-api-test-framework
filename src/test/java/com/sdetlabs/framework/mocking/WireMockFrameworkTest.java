package com.sdetlabs.framework.mocking;

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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class WireMockFrameworkTest {
    @Test(groups = {"mocking", "positive", "regression"})
    public void loadsFileStubMatchesRequestAndCreatesDynamicResponse() {
        try (WireMockTestServer mock = new WireMockTestServer()) {
            RestClient client = mock.client();

            Response health = client.get("/mock/health");
            assertEquals(health.statusCode(), 200);
            assertEquals(health.jsonPath().getString("status"), "healthy");

            mock.wireMock().stubFor(post(urlPathEqualTo("/echo"))
                    .withQueryParam("tenant", equalTo("acme"))
                    .withHeader("X-Request-Source", equalTo("test-suite"))
                    .withRequestBody(matchingJsonPath("$.sku", equalTo("sku-123")))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {"sku":"{{jsonPath request.body '$.sku'}}",
                                     "requestId":"{{randomValue type='UUID'}}"}
                                    """)
                            .withTransformers("response-template")));

            Response echoed = client.post("/echo", client.newRequest()
                    .queryParam("tenant", "acme")
                    .header("X-Request-Source", "test-suite")
                    .body(Map.of("sku", "sku-123")));
            assertEquals(echoed.statusCode(), 201);
            assertEquals(echoed.jsonPath().getString("sku"), "sku-123");
            String requestId = echoed.jsonPath().getString("requestId");
            assertTrue(requestId.length() > 10, "The response should contain a generated UUID");

            mock.wireMock().verify(1, postRequestedFor(urlPathEqualTo("/echo"))
                    .withQueryParam("tenant", equalTo("acme"))
                    .withHeader("X-Request-Source", equalTo("test-suite"))
                    .withRequestBody(matchingJsonPath("$.sku", equalTo("sku-123"))));
        }
    }

    @Test(groups = {"mocking", "negative", "regression"})
    public void simulatesHttpErrorAndClientTimeout() throws Exception {
        try (WireMockTestServer mock = new WireMockTestServer()) {
            mock.wireMock().stubFor(get(urlEqualTo("/errors/unavailable"))
                    .willReturn(aResponse().withStatus(503).withBody("service unavailable")));
            mock.wireMock().stubFor(get(urlEqualTo("/slow"))
                    .willReturn(aResponse().withFixedDelay(500).withStatus(200).withBody("slow response")));

            assertEquals(mock.client().get("/errors/unavailable").statusCode(), 503);

            HttpRequest request = HttpRequest.newBuilder(URI.create(mock.baseUrl() + "/slow"))
                    .timeout(Duration.ofMillis(50))
                    .GET()
                    .build();
            boolean timedOut = false;
            try {
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            } catch (HttpTimeoutException expected) {
                timedOut = true;
            }
            assertTrue(timedOut, "The delayed WireMock response should exceed the client's timeout");
        }
    }
}
