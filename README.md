# REST API Test Framework

Java 17, Maven, TestNG and RestAssured framework for API automation against public practice APIs and controlled local mocks. Real API tests and WireMock simulations use separate clients, suites and reporting labels.

## Current status

Phases 1–11 are in place: environment configuration, per-request authentication, a stateless HTTP client, sensitive-data-aware exchange logging, API coverage for Restful Booker, DummyJSON and GoREST v3, reusable JSON/CSV/Excel data readers, JSON Schema validation, WireMock API simulations, a simulated order journey, async/resilience helpers, parallel-safe local test execution, deterministic security/performance checks against local mocks, Allure reporting, and GitHub Actions/Jenkins pipelines.

## Requirements

- JDK 17 or newer
- Maven 3.9 or newer

## Run

```bash
mvn clean test
```

Run the local parallel-safe suite with:

```bash
mvn -Pparallel-tests clean test
```

In IntelliJ IDEA, select the shared **Parallel Local Tests** run configuration (or **All Tests**) from the Run widget. They are stored under `.idea/runConfigurations/`. In VS Code, open **Terminal → Run Task** and choose **Tests: All (Maven)** or **Tests: Parallel Local (Maven)**; the tasks are in `.vscode/tasks.json`. The Java Test Runner can run discovered test classes and methods from `src/test/java`; the XML files under `src/test/resources/` define Maven's TestNG suites and are not runnable Java test files. Install the recommended Java and Maven extensions if VS Code does not discover the Java tests.

The default suite runs local framework, WireMock, simulated order-journey, async, resilience, security and performance checks, live Restful Booker, DummyJSON and GoREST reads, plus serialization, fixture-reader, and schema checks. The `parallel-tests` profile runs parallel-safe local checks with concurrent TestNG classes and parallel DataProvider cases; it avoids concurrent requests to public practice APIs. Per-thread test context is cleared by a TestNG listener. WireMock mappings and response fixtures are under `src/test/resources/wiremock/`; the simulations cover request matching, dynamic responses, HTTP errors, delays, timeouts, stateful inventory and checkout, payments, shipments, refunds, and request verification. The simulated order journey covers product discovery, inventory reservation, cart, address validation, order, payment, shipment, notification, delivery, cancellation, and refund without external services. Resilience checks cover Awaitility polling, bounded retry/backoff, idempotent payment replay, timeout recovery, and circuit-breaker state transitions. Security/performance checks use isolated local mocks for expired/missing credentials, role enforcement, customer data isolation, malicious input and content-type validation, repeated calls, and controlled concurrency. These mock checks exercise the framework's request behavior; they do not certify the security or performance of a production service. CSV and Excel product cases are under `src/test/resources/data/`; the product response schema is under `src/test/resources/schemas/`. Restful Booker tests create unique bookings and delete them during cleanup. DummyJSON cart writes are simulations; tests validate returned responses and do not expect server persistence. GoREST read and authentication-negative tests run without an access token. GoREST payload-validation and user/post lifecycle tests use a local HTTP server, so they run without credentials or network access. The live sample-row write-protection check requires `GOREST_ACCESS_TOKEN` and skips when it is absent.
The default suite runs local framework, WireMock, simulated order-journey, async, resilience, security and performance checks, live Restful Booker, DummyJSON and GoREST reads, plus serialization, fixture-reader, and schema checks. The `parallel-tests` profile runs parallel-safe local checks with concurrent TestNG classes and parallel DataProvider cases; it avoids concurrent requests to public practice APIs. Per-thread test context is cleared by a TestNG listener. WireMock mappings and response fixtures are under `src/test/resources/wiremock/`; the simulations cover request matching, dynamic responses, HTTP errors, delays, timeouts, stateful inventory and checkout, payments, shipments, refunds, and request verification. The simulated order journey covers product discovery, inventory reservation, cart, address validation, order, payment, shipment, notification, delivery, cancellation, and refund without external services. Resilience checks cover Awaitility polling, bounded retry/backoff, idempotent payment replay, timeout recovery, and circuit-breaker state transitions. Security/performance checks use isolated local mocks for expired/missing credentials, role enforcement, customer data isolation, malicious input and content-type validation, repeated calls, and controlled concurrency. These mock checks exercise the framework's request behavior; they do not certify the security or performance of a production service. CSV and Excel product cases are under `src/test/resources/data/`; the product response schema is under `src/test/resources/schemas/`. Restful Booker tests create unique bookings and delete them during cleanup. DummyJSON cart writes are simulations; tests validate returned responses and do not expect server persistence. GoREST read and authentication-negative tests run without an access token. GoREST payload-validation and user/post lifecycle tests use a local HTTP server, so they run without credentials or network access. The live sample-row write-protection check requires `GOREST_ACCESS_TOKEN` and skips when it is absent.

## Allure reports

The TestNG adapter writes results to `target/allure-results` and records test status, duration and failure details. The report listener attaches sanitized request/response exchanges. The HTTP filter masks credentials and common personal fields before logging or attaching data.

```bash
mvn clean test
mvn allure:report
```

Open `target/site/allure-maven-plugin/index.html`. To generate and view the report through a local server, run `mvn allure:serve`.

## CI/CD

`.github/workflows/api-tests.yml` runs on pushes, pull requests and manual dispatch. It compiles tests, runs smoke, regression, end-to-end and mock TestNG groups, creates an Allure HTML report, and uploads raw results, per-suite Surefire XML and the report as a 14-day artifact. Test stages continue long enough to publish results, then the workflow fails if any required stage failed. Select `qa`, `dev` or `prod` when manually dispatching. `scripts/run-ci-suite.sh` preserves each suite's JUnit XML before Surefire replaces its default report file.

Optional GitHub Actions secrets are `GOREST_ACCESS_TOKEN`, `DUMMYJSON_TEST_USERNAME` and `DUMMYJSON_TEST_PASSWORD`; secrets are passed through environment variables and are not stored in the repository. The sample-row GoREST authorization check skips when its token is absent.

`Jenkinsfile` has matching stages. Configure Jenkins tools named `JDK17` and `Maven3`, install the Allure Jenkins plugin and configure its Allure command-line tool, then add `gorest-access-token` as a Secret Text credential. The pipeline publishes per-suite JUnit XML, archives test/report artifacts, and publishes Allure results while preserving test failures as build failures.

The CI group suites are under `src/test/resources/suites/`. The module-level coverage matrix is maintained in [docs/test-coverage-matrix.md](docs/test-coverage-matrix.md).

Select an environment with `-Denv=qa`, `-Denv=dev` or `-Denv=prod`. Configuration values can be overridden with Java system properties or environment variables. For example:

```bash
mvn test -Denv=qa -Ddummyjson.base.url=https://dummyjson.com
```

Environment defaults live in `src/main/resources/config/`. Do not put credentials or tokens in those files; provide secrets through environment variables or CI secrets. A property such as `gorest.access.token` maps to `GOREST_ACCESS_TOKEN`.

## Framework structure

```text
src/main/java/com/sdetlabs/
  api/booking/       Restful Booker API wrapper and models
  api/dummyjson/     DummyJSON API wrapper and models
  api/gorest/        GoREST API wrapper and models
  api/commerce/      Local simulated storefront API wrapper
  framework/auth/    Per-request authentication strategies
  framework/config/  Environment configuration and validation
  framework/http/    Stateless RestAssured HTTP client
  framework/logging/ Masked logs and report exchange capture
  framework/resilience/ Retry and circuit-breaker utilities
src/test/java/com/sdetlabs/
  api/               Practice API and commerce journey tests
  framework/         Framework, local mock, security and resilience tests
  framework/data/    JSON, CSV and Excel readers plus serialization tests
src/test/resources/
  testng*.xml        Default and parallel TestNG suites
  suites/            CI smoke, regression, end-to-end and mock suites
  data/              JSON, CSV and Excel test fixtures
  schemas/           JSON Schema contracts
  wiremock/          Local WireMock mappings and response fixtures
```

Every client request uses a new RestAssured request specification. The client does not impose expected status codes; endpoint tests own those assertions. HTTP GET responses with status 429 are retried up to three times, honoring a numeric `Retry-After` header when present. State-changing HTTP methods are never retried automatically. Authorization headers, cookies, API keys, password/token fields and common personal fields in bodies, headers and query parameters are masked in HTTP logs.

Architecture, real and simulated endpoint inventory, and the final review are documented in [docs/architecture.md](docs/architecture.md), [docs/api-endpoint-inventory.md](docs/api-endpoint-inventory.md), and [docs/final-architecture-review.md](docs/final-architecture-review.md).

## Known limits

The public Restful Booker API accepts the documented `checkin` and `checkout` query parameters, but the current live service did not return a newly created booking when filtered by its exact dates during validation. The suite validates the date-filter response contract and covers working combined name filters; it does not claim that the live date filtering is correct.

DummyJSON's documented cart create/update/delete operations are simulated and do not persist. The current API also returned a successful empty cart for an unknown product ID and accepted a negative cart quantity during live validation. Negative behavior tests record these service limitations rather than treating the simulation as production business validation. The bundled DummyJSON login is the public sample account from its documentation; override `dummyjson.test.username` and `dummyjson.test.password` or provide `DUMMYJSON_TEST_USERNAME` and `DUMMYJSON_TEST_PASSWORD` if the practice account changes.

GoREST v3 uses UUID string IDs and a `{data, meta}` envelope for collection reads. The current v3 user contract does not include `gender` (that field belongs to v2); user creation uses `name`, `email` and optional `status`. GoREST tests need an access token with `users:read`, `users:write`, `posts:read` and `posts:write` scopes for full CRUD and lifecycle coverage. GoREST resets account data periodically, so tests create uniquely named records and clean them up.

Public practice APIs can be unavailable or change behavior; those checks are kept distinct from deterministic local mock tests. CI jobs therefore retain raw test results and Allure report artifacts even when a test stage fails.
