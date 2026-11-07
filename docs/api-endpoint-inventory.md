# API Endpoint Inventory

The first three groups below target public practice APIs. The local storefront group targets WireMock simulations in this repository. Exact response persistence and behavior follow each provider's documented/observed limitations, not production-commerce assumptions.

## Restful Booker

Base URL: environment key `booking.base.url` (practice service).

| Method | Path | Purpose | Coverage |
|---|---|---|---|
| GET | `/ping` | Health check | Framework check |
| POST | `/auth` | Obtain a cookie token | Authentication positive/negative |
| GET | `/booking` | List booking IDs; supports documented filters | Read and query tests |
| GET | `/booking/{id}` | Read a booking | Read and lifecycle tests |
| POST | `/booking` | Create a booking | Lifecycle and payload format tests |
| PUT | `/booking/{id}` | Replace a booking | Lifecycle and authorization tests |
| PATCH | `/booking/{id}` | Partially update a booking | Lifecycle tests |
| DELETE | `/booking/{id}` | Delete a booking | Lifecycle cleanup |

## DummyJSON

Base URL: environment key `dummyjson.base.url`.

| Method | Path | Purpose | Coverage |
|---|---|---|---|
| POST | `/auth/login` | Authenticate sample user | Authentication tests |
| POST | `/auth/refresh` | Refresh an access token | Authentication tests |
| GET | `/auth/me` | Read current user with bearer auth | Authentication tests |
| GET | `/products` | List/search/filter products | Catalog and data-driven tests |
| GET | `/products/{id}` | Read product detail | Catalog and schema tests |
| GET | `/products/search?q={query}` | Search products | Catalog tests |
| GET | `/products/categories` | Read category resources | Catalog tests |
| GET | `/products/category-list` | Read category names | Catalog tests |
| GET | `/products/category/{category}` | Filter products by category | Catalog tests |
| GET | `/carts` | List carts | Cart contract tests |
| GET | `/carts/{id}` | Read cart | Cart contract tests |
| GET | `/carts/user/{userId}` | List carts for a user | Cart/customer journey tests |
| POST | `/carts/add` | Simulate cart creation | Cart simulation tests |
| PUT | `/carts/{id}` | Simulate cart replacement | Cart simulation tests |
| PATCH | `/carts/{id}` | Simulate cart update | Cart simulation tests |
| DELETE | `/carts/{id}` | Simulate cart deletion | Cart simulation tests |

DummyJSON cart writes return simulated responses and are not expected to persist.

## GoREST v3

Base URL: environment key `gorest.base.url`, currently configured for `/public/v3`.

| Method | Path | Purpose | Coverage |
|---|---|---|---|
| GET | `/users` | List users with query filters | Read tests |
| GET | `/users/{uuid}` | Read user | Read tests |
| POST | `/users` | Create user (bearer token required) | Lifecycle and auth tests |
| PATCH | `/users/{uuid}` | Update user | Lifecycle and authorization tests |
| DELETE | `/users/{uuid}` | Delete user | Lifecycle cleanup |
| GET | `/posts` | List posts with query filters | Read tests |
| GET | `/posts/{uuid}` | Read post | Read tests |
| POST | `/posts` | Create post (bearer token required) | Lifecycle and auth tests |
| PATCH | `/posts/{uuid}` | Update post | Lifecycle tests |
| DELETE | `/posts/{uuid}` | Delete post | Lifecycle cleanup |

GoREST v3 IDs are UUID strings. Full write coverage requires a token with the documented scopes; tests that require unavailable credentials skip or use local HTTP servers as described in the README.

## Local simulated storefront

These routes are served by WireMock for deterministic tests. They are not public services.

| Method | Path | Purpose |
|---|---|---|
| GET | `/products/{productId}` | Product lookup |
| GET | `/inventory/{sku}` | Inventory lookup |
| POST | `/inventory/reservations` | Reserve stock |
| POST | `/inventory/releases` | Release stock |
| POST | `/carts` | Create cart |
| POST | `/carts/{cartId}/items` | Add a cart item |
| POST | `/addresses/validate` | Validate delivery address |
| POST | `/orders` | Create order |
| GET | `/orders/{orderId}` | Read order state |
| POST | `/orders/{orderId}/cancel` | Cancel order |
| POST | `/payments` | Simulate payment authorization/decline |
| POST | `/shipments` | Create shipment |
| GET | `/shipments/{trackingId}` | Read shipment state |
| GET | `/deliveries/{trackingId}` | Read delivery state |
| GET | `/notifications?orderId={orderId}` | Read generated order notifications |
| POST | `/refunds` | Simulate refund |

See [the coverage matrix](test-coverage-matrix.md) for corresponding positive, negative, end-to-end and mocking coverage.
