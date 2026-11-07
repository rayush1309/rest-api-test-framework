package com.sdetlabs.api.dummyjson.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginRequest {
    private String username;
    private String password;
    private Integer expiresInMins;

    public LoginRequest() { }

    public LoginRequest(String username, String password) {
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.password = Objects.requireNonNull(password, "password must not be null");
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getExpiresInMins() { return expiresInMins; }
    public void setExpiresInMins(Integer expiresInMins) { this.expiresInMins = expiresInMins; }
}
