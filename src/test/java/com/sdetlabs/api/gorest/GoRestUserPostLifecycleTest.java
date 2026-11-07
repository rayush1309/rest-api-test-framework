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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/** Exercises the full user/post lifecycle against a local stateful GoREST contract stub. */
public class GoRestUserPostLifecycleTest {
    private static final String TEST_TOKEN = "lifecycle-test-token";

    @Test(groups = {"smoke", "e2e", "positive"},
            description = "Create, read, update, associate and delete GoREST v3 user and post resources")
    public void completesUserAndPostLifecycle() throws Exception {
        try (LifecycleServer server = new LifecycleServer()) {
            GoRestApi api = server.api();
            User newUser = new User("SDET " + GoRestTestSupport.uniqueTitle(),
                    GoRestTestSupport.uniqueEmail(), "active");
            String userId = null;
            String postId = null;
            boolean postDeleted = false;
            boolean userDeleted = false;

            try {
                Response createdUser = api.createUser(newUser, TEST_TOKEN);
                assertEquals(createdUser.statusCode(), 201, createdUser.asString());
                userId = createdUser.jsonPath().getString("id");
                assertFalse(userId.isBlank(), "GoREST v3 returns a UUID string user id");
                assertEquals(createdUser.jsonPath().getString("email"), newUser.getEmail());
                assertFalse(createdUser.jsonPath().getBoolean("read_only"));

                Response retrievedUser = api.user(userId);
                assertEquals(retrievedUser.statusCode(), 200, retrievedUser.asString());
                assertEquals(retrievedUser.jsonPath().getString("name"), newUser.getName());
                assertEquals(retrievedUser.jsonPath().getString("status"), "active");

                String updatedName = "Updated " + GoRestTestSupport.uniqueTitle();
                Response updatedUser = api.updateUser(userId,
                        Map.of("name", updatedName, "status", "inactive"), TEST_TOKEN);
                assertEquals(updatedUser.statusCode(), 200, updatedUser.asString());
                assertEquals(api.user(userId).jsonPath().getString("name"), updatedName);
                assertEquals(api.user(userId).jsonPath().getString("email"), newUser.getEmail(),
                        "PATCH must leave fields absent from the request unchanged");
                assertEquals(api.user(userId).jsonPath().getString("status"), "inactive");

                Post newPost = new Post(userId, GoRestTestSupport.uniqueTitle(),
                        "Post created by the GoREST E2E test.");
                Response createdPost = api.createPost(newPost, TEST_TOKEN);
                assertEquals(createdPost.statusCode(), 201, createdPost.asString());
                postId = createdPost.jsonPath().getString("id");
                assertEquals(createdPost.jsonPath().getString("user_id"), userId,
                        "The post must reference the user created in this workflow");

                Response postList = api.posts(Map.of("filter[user_id]", userId));
                assertEquals(postList.statusCode(), 200, postList.asString());
                boolean associatedPostFound = false;
                for (String listedPostId : postList.jsonPath().getList("data.id", String.class)) {
                    associatedPostFound |= listedPostId.equals(postId);
                }
                assertTrue(associatedPostFound, "The user's post listing should include the new post");
                assertEquals(api.post(postId).jsonPath().getString("user_id"), userId);

                Response updatedPost = api.updatePost(postId,
                        Map.of("title", "Updated " + newPost.getTitle()), TEST_TOKEN);
                assertEquals(updatedPost.statusCode(), 200, updatedPost.asString());
                assertEquals(api.post(postId).jsonPath().getString("user_id"), userId,
                        "Post update must preserve its user association");
                assertTrue(api.post(postId).jsonPath().getString("title").startsWith("Updated "));

                assertEquals(api.deletePost(postId, TEST_TOKEN).statusCode(), 204);
                postDeleted = true;
                assertEquals(api.post(postId).statusCode(), 404);
                assertEquals(api.deleteUser(userId, TEST_TOKEN).statusCode(), 204);
                userDeleted = true;
                assertEquals(api.user(userId).statusCode(), 404);
            } finally {
                if (postId != null && !postDeleted) api.deletePost(postId, TEST_TOKEN);
                if (userId != null && !userDeleted) api.deleteUser(userId, TEST_TOKEN);
            }
        }
    }

    private static final class LifecycleServer implements AutoCloseable {
        private final ObjectMapper mapper = new ObjectMapper();
        private final Map<String, Map<String, Object>> users = new LinkedHashMap<>();
        private final Map<String, Map<String, Object>> posts = new LinkedHashMap<>();
        private final HttpServer server;

        private LifecycleServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle);
            server.start();
        }

        private GoRestApi api() {
            return new GoRestApi(new RestClient("http://127.0.0.1:" + server.getAddress().getPort()));
        }

        private void handle(HttpExchange exchange) throws IOException {
            try (exchange) {
                if (isWrite(exchange) && !("Bearer " + TEST_TOKEN)
                        .equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                    respond(exchange, 401, Map.of("message", "Unauthorized"));
                    return;
                }
                String path = exchange.getRequestURI().getPath();
                String method = exchange.getRequestMethod();
                if ("POST".equals(method) && "/users".equals(path)) {
                    createUser(exchange);
                } else if (path.startsWith("/users/")) {
                    handleUser(exchange, path.substring("/users/".length()));
                } else if ("POST".equals(method) && "/posts".equals(path)) {
                    createPost(exchange);
                } else if ("GET".equals(method) && "/posts".equals(path)) {
                    respond(exchange, 200, Map.of("data", new ArrayList<>(posts.values()), "meta", Map.of()));
                } else if (path.startsWith("/posts/")) {
                    handlePost(exchange, path.substring("/posts/".length()));
                } else {
                    respond(exchange, 404, Map.of("message", "Not found"));
                }
            }
        }

        private void createUser(HttpExchange exchange) throws IOException {
            Map<String, Object> user = readBody(exchange);
            user.put("id", UUID.randomUUID().toString());
            user.put("read_only", false);
            users.put((String) user.get("id"), user);
            respond(exchange, 201, user);
        }

        private void handleUser(HttpExchange exchange, String id) throws IOException {
            Map<String, Object> user = users.get(id);
            if (user == null) {
                respond(exchange, 404, Map.of("message", "User not found"));
            } else if ("GET".equals(exchange.getRequestMethod())) {
                respond(exchange, 200, user);
            } else if ("PATCH".equals(exchange.getRequestMethod())) {
                user.putAll(readBody(exchange));
                respond(exchange, 200, user);
            } else if ("DELETE".equals(exchange.getRequestMethod())) {
                users.remove(id);
                respondEmpty(exchange, 204);
            } else {
                respond(exchange, 405, Map.of("message", "Method not allowed"));
            }
        }

        private void createPost(HttpExchange exchange) throws IOException {
            Map<String, Object> post = readBody(exchange);
            String userId = String.valueOf(post.get("user_id"));
            if (!users.containsKey(userId)) {
                respond(exchange, 422, Map.of("message", "Unknown user_id"));
                return;
            }
            post.put("id", UUID.randomUUID().toString());
            posts.put((String) post.get("id"), post);
            respond(exchange, 201, post);
        }

        private void handlePost(HttpExchange exchange, String id) throws IOException {
            Map<String, Object> post = posts.get(id);
            if (post == null) {
                respond(exchange, 404, Map.of("message", "Post not found"));
            } else if ("GET".equals(exchange.getRequestMethod())) {
                respond(exchange, 200, post);
            } else if ("PATCH".equals(exchange.getRequestMethod())) {
                post.putAll(readBody(exchange));
                respond(exchange, 200, post);
            } else if ("DELETE".equals(exchange.getRequestMethod())) {
                posts.remove(id);
                respondEmpty(exchange, 204);
            } else {
                respond(exchange, 405, Map.of("message", "Method not allowed"));
            }
        }

        private Map<String, Object> readBody(HttpExchange exchange) throws IOException {
            return mapper.readValue(exchange.getRequestBody(), new TypeReference<>() { });
        }

        private static boolean isWrite(HttpExchange exchange) {
            return !"GET".equals(exchange.getRequestMethod());
        }

        private void respond(HttpExchange exchange, int status, Object body) throws IOException {
            byte[] bytes = mapper.writeValueAsBytes(body);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }

        private static void respondEmpty(HttpExchange exchange, int status) throws IOException {
            exchange.sendResponseHeaders(status, -1);
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
