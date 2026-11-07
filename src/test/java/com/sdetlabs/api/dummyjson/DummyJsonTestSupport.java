package com.sdetlabs.api.dummyjson;

import com.sdetlabs.api.dummyjson.model.Cart;
import com.sdetlabs.api.dummyjson.model.CartProduct;
import com.sdetlabs.api.dummyjson.model.LoginRequest;
import com.sdetlabs.framework.config.ConfigManager;
import com.sdetlabs.framework.http.RestClient;

import java.util.List;

final class DummyJsonTestSupport {
    private DummyJsonTestSupport() { }

    static DummyJsonApi api(ConfigManager config) {
        return new DummyJsonApi(RestClient.forService(config, "dummyjson"));
    }

    static LoginRequest validLogin(ConfigManager config) {
        return new LoginRequest(config.getRequired("dummyjson.test.username"),
                config.getRequired("dummyjson.test.password"));
    }

    static Cart cart(int userId, int... productIdQuantityPairs) {
        if (productIdQuantityPairs.length == 0 || productIdQuantityPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Products must be specified as id/quantity pairs");
        }
        List<CartProduct> products = new java.util.ArrayList<>();
        for (int i = 0; i < productIdQuantityPairs.length; i += 2) {
            products.add(new CartProduct(productIdQuantityPairs[i], productIdQuantityPairs[i + 1]));
        }
        return new Cart(userId, products);
    }
}
