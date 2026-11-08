package com.sdetlabs.api.jsonplaceholder;

import com.sdetlabs.framework.BaseTest;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/** Small live smoke check against the public JSONPlaceholder API. */
public class JsonPlaceholderSmokeTest extends BaseTest {
    @Test(groups = {"smoke", "open-source-api"},
            description = "Reads a real post from the open-source JSONPlaceholder API")
    public void readsPostFromOpenSourceApi() {
        JsonPlaceholderApi api = new JsonPlaceholderApi(clientFor("jsonplaceholder"));
        Response response = api.post(1);

        assertEquals(response.statusCode(), 200);
        assertEquals(response.jsonPath().getInt("id"), 1);
        assertEquals(response.jsonPath().getInt("userId"), 1);
        assertNotNull(response.jsonPath().getString("title"));
    }
}
