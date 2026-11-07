package com.sdetlabs.api.dummyjson;

import com.sdetlabs.api.dummyjson.model.LoginRequest;
import com.sdetlabs.framework.BaseTest;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

public class DummyJsonAuthenticationTest extends BaseTest {
    @Test(groups = {"smoke", "positive", "authentication"})
    public void loginReturnsAccessAndRefreshTokens() {
        Response response = api().login(DummyJsonTestSupport.validLogin(config));
        assertEquals(response.statusCode(), 200);
        assertEquals(response.jsonPath().getString("username"), config.getRequired("dummyjson.test.username"));
        assertNotNull(response.jsonPath().getString("accessToken"));
        assertNotNull(response.jsonPath().getString("refreshToken"));
        assertFalse(response.jsonPath().getString("accessToken").isBlank());
    }

    @Test(dataProvider = "invalidCredentials", groups = {"negative", "authentication"})
    public void invalidCredentialsAreRejected(LoginRequest credentials) {
        Response response = api().login(credentials);
        assertEquals(response.statusCode(), 400);
        assertFalse(response.jsonPath().getString("message").isBlank());
    }

    @Test(groups = {"negative", "authentication"})
    public void missingCredentialsAreRejected() {
        Response response = api().login(java.util.Map.of());
        assertEquals(response.statusCode(), 400);
        assertFalse(response.jsonPath().getString("message").isBlank());
    }

    @Test(groups = {"smoke", "positive", "authentication"})
    public void refreshesTokensAndUsesNewAccessToken() {
        DummyJsonApi api = api();
        Response login = api.login(DummyJsonTestSupport.validLogin(config));
        assertEquals(login.statusCode(), 200);

        Response refreshed = api.refresh(login.jsonPath().getString("refreshToken"), 30);
        assertEquals(refreshed.statusCode(), 200);
        String accessToken = refreshed.jsonPath().getString("accessToken");
        assertNotNull(accessToken);
        assertFalse(accessToken.isBlank());
        assertEquals(api.currentUser(accessToken).statusCode(), 200);
    }

    @Test(groups = {"negative", "authentication", "security"})
    public void missingAndInvalidTokensCannotReadAuthenticatedUser() {
        DummyJsonApi api = api();
        assertEquals(api.currentUserWithoutToken().statusCode(), 401);
        assertEquals(api.currentUser("invalid-token-for-test").statusCode(), 401);
    }

    @DataProvider(name = "invalidCredentials")
    public Object[][] invalidCredentials() {
        return new Object[][]{
                {new LoginRequest("missing-user", config.getRequired("dummyjson.test.password"))},
                {new LoginRequest(config.getRequired("dummyjson.test.username"), "invalid-password")}
        };
    }

    private DummyJsonApi api() { return DummyJsonTestSupport.api(config); }
}
