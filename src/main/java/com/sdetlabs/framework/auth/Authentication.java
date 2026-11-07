package com.sdetlabs.framework.auth;

import io.restassured.specification.RequestSpecification;

import java.util.Objects;

/** Explicit per-request authentication; credentials are never stored in shared global state. */
public final class Authentication {
    private enum Scheme { NONE, BASIC, BEARER, COOKIE, API_KEY }

    private final Scheme scheme;
    private final String username;
    private final String secret;
    private final String name;

    private Authentication(Scheme scheme, String username, String secret, String name) {
        this.scheme = scheme;
        this.username = username;
        this.secret = secret;
        this.name = name;
    }

    public static Authentication none() {
        return new Authentication(Scheme.NONE, null, null, null);
    }

    public static Authentication basic(String username, String password) {
        return new Authentication(Scheme.BASIC, required(username, "username"), required(password, "password"), null);
    }

    public static Authentication bearer(String token) {
        return new Authentication(Scheme.BEARER, null, required(token, "token"), null);
    }

    public static Authentication cookie(String name, String value) {
        return new Authentication(Scheme.COOKIE, null, required(value, "cookie value"), required(name, "cookie name"));
    }

    public static Authentication apiKey(String headerName, String value) {
        return new Authentication(Scheme.API_KEY, null, required(value, "API key"), required(headerName, "header name"));
    }

    public RequestSpecification applyTo(RequestSpecification request) {
        Objects.requireNonNull(request, "Request specification must not be null");
        return switch (scheme) {
            case NONE -> request;
            case BASIC -> request.auth().preemptive().basic(username, secret);
            case BEARER -> request.header("Authorization", "Bearer " + secret);
            case COOKIE -> request.cookie(name, secret);
            case API_KEY -> request.header(name, secret);
        };
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}
