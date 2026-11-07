package com.sdetlabs.framework.mocking;

import com.sdetlabs.framework.http.RestClient;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

/** Stateful, local simulations for inventory, order, payment, shipment and refund APIs. */
public class CommerceSimulationTest {
    private static final String FLOW = "checkout-and-fulfilment";
    private static final String INVENTORY = "inventory-reservation";

    @Test(groups = {"mocking", "e2e", "positive", "regression"})
    public void simulatesInventoryOrderPaymentShipmentAndRefundLifecycle() {
        try (WireMockTestServer mock = new WireMockTestServer()) {
            RestClient client = mock.client();
            stubInventory(mock);
            stubOrderAndPayment(mock);
            stubShipmentAndRefund(mock);

            String sku = "sku-9001";
            Response stock = client.get("/inventory/" + sku);
            assertEquals(stock.statusCode(), 200);
            assertEquals(stock.jsonPath().getInt("available"), 10);

            Response reservation = client.post("/inventory/reservations", Map.of("sku", sku, "quantity", 2));
            assertEquals(reservation.statusCode(), 201);
            assertEquals(reservation.jsonPath().getInt("reserved"), 2);
            assertTrue(reservation.jsonPath().getString("reservationId").length() > 10);

            Response reducedStock = client.get("/inventory/" + sku);
            assertEquals(reducedStock.jsonPath().getInt("available"), 8);

            Response order = client.post("/orders", Map.of(
                    "customerId", "customer-42", "sku", sku, "quantity", 2, "amount", 49.99));
            assertEquals(order.statusCode(), 201);
            String orderId = order.jsonPath().getString("orderId");
            assertTrue(orderId.length() > 10);
            assertEquals(order.jsonPath().getString("status"), "created");

            Response declinedPayment = client.post("/payments", Map.of(
                    "orderId", orderId, "amount", 49.99, "cardToken", "declined"));
            assertEquals(declinedPayment.statusCode(), 402);
            assertEquals(declinedPayment.jsonPath().getString("status"), "declined");

            Response payment = client.post("/payments", Map.of(
                    "orderId", orderId, "amount", 49.99, "cardToken", "tok_test_approved"));
            assertEquals(payment.statusCode(), 201);
            assertEquals(payment.jsonPath().getString("orderId"), orderId);
            assertEquals(payment.jsonPath().getString("status"), "authorized");
            String transactionId = payment.jsonPath().getString("transactionId");
            assertTrue(transactionId.length() > 10);

            Response shipment = client.post("/shipments", Map.of("orderId", orderId));
            assertEquals(shipment.statusCode(), 202);
            String trackingId = shipment.jsonPath().getString("trackingId");
            assertTrue(trackingId.length() > 10);
            assertEquals(shipment.jsonPath().getString("status"), "shipped");

            Response inTransit = client.get("/shipments/" + trackingId);
            assertEquals(inTransit.jsonPath().getString("status"), "in_transit");
            Response delivered = client.get("/shipments/" + trackingId);
            assertEquals(delivered.jsonPath().getString("status"), "delivered");

            Response refund = client.post("/refunds", Map.of(
                    "orderId", orderId, "transactionId", transactionId, "amount", 49.99));
            assertEquals(refund.statusCode(), 202);
            assertEquals(refund.jsonPath().getString("status"), "refunded");
            assertNotEquals(refund.jsonPath().getString("refundId"), transactionId);

            mock.wireMock().verify(1, postRequestedFor(urlPathEqualTo("/inventory/reservations"))
                    .withRequestBody(matchingJsonPath("$.sku", equalTo(sku)))
                    .withRequestBody(matchingJsonPath("$.quantity", equalTo("2"))));
            mock.wireMock().verify(2, postRequestedFor(urlPathEqualTo("/payments"))
                    .withRequestBody(matchingJsonPath("$.orderId", equalTo(orderId))));
            mock.wireMock().verify(1, postRequestedFor(urlPathEqualTo("/shipments"))
                    .withRequestBody(matchingJsonPath("$.orderId", equalTo(orderId))));
            mock.wireMock().verify(1, postRequestedFor(urlPathEqualTo("/refunds"))
                    .withRequestBody(matchingJsonPath("$.transactionId", equalTo(transactionId))));
        }
    }

    private static void stubInventory(WireMockTestServer mock) {
        mock.wireMock().stubFor(get(urlPathMatching("/inventory/[^/]+"))
                .inScenario(INVENTORY)
                .whenScenarioStateIs(STARTED)
                .willReturn(json(200, """
                        {"sku":"{{request.path.[1]}}","available":10}
                        """)));
        mock.wireMock().stubFor(post(urlPathEqualTo("/inventory/reservations"))
                .inScenario(INVENTORY)
                .whenScenarioStateIs(STARTED)
                .withRequestBody(matchingJsonPath("$.quantity", equalTo("2")))
                .willReturn(json(201, """
                        {"reservationId":"{{randomValue type='UUID'}}",
                         "sku":"{{jsonPath request.body '$.sku'}}",
                         "reserved":{{jsonPath request.body '$.quantity'}} }
                        """))
                .willSetStateTo("reserved"));
        mock.wireMock().stubFor(get(urlPathMatching("/inventory/[^/]+"))
                .inScenario(INVENTORY)
                .whenScenarioStateIs("reserved")
                .willReturn(json(200, """
                        {"sku":"{{request.path.[1]}}","available":8}
                        """)));
    }

    private static void stubOrderAndPayment(WireMockTestServer mock) {
        mock.wireMock().stubFor(post(urlPathEqualTo("/orders"))
                .inScenario(FLOW)
                .whenScenarioStateIs(STARTED)
                .withRequestBody(matchingJsonPath("$.customerId", equalTo("customer-42")))
                .willReturn(json(201, """
                        {"orderId":"{{randomValue type='UUID'}}",
                         "customerId":"{{jsonPath request.body '$.customerId'}}",
                         "status":"created"}
                        """))
                .willSetStateTo("order_created"));
        mock.wireMock().stubFor(post(urlPathEqualTo("/payments"))
                .atPriority(1)
                .withRequestBody(matchingJsonPath("$.cardToken", equalTo("declined")))
                .willReturn(json(402, """
                        {"status":"declined","code":"card_declined"}
                        """)));
        mock.wireMock().stubFor(post(urlPathEqualTo("/payments"))
                .inScenario(FLOW)
                .whenScenarioStateIs("order_created")
                .withRequestBody(matchingJsonPath("$.orderId"))
                .willReturn(json(201, """
                        {"orderId":"{{jsonPath request.body '$.orderId'}}",
                         "transactionId":"{{randomValue type='UUID'}}",
                         "status":"authorized"}
                        """))
                .willSetStateTo("paid"));
    }

    private static void stubShipmentAndRefund(WireMockTestServer mock) {
        mock.wireMock().stubFor(post(urlPathEqualTo("/shipments"))
                .inScenario(FLOW)
                .whenScenarioStateIs("paid")
                .withRequestBody(matchingJsonPath("$.orderId"))
                .willReturn(json(202, """
                        {"orderId":"{{jsonPath request.body '$.orderId'}}",
                         "trackingId":"{{randomValue type='UUID'}}",
                         "status":"shipped"}
                        """))
                .willSetStateTo("shipped"));
        mock.wireMock().stubFor(get(urlPathMatching("/shipments/[^/]+"))
                .inScenario(FLOW)
                .whenScenarioStateIs("shipped")
                .willReturn(json(200, """
                        {"trackingId":"{{request.path.[1]}}","status":"in_transit"}
                        """))
                .willSetStateTo("delivered"));
        mock.wireMock().stubFor(get(urlPathMatching("/shipments/[^/]+"))
                .inScenario(FLOW)
                .whenScenarioStateIs("delivered")
                .willReturn(json(200, """
                        {"trackingId":"{{request.path.[1]}}","status":"delivered"}
                        """)));
        mock.wireMock().stubFor(post(urlPathEqualTo("/refunds"))
                .inScenario(FLOW)
                .whenScenarioStateIs("delivered")
                .withRequestBody(matchingJsonPath("$.transactionId"))
                .willReturn(json(202, """
                        {"refundId":"{{randomValue type='UUID'}}",
                         "orderId":"{{jsonPath request.body '$.orderId'}}",
                         "status":"refunded"}
                        """))
                .willSetStateTo("refunded"));
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(int status, String body) {
        return aResponse().withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body)
                .withTransformers("response-template");
    }
}
