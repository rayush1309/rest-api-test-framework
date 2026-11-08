package com.sdetlabs.api.jsonplaceholder;

import com.sdetlabs.framework.http.RestClient;
import io.restassured.response.Response;

import java.util.Objects;

/** Thin client for the public JSONPlaceholder posts API. */
public final class JsonPlaceholderApi {
    private final RestClient client;

    public JsonPlaceholderApi(RestClient client) {
        this.client = Objects.requireNonNull(client, "RestClient must not be null");
    }

    public Response post(int id) {
        if (id < 1) throw new IllegalArgumentException("Post ID must be positive");
        return client.get("/posts/" + id);
    }
}
