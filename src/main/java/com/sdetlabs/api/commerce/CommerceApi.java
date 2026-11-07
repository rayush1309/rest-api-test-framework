package com.sdetlabs.api.commerce;

import com.sdetlabs.framework.http.RestClient;
import io.restassured.response.Response;

import java.util.Objects;

/** Endpoint client for the local simulated storefront and fulfilment services. */
public final class CommerceApi {
    private final RestClient client;

    public CommerceApi(RestClient client) {
        this.client = Objects.requireNonNull(client, "RestClient must not be null");
    }

    public Response product(String productId) { return client.get("/products/" + productId); }
    public Response inventory(String sku) { return client.get("/inventory/" + sku); }
    public Response reserveInventory(Object request) { return client.post("/inventory/reservations", request); }
    public Response releaseInventory(Object request) { return client.post("/inventory/releases", request); }
    public Response createCart(Object request) { return client.post("/carts", request); }

    public Response addCartItem(String cartId, Object request) {
        return client.post("/carts/" + cartId + "/items", request);
    }

    public Response validateAddress(Object request) { return client.post("/addresses/validate", request); }
    public Response createOrder(Object request) { return client.post("/orders", request); }
    public Response order(String orderId) { return client.get("/orders/" + orderId); }
    public Response pay(Object request) { return client.post("/payments", request); }
    public Response createShipment(Object request) { return client.post("/shipments", request); }
    public Response shipment(String trackingId) { return client.get("/shipments/" + trackingId); }
    public Response delivery(String trackingId) { return client.get("/deliveries/" + trackingId); }

    public Response notifications(String orderId) {
        return client.get("/notifications", client.newRequest().queryParam("orderId", orderId));
    }

    public Response cancelOrder(String orderId) {
        return client.post("/orders/" + orderId + "/cancel", client.newRequest());
    }
    public Response refund(Object request) { return client.post("/refunds", request); }
}
