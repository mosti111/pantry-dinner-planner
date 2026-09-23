# Changelog

All notable changes follow [Keep a Changelog](https://keepachangelog.com/) and Semantic Versioning.

## [Unreleased]

### Added

- Reconstructed responsive Pantry planning, dinner, recipe, basket, and demo-cart experience.
- Java 21 Spring Boot modular backend with guest ownership, PostgreSQL/Flyway, deterministic planning and pricing, recovery, and retry-safe cart attempts.
- Next.js TypeScript frontend with secure API client, guest resume, alternatives, replacement, and failure-recovery lab.
- Docker Compose, GitHub CI/CodeQL/Dependabot, architecture decisions, threat model, API/data/domain specifications, and contributor templates.
- Typed recipe, retailer, AI, and verified-identity ports with deterministic provider-free adapters and a normalized provider-onboarding schema.
- Reviewed OpenAPI 3.1 contract, plan-transition and cart-reconciliation records, Prometheus/OpenTelemetry scaffolding, and account-claim seam.
- Playwright critical browser journey, real-PostgreSQL application integration test, provider onboarding guide, deployment guide, and incident runbooks.

### Security

- Hash-only guest tokens, HttpOnly/SameSite cookies, CSRF, exact CORS, deny-by-default routing, safe production defaults, ownership-scoped queries, idempotency fingerprints, correlation IDs, and dependency-audit remediation.
- Frontend content security policy, hashed external identity subjects, cart-request fingerprints, and public-chain-denied operational endpoints.
