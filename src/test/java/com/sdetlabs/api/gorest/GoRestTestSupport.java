package com.sdetlabs.api.gorest;

import com.sdetlabs.framework.config.ConfigManager;
import com.sdetlabs.framework.http.RestClient;
import org.testng.SkipException;

import java.util.UUID;

final class GoRestTestSupport {
    private GoRestTestSupport() { }

    static GoRestApi api(ConfigManager config) {
        return new GoRestApi(RestClient.forService(config, "gorest"));
    }

    static String accessToken() {
        String token = System.getProperty("gorest.access.token");
        if (token == null || token.isBlank()) token = System.getenv("GOREST_ACCESS_TOKEN");
        if (token == null || token.isBlank()) {
            throw new SkipException("Set GOREST_ACCESS_TOKEN (users:read, users:write, posts:read and posts:write scopes) to run GoREST write scenarios");
        }
        return token.trim();
    }

    static String uniqueEmail() {
        return "sdet." + UUID.randomUUID() + "@example.com";
    }

    static String uniqueTitle() {
        return "API Automation " + UUID.randomUUID();
    }
}
