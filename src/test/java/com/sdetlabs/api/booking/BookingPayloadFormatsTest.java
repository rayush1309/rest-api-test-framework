package com.sdetlabs.api.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdetlabs.api.booking.model.Booking;
import com.sdetlabs.api.booking.model.BookingAuth;
import com.sdetlabs.framework.BaseTest;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.testng.Assert.assertEquals;

public class BookingPayloadFormatsTest extends BaseTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test(groups = {"regression", "positive"})
    public void createsBookingFromPojo() {
        Booking booking = BookingTestData.booking();
        createAssertAndDelete(booking);
    }

    @Test(groups = {"regression", "positive"})
    public void createsBookingFromMap() {
        Booking booking = BookingTestData.booking();
        createAssertAndDelete(BookingTestData.asMap(booking));
    }

    @Test(groups = {"regression", "positive"})
    public void createsBookingFromJsonResource() throws IOException {
        Booking booking = BookingTestData.booking();
        JsonNode payload;
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("data/booking.json")) {
            if (stream == null) throw new IllegalStateException("Missing test resource data/booking.json");
            payload = JSON.readTree(stream);
        }
        ((com.fasterxml.jackson.databind.node.ObjectNode) payload).put("firstname", booking.getFirstname());
        ((com.fasterxml.jackson.databind.node.ObjectNode) payload).put("lastname", booking.getLastname());
        ((com.fasterxml.jackson.databind.node.ObjectNode) payload).put("totalprice", booking.getTotalprice());
        ((com.fasterxml.jackson.databind.node.ObjectNode) payload).put("depositpaid", booking.getDepositpaid());
        ((com.fasterxml.jackson.databind.node.ObjectNode) payload.path("bookingdates"))
                .put("checkin", booking.getBookingdates().getCheckin())
                .put("checkout", booking.getBookingdates().getCheckout());
        ((com.fasterxml.jackson.databind.node.ObjectNode) payload).put("additionalneeds", booking.getAdditionalneeds());
        createAssertAndDelete(payload.toString());
    }

    private void createAssertAndDelete(Object payload) {
        BookingApi api = new BookingApi(clientFor("restfulbooker"));
        String token = api.authenticate(new BookingAuth("admin", "password123")).jsonPath().getString("token");
        int id = -1;
        try {
            Response created = api.createBooking(payload);
            assertEquals(created.statusCode(), 200);
            id = created.jsonPath().getInt("bookingid");
            assertEquals(api.getBooking(id).statusCode(), 200);
            assertEquals(api.deleteBooking(id, token).statusCode(), 201);
            id = -1;
        } finally {
            if (id > 0) api.deleteBooking(id, token);
        }
    }
}
