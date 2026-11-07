package com.sdetlabs.api.booking;

import com.sdetlabs.api.booking.model.Booking;
import com.sdetlabs.api.booking.model.BookingDates;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

final class BookingTestData {
    private BookingTestData() { }

    static Booking booking() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return new Booking("Sdet" + suffix, "Guest" + suffix, 5000, true,
                new BookingDates(LocalDate.now().plusDays(10).toString(), LocalDate.now().plusDays(15).toString()),
                "Breakfast");
    }

    static Map<String, Object> asMap(Booking booking) {
        return Map.of(
                "firstname", booking.getFirstname(),
                "lastname", booking.getLastname(),
                "totalprice", booking.getTotalprice(),
                "depositpaid", booking.getDepositpaid(),
                "bookingdates", Map.of(
                        "checkin", booking.getBookingdates().getCheckin(),
                        "checkout", booking.getBookingdates().getCheckout()),
                "additionalneeds", booking.getAdditionalneeds());
    }
}
