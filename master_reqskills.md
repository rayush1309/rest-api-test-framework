# MASTER REQUIREMENTS DOCUMENT
# Enterprise-Grade API Automation Framework – Amazon-Style E-Commerce Reference Project

**Document Type:** Software Requirements Specification (SRS) + Technical Design + Implementation Plan

**Project Name:** `rest-api-test-framework`

**Project Category:** Enterprise API Automation Framework

**Primary Objective:** Build a production-quality API automation framework demonstrating Senior SDET / SDET Architect-level engineering capabilities.

**Target Implementation:** Java 17, Maven, RestAssured, TestNG

**Target Coding Agents:** Claude Code / OpenAI Codex

**Document Status:** Master Implementation Specification

---

# 1. PROJECT VISION

## 1.1 Objective

Build an enterprise-grade API automation framework using an Amazon-style e-commerce application as the business reference.

The framework must demonstrate how an SDET designs, implements, validates, debugs and maintains an API automation solution covering complete business workflows.

The project must cover:

1. Customer registration and authentication.
2. Customer profile management.
3. Product catalog management.
4. Product search and filtering.
5. Inventory management.
6. Shopping cart management.
7. Address management.
8. Order management.
9. Payment processing.
10. Shipment management.
11. Delivery tracking.
12. Order cancellation.
13. Refund processing.
14. Notification management.
15. API mocking and service virtualization.
16. Negative and boundary testing.
17. Integration and end-to-end testing.
18. Distributed system failure scenarios.
19. Parallel execution and concurrency.
20. CI/CD automation and reporting.

The framework should not be a collection of independent API tests only. It must demonstrate how APIs work together to complete business operations.

## 1.2 Reference Business Application

Use an Amazon-style e-commerce platform as the reference domain.

Example business journey:

A customer logs in, searches for a mobile phone, checks product details, adds the product to the cart, selects a delivery address, places an order, completes payment, receives order confirmation, tracks shipment and eventually receives the product.

If the customer cancels an eligible order, the system must handle cancellation, inventory restoration and refund processing according to the business rules.

**Important distinction:** This project is an educational reference implementation. It is not affiliated with Amazon and must not claim to use Amazon's internal APIs or architecture.

---

# 2. TECHNOLOGY STACK

| Component | Required technology |
|---|---|
| Programming language | Java 17 |
| Build tool | Maven |
| API automation | RestAssured 5.x |
| Test framework | TestNG |
| JSON serialization | Jackson |
| JSONPath | RestAssured JsonPath |
| JSON schema | JSON Schema Validator |
| API mocking | WireMock 3.x |
| Async polling | Awaitility |
| Reporting | Allure |
| Logging | Log4j2 or SLF4J with Logback |
| Test data | JSON, CSV, Excel |
| Version control | Git |
| Repository | GitHub |
| CI/CD | GitHub Actions |
| Secondary CI/CD | Jenkins |
| IDE | IntelliJ IDEA |
| Java runtime | Java 17 |

Use compatible, stable dependency versions. Centralize versions in `pom.xml` properties.

Do not introduce unnecessary dependencies.

---

# 3. API SOURCES AND EXECUTION STRATEGY

Use three real public practice APIs.

| API | Base URL | Main purpose |
|---|---|---|
| Restful Booker | `https://restful-booker.herokuapp.com` | Booking CRUD and authentication |
| DummyJSON | `https://dummyjson.com` | E-commerce products, users and carts |
| GoREST | `https://gorest.co.in/public/v3` | Authenticated user and post CRUD |

Official documentation:

- Restful Booker: https://restful-booker.herokuapp.com/apidoc/index.html
- DummyJSON: https://dummyjson.com/docs
- GoREST: https://gorest.co.in/reference/users

## 3.1 API limitations

These services do not form one real integrated e-commerce backend.

The implementation must respect the following:

- Restful Booker is a booking API, not an e-commerce order API.
- DummyJSON provides simulated e-commerce operations. Its write operations generally do not persist to the underlying data store.
- GoREST provides user and post management APIs.
- None of these services provides a complete real payment, inventory, shipment or refund platform.

Therefore:

1. Use real APIs for their supported operations.
2. Use WireMock for unsupported services and controlled failure simulation.
3. Do not pretend a simulated payment is a real financial transaction.
4. Do not assume one API's user ID or product ID belongs to another API.
5. Do not claim cross-service persistence where it does not exist.

The framework must clearly distinguish REAL API TESTS from MOCK-BASED TESTS.

---

# 4. AMAZON-STYLE BUSINESS ARCHITECTURE

## 4.1 Logical service architecture

The reference business architecture should contain the following logical services:

1. API Gateway.
2. Authentication Service.
3. Customer Service.
4. Product Catalog Service.
5. Inventory Service.
6. Cart Service.
7. Address Service.
8. Order Service.
9. Payment Service.
10. Shipping Service.
11. Notification Service.
12. Refund Service.

These are logical business components for testing design. They are not all implemented by the public APIs.

## 4.2 Service architecture diagram

```text
                  CUSTOMER / CLIENT
                         |
                         v
                    API GATEWAY
                         |
          +--------------+--------------+
          |              |              |
          v              v              v
     AUTH SERVICE   CUSTOMER SERVICE   PRODUCT SERVICE
          |              |              |
          +--------------+--------------+
                         |
                         v
                     CART SERVICE
                         |
                         v
                    ORDER SERVICE
                         |
             +-----------+-----------+
             |           |           |
             v           v           v
         INVENTORY    PAYMENT     NOTIFICATION
          SERVICE     SERVICE       SERVICE
             |           |
             +-----------+
                         |
                         v
                   SHIPPING SERVICE
                         |
                         v
                    REFUND SERVICE
```

## 4.3 Real and simulated service mapping

| Logical service | Practice implementation |
|---|---|
| Authentication | DummyJSON, Restful Booker, GoREST |
| Customer | DummyJSON / GoREST |
| Product catalog | DummyJSON |
| Cart | DummyJSON |
| Booking lifecycle | Restful Booker |
| Inventory | WireMock |
| Address | WireMock |
| Order | WireMock |
| Payment | WireMock |
| Shipping | WireMock |
| Notification | WireMock |
| Refund | WireMock |

This architecture provides real API practice while allowing controlled simulation of missing business services.

---

# 5. COMPLETE API ENDPOINT REQUIREMENTS

## 5.1 Restful Booker API

**Base URL:** `https://restful-booker.herokuapp.com`

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth` | Generate authentication token |

Request:

```json
{
  "username": "admin",
  "password": "password123"
}
```

Expected response:

```json
{
  "token": "generated-token"
}
```

Requirements:

- Successful authentication.
- Invalid username.
- Invalid password.
- Missing credentials.
- Invalid token.
- Token extraction.
- Token reuse.
- Cookie authentication.

Use the documented cookie:

`Cookie: token=YOUR_TOKEN`

Never hardcode generated tokens.

### Booking APIs

| Method | Endpoint | Description |
|---|---|---|
| GET | `/ping` | Health check |
| GET | `/booking` | Retrieve booking IDs |
| GET | `/booking/{id}` | Retrieve booking |
| POST | `/booking` | Create booking |
| PUT | `/booking/{id}` | Full update |
| PATCH | `/booking/{id}` | Partial update |
| DELETE | `/booking/{id}` | Delete booking |

Booking request:

```json
{
  "firstname": "Ayush",
  "lastname": "Raj",
  "totalprice": 5000,
  "depositpaid": true,
  "bookingdates": {
    "checkin": "2026-12-10",
    "checkout": "2026-12-15"
  },
  "additionalneeds": "Breakfast"
}
```

Requirements:

- Create booking using POJO.
- Create booking using Map.
- Create booking using JSON file.
- Extract booking ID.
- Retrieve created booking.
- Validate all fields.
- Update complete booking.
- Partially update booking.
- Verify unchanged fields.
- Delete booking.
- Verify deleted booking behavior.
- Test invalid booking IDs.
- Test authentication failures.

### Query parameters

Support documented booking filters:

- `firstname`
- `lastname`
- `checkin`
- `checkout`

Validate combinations of query parameters.

---

## 5.2 DummyJSON API

**Base URL:** `https://dummyjson.com`

Official documentation: https://dummyjson.com/docs

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/login` | Login |
| GET | `/auth/me` | Get authenticated user |
| POST | `/auth/refresh` | Refresh token |

Example login:

```json
{
  "username": "emilys",
  "password": "emilyspass"
}
```

Requirements:

- Valid login.
- Invalid username.
- Invalid password.
- Missing credentials.
- Token extraction.
- Authenticated user retrieval.
- Invalid token.
- Missing token.
- Refresh token flow.

### Product APIs

| Method | Endpoint | Description |
|---|---|---|
| GET | `/products` | Get products |
| GET | `/products/{id}` | Get product |
| GET | `/products/search?q=phone` | Search products |
| GET | `/products/category-list` | Get category names |
| GET | `/products/categories` | Get category objects |
| GET | `/products/category/{category}` | Get category products |
| GET | `/products?limit=10&skip=10` | Pagination |
| GET | `/products?sortBy=price&order=asc` | Sorting |

Requirements:

- Product retrieval.
- Product search.
- Category filtering.
- Pagination.
- Sorting.
- Invalid product IDs.
- Empty search results.
- Price validation.
- Product response schema validation.

### Cart APIs

| Method | Endpoint | Description |
|---|---|---|
| GET | `/carts` | Retrieve carts |
| GET | `/carts/{id}` | Retrieve cart |
| GET | `/carts/user/{userId}` | Retrieve user carts |
| POST | `/carts/add` | Simulate cart creation |
| PUT | `/carts/{id}` | Simulate cart update |
| PATCH | `/carts/{id}` | Simulate partial update |
| DELETE | `/carts/{id}` | Simulate cart deletion |

Example:

```json
{
  "userId": 1,
  "products": [
    {
      "id": 1,
      "quantity": 2
    },
    {
      "id": 2,
      "quantity": 1
    }
  ]
}
```

Requirements:

- Retrieve carts.
- Retrieve cart by ID.
- Retrieve user carts.
- Add products.
- Update quantities.
- Delete cart.
- Validate total.
- Validate discounted total.
- Validate product quantities.
- Validate negative quantities.
- Validate invalid products.

**Note:** Write operations are simulated. Validate their returned response rather than expecting permanent server-side persistence.

---

## 5.3 GoREST API

**Base URL:** `https://gorest.co.in/public/v3`

Official documentation: https://gorest.co.in/reference/users

Authentication:

`Authorization: Bearer YOUR_ACCESS_TOKEN`

Obtain the access token from https://gorest.co.in/.

### User APIs

| Method | Endpoint | Description |
|---|---|---|
| GET | `/users` | Retrieve users |
| POST | `/users` | Create user |
| GET | `/users/{id}` | Retrieve user |
| PATCH | `/users/{id}` | Update user |
| DELETE | `/users/{id}` | Delete user |

Create user:

```json
{
  "name": "Ayush Raj",
  "email": "unique-email@example.com",
  "gender": "male",
  "status": "active"
}
```

Requirements:

- Create user.
- Retrieve user.
- Update user.
- Delete user.
- Verify deletion.
- Duplicate email validation.
- Invalid email validation.
- Missing mandatory fields.
- Invalid gender.
- Invalid status.
- Invalid authentication.
- Unauthorized operations.

Generate unique email addresses dynamically.

### Post APIs

| Method | Endpoint | Description |
|---|---|---|
| GET | `/posts` | Retrieve posts |
| POST | `/posts` | Create post |
| GET | `/posts/{id}` | Retrieve post |
| PATCH | `/posts/{id}` | Update post |
| DELETE | `/posts/{id}` | Delete post |
| GET | `/users/{userId}/posts` | Get user posts |

Example:

```json
{
  "user_id": 123,
  "title": "API Automation",
  "body": "Testing APIs with Java and RestAssured."
}
```

Requirements:

- Create post.
- Retrieve post.
- Update post.
- Delete post.
- Retrieve posts by user.
- Validate user-post relationship.
- Invalid user ID.
- Invalid post ID.
- Missing fields.
- Authentication failures.

---

# 6. END-TO-END BUSINESS WORKFLOWS

## 6.1 Workflow A – Hotel Booking Lifecycle

API: Restful Booker.

```text
Authenticate
     |
     v
Generate Token
     |
     v
Create Booking
     |
     v
Extract Booking ID
     |
     v
Retrieve Booking
     |
     v
Validate Booking
     |
     v
Update Booking
     |
     v
Verify Update
     |
     v
Partial Update
     |
     v
Verify Partial Update
     |
     v
Delete Booking
     |
     v
Verify Deletion
```

Acceptance criteria:

- Booking ID generated dynamically.
- Response matches request.
- GET returns expected data.
- PUT updates fields.
- PATCH updates selected fields.
- DELETE succeeds.
- Deleted booking cannot be retrieved as an active booking.

## 6.2 Workflow B – E-Commerce Customer Journey

API: DummyJSON.

```text
Login
  |
  v
Extract Token
  |
  v
Get User Details
  |
  v
Search Product
  |
  v
Retrieve Product
  |
  v
Validate Price
  |
  v
Retrieve User Cart
  |
  v
Simulate Add Cart
  |
  v
Validate Cart Response
  |
  v
Simulate Cart Update
  |
  v
Validate Updated Response
```

Acceptance criteria:

- Authentication succeeds.
- Product exists.
- Product data is validated.
- Cart response is correct.
- Quantity and totals are validated.
- Simulated write behavior is documented accurately.

## 6.3 Workflow C – User Management Lifecycle

API: GoREST.

```text
Generate Unique Email
        |
        v
Create User
        |
        v
Extract User ID
        |
        v
Retrieve User
        |
        v
Validate User
        |
        v
Update User
        |
        v
Verify Updated User
        |
        v
Create Post
        |
        v
Retrieve User Posts
        |
        v
Delete Post
        |
        v
Delete User
        |
        v
Verify Deletion
```

Acceptance criteria:

- Unique user is created.
- User ID is extracted.
- User is updated.
- Post is associated with the correct user.
- Post and user deletion are validated.

## 6.4 Workflow D – Amazon-Style Order Processing

Implementation: WireMock-based simulated services.

This workflow must demonstrate an integrated business flow without pretending the three public APIs are connected.

```text
Customer Authentication
          |
          v
Product Discovery
          |
          v
Inventory Availability
          |
          v
Cart Validation
          |
          v
Address Validation
          |
          v
Create Order
          |
          v
Reserve Inventory
          |
          v
Process Payment
          |
          v
Confirm Order
          |
          v
Create Shipment
          |
          v
Send Notification
          |
          v
Track Delivery
          |
          v
Mark Order Delivered
```

Each simulated service must have a defined request and response contract, status transitions and test scenarios.

## 6.5 Workflow E – Payment Failure

```text
Create Order
     |
     v
Reserve Inventory
     |
     v
Initiate Payment
     |
     v
Payment Declined
     |
     v
Mark Payment Failed
     |
     v
Cancel or Hold Order
     |
     v
Release Inventory
     |
     v
Send Failure Notification
```

Acceptance criteria:

- No successful order confirmation.
- No shipment created.
- Inventory reservation released according to business rules.
- Payment failure recorded.
- Notification generated.

## 6.6 Workflow F – Cancellation and Refund

```text
Confirmed Order
       |
       v
Cancellation Request
       |
       v
Check Eligibility
       |
       v
Cancel Order
       |
       v
Initiate Refund
       |
       v
Validate Refund
       |
       v
Restore Inventory
       |
       v
Notify Customer
```

Acceptance criteria:

- Eligible order can be cancelled.
- Ineligible order cannot be cancelled.
- Refund is not duplicated.
- Inventory restoration follows the configured policy.
- Notification is generated.

## 6.7 Workflow G – Concurrent Purchase

Two customers attempt to purchase the final available product.

Acceptance criteria:

- Only one reservation succeeds.
- Other request receives an appropriate conflict response.
- Inventory never becomes negative.
- No duplicate successful reservation.
- Order and inventory state remain consistent.

Implement using WireMock or a controlled local mock service. Do not send concurrency tests to public APIs.

---

# 7. API MOCKING AND SERVICE VIRTUALIZATION – MANDATORY MODULE

Technology: WireMock 3.x.

This is a core requirement, not an optional enhancement.

## 7.1 Objective

Simulate services that are unavailable through the public APIs.

Mock:

- Inventory.
- Address.
- Order.
- Payment.
- Shipment.
- Notification.
- Refund.
- Third-party integrations.

## 7.2 WireMock architecture

```text
               TESTNG TEST
                    |
                    v
                RESTCLIENT
                    |
                    v
                RESTASSURED
                    |
                    v
              WIREMOCK SERVER
                    |
         +----------+----------+
         |          |          |
         v          v          v
       STUBS     SCENARIOS    MATCHING
         |          |          |
         +----------+----------+
                    |
                    v
              MOCK RESPONSE
                    |
                    v
                ASSERTIONS
```

## 7.3 Mock endpoints

These are local simulated endpoints. They are not real public API endpoints.

### Inventory

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/mock/inventory/{productId}` | Check stock |
| POST | `/mock/inventory/reservations` | Reserve stock |
| GET | `/mock/inventory/reservations/{id}` | Retrieve reservation |
| DELETE | `/mock/inventory/reservations/{id}` | Release reservation |

### Address

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/mock/addresses/validate` | Validate address |
| GET | `/mock/addresses/{id}` | Retrieve address |

### Order

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/mock/orders` | Create order |
| GET | `/mock/orders/{id}` | Retrieve order |
| PATCH | `/mock/orders/{id}/status` | Update order status |
| POST | `/mock/orders/{id}/cancel` | Cancel order |

### Payment

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/mock/payments` | Initiate payment |
| GET | `/mock/payments/{id}` | Retrieve payment |
| POST | `/mock/payments/{id}/confirm` | Confirm payment |
| POST | `/mock/payments/{id}/capture` | Capture payment |
| POST | `/mock/payments/{id}/void` | Void payment |

### Shipment

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/mock/shipments` | Create shipment |
| GET | `/mock/shipments/{id}` | Retrieve shipment |
| GET | `/mock/orders/{id}/tracking` | Track order |
| PATCH | `/mock/shipments/{id}/status` | Update shipment status |

### Notification

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/mock/notifications` | Generate notification |
| GET | `/mock/notifications/{id}` | Retrieve notification |
| PATCH | `/mock/notifications/{id}/read` | Mark notification as read |

### Refund

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/mock/refunds` | Initiate refund |
| GET | `/mock/refunds/{id}` | Retrieve refund |

## 7.4 Mock response contracts

Inventory success:

```json
{
  "productId": 101,
  "availableQuantity": 10,
  "status": "AVAILABLE"
}
```

Inventory failure:

```json
{
  "productId": 101,
  "availableQuantity": 0,
  "status": "OUT_OF_STOCK"
}
```

Payment success:

```json
{
  "paymentId": "PAY-101",
  "orderId": "ORD-101",
  "amount": 5000,
  "currency": "INR",
  "status": "SUCCESS"
}
```

Payment failure:

```json
{
  "paymentId": "PAY-102",
  "status": "FAILED",
  "errorCode": "INSUFFICIENT_FUNDS",
  "message": "Payment declined"
}
```

Order confirmation:

```json
{
  "orderId": "ORD-101",
  "customerId": 101,
  "status": "CONFIRMED",
  "paymentStatus": "PAID",
  "totalAmount": 5000
}
```

Shipment:

```json
{
  "shipmentId": "SHIP-101",
  "orderId": "ORD-101",
  "trackingNumber": "TRACK-101",
  "status": "SHIPPED"
}
```

Refund:

```json
{
  "refundId": "REF-101",
  "orderId": "ORD-101",
  "amount": 5000,
  "status": "PROCESSED"
}
```

## 7.5 Mocking scenarios

Implement independent tests for:

1. HTTP 200.
2. HTTP 201.
3. HTTP 204.
4. HTTP 400.
5. HTTP 401.
6. HTTP 403.
7. HTTP 404.
8. HTTP 409.
9. HTTP 429.
10. HTTP 500.
11. HTTP 502.
12. HTTP 503.
13. HTTP 504.
14. Empty response.
15. Malformed JSON.
16. Delayed response.
17. Timeout.
18. Connection failure simulation.
19. Dynamic response.
20. Query parameter matching.
21. Header matching.
22. Body matching.
23. JSONPath matching.
24. Stateful scenarios.
25. Request verification.

## 7.6 Advanced WireMock capabilities

Implement:

- Stub mappings.
- Request matching.
- Response templating.
- Scenario state transitions.
- Request journal verification.
- Delay simulation.
- Fault injection.
- Dynamic response fields.
- Stub reset.
- Scenario reset.
- Mapping isolation.

## 7.7 Mocking test classes

```text
mocking/
    tests/
        MockGetUserAPITest.java
        MockPostUserAPITest.java
        MockStatusCodeTest.java
        MockRequestMatchingTest.java
        MockDynamicResponseTest.java
        MockInventoryTest.java
        MockPaymentGatewayTest.java
        MockOrderServiceTest.java
        MockShipmentServiceTest.java
        MockNotificationServiceTest.java
        MockRefundServiceTest.java
        MockTimeoutTest.java
        MockStatefulScenarioTest.java
        MockRetryTest.java
        MockRequestVerificationTest.java

    config/
        WireMockSetup.java

    stubs/
        APIMocks.java
```

Reuse existing WireMock utilities if already implemented.

---

# 8. FRAMEWORK ARCHITECTURE

## 8.1 Layered design

```text
                 TEST CASES
                     |
                     v
                   BASE TEST
                     |
                     v
                API CLIENT LAYER
                     |
                     v
              REQUEST SPECIFICATION
                     |
                     v
                 RESTASSURED
                     |
                     v
             REAL API / WIREMOCK
                     |
                     v
              RESPONSE VALIDATION
                     |
          +----------+----------+
          |          |          |
          v          v          v
       JSONPATH    SCHEMA     BUSINESS
       VALIDATION  VALIDATION ASSERTIONS
          |          |          |
          +----------+----------+
                     |
                     v
                   TESTNG
                     |
                     v
                   ALLURE
```

## 8.2 Package structure

Adapt the existing repository. Reuse existing classes instead of creating duplicates.

```text
src/
|
+-- main/
|   |
|   +-- java/
|       |
|       +-- client/
|       |    +-- RestClient.java
|       |
|       +-- constants/
|       |    +-- AppConstants.java
|       |    +-- AuthType.java
|       |
|       +-- manager/
|       |    +-- ConfigManager.java
|       |
|       +-- pojo/
|       |    +-- booking/
|       |    +-- product/
|       |    +-- cart/
|       |    +-- user/
|       |    +-- order/
|       |    +-- payment/
|       |    +-- shipment/
|       |
|       +-- utils/
|       |    +-- JsonUtils.java
|       |    +-- ObjectMapperUtil.java
|       |    +-- SchemaValidator.java
|       |    +-- CSVReader.java
|       |    +-- ExcelUtil.java
|       |
|       +-- exceptions/
|       +-- errors/
|
+-- test/
    |
    +-- java/
    |   |
    |   +-- base/
    |
    |   +-- restfulbooker/
    |   |    +-- auth/tests/
    |   |    +-- booking/tests/
    |   |    +-- negative/tests/
    |   |    +-- e2e/tests/
    |
    |   +-- dummyjson/
    |   |    +-- auth/tests/
    |   |    +-- products/tests/
    |   |    +-- carts/tests/
    |   |    +-- e2e/tests/
    |
    |   +-- gorest/
    |   |    +-- users/tests/
    |   |    +-- posts/tests/
    |   |    +-- e2e/tests/
    |
    |   +-- mocking/
    |   |    +-- tests/
    |
    |   +-- ecommerce/
    |   |    +-- e2e/tests/
    |
    |   +-- parallel/
    |        +-- tests/
    |
    +-- resources/
         |
         +-- config/
         +-- testdata/
         +-- schemas/
         +-- suites/
         +-- wiremock/
              +-- mappings/
              +-- __files/
```

---

# 9. CORE FRAMEWORK COMPONENT REQUIREMENTS

## 9.1 RestClient

Centralize reusable HTTP operations.

Required capabilities:

- GET.
- POST.
- PUT.
- PATCH.
- DELETE.
- Query parameters.
- Path parameters.
- Headers.
- Request body.
- File payload.
- Authentication.
- Request specification.
- Response specification.
- Request and response logging.

Design rules:

- Do not hardcode HTTP status expectations globally.
- Allow tests to validate endpoint-specific status codes.
- Do not mix business assertions into the client.
- Avoid shared mutable specifications.
- Support independent request configurations.
- Handle exceptions meaningfully.

## 9.2 ConfigManager

Maintain base URLs and environment-specific properties.

Example:

```properties
restfulbooker.base.url=https://restful-booker.herokuapp.com
dummyjson.base.url=https://dummyjson.com
gorest.base.url=https://gorest.co.in/public/v3
```

Requirements:

- Environment selection.
- Safe default environment.
- Fail-fast configuration validation.
- Secure secret injection.
- No committed tokens.
- No password or token logging.

## 9.3 Authentication

Support:

- Basic authentication.
- Bearer authentication.
- Cookie authentication.
- API key.
- No authentication.

Authentication handling must be configurable for each API.

## 9.4 POJO models

Create request and response models for:

- Booking.
- BookingDates.
- BookingAuth.
- Product.
- Cart.
- CartProduct.
- User.
- Post.
- Order.
- OrderItem.
- Inventory.
- Payment.
- Shipment.
- Refund.
- Notification.

Use correct Java types.

## 9.5 Response validation

Support:

- Status code.
- Headers.
- Content type.
- Response body.
- JSONPath.
- JSON Schema.
- Response time.
- Business logic.
- Data consistency.
- Error messages.

## 9.6 Logging

Capture:

- HTTP method.
- Endpoint.
- Request headers, with sensitive values masked.
- Request payload.
- Response status.
- Response headers.
- Response body.
- Execution duration.
- Correlation ID when available.

Do not expose credentials or access tokens.

---

# 10. DATA-DRIVEN TESTING

Implement:

- TestNG DataProvider.
- Object[][].
- CSV-based test data.
- Excel-based test data.
- JSON-based test data.
- Properties-based configuration.
- Dynamic test data generation.

Scenarios:

- Multiple login credentials.
- Multiple product search terms.
- Multiple booking payloads.
- Multiple user payloads.
- Positive and negative data.
- Boundary values.

Use separate test classes for separate concepts.

---

# 11. JSON AND API CONTRACT VALIDATION

Implement independent examples for:

1. JSON serialization.
2. JSON deserialization.
3. POJO mapping.
4. Nested POJO.
5. Map-based payload.
6. JsonPath extraction.
7. Array validation.
8. Dynamic ID extraction.
9. JSON Schema validation.
10. Unknown JSON field handling.
11. Response contract validation.
12. Null and optional fields.

Schemas must match the actual response contracts.

---

# 12. NEGATIVE AND BOUNDARY TESTING

Cover:

- Missing mandatory fields.
- Invalid data types.
- Empty request.
- Invalid JSON.
- Invalid credentials.
- Missing token.
- Invalid token.
- Invalid resource ID.
- Duplicate data.
- Invalid query parameter.
- Invalid path parameter.
- Boundary values.
- Unauthorized access.
- Forbidden access.
- Unsupported methods.
- Rate limiting.
- Server error.
- Timeout.

Validate actual API behavior and document limitations where an API does not support a particular scenario.

---

# 13. ASYNCHRONOUS API TESTING

Use Awaitility.

Scenarios:

- Poll payment status.
- Poll order status.
- Poll inventory availability.
- Poll shipment status.
- Poll refund status.
- Poll notification status.

Requirements:

- Configurable timeout.
- Configurable polling interval.
- Expected condition.
- Timeout exception.
- Meaningful failure messages.
- No unnecessary fixed sleeps.

Public APIs may not provide asynchronous workflows. Use WireMock stateful scenarios to practise these flows.

---

# 14. PARALLEL EXECUTION AND THREAD SAFETY

Implement and demonstrate:

- Parallel methods.
- Parallel classes.
- Parallel DataProviders.
- ThreadLocal.
- Shared static state.
- Race conditions.
- Thread-safe request configuration.
- Test data isolation.
- Cleanup.

TestNG configuration must support configurable worker counts.

Example:

```xml
<suite name="ParallelSuite"
       parallel="methods"
       thread-count="4">
</suite>
```

Requirements:

- No shared mutable token variable.
- No cross-test data contamination.
- No duplicate test data.
- No shared mutable RequestSpecification.
- No WireMock port conflicts.
- No shared mock state leakage.

Use `ThreadLocal` only where thread-specific state is needed. Clean it using `remove()`.

---

# 15. RETRY, IDEMPOTENCY AND RESILIENCE

Implement:

- Retry for transient GET failures.
- Configurable retry count.
- Retry intervals.
- Exponential backoff where appropriate.
- Retry exhaustion.
- Retry on selected HTTP status codes.
- No retry on non-retryable errors.
- Idempotency concepts.
- Duplicate request handling.
- Timeout handling.
- Circuit-breaker concepts through controlled simulation.

Important:

- Do not blindly retry POST operations.
- Do not assume the public APIs support idempotency keys.
- Demonstrate unsupported behavior using WireMock.
- Do not create duplicate payments or orders in the simulated business workflow.

---

# 16. API SECURITY TESTING

Implement:

- Missing authentication.
- Invalid authentication.
- Expired token simulation.
- Unauthorized user.
- Forbidden resource access.
- User data isolation.
- Sensitive data masking.
- Input validation.
- Basic injection-related input checks.
- Invalid content type.
- Header manipulation.

Do not perform destructive security testing against public services without permission.

---

# 17. PERFORMANCE AND RELIABILITY TESTING

Implement controlled tests for:

- Response time assertions.
- Repeated API calls.
- Basic concurrency.
- Timeout.
- Slow response.
- Retry behavior.
- Rate-limit handling.
- Service failure recovery.

Use local mocks for high-volume and fault-injection testing.

Do not conduct unapproved load tests against public practice APIs.

---

# 18. TESTNG ORGANIZATION

Create independent suite files:

```text
suites/
    restfulbooker-smoke.xml
    restfulbooker-regression.xml
    dummyjson-smoke.xml
    dummyjson-regression.xml
    gorest-smoke.xml
    gorest-regression.xml
    mocking-suite.xml
    ecommerce-e2e-suite.xml
    negative-suite.xml
    parallel-suite.xml
```

Use TestNG groups:

- smoke
- regression
- positive
- negative
- authentication
- e2e
- schema
- mocking
- parallel
- performance

Execution commands:

```bash
mvn clean test
```

```bash
mvn clean test -Dgroups=smoke
```

```bash
mvn clean test -Dgroups=regression
```

```bash
mvn clean test -Dgroups=e2e
```

```bash
mvn clean test -Dgroups=mocking
```

```bash
mvn clean test -Denv=qa
```

Ensure Surefire is correctly integrated with TestNG and suite selection.

---

# 19. REPORTING AND OBSERVABILITY

Use Allure.

Each test must capture:

- Test name.
- Test description.
- Endpoint.
- HTTP method.
- Request payload.
- Response payload.
- Status code.
- Execution time.
- Failure reason.
- Relevant logs.

Mask:

- Passwords.
- Tokens.
- API keys.
- Sensitive personal information.

Reports must show:

- Passed tests.
- Failed tests.
- Skipped tests.
- Broken tests.
- Retried tests.
- Execution duration.

---

# 20. CI/CD REQUIREMENTS

Implement GitHub Actions first.

Pipeline:

```text
Developer Push / Pull Request
             |
             v
        Checkout Code
             |
             v
         Setup Java
             |
             v
          Maven Build
             |
             v
        Compile Tests
             |
             v
         Smoke Suite
             |
             v
       Regression Suite
             |
             v
           E2E Suite
             |
             v
         Mock Suite
             |
             v
       Generate Allure
             |
             v
      Publish Test Results
```

Requirements:

- Java 17.
- Maven build.
- Secure secrets.
- Configurable environments.
- Smoke execution.
- Regression execution.
- E2E execution.
- Mock execution.
- Allure results.
- Test failure propagation.
- Build artifact retention.

Create a Jenkinsfile with equivalent pipeline stages.

Do not store credentials in the repository.

---

# 21. TEST COVERAGE MATRIX

Maintain a test coverage matrix.

| Module | Positive | Negative | E2E | Mocking |
|---|---|---|---|---|
| Authentication | Yes | Yes | Yes | Yes |
| Customer | Yes | Yes | Yes | Yes |
| Products | Yes | Yes | Yes | Yes |
| Cart | Yes | Yes | Yes | Yes |
| Booking | Yes | Yes | Yes | Yes |
| User management | Yes | Yes | Yes | Yes |
| Inventory | Simulated | Yes | Yes | Yes |
| Address | Simulated | Yes | Yes | Yes |
| Order | Simulated | Yes | Yes | Yes |
| Payment | Simulated | Yes | Yes | Yes |
| Shipment | Simulated | Yes | Yes | Yes |
| Refund | Simulated | Yes | Yes | Yes |
| Notification | Simulated | Yes | Yes | Yes |

Keep the matrix updated as implementation progresses.

---

# 22. PROJECT IMPLEMENTATION ROADMAP

Codex and Claude must implement this project in the following order.

Do not attempt to implement all modules in one change.

## Phase 0 – Repository Analysis

- Inspect complete existing repository.
- Read pom.xml.
- Inspect source classes.
- Inspect test classes.
- Inspect configuration.
- Inspect TestNG suites.
- Inspect GitHub Actions.
- Identify reusable utilities.
- Identify missing capabilities.
- Create gap analysis.

No code changes before approval.

## Phase 1 – Framework Foundation

- Review RestClient.
- Review ConfigManager.
- Review BaseTest.
- Review authentication.
- Review request specification.
- Review response validation.
- Review logging.
- Review dependencies.

Deliverable: Stable framework foundation.

## Phase 2 – Restful Booker

- Authentication.
- Booking GET.
- Booking POST.
- Booking PUT.
- Booking PATCH.
- Booking DELETE.
- Negative tests.
- Booking lifecycle E2E.

Deliverable: Complete booking module.

## Phase 3 – DummyJSON

- Login.
- Authenticated user.
- Products.
- Product search.
- Categories.
- Pagination.
- Sorting.
- Cart operations.
- Negative tests.
- Customer journey E2E.

Deliverable: Complete e-commerce API module.

## Phase 4 – GoREST

- User CRUD.
- Post CRUD.
- Authentication.
- User-post association.
- Negative tests.
- User lifecycle E2E.

Deliverable: Complete user management module.

## Phase 5 – Serialization and Data-Driven Testing

- POJO.
- Map.
- JSON file.
- Jackson.
- JsonPath.
- CSV.
- Excel.
- DataProvider.
- Schema validation.

Deliverable: Reusable data and validation framework.

## Phase 6 – API Mocking

- WireMock setup.
- Stub mappings.
- Request matching.
- Dynamic responses.
- HTTP errors.
- Delays.
- Timeouts.
- Stateful scenarios.
- Payment simulation.
- Order simulation.
- Inventory simulation.
- Shipment simulation.
- Refund simulation.
- Request verification.

Deliverable: Complete mocking framework.

## Phase 7 – E2E Order Processing

- Product discovery.
- Inventory.
- Cart.
- Address.
- Order.
- Payment.
- Shipment.
- Notification.
- Delivery.
- Cancellation.
- Refund.

Deliverable: Amazon-style simulated business workflow.

## Phase 8 – Async and Resilience

- Awaitility.
- Polling.
- Retry.
- Idempotency simulation.
- Timeout.
- Failure recovery.
- Circuit-breaker concepts.

Deliverable: Resilience testing.

## Phase 9 – Parallel Execution

- TestNG parallel execution.
- Parallel DataProvider.
- ThreadLocal.
- Race conditions.
- Thread safety.
- Unique data.
- WireMock isolation.

Deliverable: Parallel-safe framework.

## Phase 10 – Security and Performance

- Authentication testing.
- Authorization testing.
- Data isolation.
- Sensitive data masking.
- Response time.
- Controlled concurrency.
- Failure simulation.

Deliverable: Advanced testing capabilities.

## Phase 11 – Reporting and CI/CD

- Allure.
- GitHub Actions.
- Jenkins.
- Smoke.
- Regression.
- E2E.
- Mock suites.
- Report publishing.

Deliverable: CI/CD-integrated framework.

## Phase 12 – Final Architecture Review

- Code review.
- Framework design review.
- Maintainability.
- Reusability.
- Test coverage.
- Parallel safety.
- Documentation.
- Technical debt.
- Future improvements.

Deliverable: Final SDET portfolio project.

---

# 23. CODING STANDARDS – NON-NEGOTIABLE RULES

The coding agent must follow these rules throughout the project.

1. Preserve all existing test classes.
2. Do not modify existing test classes just to add new concepts.
3. Add each new scenario in a separate test class.
4. Reuse existing framework utilities.
5. Avoid duplicate utility classes.
6. Do not overengineer.
7. Use Java 17.
8. Follow consistent naming conventions.
9. Keep request and response POJOs organized.
10. Use dynamic IDs and tokens.
11. Generate unique test data.
12. Do not hardcode secrets.
13. Avoid unnecessary fixed sleeps.
14. Keep tests independent wherever possible.
15. Separate HTTP client behavior from test assertions.
16. Use meaningful exception messages.
17. Keep logs readable.
18. Mask sensitive information.
19. Add unit tests for reusable utilities.
20. Execute Maven tests after meaningful changes.
21. Document files added and modified.
22. Never claim tests passed unless executed successfully.
23. Respect real API capabilities.
24. Do not invent production endpoints or response contracts.
25. Keep real API tests and mock tests clearly separated.

If an existing class must be modified for a genuine defect or compatibility issue, explain the reason and request approval first.

---

# 24. ACCEPTANCE CRITERIA

A phase is complete only when:

- Required endpoints are automated.
- Positive scenarios pass.
- Negative scenarios are implemented.
- Response validation is implemented.
- Dynamic data is used.
- Tests are independent.
- Existing tests remain passing.
- Logs are meaningful.
- Reports are generated.
- Maven commands work.
- Secrets are protected.
- Documentation is updated.
- Known limitations are documented.

The entire project is complete when:

1. All three real practice APIs are covered.
2. CRUD workflows are implemented.
3. Authentication is implemented.
4. E2E workflows are implemented.
5. WireMock simulates unsupported services.
6. Payment and order failures are covered.
7. Async testing is implemented.
8. Parallel execution is stable.
9. Retry and idempotency concepts are tested.
10. Allure reporting is integrated.
11. GitHub Actions is working.
12. Jenkins pipeline is provided.
13. Architecture and README documentation are complete.

---

# 25. FINAL INSTRUCTIONS TO CLAUDE / CODEX

You are acting as a Senior SDET Architect responsible for building this framework.

Treat this document as the master source of truth.

## Step 1 – Understand before coding

Inspect the entire existing repository.

Understand:

- Current architecture.
- Existing classes.
- Existing utilities.
- Existing tests.
- Maven configuration.
- TestNG execution.
- Existing API clients.
- Existing CI/CD configuration.

Do not start writing code immediately.

## Step 2 – Prepare a gap analysis

Return:

1. Current package structure.
2. Current framework architecture.
3. Existing reusable classes.
4. Implemented requirements.
5. Missing requirements.
6. Proposed architecture.
7. Dependencies required.
8. Phase-wise implementation plan.
9. Exact files to add.
10. Existing files that may need modification, with justification.

Wait for approval.

## Step 3 – Implement incrementally

After approval:

- Implement only the approved phase.
- Reuse existing utilities.
- Add separate test classes.
- Add necessary test data.
- Add documentation.
- Execute relevant Maven tests.
- Report actual test results.
- List added and modified files.
- Explain design decisions.

Wait for approval before starting the next phase.

## Step 4 – Code review

At the end of each phase, review:

- Correctness.
- Code quality.
- Reusability.
- Maintainability.
- Test independence.
- Thread safety.
- Error handling.
- Logging.
- Security.
- Performance.

Do not move forward if critical tests fail.

## Step 5 – Final deliverable

Provide:

- Complete source code.
- Updated README.
- Architecture diagram.
- API endpoint inventory.
- Test coverage matrix.
- Execution commands.
- CI/CD configuration.
- Allure reporting instructions.
- Known limitations.
- Final architecture review.

**Final objective:**

Build an enterprise-grade API automation framework that demonstrates the engineering capabilities expected from a Senior SDET / SDET-3 / Test Automation Architect.

The framework must demonstrate not only how to automate REST APIs, but also how to design reliable tests for distributed systems, validate business workflows, simulate service failures, manage concurrency, debug production-like issues and integrate automation into CI/CD pipelines.

Do not sacrifice code quality for the number of test cases. Build the framework incrementally, with correctness, maintainability, reusability and test isolation as the primary engineering principles.