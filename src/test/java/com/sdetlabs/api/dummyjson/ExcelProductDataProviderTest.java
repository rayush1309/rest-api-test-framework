package com.sdetlabs.api.dummyjson;

import com.sdetlabs.framework.BaseTest;
import com.sdetlabs.framework.data.ExcelDataReader;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class ExcelProductDataProviderTest extends BaseTest {
    @Test(dataProvider = "productCases", groups = {"positive", "regression"})
    public void retrievesExcelDefinedProducts(String id, String expectedCategory) {
        Response response = DummyJsonTestSupport.api(config).product(Integer.parseInt(id));
        assertEquals(response.statusCode(), 200);
        assertEquals(response.jsonPath().getString("category"), expectedCategory);
    }

    @DataProvider(name = "productCases")
    public Object[][] productCases() throws IOException {
        List<Map<String, String>> rows = ExcelDataReader.read("data/product-cases.xlsx", "products");
        return rows.stream()
                .map(row -> new Object[]{row.get("productId"), row.get("expectedCategory")})
                .toArray(Object[][]::new);
    }
}
