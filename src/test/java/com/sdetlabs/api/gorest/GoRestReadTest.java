package com.sdetlabs.api.gorest;

import com.sdetlabs.framework.BaseTest;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class GoRestReadTest extends BaseTest {
    @Test(groups = {"smoke", "positive"})
    public void listsAndRetrievesUsersUsingV3EnvelopeAndUuidIds() {
        GoRestApi api = api();
        Response list = api.users(Map.of("page[size]", 3));
        assertEquals(list.statusCode(), 200);
        assertTrue(list.jsonPath().getList("data").size() > 0);
        assertTrue(list.jsonPath().getMap("meta").containsKey("has_more"));

        String id = list.jsonPath().getString("data[0].id");
        assertFalse(id.isBlank());
        Response user = api.user(id);
        assertEquals(user.statusCode(), 200);
        assertEquals(user.jsonPath().getString("id"), id);
        assertFalse(user.jsonPath().getString("email").isBlank());
        assertTrue(user.jsonPath().getString("status").matches("active|inactive"));
    }

    @Test(groups = {"regression", "positive"})
    public void retrievesPostsAssociatedWithAUser() {
        GoRestApi api = api();
        String userId = api.users(Map.of("page[size]", 1)).jsonPath().getString("data[0].id");
        Response posts = api.posts(Map.of("filter[user_id]", userId, "page[size]", 5));
        assertEquals(posts.statusCode(), 200);
        for (String postUserId : posts.jsonPath().getList("data.user_id", String.class)) {
            assertEquals(postUserId, userId);
        }
    }

    @Test(groups = {"negative", "regression"})
    public void unknownUserAndPostIdsReturnNotFound() {
        GoRestApi api = api();
        String unknownId = "00000000-0000-7000-8000-000000000000";
        assertEquals(api.user(unknownId).statusCode(), 404);
        assertEquals(api.post(unknownId).statusCode(), 404);
    }

    @Test(groups = {"negative", "regression"})
    public void malformedUuidPathIsRejected() {
        assertEquals(api().user("not-a-uuid").statusCode(), 400);
    }

    private GoRestApi api() { return GoRestTestSupport.api(config); }
}
