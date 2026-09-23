# Testing strategy

Quality is organized around risks rather than a target coverage percentage. Allergy safety, money, package math, authorization, idempotency, and recovery receive the strongest tests.

## Test layers

### Pure unit tests

- Money and quantity value objects.
- Unit conversion and rounding.
- Aggregation and package calculations.
- Dietary/allergen rules, including unknown metadata failing closed.
- Match filtering/ranking and explanation components.
- Budget and waste optimization.
- State transitions and substitution order.
- Idempotency fingerprint behavior.

Use parameterized and property-based tests for numeric invariants and boundary cases.

### Module tests

- Spring Modulith module-boundary verification.
- Application use cases with fake ports.
- Transaction and event behavior per module.
- ArchUnit clean-architecture rules.

### Integration tests

- Real PostgreSQL 17 through Testcontainers when Docker is available, with a managed embedded PostgreSQL 17 binary fallback otherwise; never H2 and never a conditional skip.
- Flyway clean install and upgrade paths.
- JPA mappings, optimistic locking, constraints, and `pg_trgm` ranking.
- Security filters and ownership isolation.
- Modulith event publication completion/retry.

### Contract tests

- OpenAPI request/response compatibility.
- Consumer/provider contracts for retailer adapters.
- Recorded sanitized fixtures for provider schemas, with live-provider tests isolated from normal CI.
- AI structured-output parser against valid, incomplete, malicious, and schema-drift payloads.

### End-to-end tests

The CI Playwright critical journey currently proves guest restoration, plan creation, dinner review, the already-have adjustment, recalculated totals, and idempotent cart completion through mocked provider-independent HTTP boundaries.

Expansion scenarios for provider/account rollout:

1. dietary/allergy constraints survive every replacement;
2. a stale update produces a recoverable conflict;
3. same-ingredient SKU, approved ingredient, and affected-meal recovery;
4. price increase pushes a plan over budget safely;
5. retailer/cart failure preserves recoverable state;
6. a verified login claims a guest session and rotates credentials.

## Non-functional tests

- k6/Gatling load test for catalog search, plan status polling, and basket reads.
- Concurrency tests for competing basket edits and repeated finalization.
- Failure injection for AI/retailer timeouts, malformed responses, and database retry boundaries.
- OWASP dependency/container/code scanning and authorization tests.
- Accessibility automation plus manual review.
- Backup-restore and migration rehearsal before production releases.

## CI gates

Pull requests run unit tests, module/architecture checks, real-PostgreSQL integration tests, reviewed API-contract checks, frontend lint/type/component tests, production build, CodeQL/dependency checks, a critical Playwright journey, and container builds. Broader provider/performance/restore suites belong to main/nightly or release candidates once their environments exist.

A test may not be disabled without an issue, owner, reason, and expiry date. The database provider is infrastructure selection, not test selection: the same migration and application tests execute on both paths.

## Definition of done

A feature is done when behavior and failure modes are specified; code respects module boundaries; migrations and APIs are compatible; tests cover core and edge cases; logs/metrics exist; security/privacy implications are reviewed; user-facing states are accessible; and relevant documentation is updated.
