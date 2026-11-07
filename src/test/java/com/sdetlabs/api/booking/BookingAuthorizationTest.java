package com.sdetlabs.api.booking;

import com.sdetlabs.api.booking.model.Booking;
import com.sdetlabs.api.booking.model.BookingAuth;
import com.sdetlabs.framework.BaseTest;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class BookingAuthorizationTest extends BaseTest {
    @Test(groups = {"negative", "authentication", "security"})
    public void updateWithoutTokenIsForbidden() {
        verifyUnauthorizedUpdate(null);
    }

    @Test(groups = {"negative", "authentication", "security"})
    public void updateWithInvalidTokenIsForbidden() {
        verifyUnauthorizedUpdate("invalid-token-for-test");
    }

    private void verifyUnauthorizedUpdate(String token) {
        BookingApi api = new BookingApi(clientFor("restfulbooker"));
        Booking booking = BookingTestData.booking();
        String validToken = api.authenticate(new BookingAuth("admin", "password123"))
                .jsonPath().getString("token");
        int id = -1;
        try {
            Response created = api.createBooking(booking);
            assertEquals(created.statusCode(), 200);
            id = created.jsonPath().getInt("bookingid");

            Response update = token == null
                    ? api.updateBookingWithoutAuthentication(id, booking)
                    : api.updateBookingWithToken(id, booking, token);
            assertEquals(update.statusCode(), 403);
        } finally {
            if (id > 0) assertEquals(api.deleteBooking(id, validToken).statusCode(), 201);
        }
    }
}
