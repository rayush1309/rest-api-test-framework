package com.sdetlabs.framework.logging;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Logs useful exchange details while masking common authentication and personal secrets. */
public final class SensitiveDataFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(SensitiveDataFilter.class);
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "cookie", "set-cookie", "x-api-key", "api-key");
    private static final Pattern JSON_SECRET = Pattern.compile(
            "(?i)(\\\"(?:password|(?:[a-z]+[_-]?)?token|(?:[a-z]+[_-]?)?secret|api[_-]?key|email|username|user_?name|name|"
                    + "reviewer_?(?:email|name)|phone|mobile|ssn|dob|date_?of_?birth|first_?name|last_?name|"
                    + "address|postal_?code|zip_?code|card_?(?:number|expire)|iban)"
                    + "\\\"\\s*:\\s*\\\")[^\\\"]*(\\\")");
    private static final Pattern JSON_NUMERIC_SECRET = Pattern.compile(
            "(?i)(\\\"(?:phone|mobile|ssn|postal_?code|zip_?code|card_?number)\\\"\\s*:\\s*)(-?\\d+(?:\\.\\d+)?|true|false|null)");
    private static final Pattern QUERY_SECRET = Pattern.compile(
            "(?i)([?&](?:(?:[a-z]+[_-]?)?token|(?:[a-z]+[_-]?)?secret|api[_-]?key|password|username|email|phone|mobile|ssn|dob|date_of_birth)=)[^&#]*");

    @Override
    public Response filter(FilterableRequestSpecification request,
                           FilterableResponseSpecification responseSpecification,
                           FilterContext context) {
        long started = System.nanoTime();
        LOG.info("HTTP {} {} headers={} body={}", request.getMethod(), sanitizeUrl(request.getURI()),
                safeHeaders(request.getHeaders()), sanitizeBody(request.getBody()));
        Response response = context.next(request, responseSpecification);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
        LOG.info("HTTP {} {} -> {} ({} ms) headers={} body={}", request.getMethod(),
                sanitizeUrl(request.getURI()), response.statusCode(), elapsedMs,
                safeHeaders(response.getHeaders()), sanitizeBody(response.getBody().asString()));
        HttpExchangeRecorder.record("HTTP " + request.getMethod() + " " + sanitizeUrl(request.getURI())
                + " (" + elapsedMs + " ms)\nRequest headers: " + safeHeaders(request.getHeaders())
                + "\nRequest body: " + sanitizeBody(request.getBody())
                + "\nStatus: " + response.statusCode()
                + "\nResponse headers: " + safeHeaders(response.getHeaders())
                + "\nResponse body: " + sanitizeBody(response.getBody().asString()));
        return response;
    }

    static String sanitizeUrl(String value) {
        return QUERY_SECRET.matcher(value).replaceAll("$1[REDACTED]");
    }

    static String sanitizeBody(Object body) {
        if (body == null) {
            return "<empty>";
        }
        if (body instanceof byte[]) {
            return "<binary payload omitted>";
        }
        String sanitized = JSON_SECRET.matcher(String.valueOf(body)).replaceAll("$1[REDACTED]$2");
        return JSON_NUMERIC_SECRET.matcher(sanitized).replaceAll("$1\"[REDACTED]\"");
    }

    private static String safeHeaders(Iterable<io.restassured.http.Header> headers) {
        StringBuilder result = new StringBuilder("{");
        boolean first = true;
        for (io.restassured.http.Header header : headers) {
            if (!first) result.append(", ");
            String name = header.getName();
            result.append(name).append('=').append(isSensitiveHeader(name)
                    ? "[REDACTED]" : header.getValue());
            first = false;
        }
        return result.append('}').toString();
    }

    private static boolean isSensitiveHeader(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return SENSITIVE_HEADERS.contains(normalized)
                || normalized.matches(".*(?:password|token|api[-_]?key|username|email|phone|ssn).*");
    }
}
