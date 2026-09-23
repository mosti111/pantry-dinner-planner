# Security, privacy, and operations

## Threat priorities

- Cross-guest/account access to plans or baskets.
- Guest-token theft or fixation.
- Allergy/dietary rule bypass.
- Prompt injection or malformed AI output affecting trusted decisions.
- Price/catalog manipulation and stale commercial data.
- Duplicate cart creation through retries.
- SSRF, secrets leakage, excessive logging, and provider webhook spoofing.
- Abuse of costly planning/AI/catalog endpoints.

## Controls

- Opaque high-entropy guest token in Secure, HttpOnly, SameSite cookie; hash at rest; rotate on privilege/account transition.
- Every resource query is ownership-scoped; object IDs alone never authorize access.
- Spring Security deny-by-default policies and method/use-case authorization.
- CSRF protection for cookie-authenticated mutations, restrictive CORS, CSP and secure headers.
- Request size, timeout, and rate limits at edge and application boundaries.
- Strict structured AI schemas, bounded values, escaping, provider timeouts, and no tool authority for the model.
- Retailer host allowlists and safe HTTP client defaults to prevent SSRF.
- Secrets from managed secret storage; automated secret scanning and rotation process.
- Sensitive structured fields redacted from logs. No raw guest tokens, prompts containing personal data, or full provider payloads in routine telemetry.
- SBOM, pinned build actions, dependency updates, SAST, container scanning, and signed release artifacts when practical.

## Privacy

Collect only information needed for planning. Dietary and allergy data may be sensitive even when not legally classified as medical data; treat it with elevated care.

Required product decisions before launch:

- retention for anonymous sessions and operational logs;
- deletion/export workflow;
- lawful notices and cookie policy;
- subprocessors and data residency;
- whether AI provider input contains any personal data and the provider's retention controls.

Account claiming must preserve provenance and prevent one account from claiming another guest's session. Expired sessions are purged by a retryable background job.

## Food-safety position

Pantry is a planning aid, not medical advice. Product labels and retailer data are authoritative for real purchases. Unknown allergen metadata blocks automatic acceptance. Cross-contact requirements must be explicit. Safety messages are visible at planning and recipe/basket review, not buried solely in legal text.

## Observability

- The repository exposes minimal public liveness/readiness. `metrics` and `prometheus` are present but denied by the public security chain until production supplies a private management network or explicit operations authentication. OTLP export is disabled by default and becomes active only with explicit environment configuration.
- Custom counters track plan creation/replay, cart creation/replay, and idempotency conflicts. HTTP, database, and future provider spans use Micrometer/OpenTelemetry.
- Structured JSON logs with correlation, plan, workflow, and provider-attempt IDs—never raw secrets.
- Metrics for workflow duration/state, proposal failures, unresolved ingredients, match confidence, budget failures, stale offers, recovery outcomes, and cart idempotency conflicts.
- Distributed traces around API, database, AI, and retailer boundaries using Micrometer/OpenTelemetry.
- Liveness means process health; readiness checks essential dependencies without making destructive calls.

Alert on sustained planning failure, unsafe validation anomalies, queue/event backlog, retailer error spikes, database saturation, and elevated authorization failures. Alerts link to versioned runbooks.

## Reliability

- Timeouts on every external call; narrow retries with jitter only for safe transient errors.
- Circuit breakers/bulkheads when a real provider demonstrates the need.
- Idempotent workflow steps and reconciliation instead of blind retries.
- PostgreSQL backups with tested restore objectives; document RPO/RTO before production.
- Graceful shutdown and no lost in-progress event publications.
- Feature flags and rollback-compatible migrations for risky releases.

## Operational runbooks

Versioned first-response procedures and escalation/verification checklists are in [runbooks.md](runbooks.md). Real contacts, paging routes, RPO/RTO and provider status links must be filled by the production owner before launch.

- AI provider degraded/unavailable.
- Retailer catalog, price, or cart API degraded.
- Event publication backlog.
- Database saturation or failed migration.
- Suspected guest/account data exposure.
- Incorrect allergen metadata or unsafe recipe report.
- Key/secret compromise.
- Release rollback and data restore.
