# Delivery roadmap

This is an outcome roadmap, not a promise of calendar dates. Each milestone ends in a demonstrable vertical slice and a tagged release candidate.

## Current checkpoint

Milestones 0–4 and the provider-independent parts of 5–7 are implemented: clean provider ports/fakes, normalized schema, account-claim seam, reviewed OpenAPI, metrics/tracing scaffolding, Playwright browser coverage, deployment/runbook documentation, and persisted cart reconciliation. Live provider and production rollout items require owner/provider/legal inputs documented in [implementation-status.md](implementation-status.md).

## Milestone 0 — Repository foundation

- Create Maven backend and Next.js frontend workspaces.
- Pin Java/Node/toolchain versions; add formatting, linting, commit hooks, Docker Compose, and environment examples.
- Configure GitHub Actions, Dependabot/Renovate, code scanning, issue/PR templates, ownership, and branch protection guidance.
- Add Spring Modulith/ArchUnit boundary tests and initial ADR checks.
- Add local PostgreSQL with `pg_trgm`, Flyway, health endpoints, and structured error model.

Exit: clean checkout builds and tests on Windows/Linux CI with no secrets.

## Milestone 1 — Guest household and planning shell

- Guest session lifecycle and ownership.
- Household requirements, controlled dietary/allergen vocabularies, and confirmation checkpoint.
- Rebuilt responsive household screen.
- Create/status/cancel plan API with persisted state machine and fake planner.

Exit: guest submits requirements, refreshes the browser, and resumes the same planning workflow.

## Milestone 2 — Ingredients and curated recipes

- Canonical ingredients, aliases, units, conversions, allergens, and dietary metadata.
- Immutable recipe versions, scaling, method steps, and editorial provenance.
- Deterministic validation and curated recipe selector.
- Dinner-plan and recipe-detail UI.

Exit: plan generation works with AI disabled and rejects unsafe/ambiguous content.

## Milestone 3 — Catalog matching and basket math

- Reference retailer/catalog and store-scoped offer snapshots.
- Exact alias plus `pg_trgm` candidate matching with explainable ranking.
- Demand aggregation, package math, weighted products, leftovers, and budget validation.
- Basket review and already-have interactions.

Exit: every displayed total is reproducible from persisted inputs and PostgreSQL data.

## Milestone 4 — Recovery and idempotent cart handoff

- Same-ingredient product alternatives, approved ingredient substitutions, and meal replacement.
- Price/stock refresh before finalization.
- Capability-aware fake retailer adapter, idempotent attempts, reconciliation, and failure scenarios.
- Complete demo-equivalent flow with recovery explanations.

Exit: automated tests cover all six observed failure scenarios and repeated finalization cannot duplicate a cart.

## Milestone 5 — AI-assisted proposals

- Spring AI adapter with versioned structured schema and stored provenance.
- Prompt/response evaluation dataset and guardrails.
- Bounded proposal repair; deterministic pipeline remains authoritative.
- Provider outage fallback to curated recipes.

Exit: changing or disabling the model cannot bypass safety or alter retailer truth.

## Milestone 6 — Accounts and production hardening

- Identity provider integration and idempotent guest claiming.
- Saved plans/preferences with privacy controls.
- Rate limits, observability, runbooks, backup/restore rehearsal, accessibility review, load tests, and threat-model closure.
- Authorized live retailer integration or clearly limited shopping-list handoff.

Exit: production readiness checklist passes and legal/product capability labels match reality.

## Milestone 7 — Public portfolio release

- Architecture diagrams, demo video/screenshots, seeded local demo, and one-command developer setup.
- Sanitized sample data and provider fakes so contributors need no paid keys.
- Final license, code of conduct, security policy, contribution guide, changelog, and tagged `v1.0.0`.
- Public deployment with monitoring and cost limits.

Exit: a reviewer can understand the problem, run the system, trigger recovery scenarios, inspect tests/architecture, and evaluate engineering decisions.

## Work breakdown rule

Issues are thin vertical slices. Each issue states user outcome, domain rules, API/UI changes, failure cases, observability, test evidence, docs, and acceptance criteria. Avoid horizontal issues such as “build all repositories” that produce no demonstrable behavior.

## Provider/onboarding backlog

1. Choose the production identity provider and connect its verified identity to the implemented guest-claim seam.
2. Obtain an authorized retailer sandbox and implement/certify its capability-aware adapter.
3. Choose the AI provider/retention policy, implement the typed adapter, and build the production evaluation dataset.
4. Provision the target environment and add deployment-environment smoke/load/restore tests.
5. Complete privacy/legal/food-safety review, operational ownership, and public-repository license decisions.
