package com.sdetlabs.api.dummyjson;

import com.sdetlabs.api.dummyjson.model.Cart;
import com.sdetlabs.framework.BaseTest;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DummyJsonCustomerJourneyTest extends BaseTest {
    @Test(groups = {"smoke", "e2e", "positive"},
            description = "Login, discover a product, read the user's carts, and validate simulated cart writes")
    public void completesDummyJsonCustomerJourney() {
        DummyJsonApi api = DummyJsonTestSupport.api(config);
        Response login = api.login(DummyJsonTestSupport.validLogin(config));
        assertEquals(login.statusCode(), 200);
        String accessToken = login.jsonPath().getString("accessToken");
        int userId = login.jsonPath().getInt("id");

        Response profile = api.currentUser(accessToken);
        assertEquals(profile.statusCode(), 200);
        assertEquals(profile.jsonPath().getInt("id"), userId);
        assertEquals(profile.jsonPath().getString("username"), config.getRequired("dummyjson.test.username"));

        Response search = api.searchProducts("phone");
        assertEquals(search.statusCode(), 200);
        assertTrue(search.jsonPath().getInt("total") > 0);
        int productId = search.jsonPath().getInt("products[0].id");
        Response product = api.product(productId);
        assertEquals(product.statusCode(), 200);
        assertTrue(product.jsonPath().getDouble("price") > 0);

        Response userCarts = api.cartsForUser(userId);
        assertEquals(userCarts.statusCode(), 200);
        assertTrue(userCarts.jsonPath().getList("carts") != null);

        Cart cart = DummyJsonTestSupport.cart(userId, productId, 2);
        Response added = api.addCart(cart);
        assertEquals(added.statusCode(), 201);
        assertEquals(added.jsonPath().getInt("userId"), userId);
        assertEquals(added.jsonPath().getInt("totalQuantity"), 2);
        assertEquals(added.jsonPath().getInt("products[0].id"), productId);
        assertTrue(added.jsonPath().getDouble("total") > 0);

        // The API explicitly simulates writes; use a seeded cart for the separate update simulation.
        Response updated = api.updateCart(1,
                Map.of("merge", true, "products", java.util.List.of(Map.of("id", productId, "quantity", 1))));
        assertEquals(updated.statusCode(), 200);
        assertEquals(updated.jsonPath().getInt("id"), 1);
    }
}
