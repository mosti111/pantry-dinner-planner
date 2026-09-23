# ADR 0005: Secure stateless scale baseline

- Status: Accepted
- Date: 2026-09-21

## Context

Pantry must be credible as a portfolio system without paying the operational cost of distributed infrastructure before traffic or provider workloads justify it. Guest ownership, food constraints, monetary calculations, and eventual retailer cart writes create higher risks than ordinary tutorial CRUD.

## Decision

Keep frontend and backend processes stateless and place all authoritative workflow/idempotency state in PostgreSQL. Use high-entropy guest tokens in Secure, HttpOnly, SameSite cookies; store only token hashes; scope every query by owner; require CSRF for cookie-authenticated writes; and use exact credentialed CORS origins. Planning and cart mutations use idempotency keys, fingerprints, optimistic locking, and persisted outcomes.

Scale replicas behind a load balancer with managed PostgreSQL and connection-pool budgets. Apply coarse request/body/rate limits at the ingress. Add Redis only when measured distributed coordination or hot-cache pressure requires it; add Kafka only when Modulith event delivery or workload isolation is insufficient. Demo failure controls are disabled by default and cannot be exposed accidentally in production configuration.

Provider output is untrusted. AI cannot create retailer facts or approve safety. Retailer calls will use host allowlists, timeouts, capability checks, and reconciliation; a demo cart never claims a real order.

## Alternatives considered

- Microservices immediately: rejected because it multiplies trust boundaries, deployment failure modes, and consistency work before module ownership or load requires independent scaling.
- Redis-backed sessions immediately: rejected because opaque token hashes and plans fit PostgreSQL and do not yet create measurable pressure.
- Client-side/local-only workflow state: rejected because it cannot enforce ownership, retry safety, or auditable deterministic rules.

## Consequences

PostgreSQL is a critical dependency and must have backups, tested restores, capacity monitoring, and bounded connection pools. Horizontal application scaling is straightforward, while scheduled work that can run on multiple replicas must use a database lease/lock before it becomes non-idempotent. Production still requires ingress, secret-manager, identity-provider, legal/privacy, and operational controls outside this repository.

## Revisit triggers

- Database session/catalog traffic causes measured latency or connection contention.
- A workflow needs durable high-throughput streaming or independent deployment.
- A module has a stable API, separate ownership, and a proven independent scaling/isolation requirement.
