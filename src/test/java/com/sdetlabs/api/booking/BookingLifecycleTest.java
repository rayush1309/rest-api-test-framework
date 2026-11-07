package com.sdetlabs.api.booking;

import com.sdetlabs.api.booking.model.Booking;
import com.sdetlabs.api.booking.model.BookingAuth;
import com.sdetlabs.framework.BaseTest;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class BookingLifecycleTest extends BaseTest {
    @Test(groups = {"smoke", "e2e", "positive"}, description = "Create, read, replace, partially update, then delete a booking")
    public void completesBookingLifecycleWithCookieAuthentication() {
        BookingApi api = new BookingApi(clientFor("restfulbooker"));
        String token = api.authenticate(new BookingAuth("admin", "password123"))
                .jsonPath().getString("token");
        Booking original = BookingTestData.booking();
        int id = -1;
        boolean deleted = false;
        try {
            Response created = api.createBooking(original);
            assertEquals(created.statusCode(), 200);
            id = created.jsonPath().getInt("bookingid");
            assertNotEquals(id, 0);

            assertBooking(api.getBooking(id), original);

            Booking replacement = BookingTestData.booking();
            Response updated = api.updateBooking(id, replacement, token);
            assertEquals(updated.statusCode(), 200);
            assertBooking(api.getBooking(id), replacement);

            Response patched = api.partiallyUpdateBooking(id, Map.of("firstname", "Patched"), token);
            assertEquals(patched.statusCode(), 200);
            assertEquals(api.getBooking(id).jsonPath().getString("firstname"), "Patched");
            assertEquals(api.getBooking(id).jsonPath().getString("lastname"), replacement.getLastname(),
                    "PATCH must preserve fields omitted from the request");
            assertEquals(api.getBooking(id).jsonPath().getInt("totalprice"), replacement.getTotalprice());
            assertEquals(api.getBooking(id).jsonPath().getBoolean("depositpaid"), replacement.getDepositpaid());
            assertEquals(api.getBooking(id).jsonPath().getString("additionalneeds"), replacement.getAdditionalneeds());
            assertEquals(api.getBooking(id).jsonPath().getString("bookingdates.checkin"),
                    replacement.getBookingdates().getCheckin());
            assertEquals(api.getBooking(id).jsonPath().getString("bookingdates.checkout"),
                    replacement.getBookingdates().getCheckout());

            assertEquals(api.deleteBooking(id, token).statusCode(), 201);
            deleted = true;
            assertEquals(api.getBooking(id).statusCode(), 404,
                    "A deleted booking should no longer be available as an active booking");
        } finally {
            if (!deleted && id > 0) api.deleteBooking(id, token);
        }
    }

    private static void assertBooking(Response response, Booking expected) {
        assertEquals(response.statusCode(), 200);
        assertEquals(response.jsonPath().getString("firstname"), expected.getFirstname());
        assertEquals(response.jsonPath().getString("lastname"), expected.getLastname());
        assertEquals(response.jsonPath().getInt("totalprice"), expected.getTotalprice());
        assertEquals(response.jsonPath().getBoolean("depositpaid"), expected.getDepositpaid());
        assertEquals(response.jsonPath().getString("bookingdates.checkin"), expected.getBookingdates().getCheckin());
        assertEquals(response.jsonPath().getString("bookingdates.checkout"), expected.getBookingdates().getCheckout());
        assertEquals(response.jsonPath().getString("additionalneeds"), expected.getAdditionalneeds());
    }
}
