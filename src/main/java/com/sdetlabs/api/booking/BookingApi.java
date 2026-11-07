package com.sdetlabs.api.booking;

import com.sdetlabs.api.booking.model.BookingAuth;
import com.sdetlabs.framework.auth.Authentication;
import com.sdetlabs.framework.http.RestClient;
import io.restassured.response.Response;

import java.util.Map;
import java.util.Objects;

/** Restful Booker endpoint wrapper. It contains no business assertions. */
public final class BookingApi {
    private static final String BOOKING_PATH = "/booking";
    private final RestClient client;

    public BookingApi(RestClient client) {
        this.client = Objects.requireNonNull(client, "RestClient must not be null");
    }

    public Response ping() {
        return client.get("/ping");
    }

    public Response authenticate(BookingAuth credentials) {
        return client.post("/auth", credentials);
    }

    public Response authenticate(Map<String, ?> credentials) {
        return client.post("/auth", credentials);
    }

    public Response getBookingIds() {
        return client.get(BOOKING_PATH);
    }

    public Response getBookingIds(Map<String, ?> filters) {
        return client.get(BOOKING_PATH, client.newRequest().queryParams(filters));
    }

    public Response getBooking(int id) {
        return client.get(BOOKING_PATH + "/" + id);
    }

    public Response createBooking(Object payload) {
        return client.post(BOOKING_PATH, payload);
    }

    public Response updateBooking(int id, Object payload, String token) {
        return client.put(BOOKING_PATH + "/" + id, authenticated(token).body(payload));
    }

    public Response partiallyUpdateBooking(int id, Object payload, String token) {
        return client.patch(BOOKING_PATH + "/" + id, authenticated(token).body(payload));
    }

    public Response deleteBooking(int id, String token) {
        return client.delete(BOOKING_PATH + "/" + id, authenticated(token));
    }

    public Response updateBookingWithoutAuthentication(int id, Object payload) {
        return client.put(BOOKING_PATH + "/" + id, client.newRequest().body(payload));
    }

    public Response updateBookingWithToken(int id, Object payload, String token) {
        return client.put(BOOKING_PATH + "/" + id,
                client.newAuthenticatedRequest(Authentication.cookie("token", token)).body(payload));
    }

    private io.restassured.specification.RequestSpecification authenticated(String token) {
        return client.newAuthenticatedRequest(Authentication.cookie("token", token));
    }
}
