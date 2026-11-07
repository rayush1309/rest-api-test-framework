package com.sdetlabs.api.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdetlabs.api.booking.model.BookingAuth;
import com.sdetlabs.framework.BaseTest;
import com.sdetlabs.framework.validation.ResponseSpecifications;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

public class BookingAuthenticationTest extends BaseTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test(groups = {"smoke", "positive", "authentication"})
    public void authenticatesAndReturnsReusableToken() throws Exception {
        Response response = api().authenticate(new BookingAuth("admin", "password123"));
        response.then().spec(ResponseSpecifications.expectStatusAndContentType(200, "application/json"));

        JsonNode body = JSON.readTree(response.asString());
        assertNotNull(body.path("token").textValue(), "Successful authentication must return a token");
        assertFalse(body.path("token").textValue().isBlank(), "Token must not be blank");
    }

    @Test(dataProvider = "invalidCredentials", groups = {"negative", "authentication"})
    public void rejectsInvalidCredentials(Map<String, String> credentials) throws Exception {
        Response response = api().authenticate(credentials);
        assertEquals(response.statusCode(), 200, "The documented API reports auth failure in its response body");
        assertEquals(JSON.readTree(response.asString()).path("reason").asText(), "Bad credentials");
    }

    @Test(groups = {"negative", "authentication"})
    public void rejectsMissingCredentials() throws Exception {
        Response response = api().authenticate(Map.of());
        assertEquals(response.statusCode(), 200);
        assertEquals(JSON.readTree(response.asString()).path("reason").asText(), "Bad credentials");
    }

    @DataProvider(name = "invalidCredentials")
    public Object[][] invalidCredentials() {
        return new Object[][]{
                {Map.of("username", "not-admin", "password", "password123")},
                {Map.of("username", "admin", "password", "wrong-password")}
        };
    }

    private BookingApi api() {
        return new BookingApi(clientFor("restfulbooker"));
    }
}
