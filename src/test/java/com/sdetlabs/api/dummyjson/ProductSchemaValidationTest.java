package com.sdetlabs.api.dummyjson;

import com.sdetlabs.framework.BaseTest;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.testng.Assert.assertEquals;

public class ProductSchemaValidationTest extends BaseTest {
    @Test(groups = {"schema", "positive", "regression"})
    public void productResponseMatchesDocumentedContractSchema() {
        Response response = DummyJsonTestSupport.api(config).product(1);
        assertEquals(response.statusCode(), 200);
        response.then().body(matchesJsonSchemaInClasspath("schemas/product.schema.json"));
    }
}
