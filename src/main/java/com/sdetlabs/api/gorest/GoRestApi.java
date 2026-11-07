package com.sdetlabs.api.gorest;

import com.sdetlabs.framework.auth.Authentication;
import com.sdetlabs.framework.http.RestClient;
import io.restassured.response.Response;

import java.util.Map;
import java.util.Objects;

/** GoREST v3 endpoint wrapper. IDs are UUID strings in the current v3 contract. */
public final class GoRestApi {
    private final RestClient client;

    public GoRestApi(RestClient client) {
        this.client = Objects.requireNonNull(client, "RestClient must not be null");
    }

    public Response users(Map<String, ?> queryParams) {
        return client.get("/users", client.newRequest().queryParams(queryParams));
    }

    public Response user(String id) { return client.get("/users/" + id); }

    public Response createUser(Object user, String token) {
        return client.post("/users", authorized(token).body(user));
    }

    public Response createUserWithoutAuthentication(Object user) {
        return client.post("/users", client.newRequest().body(user));
    }

    public Response createUserWithToken(Object user, String token) {
        return client.post("/users", authorized(token).body(user));
    }

    public Response updateUser(String id, Object user, String token) {
        return client.patch("/users/" + id, authorized(token).body(user));
    }

    public Response deleteUser(String id, String token) {
        return client.delete("/users/" + id, authorized(token));
    }

    public Response posts(Map<String, ?> queryParams) {
        return client.get("/posts", client.newRequest().queryParams(queryParams));
    }

    public Response post(String id) { return client.get("/posts/" + id); }

    public Response createPost(Object post, String token) {
        return client.post("/posts", authorized(token).body(post));
    }

    public Response createPostWithoutAuthentication(Object post) {
        return client.post("/posts", client.newRequest().body(post));
    }

    public Response createPostWithToken(Object post, String token) {
        return client.post("/posts", authorized(token).body(post));
    }

    public Response updatePost(String id, Object post, String token) {
        return client.patch("/posts/" + id, authorized(token).body(post));
    }

    public Response deletePost(String id, String token) {
        return client.delete("/posts/" + id, authorized(token));
    }

    private io.restassured.specification.RequestSpecification authorized(String token) {
        return client.newAuthenticatedRequest(Authentication.bearer(token));
    }
}
