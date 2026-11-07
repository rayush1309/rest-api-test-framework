package com.sdetlabs.api.commerce;

import com.sdetlabs.framework.mocking.WireMockTestServer;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/** Complete simulated storefront journey with independently verifiable fulfilment side effects. */
public class OrderProcessingE2ETest {
    private static final String SKU = "sku-9001";
    private static final String CUSTOMER_ID = "customer-42";
    private static final String CHECKOUT = "e2e-checkout";
    private static final String CANCELLATION = "e2e-cancellation";
    private static final String INVENTORY = "e2e-inventory";

    @Test(groups = {"e2e", "smoke", "positive", "mocking", "regression"})
    public void discoversProductAndCompletesOrderThroughRefund() {
        try (WireMockTestServer mock = new WireMockTestServer()) {
            CommerceApi api = new CommerceApi(mock.client());
            stubStorefront(mock);
            stubCheckout(mock);
            stubCancellation(mock);

            PreparedOrder prepared = prepareOrder(api, "checkout", 2);

            Response payment = api.pay(Map.of("orderId", prepared.orderId(), "amount", 99.98,
                    "paymentToken", "payment-token-approved"));
            assertEquals(payment.statusCode(), 201, payment.asString());
            assertEquals(payment.jsonPath().getString("status"), "authorized");
            String transactionId = payment.jsonPath().getString("transactionId");

            Response shipment = api.createShipment(Map.of("orderId", prepared.orderId()));
            assertEquals(shipment.statusCode(), 202, shipment.asString());
            String trackingId = shipment.jsonPath().getString("trackingId");
            assertEquals(shipment.jsonPath().getString("status"), "shipped");

            Response inTransit = api.shipment(trackingId);
            assertEquals(inTransit.jsonPath().getString("status"), "in_transit");
            Response delivery = api.delivery(trackingId);
            assertEquals(delivery.statusCode(), 200, delivery.asString());
            assertEquals(delivery.jsonPath().getString("status"), "delivered");

            Response notifications = api.notifications(prepared.orderId());
            assertEquals(notifications.statusCode(), 200, notifications.asString());
            List<String> events = notifications.jsonPath().getList("events", String.class);
            assertTrue(events.containsAll(List.of("order_confirmed", "payment_authorized",
                    "shipment_dispatched", "delivery_completed")), "Missing lifecycle notifications: " + events);

            Response refund = api.refund(Map.of("orderId", prepared.orderId(),
                    "transactionId", transactionId, "amount", 99.98));
            assertEquals(refund.statusCode(), 202, refund.asString());
            assertEquals(refund.jsonPath().getString("status"), "refunded");
            assertEquals(api.order(prepared.orderId()).jsonPath().getString("status"), "refunded");

            verifyCheckoutRequests(mock, prepared.orderId(), transactionId, trackingId);
        }
    }

    @Test(groups = {"e2e", "negative", "mocking", "regression"})
    public void cancelsUnpaidOrderAndReleasesReservedInventory() {
        try (WireMockTestServer mock = new WireMockTestServer()) {
            CommerceApi api = new CommerceApi(mock.client());
            stubStorefront(mock);
            stubCheckout(mock);
            stubCancellation(mock);

            PreparedOrder prepared = prepareOrder(api, "cancel", 1);
            Response cancellation = api.cancelOrder(prepared.orderId());
            assertEquals(cancellation.statusCode(), 200, cancellation.asString());
            assertEquals(cancellation.jsonPath().getString("status"), "cancelled");

            Response released = api.releaseInventory(Map.of("sku", SKU, "quantity", 1,
                    "orderId", prepared.orderId()));
            assertEquals(released.statusCode(), 200, released.asString());
            assertEquals(released.jsonPath().getInt("available"), 10);
            assertEquals(api.inventory(SKU).jsonPath().getInt("available"), 10);

            Response notifications = api.notifications(prepared.orderId());
            List<String> events = notifications.jsonPath().getList("events", String.class);
            assertTrue(events.containsAll(List.of("order_confirmed", "order_cancelled")),
                    "Cancellation notifications were not recorded: " + events);
            assertEquals(api.order(prepared.orderId()).jsonPath().getString("status"), "cancelled");

            mock.wireMock().verify(0, postRequestedFor(urlPathEqualTo("/payments")));
            mock.wireMock().verify(0, postRequestedFor(urlPathEqualTo("/shipments")));
            mock.wireMock().verify(1, postRequestedFor(urlPathMatching("/orders/[^/]+/cancel")));
            mock.wireMock().verify(1, postRequestedFor(urlPathEqualTo("/inventory/releases"))
                    .withRequestBody(matchingJsonPath("$.orderId", equalTo(prepared.orderId()))));
        }
    }

    private static PreparedOrder prepareOrder(CommerceApi api, String workflow, int quantity) {
        Response product = api.product("prod-9001");
        assertEquals(product.statusCode(), 200, product.asString());
        assertEquals(product.jsonPath().getString("sku"), SKU);
        assertEquals(product.jsonPath().getDouble("price"), 49.99);

        Response stock = api.inventory(SKU);
        assertEquals(stock.statusCode(), 200, stock.asString());
        assertTrue(stock.jsonPath().getInt("available") >= quantity);
        Response reservation = api.reserveInventory(Map.of("sku", SKU, "quantity", quantity));
        assertEquals(reservation.statusCode(), 201, reservation.asString());

        Response cart = api.createCart(Map.of("customerId", CUSTOMER_ID));
        assertEquals(cart.statusCode(), 201, cart.asString());
        String cartId = cart.jsonPath().getString("cartId");
        Response cartItem = api.addCartItem(cartId, Map.of("productId", "prod-9001", "sku", SKU,
                "quantity", quantity, "unitPrice", 49.99));
        assertEquals(cartItem.statusCode(), 200, cartItem.asString());
        assertEquals(cartItem.jsonPath().getInt("totalQuantity"), quantity);

        Response address = api.validateAddress(Map.of("customerId", CUSTOMER_ID, "street", "10 Market Street",
                "city", "San Francisco", "region", "CA", "postalCode", "94105", "country", "US"));
        assertEquals(address.statusCode(), 200, address.asString());
        assertTrue(address.jsonPath().getBoolean("verified"));
        String addressId = address.jsonPath().getString("addressId");

        double amount = 49.99 * quantity;
        Response order = api.createOrder(Map.of("workflow", workflow, "customerId", CUSTOMER_ID,
                "cartId", cartId, "addressId", addressId, "sku", SKU, "quantity", quantity, "amount", amount));
        assertEquals(order.statusCode(), 201, order.asString());
        assertEquals(order.jsonPath().getString("status"), "created");
        assertEquals(order.jsonPath().getString("cartId"), cartId);
        assertEquals(order.jsonPath().getString("addressId"), addressId);
        return new PreparedOrder(order.jsonPath().getString("orderId"));
    }

    private static void stubStorefront(WireMockTestServer mock) {
        mock.wireMock().stubFor(get(urlPathEqualTo("/products/prod-9001"))
                .willReturn(json(200, """
                        {"productId":"prod-9001","sku":"sku-9001","name":"Adjustable Desk Lamp","price":49.99}
                        """)));

        mock.wireMock().stubFor(get(urlPathEqualTo("/inventory/" + SKU))
                .inScenario(INVENTORY).whenScenarioStateIs(STARTED)
                .willReturn(json(200, "{\"sku\":\"sku-9001\",\"available\":10}")));
        mock.wireMock().stubFor(post(urlPathEqualTo("/inventory/reservations"))
                .inScenario(INVENTORY).whenScenarioStateIs(STARTED)
                .withRequestBody(matchingJsonPath("$.sku", equalTo(SKU)))
                .willReturn(json(201, """
                        {"reservationId":"{{randomValue type='UUID'}}","sku":"{{jsonPath request.body '$.sku'}}",
                         "reserved":{{jsonPath request.body '$.quantity'}} }
                        """))
                .willSetStateTo("reserved"));
        mock.wireMock().stubFor(get(urlPathEqualTo("/inventory/" + SKU))
                .inScenario(INVENTORY).whenScenarioStateIs("reserved")
                .willReturn(json(200, "{\"sku\":\"sku-9001\",\"available\":8}")));
        mock.wireMock().stubFor(post(urlPathEqualTo("/inventory/releases"))
                .inScenario(INVENTORY).whenScenarioStateIs("reserved")
                .withRequestBody(matchingJsonPath("$.sku", equalTo(SKU)))
                .willReturn(json(200, "{\"sku\":\"sku-9001\",\"available\":10}"))
                .willSetStateTo("released"));
        mock.wireMock().stubFor(get(urlPathEqualTo("/inventory/" + SKU))
                .inScenario(INVENTORY).whenScenarioStateIs("released")
                .willReturn(json(200, "{\"sku\":\"sku-9001\",\"available\":10}")));

        mock.wireMock().stubFor(post(urlPathEqualTo("/carts"))
                .withRequestBody(matchingJsonPath("$.customerId", equalTo(CUSTOMER_ID)))
                .willReturn(json(201, """
                        {"cartId":"{{randomValue type='UUID'}}","customerId":"{{jsonPath request.body '$.customerId'}}",
                         "status":"open"}
                        """)));
        mock.wireMock().stubFor(post(urlPathMatching("/carts/[^/]+/items"))
                .withRequestBody(matchingJsonPath("$.productId", equalTo("prod-9001")))
                .willReturn(json(200, """
                        {"cartId":"{{request.path.[1]}}","productId":"{{jsonPath request.body '$.productId'}}",
                         "quantity":{{jsonPath request.body '$.quantity'}},"totalQuantity":{{jsonPath request.body '$.quantity'}} }
                        """)));
        mock.wireMock().stubFor(post(urlPathEqualTo("/addresses/validate"))
                .withRequestBody(matchingJsonPath("$.postalCode", equalTo("94105")))
                .willReturn(json(200, """
                        {"addressId":"{{randomValue type='UUID'}}","verified":true,
                         "postalCode":"{{jsonPath request.body '$.postalCode'}}"}
                        """)));
    }

    private static void stubCheckout(WireMockTestServer mock) {
        mock.wireMock().stubFor(post(urlPathEqualTo("/orders"))
                .inScenario(CHECKOUT).whenScenarioStateIs(STARTED)
                .withRequestBody(matchingJsonPath("$.workflow", equalTo("checkout")))
                .willReturn(json(201, """
                        {"orderId":"{{randomValue type='UUID'}}","status":"created",
                         "cartId":"{{jsonPath request.body '$.cartId'}}",
                         "addressId":"{{jsonPath request.body '$.addressId'}}",
                         "amount":{{jsonPath request.body '$.amount'}} }
                        """))
                .willSetStateTo("order_created"));
        mock.wireMock().stubFor(post(urlPathEqualTo("/payments"))
                .inScenario(CHECKOUT).whenScenarioStateIs("order_created")
                .withRequestBody(matchingJsonPath("$.paymentToken", equalTo("payment-token-approved")))
                .willReturn(json(201, """
                        {"transactionId":"{{randomValue type='UUID'}}",
                         "orderId":"{{jsonPath request.body '$.orderId'}}","status":"authorized"}
                        """))
                .willSetStateTo("paid"));
        mock.wireMock().stubFor(post(urlPathEqualTo("/shipments"))
                .inScenario(CHECKOUT).whenScenarioStateIs("paid")
                .withRequestBody(matchingJsonPath("$.orderId"))
                .willReturn(json(202, """
                        {"trackingId":"{{randomValue type='UUID'}}",
                         "orderId":"{{jsonPath request.body '$.orderId'}}","status":"shipped"}
                        """))
                .willSetStateTo("shipped"));
        mock.wireMock().stubFor(get(urlPathMatching("/shipments/[^/]+"))
                .inScenario(CHECKOUT).whenScenarioStateIs("shipped")
                .willReturn(json(200, """
                        {"trackingId":"{{request.path.[1]}}","status":"in_transit"}
                        """))
                .willSetStateTo("in_transit"));
        mock.wireMock().stubFor(get(urlPathMatching("/deliveries/[^/]+"))
                .inScenario(CHECKOUT).whenScenarioStateIs("in_transit")
                .willReturn(json(200, """
                        {"trackingId":"{{request.path.[1]}}","status":"delivered"}
                        """))
                .willSetStateTo("delivered"));
        mock.wireMock().stubFor(get(urlPathEqualTo("/notifications"))
                .inScenario(CHECKOUT).whenScenarioStateIs("delivered")
                .withQueryParam("orderId", matching(".+"))
                .willReturn(json(200, """
                        {"orderId":"{{request.query.orderId}}",
                         "events":["order_confirmed","payment_authorized","shipment_dispatched","delivery_completed"]}
                        """)));
        mock.wireMock().stubFor(post(urlPathEqualTo("/refunds"))
                .inScenario(CHECKOUT).whenScenarioStateIs("delivered")
                .withRequestBody(matchingJsonPath("$.transactionId"))
                .willReturn(json(202, """
                        {"refundId":"{{randomValue type='UUID'}}",
                         "orderId":"{{jsonPath request.body '$.orderId'}}","status":"refunded"}
                        """))
                .willSetStateTo("refunded"));
        mock.wireMock().stubFor(get(urlPathMatching("/orders/[^/]+"))
                .inScenario(CHECKOUT).whenScenarioStateIs("refunded")
                .willReturn(json(200, """
                        {"orderId":"{{request.path.[1]}}","status":"refunded"}
                        """)));
    }

    private static void stubCancellation(WireMockTestServer mock) {
        mock.wireMock().stubFor(post(urlPathEqualTo("/orders"))
                .inScenario(CANCELLATION).whenScenarioStateIs(STARTED)
                .withRequestBody(matchingJsonPath("$.workflow", equalTo("cancel")))
                .willReturn(json(201, """
                        {"orderId":"{{randomValue type='UUID'}}","status":"created",
                         "cartId":"{{jsonPath request.body '$.cartId'}}",
                         "addressId":"{{jsonPath request.body '$.addressId'}}"}
                        """))
                .willSetStateTo("cancel_order_created"));
        mock.wireMock().stubFor(post(urlPathMatching("/orders/[^/]+/cancel"))
                .inScenario(CANCELLATION).whenScenarioStateIs("cancel_order_created")
                .willReturn(json(200, """
                        {"orderId":"{{request.path.[1]}}","status":"cancelled"}
                        """))
                .willSetStateTo("cancelled"));
        mock.wireMock().stubFor(get(urlPathEqualTo("/notifications"))
                .inScenario(CANCELLATION).whenScenarioStateIs("cancelled")
                .withQueryParam("orderId", matching(".+"))
                .willReturn(json(200, """
                        {"orderId":"{{request.query.orderId}}","events":["order_confirmed","order_cancelled"]}
                        """)));
        mock.wireMock().stubFor(get(urlPathMatching("/orders/[^/]+"))
                .inScenario(CANCELLATION).whenScenarioStateIs("cancelled")
                .willReturn(json(200, """
                        {"orderId":"{{request.path.[1]}}","status":"cancelled"}
                        """)));
    }

    private static void verifyCheckoutRequests(WireMockTestServer mock, String orderId,
                                               String transactionId, String trackingId) {
        mock.wireMock().verify(1, getRequestedFor(urlPathEqualTo("/products/prod-9001")));
        mock.wireMock().verify(1, postRequestedFor(urlPathEqualTo("/inventory/reservations"))
                .withRequestBody(matchingJsonPath("$.sku", equalTo(SKU))));
        mock.wireMock().verify(1, postRequestedFor(urlPathEqualTo("/addresses/validate")));
        mock.wireMock().verify(1, postRequestedFor(urlPathEqualTo("/payments"))
                .withRequestBody(matchingJsonPath("$.orderId", equalTo(orderId))));
        mock.wireMock().verify(1, postRequestedFor(urlPathEqualTo("/shipments"))
                .withRequestBody(matchingJsonPath("$.orderId", equalTo(orderId))));
        mock.wireMock().verify(1, getRequestedFor(urlPathEqualTo("/deliveries/" + trackingId)));
        mock.wireMock().verify(1, postRequestedFor(urlPathEqualTo("/refunds"))
                .withRequestBody(matchingJsonPath("$.transactionId", equalTo(transactionId))));
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(int status, String body) {
        return aResponse().withStatus(status).withHeader("Content-Type", "application/json")
                .withBody(body).withTransformers("response-template");
    }

    private record PreparedOrder(String orderId) { }
}
