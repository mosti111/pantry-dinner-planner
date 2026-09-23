# Deployment guide

## Supported shape

Deploy one stateless Next.js service, one stateless Spring Boot service, and PostgreSQL 17. The browser talks only to Spring Boot; AI and retailer traffic originates from backend adapters. Start with at least two backend replicas only after scheduled-job coordination is added. Do not add Redis, Kafka, or search infrastructure without the measured gates in [architecture.md](architecture.md).

## Required production configuration

| Variable | Requirement |
|---|---|
| `SPRING_DATASOURCE_URL` | TLS-enabled managed PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME/PASSWORD` | Least-privilege application role from secret storage |
| `PANTRY_ALLOWED_ORIGIN` | Exact HTTPS frontend origin; never `*` with credentials |
| `PANTRY_DEMO_MODE` | `false` |
| `PANTRY_DB_POOL_SIZE` | Sized against total replicas and database connection budget |
| `PANTRY_TRACE_SAMPLE_RATE` | Approved cost-sensitive value, normally `0.01`–`0.10` |
| `PANTRY_OTLP_ENABLED` | `true` only when a protected OTLP collector is configured |
| `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` | Private collector endpoint when OTLP is enabled |
| `NEXT_PUBLIC_API_BASE_URL` | Public HTTPS API base ending in `/api/v1` |

Provider credentials are added only by the selected adapters and must never be baked into images or frontend variables.

Guest-session cookies are always `HttpOnly`, `Secure`, and `SameSite=Lax`; there is no configuration switch that can weaken them. Local browsers treat `localhost` as a trustworthy development origin, while every non-local deployment must terminate HTTPS before traffic reaches the application.

## Release sequence

1. Build immutable backend/frontend images from a reviewed commit; record image digests and dependency/SAST results.
2. Back up the database and confirm the last restore rehearsal met the approved RPO/RTO.
3. Run Flyway once as a controlled release job using a migration role. Application roles should not own schema objects.
4. Verify migrations are backward compatible with the previous application version.
5. Roll out backend instances with readiness at `/actuator/health/readiness`, then the frontend.
6. Smoke-test guest creation, plan generation, resume, ownership denial, already-have selection, refresh, and idempotent cart replay.
7. Watch error rate, latency, database saturation and Pantry counters through the agreed observation window.

Rollback application images without rolling back a database migration. Use expand/migrate/contract and ship a forward repair migration if schema correction is necessary.

## Scaling and capacity

- Keep HTTP instances stateless; the opaque guest cookie contains only a random token and all workflow state is in PostgreSQL.
- Calculate database pool budget as `replicas × maximum-pool-size + migration/operations headroom` below the managed database limit.
- Scale from latency, CPU and pool-wait evidence. Planning is currently local/deterministic; provider calls must gain timeouts, bulkheads and asynchronous reconciliation before high concurrency.
- Place request-body and distributed rate/cost limits at the trusted ingress. Add Redis only if limits must remain exact across replicas and ingress facilities are insufficient.
- Before multiple scheduler replicas, add PostgreSQL advisory locking or a maintained distributed scheduling lock so cleanup/refresh jobs have one logical executor.

## Production verification

CI verifies Java unit/module/contract tests, React unit/type/lint/build checks, the Playwright journey, real PostgreSQL migrations/application behavior, and container builds. The deployment environment must additionally run TLS, headers, CORS/CSRF, backup restore, load, accessibility, dependency/container scanning, and provider sandbox smoke tests.
