package com.sdetlabs.framework.http;

import com.sdetlabs.framework.auth.Authentication;
import com.sdetlabs.framework.config.ConfigManager;
import com.sdetlabs.framework.logging.SensitiveDataFilter;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/** Stateless HTTP facade. Every call receives a fresh request specification. */
public final class RestClient {
    private final String baseUrl;

    public RestClient(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Base URL must not be blank");
        }
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public static RestClient forService(ConfigManager config, String service) {
        Objects.requireNonNull(config, "ConfigManager must not be null");
        return new RestClient(config.getBaseUrl(service));
    }

    public RequestSpecification newRequest() {
        return RestAssured.given()
                .baseUri(baseUrl)
                .accept("application/json")
                .contentType("application/json")
                .filter(new SensitiveDataFilter());
    }

    public RequestSpecification newAuthenticatedRequest(Authentication authentication) {
        return Objects.requireNonNull(authentication, "Authentication must not be null")
                .applyTo(newRequest());
    }

    public Response get(String endpoint) {
        return get(endpoint, newRequest());
    }

    public Response get(String endpoint, RequestSpecification request) {
        return executeWithRateLimit(request, endpoint, RequestSpecification::get, true);
    }

    public Response post(String endpoint, Object body) {
        return post(endpoint, newRequest().body(body));
    }

    public Response post(String endpoint, RequestSpecification request) {
        return executeWithRateLimit(request, endpoint, RequestSpecification::post, false);
    }

    public Response put(String endpoint, Object body) {
        return put(endpoint, newRequest().body(body));
    }

    public Response put(String endpoint, RequestSpecification request) {
        return executeWithRateLimit(request, endpoint, RequestSpecification::put, false);
    }

    public Response patch(String endpoint, Object body) {
        return patch(endpoint, newRequest().body(body));
    }

    public Response patch(String endpoint, RequestSpecification request) {
        return executeWithRateLimit(request, endpoint, RequestSpecification::patch, false);
    }

    public Response delete(String endpoint) {
        return delete(endpoint, newRequest());
    }

    public Response delete(String endpoint, RequestSpecification request) {
        return executeWithRateLimit(request, endpoint, RequestSpecification::delete, false);
    }

    private Response executeWithRateLimit(RequestSpecification request, String endpoint,
                                          Function<RequestSpecification, Response> httpMethod,
                                          boolean retrySafe) {
        Response response = httpMethod.apply(execute(request, endpoint));
        for (int retry = 0; retrySafe && response.statusCode() == 429 && retry < 3; retry++) {
            if (!waitBeforeRetry(response, retry)) break;
            response = httpMethod.apply(execute(request, endpoint));
        }
        return response;
    }

    private boolean waitBeforeRetry(Response response, int retry) {
        long delay = retryAfterMillis(response);
        if (delay < 0) delay = Math.min(TimeUnit.SECONDS.toMillis(retry + 1L), TimeUnit.SECONDS.toMillis(30));
        try {
            TimeUnit.MILLISECONDS.sleep(delay);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private long retryAfterMillis(Response response) {
        String retryAfter = response.getHeader("Retry-After");
        if (retryAfter == null || retryAfter.isBlank()) return -1;
        try {
            return Math.min(TimeUnit.SECONDS.toMillis(Math.max(Long.parseLong(retryAfter.trim()), 0)),
                    TimeUnit.SECONDS.toMillis(30));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private RequestSpecification execute(RequestSpecification request, String endpoint) {
        Objects.requireNonNull(request, "Request specification must not be null");
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("Endpoint must not be blank");
        }
        return request.baseUri(baseUrl).basePath(endpoint);
    }
}
