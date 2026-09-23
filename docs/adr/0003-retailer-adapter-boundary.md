# ADR 0003: Isolate retailer integrations behind capability-aware adapters

- Status: accepted
- Date: 2026-09-21

## Context

Retailers differ in catalog access, location scoping, stock fidelity, authentication, cart operations, rate limits, and partnership requirements. Some may support only a product link or checkout handoff.

## Decision

Define a retailer gateway interface around explicit capabilities: search/catalog, offer refresh, stock verification, cart creation, cart mutation, and checkout handoff. Each adapter translates external data into Pantry DTOs and persists idempotent attempt/reconciliation state. Core domain modules do not import retailer SDKs or external payload classes.

Where APIs are unavailable, use a fake adapter and label the experience honestly. Do not scrape or automate checkout without authorization and a legal/product review.

## Consequences

Retailers can be added independently and degraded capabilities are visible to orchestration logic. Integration failures are recoverable without contaminating core product models.
