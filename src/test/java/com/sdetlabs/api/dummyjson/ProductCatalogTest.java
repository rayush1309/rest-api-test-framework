package com.sdetlabs.api.dummyjson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdetlabs.api.dummyjson.model.Product;
import com.sdetlabs.framework.BaseTest;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ProductCatalogTest extends BaseTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test(groups = {"smoke", "positive"})
    public void retrievesProductCollectionAndValidatesCoreFields() {
        Response response = api().products();
        assertEquals(response.statusCode(), 200);
        assertTrue(response.jsonPath().getInt("total") > 0);
        assertTrue(response.jsonPath().getList("products").size() > 0);
        assertTrue(response.jsonPath().getDouble("products[0].price") > 0);
        assertFalse(response.jsonPath().getString("products[0].title").isBlank());
    }

    @Test(groups = {"regression", "positive"})
    public void retrievesProductAndValidatesPriceAndCategory() {
        Response response = api().product(1);
        assertEquals(response.statusCode(), 200);
        assertEquals(response.jsonPath().getInt("id"), 1);
        assertTrue(response.jsonPath().getDouble("price") > 0);
        assertTrue(response.jsonPath().getDouble("rating") >= 0);
        assertFalse(response.jsonPath().getString("category").isBlank());
        Product product = response.as(Product.class);
        assertEquals(product.getId().intValue(), 1);
        assertTrue(product.getPrice() > 0);
    }

    @Test(groups = {"regression", "positive"})
    public void searchesProductsAndReturnsEmptyForUnmatchedTerm() {
        Response match = api().searchProducts("phone");
        assertEquals(match.statusCode(), 200);
        assertTrue(match.jsonPath().getInt("total") > 0);
        assertTrue(match.jsonPath().getList("products").size() > 0);

        Response noMatch = api().searchProducts("no-match-sdet-" + System.nanoTime());
        assertEquals(noMatch.statusCode(), 200);
        assertEquals(noMatch.jsonPath().getInt("total"), 0);
        assertTrue(noMatch.jsonPath().getList("products").isEmpty());
    }

    @Test(groups = {"regression", "positive"})
    public void filtersByCategoryAndRetrievesBothCategoryFormats() throws Exception {
        Response byCategory = api().productsByCategory("smartphones");
        assertEquals(byCategory.statusCode(), 200);
        assertTrue(byCategory.jsonPath().getInt("total") > 0);
        for (String category : byCategory.jsonPath().getList("products.category", String.class)) {
            assertEquals(category, "smartphones");
        }

        Response slugs = api().productCategoryList();
        assertEquals(slugs.statusCode(), 200);
        assertTrue(slugs.jsonPath().getList("$").contains("smartphones"));

        JsonNode categoryObjects = JSON.readTree(api().productCategories().asString());
        assertTrue(categoryObjects.isArray());
        assertTrue(categoryObjects.toString().contains("\"slug\":\"smartphones\""));
    }

    @Test(groups = {"regression", "positive"})
    public void paginatesAndSortsProductResults() {
        Response page = api().products(Map.of("limit", 5, "skip", 10));
        assertEquals(page.statusCode(), 200);
        assertEquals(page.jsonPath().getInt("limit"), 5);
        assertEquals(page.jsonPath().getInt("skip"), 10);
        assertEquals(page.jsonPath().getList("products").size(), 5);

        Response sorted = api().products(Map.of("sortBy", "price", "order", "asc"));
        assertEquals(sorted.statusCode(), 200);
        java.util.List<Float> prices = sorted.jsonPath().getList("products.price", Float.class);
        for (int i = 1; i < prices.size(); i++) {
            assertTrue(prices.get(i - 1) <= prices.get(i), "Product prices should be ascending");
        }
    }

    @Test(groups = {"negative", "regression"})
    public void unknownProductIdReturnsNotFound() {
        assertEquals(api().product(-1).statusCode(), 404);
    }

    private DummyJsonApi api() { return DummyJsonTestSupport.api(config); }
}
