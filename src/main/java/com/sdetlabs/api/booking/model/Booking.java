package com.sdetlabs.api.booking.model;

import java.util.Objects;

public class Booking {
    private String firstname;
    private String lastname;
    private Integer totalprice;
    private Boolean depositpaid;
    private BookingDates bookingdates;
    private String additionalneeds;

    public Booking() { }

    public Booking(String firstname, String lastname, Integer totalprice, Boolean depositpaid,
                   BookingDates bookingdates, String additionalneeds) {
        this.firstname = Objects.requireNonNull(firstname, "firstname must not be null");
        this.lastname = Objects.requireNonNull(lastname, "lastname must not be null");
        this.totalprice = Objects.requireNonNull(totalprice, "totalprice must not be null");
        this.depositpaid = Objects.requireNonNull(depositpaid, "depositpaid must not be null");
        this.bookingdates = Objects.requireNonNull(bookingdates, "bookingdates must not be null");
        this.additionalneeds = additionalneeds;
    }

    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }
    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }
    public Integer getTotalprice() { return totalprice; }
    public void setTotalprice(Integer totalprice) { this.totalprice = totalprice; }
    public Boolean getDepositpaid() { return depositpaid; }
    public void setDepositpaid(Boolean depositpaid) { this.depositpaid = depositpaid; }
    public BookingDates getBookingdates() { return bookingdates; }
    public void setBookingdates(BookingDates bookingdates) { this.bookingdates = bookingdates; }
    public String getAdditionalneeds() { return additionalneeds; }
    public void setAdditionalneeds(String additionalneeds) { this.additionalneeds = additionalneeds; }
}
