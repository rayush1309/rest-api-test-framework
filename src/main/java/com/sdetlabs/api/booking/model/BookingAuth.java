package com.sdetlabs.api.booking.model;

import java.util.Objects;

public class BookingAuth {
    private String username;
    private String password;

    public BookingAuth() { }

    public BookingAuth(String username, String password) {
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.password = Objects.requireNonNull(password, "password must not be null");
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
