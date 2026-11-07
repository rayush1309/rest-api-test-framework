package com.sdetlabs.framework;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdetlabs.framework.auth.Authentication;
import com.sdetlabs.framework.config.FrameworkConfigurationException;
import com.sdetlabs.framework.http.RestClient;
import com.sdetlabs.framework.validation.ResponseSpecifications;
import com.sun.net.httpserver.HttpServer;
import io.restassured.response.Response;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public class FoundationTest extends BaseTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private HttpServer server;
    private RestClient localClient;

    @BeforeClass(alwaysRun = true)
    public void startLocalService() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            String body = new String(requestBody, StandardCharsets.UTF_8).replace("\\", "\\\\").replace("\"", "\\\"");
            String response = "{\"method\":\"" + exchange.getRequestMethod() + "\",\"path\":\""
                    + exchange.getRequestURI() + "\",\"body\":\"" + body + "\"}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        localClient = new RestClient("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterClass(alwaysRun = true)
    public void stopLocalService() {
        if (server != null) server.stop(0);
    }

    @Test(groups = {"smoke", "positive"})
    public void loadsEnvironmentDefaultsAndValidatesBaseUrls() {
        assertEquals(config.environment(), "qa");
        assertEquals(config.getBaseUrl("dummyjson"), "https://dummyjson.com");
        assertThrows(FrameworkConfigurationException.class, () -> config.getBaseUrl("unknown-service"));
    }

    @Test(groups = {"smoke", "positive"})
    public void supportsEachHttpVerbWithIndependentRequestSpecifications() throws Exception {
        Response get = localClient.get("/items/{id}", localClient.newRequest()
                .queryParam("q", "red shoes")
                .pathParam("id", 3));
        Response post = localClient.post("/post", Map.of("name", "sample"));
        Response put = localClient.put("/put", Map.of("name", "updated"));
        Response patch = localClient.patch("/patch", Map.of("name", "partial"));
        Response delete = localClient.delete("/delete");

        assertVerbAndStatus(get, "GET");
        assertTrue(get.asString().contains("/items/3?q=red+shoes")
                || get.asString().contains("/items/3?q=red%20shoes"));
        assertVerbAndStatus(post, "POST");
        assertVerbAndStatus(put, "PUT");
        assertVerbAndStatus(patch, "PATCH");
        assertVerbAndStatus(delete, "DELETE");
        assertEquals(JSON.readTree(post.asString()).path("body").asText(), "{\"name\":\"sample\"}");
    }

    @Test(groups = {"smoke", "positive"})
    public void supportsPerRequestAuthenticationAndEndpointSpecificResponseSpecs() {
        Response response = localClient.get("/auth", localClient.newAuthenticatedRequest(
                Authentication.bearer("test-secret")));
        response.then().spec(ResponseSpecifications.expectStatusAndContentType(200, "application/json"));
        assertEquals(response.statusCode(), 200);
    }

    private static void assertVerbAndStatus(Response response, String expectedMethod) throws Exception {
        assertEquals(response.statusCode(), 200);
        JsonNode json = JSON.readTree(response.asString());
        assertEquals(json.path("method").asText(), expectedMethod);
    }
}
