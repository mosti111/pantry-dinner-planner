# Provider onboarding contracts

Provider adapters are optional plugins to a complete local workflow. They may add capabilities; they may not weaken deterministic rules or change the API's ownership/security model.

## Retailer adapter

Implement `RetailerGateway` only after authorized API access exists. Record and certify:

- retailer/store/channel scope and terms permitting catalog/cart use;
- authentication, secret rotation, host allowlist, timeout and rate-limit policy;
- capability flags for catalog, live price, live stock, cart creation/mutation and checkout handoff;
- external SKU stability, package units, weighted-item semantics, currency/tax/fee treatment and snapshot freshness;
- idempotency behavior, external references, partial acceptance, retries, reconciliation and webhook signature verification;
- mappings to canonical ingredients with provenance/confidence/review status;
- sanitized integration-call metadata and provider-specific operational dashboards.

Acceptance requires contract tests for timeouts, malformed data, stale prices, unavailable/partial stock, key replay, uncertain cart results and recovery ordering. Never label a capability live unless its flag and tests agree.

## AI proposal adapter

Implement `MealProposalGateway`; do not expose a model client to planning code. The model returns only the versioned typed meal/ingredient proposal. It cannot return or select SKUs, retailer stock, package counts, prices, totals or cart outcomes.

Before enabling it:

- approve model, region, data retention, prompt logging, cost limits and fallback;
- pin schema/prompt/model identifiers and persist sanitized provenance in `ai_proposal`;
- validate JSON shape and bounds, then run every proposal through cooking-time, diet, allergen, exclusion, normalization, matching, package-math and budget rules;
- reject unknown allergen metadata and unresolved ingredients rather than guessing;
- evaluate representative Turkish/English aliases, adversarial prompt text, allergy conflicts, budget pressure, duplicates, timeouts and malformed output;
- keep the curated catalog path as a kill-switch/fallback.

## Identity provider

Implement `IdentityProviderGateway` with standard OIDC validation: issuer, audience, signature/JWKS rotation, expiry/not-before, nonce/state/PKCE and redirect allowlists. Only its `VerifiedIdentity` reaches `AccountClaimService`; never accept provider/subject fields directly from the browser.

The claim flow hashes the provider subject at rest and prevents a guest session from moving between accounts. Before exposing it, define session rotation, reauthentication, account deletion/export, merge conflicts, audit records and recovery policy.

## Capability launch rule

Every provider starts disabled. Enable progressively in a sandbox, then internal/canary cohorts, with a tested kill switch. Product copy and telemetry must distinguish reference, stale, estimated and live values. The deterministic baseline remains usable when any external provider is unavailable.
