# Final Architecture Review

**Review date:** 2026-11-07  
**Scope:** Phases 1–12 of `master_reqskills.md`

## Review summary

The framework separates provider-specific endpoint clients, test assertions, shared HTTP behavior, configuration, logging, resilience utilities and local service simulations. API wrappers are intentionally thin. Tests use unique data for mutable practice API workflows, while unsupported commerce services run against isolated local WireMock instances.

### Architecture and maintainability

- `RestClient` owns request construction and transport behavior; endpoint tests own assertions.
- Authentication is applied to individual requests, and each operation receives a fresh RestAssured request specification.
- Configuration uses environment defaults with system-property and environment-variable overrides.
- Test fixtures and data readers remain in test source scope, so they do not enter the runtime framework artifact.
- WireMock tests use dynamic ports. The parallel suite limits concurrency to local-safe tests.
- HTTP GET rate-limit retries honor a numeric `Retry-After` value or bounded fallback delay. POST, PUT, PATCH and DELETE are not retried automatically.
- Masked exchanges are captured for Allure on the test thread. The filter masks configured secret and personal-data field names in logs and attachments.

## Acceptance review

| Area | Status | Evidence |
|---|---|---|
| Restful Booker, DummyJSON and GoREST | Implemented | Provider API wrappers, endpoint tests and documented service limits |
| CRUD and authentication | Implemented where providers support them | Lifecycle, auth and authorization suites |
| Cross-service commerce workflow | Simulated | `CommerceApi`, WireMock scenarios and order-processing E2E tests |
| Failure, async, retry and circuit-breaker scenarios | Implemented with controlled local services | Resilience and mock suites |
| Security and performance examples | Implemented as deterministic local mock tests | `SecurityPerformanceTest` and sensitive-data filter checks |
| Parallel-safe execution | Implemented | `testng-parallel.xml`, thread-local cleanup and dynamic WireMock ports |
| Allure reporting | Implemented and locally generated | Full-suite results generated as HTML at `target/site/allure-maven-plugin/index.html` after the final review fixes |
| GitHub Actions and Jenkins | Pipeline definitions provided | Workflow YAML and helper script syntax validated; local mocking suite confirmed per-suite report preservation. Hosted services were not available for an actual remote run |
| Documentation and coverage inventory | Implemented | README, architecture, endpoint inventory and coverage matrix |

## Known technical debt and limits

1. Public practice services can change, throttle, or become unavailable. Their tests are integration checks, not deterministic unit tests.
2. The complete GoREST write check needs `GOREST_ACCESS_TOKEN`; it skips when no token is configured. The token scopes and Jenkins credential setup are documented.
3. DummyJSON cart operations return simulations and do not persist. Restful Booker date-filter behavior differed from its expected contract during validation. These are provider limitations, not modeled production guarantees.
4. Sensitive-data masking uses field-name patterns. New secret or personal-data field names must be added to the filter and its tests; opaque payloads cannot be inspected safely and binary bodies are omitted from logs.
5. `RetryExecutor` is a generic action utility. Callers must only supply operations that are safe to repeat; it does not infer HTTP method or idempotency.
6. The checked-in GitHub Actions and Jenkins definitions need to be exercised in the target repository/instance after required secrets, tools, permissions and Jenkins plugins are configured.
7. Contracts are strongest for the covered booking flows, GoREST payload validation, and the DummyJSON product schema. Expanding schema and contract coverage to every provider endpoint is a future improvement.

## Recommended follow-up

- Add provider contract schemas for booking and GoREST responses when stable schemas are available.
- Keep CI results and flaky external-service observations visible as separate integration and mock suite outcomes.
- Add a typed retry policy that accepts an explicit repeat-safety classification if more HTTP methods need automatic retry behavior.
- Validate the Jenkins pipeline and GitHub Actions workflow in their hosted environments after repository secrets and tool installations are configured.
- Review dependency and action versions on a scheduled maintenance cycle.

## Final test run

After the architecture-review fixes, validation completed with:

```bash
mvn clean test                         # 83 run, 0 failures, 0 errors, 1 expected skip
mvn -Pparallel-tests clean test        # 41 run, 0 failures, 0 errors, 0 skipped
bash scripts/run-ci-suite.sh mocking   # 11 run, 0 failures, 0 errors, 0 skipped
mvn allure:report                      # generated full-suite HTML report
```

The default suite includes live practice API calls; the parallel profile runs local parallel-safe checks. The GoREST write test is the expected skip when `GOREST_ACCESS_TOKEN` is not configured. Hosted GitHub Actions and Jenkins execution remains to be confirmed in their configured environments.
