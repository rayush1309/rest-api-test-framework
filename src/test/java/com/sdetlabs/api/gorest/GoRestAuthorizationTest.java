package com.sdetlabs.api.gorest;

import com.sdetlabs.framework.BaseTest;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;

public class GoRestAuthorizationTest extends BaseTest {
    @Test(groups = {"negative", "authentication", "security"})
    public void userAndPostWritesRequireBearerAuthentication() {
        GoRestApi api = GoRestTestSupport.api(config);
        Map<String, Object> user = Map.of("name", "Unauthorized Test", "email", GoRestTestSupport.uniqueEmail());
        Map<String, Object> post = Map.of("user_id", "00000000-0000-7000-8000-000000000000",
                "title", "Unauthorized Test", "body", "This request must not create a post.");

        assertEquals(api.createUserWithoutAuthentication(user).statusCode(), 401);
        assertEquals(api.createUserWithToken(user, "invalid-token-for-test").statusCode(), 401);
        assertEquals(api.createPostWithoutAuthentication(post).statusCode(), 401);
        assertEquals(api.createPostWithToken(post, "invalid-token-for-test").statusCode(), 401);
    }

    @Test(groups = {"negative", "authentication", "security"})
    public void sampleRowsCannotBeChangedWithoutAWriteToken() {
        GoRestApi api = GoRestTestSupport.api(config);
        String sampleUserId = api.users(Map.of("page[size]", 1)).jsonPath().getString("data[0].id");
        String token = GoRestTestSupport.accessToken();
        assertEquals(api.updateUser(sampleUserId, Map.of("name", "Attempted Sample Edit"), token).statusCode(), 403);
    }
}
