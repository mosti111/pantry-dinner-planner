# Implementation status

Status: GitHub-ready, provider-independent production foundation. The complete local journey works; external capabilities remain accurately disabled.

## Implemented end to end

| Area | Evidence |
|---|---|
| Guest lifecycle | Secure random token, SHA-256 storage, HttpOnly/SameSite cookie, resume, expiry, cleanup, ownership-scoped queries, deletion cascade |
| Planning | Validated request, curated safe recipes, time/allergen/diet/exclusion filtering, persisted request and plan snapshots |
| Ingredient handling | Canonical keys, English/Turkish alias normalization, separate recipe ingredients and retailer SKUs, `pg_trgm` schema foundation |
| Pricing | Decimal money/quantity types, demand aggregation, `ceil(required / package size)`, leftovers, full-package totals, budget gate |
| Basket | Already-have selection, alternatives, refresh, optimistic locking, meal replacement, review and completion states |
| Recovery | Six demo scenarios and the SKU → ingredient/meal → retailer/retry escalation story |
| Retry safety | Idempotency key plus request fingerprint for planning; persisted idempotency result for cart creation |
| Provider seams | Typed AI proposal contract and validator, recipe catalog port, capability-aware retailer gateway, deterministic fakes, no provider facts trusted from AI |
| Provider-ready persistence | Normalized account, ingredient, recipe version, retailer/product/mapping, offer snapshot, transition, AI proposal, integration-call, and reconciled cart-attempt schema |
| Account readiness | Hashed provider subjects and an idempotent guest-claim service; deliberately no public claim endpoint before verified OIDC wiring exists |
| Security | CSRF, restrictive credentialed CORS, safe cookie defaults, CSP/API headers, frontend headers, correlation IDs, sanitized Problem Details, no committed secrets |
| Observability | Actuator probes, public-chain-denied metrics/Prometheus endpoints, OpenTelemetry bridge/optional OTLP export, and planning/cart/idempotency counters |
| Delivery | Multi-stage non-root containers, Compose, Maven/pnpm wrappers and lockfiles, CI, browser journey, CodeQL, Dependabot, contribution/security templates |
| Frontend | Faithful reconstruction of the surviving Pantry deployment with recovered local assets, responsive setup/plan/recipe/basket/completion flows, demo recovery controls, and guest-data controls wired to the real API |
| Verification | 40 backend tests with zero skips, including Modulith boundaries, the reviewed OpenAPI contract, and real PostgreSQL 17 migration/application behavior; frontend lint/type/component/API tests and Playwright E2E |

## Intentional provider boundaries

The reference catalog and prices are deterministic demo fixtures. They are not presented as Migros, A101, ŞOK, CarrefourSA, or any other retailer's live facts. A real adapter must supply a store context, freshness metadata, capabilities, rate limits, stock/price refresh, idempotent cart semantics, and reconciliation behavior. Basket/cart manipulation cannot be enabled without authorized retailer access.

The curated planner is the always-available baseline. A Spring AI adapter is an optional proposal source behind the contract described in the domain model. Its structured result remains untrusted until the same deterministic safety, ingredient, pricing, and budget pipeline accepts it. No API key is necessary to evaluate this repository.

## Production deployment checklist

Code-level safeguards are present, but production readiness also depends on environment and organizational controls:

1. Set `PANTRY_DEMO_MODE=false`, an exact HTTPS `PANTRY_ALLOWED_ORIGIN`, and managed database credentials. Guest-session cookies are always marked `Secure` and cannot be weakened through configuration.
2. Terminate TLS at a trusted ingress; apply request-body limits and distributed rate/cost limits there. Add Redis only if those limits must be coordinated in the application tier.
3. Use managed secret storage, restricted database roles, encrypted backups, restore tests, centralized logs/metrics/traces, and alerting.
4. Complete privacy retention notices, food-safety review, retailer contracts, and the incident runbooks in `security-operations.md`.
5. Keep the mandatory PostgreSQL test provider enabled in CI. It uses Testcontainers with Docker and automatically falls back to a managed PostgreSQL 17 binary without Docker; database tests are never conditionally skipped.
6. Choose a license and private security contact before changing repository visibility to public.

## The exact remaining external work

- Identity login: choose an OIDC provider, register clients/redirects, validate issuer/audience/keys, and expose the already-built guest-claim use case only after that validation boundary exists.
- Live AI: choose a provider/model, approve cost and retention, supply credentials, implement one `MealProposalGateway`, and run a representative safety/relevance evaluation set.
- Live retailer catalog/cart: obtain an authorized sandbox/partner contract, store/storefront identifiers, credentials and webhook details; implement one `RetailerGateway` and certify its capability flags/reconciliation behavior.
- Production environment: provision DNS/TLS, managed PostgreSQL, secret storage, telemetry backend, backups, alert ownership and cost limits; run restore/load/security rehearsals.
- Governance: choose the public license/security contact and approve privacy, cookie, food-safety, AI-subprocessor and retailer terms.
- Redis, Kafka, and Elasticsearch/OpenSearch: adoption gates are defined in `architecture.md`.
- Microservices: module boundaries are extraction seams; the modular monolith remains the right first deployment.
