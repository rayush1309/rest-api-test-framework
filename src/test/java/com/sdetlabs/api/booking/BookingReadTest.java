package com.sdetlabs.api.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdetlabs.api.booking.model.Booking;
import com.sdetlabs.api.booking.model.BookingAuth;
import com.sdetlabs.framework.BaseTest;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class BookingReadTest extends BaseTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test(groups = {"smoke", "positive"})
    public void healthCheckAndBookingIdCollectionAreAvailable() {
        BookingApi api = api();
        assertEquals(api.ping().statusCode(), 201);
        Response response = api.getBookingIds();
        assertEquals(response.statusCode(), 200);
        assertTrue(response.jsonPath().getList("bookingid").size() > 0,
                "The practice service should return its existing booking IDs");
    }

    @Test(groups = {"regression", "positive"})
    public void retrievesBookingUsingCombinedNameFiltersAndExercisesDateFilters() throws Exception {
        BookingApi api = api();
        Booking booking = BookingTestData.booking();
        String token = token(api);
        int id = -1;
        try {
            Response created = api.createBooking(booking);
            assertEquals(created.statusCode(), 200);
            id = created.jsonPath().getInt("bookingid");

            Map<String, Object> nameFilters = new HashMap<>();
            nameFilters.put("firstname", booking.getFirstname());
            nameFilters.put("lastname", booking.getLastname());
            JsonNode ids = JSON.readTree(api.getBookingIds(nameFilters).asString());

            boolean found = false;
            for (JsonNode item : ids) found |= item.path("bookingid").asInt() == id;
            assertTrue(found, "Combined first and last name filters should include the created booking");

            Map<String, Object> dateFilters = Map.of(
                    "checkin", booking.getBookingdates().getCheckin(),
                    "checkout", booking.getBookingdates().getCheckout());
            Response dateFiltered = api.getBookingIds(dateFilters);
            assertEquals(dateFiltered.statusCode(), 200);
            assertTrue(JSON.readTree(dateFiltered.asString()).isArray(),
                    "Date filter requests should return the documented booking ID array contract");
        } finally {
            if (id > 0) assertEquals(api.deleteBooking(id, token).statusCode(), 201);
        }
    }

    @Test(groups = {"negative", "regression"})
    public void unknownBookingIdReturnsNotFound() {
        assertEquals(api().getBooking(-1).statusCode(), 404);
    }

    private String token(BookingApi api) {
        return api.authenticate(new BookingAuth("admin", "password123")).jsonPath().getString("token");
    }

    private BookingApi api() {
        return new BookingApi(clientFor("restfulbooker"));
    }
}
