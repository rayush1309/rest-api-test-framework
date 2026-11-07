# Test Coverage Matrix

| Module | Positive | Negative | End-to-end | Mocking |
|---|---|---|---|---|
| Framework configuration and HTTP client | Configuration, serialization, request execution | Invalid configuration, rate-limit and error handling | Shared client used by workflows | WireMock request/response and fault checks |
| Restful Booker | Authentication, reads, booking CRUD | Invalid credentials, authorization and query edge cases | Booking create/read/update/delete lifecycle | Not applicable |
| DummyJSON | Product, authentication and cart contracts | Invalid credentials, product and cart boundary cases | Customer product-to-cart journey | Cart writes are documented service simulations |
| GoREST | User and post reads | Missing/invalid credentials and payload validation | User/post lifecycle | Local HTTP server for deterministic validation/lifecycle cases |
| Simulated commerce | Inventory, cart, order, payment and fulfillment | Declined payment, cancellation and refund errors | Order fulfillment, delivery, cancellation and refund | WireMock-backed inventory, address, order, payment, shipment, notification and refund services |
| Security and reliability | Credential isolation and bounded response checks | Expired credentials, forbidden access, unsafe input and unsupported content type | Security behavior exercised inside local simulated workflows | Isolated WireMock scenarios; no destructive public API security testing |

The mock-based cases validate expected client and workflow behavior against controlled responses. They do not measure or certify production service security, availability, or throughput.
