package com.sdetlabs.framework.data;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Reads JSON fixtures from the test classpath and maps them with Jackson. */
public final class JsonDataReader {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonDataReader() { }

    public static <T> T read(String resourcePath, Class<T> type) throws IOException {
        try (InputStream input = open(resourcePath)) {
            return MAPPER.readValue(input, type);
        }
    }

    public static String readText(String resourcePath) throws IOException {
        try (InputStream input = open(resourcePath)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

private static InputStream open(String resourcePath) {
        InputStream input = JsonDataReader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (input == null) {
            throw new IllegalArgumentException("Test resource not found on classpath: " + resourcePath);
        }
        return input;
    }
}
