package com.sdetlabs.api.dummyjson;

import com.sdetlabs.framework.auth.Authentication;
import com.sdetlabs.framework.http.RestClient;
import io.restassured.response.Response;

import java.util.Map;
import java.util.Objects;

/** DummyJSON endpoint wrapper. Write operations on this API are simulations. */
public final class DummyJsonApi {
    private final RestClient client;

    public DummyJsonApi(RestClient client) {
        this.client = Objects.requireNonNull(client, "RestClient must not be null");
    }

    public Response login(Object credentials) { return client.post("/auth/login", credentials); }

    public Response currentUser(String accessToken) {
        return client.get("/auth/me", client.newAuthenticatedRequest(Authentication.bearer(accessToken)));
    }

    public Response currentUserWithoutToken() { return client.get("/auth/me"); }

    public Response refresh(String refreshToken, int expiresInMins) {
        return client.post("/auth/refresh", Map.of("refreshToken", refreshToken, "expiresInMins", expiresInMins));
    }

    public Response products() { return client.get("/products"); }
    public Response products(Map<String, ?> queryParams) {
        return client.get("/products", client.newRequest().queryParams(queryParams));
    }
    public Response product(int id) { return client.get("/products/" + id); }
    public Response searchProducts(String query) {
        return client.get("/products/search", client.newRequest().queryParam("q", query));
    }
    public Response productCategories() { return client.get("/products/categories"); }
    public Response productCategoryList() { return client.get("/products/category-list"); }
    public Response productsByCategory(String category) { return client.get("/products/category/" + category); }

    public Response carts() { return client.get("/carts"); }
    public Response cart(int id) { return client.get("/carts/" + id); }
    public Response cartsForUser(int userId) { return client.get("/carts/user/" + userId); }
    public Response addCart(Object cart) { return client.post("/carts/add", cart); }
    public Response updateCart(int id, Object cart) { return client.put("/carts/" + id, cart); }
    public Response partiallyUpdateCart(int id, Object cart) { return client.patch("/carts/" + id, cart); }
    public Response deleteCart(int id) { return client.delete("/carts/" + id); }
}
