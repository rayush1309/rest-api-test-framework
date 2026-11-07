package com.sdetlabs.api.dummyjson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdetlabs.api.dummyjson.model.Cart;
import com.sdetlabs.framework.BaseTest;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/** Cart write endpoints are simulations; assertions are against their returned payloads only. */
public class CartSimulationTest extends BaseTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test(groups = {"smoke", "positive"})
    public void retrievesCartsByCollectionIdAndUser() {
        DummyJsonApi api = api();
        Response all = api.carts();
        assertEquals(all.statusCode(), 200);
        assertTrue(all.jsonPath().getInt("total") > 0);
        assertTrue(all.jsonPath().getList("carts").size() > 0);

        Response one = api.cart(1);
        assertEquals(one.statusCode(), 200);
        assertEquals(one.jsonPath().getInt("id"), 1);
        assertTrue(one.jsonPath().getList("products").size() > 0);

        Response user = api.cartsForUser(5);
        assertEquals(user.statusCode(), 200);
        assertTrue(user.jsonPath().getList("carts").size() > 0);
        for (Integer userId : user.jsonPath().getList("carts.userId", Integer.class)) {
            assertEquals(userId.intValue(), 5);
        }
    }

    @Test(groups = {"regression", "positive", "mocking"})
    public void simulatesCartAddAndValidatesQuantitiesAndTotals() throws Exception {
        Cart request = DummyJsonTestSupport.cart(1, 1, 2, 2, 1);
        Response response = api().addCart(request);
        assertEquals(response.statusCode(), 201);
        assertEquals(response.jsonPath().getInt("userId"), 1);
        assertEquals(response.jsonPath().getInt("totalProducts"), 2);
        assertEquals(response.jsonPath().getInt("totalQuantity"), 3);
        assertEquals(response.jsonPath().getList("products").size(), 2);

        JsonNode products = JSON.readTree(response.asString()).path("products");
        double expectedTotal = 0;
        double expectedDiscountedTotal = 0;
        for (JsonNode product : products) {
            int quantity = product.path("quantity").asInt();
            double price = product.path("price").asDouble();
            assertTrue(quantity > 0);
            assertTrue(Math.abs(product.path("total").asDouble() - price * quantity) <= 0.01);
            expectedTotal += product.path("total").asDouble();
            expectedDiscountedTotal += product.path("discountedPrice").asDouble();
        }
        assertTrue(Math.abs(response.jsonPath().getDouble("total") - expectedTotal) <= 0.01);
        assertTrue(Math.abs(response.jsonPath().getDouble("discountedTotal") - expectedDiscountedTotal) <= 0.05);
    }

    @Test(groups = {"regression", "positive", "mocking"})
    public void simulatesCartUpdateAndDelete() {
        DummyJsonApi api = api();
        Map<String, Object> update = Map.of("merge", true, "products", java.util.List.of(Map.of("id", 1, "quantity", 1)));
        Response put = api.updateCart(1, update);
        assertEquals(put.statusCode(), 200);
        assertEquals(put.jsonPath().getInt("id"), 1);
        assertTrue(put.jsonPath().getList("products").size() > 0);

        Response patch = api.partiallyUpdateCart(1,
                Map.of("products", java.util.List.of(Map.of("id", 1, "quantity", 2))));
        assertEquals(patch.statusCode(), 200);
        assertEquals(patch.jsonPath().getInt("id"), 1);

        Response deleted = api.deleteCart(1);
        assertEquals(deleted.statusCode(), 200);
        assertTrue(deleted.jsonPath().getBoolean("isDeleted"));
        assertTrue(deleted.jsonPath().getString("deletedOn") != null);
    }

    @Test(groups = {"negative", "mocking"})
    public void simulatedAddOmitsAnUnknownProduct() {
        Response response = api().addCart(DummyJsonTestSupport.cart(1, 999999, 1));
        assertEquals(response.statusCode(), 201);
        assertEquals(response.jsonPath().getInt("totalProducts"), 0);
        assertTrue(response.jsonPath().getList("products").isEmpty());
    }

    @Test(groups = {"negative", "mocking"})
    public void simulatedAddReturnsNegativeQuantityInsteadOfRejectingIt() {
        Response response = api().addCart(DummyJsonTestSupport.cart(1, 1, -2));
        assertEquals(response.statusCode(), 201);
        assertEquals(response.jsonPath().getInt("totalQuantity"), -2);
        assertTrue(response.jsonPath().getDouble("total") < 0,
                "This practice API currently accepts negative quantities; the test records that limitation");
    }

    private DummyJsonApi api() { return DummyJsonTestSupport.api(config); }
}
