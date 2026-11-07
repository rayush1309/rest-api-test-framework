package com.sdetlabs.api.booking.model;

import java.util.Objects;

public class BookingDates {
    private String checkin;
    private String checkout;

    public BookingDates() { }

    public BookingDates(String checkin, String checkout) {
        this.checkin = Objects.requireNonNull(checkin, "checkin must not be null");
        this.checkout = Objects.requireNonNull(checkout, "checkout must not be null");
    }

    public String getCheckin() { return checkin; }
    public void setCheckin(String checkin) { this.checkin = checkin; }
    public String getCheckout() { return checkout; }
    public void setCheckout(String checkout) { this.checkout = checkout; }
}
