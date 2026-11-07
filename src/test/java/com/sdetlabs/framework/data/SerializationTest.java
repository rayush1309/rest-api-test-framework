package com.sdetlabs.framework.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdetlabs.api.booking.model.Booking;
import com.sdetlabs.api.booking.model.BookingDates;
import com.sdetlabs.api.dummyjson.model.Product;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class SerializationTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test(groups = {"positive", "regression"})
    public void serializesAndDeserializesNestedPojo() throws Exception {
        Booking original = new Booking("Asha", "Rao", 2500, true,
                new BookingDates("2027-01-10", "2027-01-12"), "Breakfast");
        String json = mapper.writeValueAsString(original);
        Booking restored = mapper.readValue(json, Booking.class);

        assertEquals(restored.getFirstname(), original.getFirstname());
        assertEquals(restored.getBookingdates().getCheckout(), "2027-01-12");
        assertEquals(JsonPath.from(json).getString("bookingdates.checkin"), "2027-01-10");
    }

    @Test(groups = {"positive", "regression"})
    public void serializesMapPayloadAndReadsJsonResource() throws Exception {
        Map<String, Object> payload = Map.of(
                "firstname", "Map",
                "lastname", "Payload",
                "bookingdates", Map.of("checkin", "2027-02-01", "checkout", "2027-02-03"));
        String json = mapper.writeValueAsString(payload);
        Map<String, Object> roundTrip = mapper.readValue(json, new TypeReference<>() { });
        assertEquals(JsonPath.from(mapper.writeValueAsString(roundTrip)).getString("bookingdates.checkout"), "2027-02-03");

        Booking fixture = JsonDataReader.read("data/booking.json", Booking.class);
        assertEquals(fixture.getFirstname(), "Placeholder");
        assertEquals(fixture.getBookingdates().getCheckin(), "2030-01-10");
        assertTrue(JsonDataReader.readText("data/booking.json").contains("additionalneeds"));
    }

    @Test(groups = {"positive", "regression"})
    public void pojoMappingAllowsUnknownAndOptionalResponseFields() throws IOException {
        Product product = mapper.readValue("""
                {"id":1,"title":"Example","category":"beauty","price":9.99,"futureField":"added later"}
                """, Product.class);
        assertEquals(product.getId().intValue(), 1);
        assertEquals(product.getTitle(), "Example");
        assertNull(product.getStock(), "Absent optional fields should remain null");
    }
}
