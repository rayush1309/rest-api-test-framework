package com.sdetlabs.framework.logging;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SensitiveDataFilterTest {
    @Test(groups = {"security", "positive", "regression"})
    public void masksSecretsInJsonAndQueryStrings() {
        String body = SensitiveDataFilter.sanitizeBody("{\"password\":\"p@ss\",\"access_token\":\"abc\","
                + "\"accessToken\":\"camel-access\",\"refreshToken\":\"camel-refresh\","
                + "\"paymentToken\":\"payment-token-value\",\"clientSecret\":\"client-secret-value\","
                + "\"email\":\"sam@example.test\",\"firstName\":\"Sam\","
                + "\"lastName\":\"User\",\"cardNumber\":\"4111111111111111\","
                + "\"reviewerEmail\":\"reviewer@example.test\",\"reviewerName\":\"Review Person\","
                + "\"phone\":14155550123,\"postalCode\":94105,\"name\":\"Sensitive Name\","
                + "\"title\":\"Visible label\"}");
        String url = SensitiveDataFilter.sanitizeUrl(
                "https://example.test/login?token=abc&refreshToken=secret&email=sam%40example.test&limit=2");

        assertFalse(body.contains("p@ss"));
        assertFalse(body.contains("abc"));
        assertFalse(body.contains("camel-access"));
        assertFalse(body.contains("camel-refresh"));
        assertFalse(body.contains("payment-token-value"));
        assertFalse(body.contains("client-secret-value"));
        assertFalse(body.contains("sam@example.test"));
        assertFalse(body.contains("Sam"));
        assertFalse(body.contains("User"));
        assertFalse(body.contains("4111111111111111"));
        assertFalse(body.contains("reviewer@example.test"));
        assertFalse(body.contains("Review Person"));
        assertFalse(body.contains("14155550123"));
        assertFalse(body.contains("94105"));
        assertFalse(body.contains("Sensitive Name"));
        assertTrue(body.contains("Visible label"));
        assertTrue(body.contains("[REDACTED]"));
        assertFalse(url.contains("token=abc"));
        assertFalse(url.contains("refreshToken=secret"));
        assertFalse(url.contains("sam%40example.test"));
        assertTrue(url.contains("limit=2"));
    }
}
