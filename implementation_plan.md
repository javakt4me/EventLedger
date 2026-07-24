# Implementation Plan

Reconstructed from the actual git history (`git log --stat`) and verified
against the current code — not an idealized retelling. Each phase lists what
was done and how it was checked.

## Phase 1 — Scaffolding
Two independent Spring Boot services (`event-gateway`, `account-service`),
each with its own H2 database, Dockerfile, and `pom.xml`.
**Verified:** both modules build and run independently (`mvn spring-boot:run`
in each), no shared DB or in-process state.

## Phase 2 — Core functionality
Idempotency (`eventId` uniqueness check in both services), out-of-order
tolerance (`findByAccountIdOrderByEventTimestamp`, balance as a pure sum),
input validation (`ValidationUtil`), balance computation.
**Verified:** unit tests in `AccountServiceTest`, `EventGatewayServiceTest`,
`ValidationUtilTest`; manually confirmed HTTP status codes match the spec
(`201` new, `200` duplicate, `400` invalid, `404` not found).

## Phase 3 — Resiliency
Resilience4j circuit breaker + retry-with-backoff on the Gateway→Account
call, `503` fallback on downstream failure.
**Verified:** `EventGatewayCircuitBreakerIntegrationTest` and
`ResilienceIntegrationTest` exercise this against a WireMock stub.
**Not fully verified — see Known Gaps below.**

## Phase 4 — Observability
Spring Cloud Sleuth for trace-ID generation/propagation, JSON structured
logs with `traceId`/`spanId` via logback MDC pattern, `/health` on both
services with DB connectivity checks, Micrometer custom metrics.
**Verified:** `EventGatewayTraceIntegrationTest` asserts a trace header
reaches the downstream WireMock stub; `/health` endpoints checked manually.

## Phase 5 — Test coverage push
Added controller/config/edge-case tests to raise coverage.
**Verified by actually running `mvn clean test jacoco:report`** (not by
trusting the commit message or README): 202 tests pass. Measured coverage —
`event-gateway` 97.5% instruction / 98.5% line; `account-service` 75.3%
instruction / 78.1% line; combined 88.9% instruction / 89.9% line. Numbers
regenerate with the same command; see `README.md#code-coverage`.

## Phase 6 — Docs/guardrails cleanup (this pass)
- Removed a false "100% code coverage" claim from `README.md` and replaced
  it with a description pointed at the real JaCoCo output instead of a
  hardcoded number that would go stale.
- Deleted 27 redundant, AI-generated "completion report" style markdown
  files at the repo root (`COMPLETION_REPORT.md`, `FINAL_VERIFICATION.md`,
  `PROJECT_SUMMARY.md`, etc.) after confirming nothing in code, `pom.xml`,
  or `.github/workflows/ci.yml` referenced them. `README.md` is now the
  single authoritative doc.
- Added this file and `prompt.md` so the process itself is reviewable.
**Verified:** `git status` reviewed before/after; `grep` confirmed no
non-markdown file referenced the deleted docs.

## Known gaps (deliberately not smoothed over)

These were found during review and are **not yet fixed** — out of scope for
the docs-only pass that produced this file, and listed here instead of
hidden:

1. **`EventGatewayCircuitBreakerIntegrationTest.java:94`** — the assertion
   `assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN)` is
   commented out. The test currently only confirms the circuit breaker
   *closes* after recovery, not that it actually *opens* on repeated
   failures, which is the specific behavior the assignment asks tests to
   cover. Needs the assertion restored and the underlying cause of it being
   flaky/failing (if any) diagnosed and fixed.
2. **Duplicate resilience config** — `event-gateway/.../config/ResilienceConfig.java`
   and `Resilience4jConfig.java` both define circuit breaker beans for
   `accountService`; `ResilienceConfig`'s is marked `@Primary` and its own
   comment states it exists to give tests a "test-friendly configuration,"
   meaning test-tuned values are wired into the real application context.
   These need to be reconciled into one properties-driven config.
3. **Committed build/test artifacts** — `src/test_backup/` (a near-duplicate
   copy of the test suite) and stray log files (`compile_test.log`,
   `jacoco_report.log`, `test_output.log`, `test_output.txt`) are tracked in
   git even though `.gitignore` now excludes them; they were added before
   the ignore rules existed and were never cleaned up. Needs `git rm`.
4. **`account-service` coverage gap** — `HealthController`,
   `GlobalExceptionHandler`, and the `ErrorResponse` DTO have little to no
   test coverage (see Phase 5 numbers). Needs new unit tests, not a
   documentation change.

Each of these requires touching `.java` files and was explicitly excluded
from the current pass by request. They are recorded here so the repo's docs
don't imply a cleaner state than the code is actually in.
