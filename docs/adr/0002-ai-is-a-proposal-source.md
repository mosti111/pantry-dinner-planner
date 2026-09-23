# ADR 0002: Treat AI as an untrusted proposal source

- Status: accepted
- Date: 2026-09-21

## Context

Generative models can produce useful recipe ideas but cannot be the source of truth for retailer facts, safety, money, or stock. Pantry must be reproducible, explainable, and safe under malformed or hallucinated output.

## Decision

Spring AI requests versioned structured output mapped to typed Java DTOs. All output passes schema validation, canonical ingredient resolution, dietary/allergen enforcement, catalog matching, quantity/package math, fresh pricing/stock checks, and budget validation before a plan can be reviewed.

AI cannot name an accepted SKU or set a price, availability, package count, basket total, or successful lifecycle state.

## Consequences

Plans can be repaired or rejected deterministically and provider models can be changed without moving business rules into prompts. The backend needs curated ingredient and allergen data plus explicit handling for unresolved inputs.
