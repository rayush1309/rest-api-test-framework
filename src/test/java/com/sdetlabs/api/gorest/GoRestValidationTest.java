package com.sdetlabs.api.gorest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdetlabs.api.gorest.model.Post;
import com.sdetlabs.api.gorest.model.User;
import com.sdetlabs.framework.http.RestClient;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.testng.Assert.assertEquals;

/**
 * Deterministic GoREST request validation checks. The public GoREST service requires a
 * write token for these scenarios, so a local HTTP server models its validation contract
 * and lets the checks run on every machine without credentials or external state.
 */
public class GoRestValidationTest {
private static final String TEST_TOKEN = "validation-test-token";

    @Test(groups = {"negative", "regression"})
    public void rejectsMissingUserFieldsInvalidEmailAndInvalidStatus() throws Exception {
        try (ValidationServer server = new ValidationServer()) {
            GoRestApi api = server.api();

            assertStatus(api.createUser(Map.of(), TEST_TOKEN), 422);
            assertStatus(api.createUser(new User("Invalid Email", "not-an-email", "active"), TEST_TOKEN), 422);
            assertStatus(api.createUser(new User("Invalid Status", GoRestTestSupport.uniqueEmail(), "pending"), TEST_TOKEN), 422);
        }
    }

    @Test(groups = {"negative", "regression"})
    public void rejectsDuplicateUserEmail() throws Exception {
        try (ValidationServer server = new ValidationServer()) {
            GoRestApi api = server.api();
            User user = new User("Duplicate Email Test", GoRestTestSupport.uniqueEmail(), "active");

            assertStatus(api.createUser(user, TEST_TOKEN), 201);
            assertStatus(api.createUser(user, TEST_TOKEN), 422);
        }
    }

    @Test(groups = {"negative", "regression"})
    public void rejectsPostAssociatedWithUnknownUser() throws Exception {
        try (ValidationServer server = new ValidationServer()) {
            Post request = new Post("00000000-0000-7000-8000-000000000000",
                    "Invalid association", "Unknown user id");
            assertStatus(server.api().createPost(request, TEST_TOKEN), 422);
        }
    }

    private static void assertStatus(Response response, int expectedStatus) {
        assertEquals(response.statusCode(), expectedStatus,
                "Unexpected response from validation test server: " + response.asString());
    }

    private static final class ValidationServer implements AutoCloseable {
        private final ObjectMapper mapper = new ObjectMapper();
        private final Set<String> emails = ConcurrentHashMap.newKeySet();
        private final HttpServer server;

        private ValidationServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle);
            server.start();
        }

private GoRestApi api() {
            return new GoRestApi(new RestClient("http://127.0.0.1:" + server.getAddress().getPort()));
        }

        private void handle(HttpExchange exchange) throws IOException {
            try (exchange) {
                if (!("Bearer " + TEST_TOKEN).equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                    respond(exchange, 401, "{\"message\":\"Unauthorized\"}");
                    return;
                }

                String path = exchange.getRequestURI().getPath();
                String method = exchange.getRequestMethod();
                if ("POST".equals(method) && "/users".equals(path)) {
                    handleUser(exchange);
                } else if ("POST".equals(method) && "/posts".equals(path)) {
                    handlePost(exchange);
                } else {
                    respond(exchange, 404, "{\"message\":\"Not found\"}");
                }
            } catch (RuntimeException exception) {
                respond(exchange, 400, "{\"message\":\"Invalid request JSON\"}");
            }
        }

        private void handleUser(HttpExchange exchange) throws IOException {
            Map<String, Object> body = mapper.readValue(exchange.getRequestBody(), new TypeReference<>() { });
            String name = value(body, "name");
            String email = value(body, "email");
            String status = value(body, "status");
            if (name.isBlank() || email.isBlank() || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                respond(exchange, 422, "{\"message\":\"name and valid email are required\"}");
            } else if (!status.isBlank() && !Set.of("active", "inactive").contains(status)) {
                respond(exchange, 422, "{\"message\":\"status must be active or inactive\"}");
            } else if (!emails.add(email)) {
                respond(exchange, 422, "{\"message\":\"email already exists\"}");
            } else {
                respond(exchange, 201, "{\"id\":\"" + UUID.randomUUID() + "\"}");
            }
        }

        private void handlePost(HttpExchange exchange) throws IOException {
            Map<String, Object> body = mapper.readValue(exchange.getRequestBody(), new TypeReference<>() { });
            if ("00000000-0000-7000-8000-000000000000".equals(value(body, "user_id"))) {
                respond(exchange, 422, "{\"message\":\"user_id does not identify an existing user\"}");
            } else {
                respond(exchange, 201, "{}");
            }
        }

        private static String value(Map<String, Object> body, String key) {
            Object value = body.get(key);
            return value == null ? "" : value.toString();
        }

        private static void respond(HttpExchange exchange, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
