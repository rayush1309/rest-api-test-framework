package com.sdetlabs.framework.validation;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;

import java.util.Objects;

/** Creates endpoint-specific response specifications; no status is assumed globally. */
public final class ResponseSpecifications {
    private ResponseSpecifications() { }

    public static ResponseSpecification expectStatus(int statusCode) {
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("HTTP status code must be between 100 and 599");
        }
        return new ResponseSpecBuilder().expectStatusCode(statusCode).build();
    }

    public static ResponseSpecification expectStatusAndContentType(int statusCode, String contentType) {
        Objects.requireNonNull(contentType, "Content type must not be null");
        return new ResponseSpecBuilder().expectStatusCode(statusCode).expectContentType(contentType).build();
    }
}
