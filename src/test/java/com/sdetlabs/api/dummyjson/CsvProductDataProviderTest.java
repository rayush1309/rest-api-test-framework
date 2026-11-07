package com.sdetlabs.api.dummyjson;

import com.sdetlabs.framework.BaseTest;
import com.sdetlabs.framework.data.CsvDataReader;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class CsvProductDataProviderTest extends BaseTest {
    @Test(dataProvider = "productCases", groups = {"positive", "regression"})
    public void retrievesCsvDefinedProducts(String id, String expectedCategory, String description) {
        Response response = DummyJsonTestSupport.api(config).product(Integer.parseInt(id));
        assertEquals(response.statusCode(), 200, description);
        assertEquals(response.jsonPath().getString("category"), expectedCategory, description);
    }

    @DataProvider(name = "productCases")
    public Object[][] productCases() throws IOException {
        List<Map<String, String>> rows = CsvDataReader.read("data/product-cases.csv");
        return rows.stream()
                .map(row -> new Object[]{row.get("productId"), row.get("expectedCategory"), row.get("description")})
                .toArray(Object[][]::new);
    }
}
