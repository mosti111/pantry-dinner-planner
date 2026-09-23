# Operational runbooks

These are provider-neutral first-response procedures. Production owners must add contacts, paging routes, provider status links, RPO/RTO, regions and exact dashboard queries before launch.

## Common first response

1. Acknowledge and assign an incident lead; record UTC start time, release/image and affected capability.
2. Check liveness/readiness, HTTP error/latency, database pool/saturation, deployment changes and Pantry workflow/idempotency counters.
3. Protect users first: disable the affected optional provider or cart capability; keep the curated shopping-list journey available when safe.
4. Preserve correlation IDs and sanitized attempt/proposal records. Never paste guest tokens, credentials, dietary details or raw provider payloads into chat/tickets.
5. Communicate scope and workaround, then verify recovery with a new request and an idempotent replay before closing.

## AI degraded or unsafe

- Disable the AI feature flag and route to curated recipes.
- Stop automatic acceptance for the affected schema/model/prompt version; do not retry malformed or safety-conflicting proposals blindly.
- Compare validation failures and latency by version. Preserve response hashes/IDs, not sensitive prompt content.
- Re-enable only after the evaluation set and allergen/exclusion adversarial cases pass.

## Retailer catalog, price, stock or cart degraded

- Mark live capability unavailable and stop claiming freshness. Keep last-known values only when visibly labelled and policy permits.
- For uncertain cart creation, query/reconcile by the original idempotency key and external reference; never create a second cart as a guess.
- Apply recovery in order: same-ingredient SKU, approved ingredient substitute, affected-meal replacement, then clear failure.
- Recheck price/stock and budget before handoff after recovery.

## Database saturation or failed migration

- Stop rollout and scale application traffic down before increasing pools; verify database connection limits and lock waits.
- Do not rerun or edit an applied Flyway migration. Diagnose checksum/state and ship a forward repair migration.
- If data integrity is at risk, move to read-only/unavailable mode, preserve evidence and invoke the approved restore procedure.
- After recovery, verify ownership queries, idempotency records, plan transitions and Modulith publication state.

## Suspected guest/account data exposure

- Disable the affected endpoint/adapter and preserve access logs under restricted incident access.
- Rotate compromised keys/secrets, invalidate affected sessions, and prevent account claiming until ownership is verified.
- Determine identifiers, fields, time range and recipients; involve privacy/legal response owners according to local obligations.
- Add a regression test for the exact broken ownership path before restoration.

## Incorrect allergen metadata or unsafe recipe report

- Immediately suppress the ingredient/product/recipe version and affected active plans from automatic use.
- Do not replace unknown safety metadata with model inference. Require reviewed canonical data.
- Identify all plans/proposals/mappings using the version and follow the approved user notification policy.
- Re-enable only after editorial/safety review and deterministic regression tests pass.

## Secret compromise

- Revoke/rotate at the provider and secret manager, then roll workloads; do not merely update repository variables.
- Review integration calls and egress for misuse without exposing the secret in queries or tickets.
- Confirm old credentials fail, new credentials work through the intended host allowlist, and no frontend artifact contains them.

## Release rollback and restore

- Roll back to a compatible prior image digest; never reverse a schema migration destructively.
- If restore is required, isolate writes, restore into a separate database, validate checksums/row counts/ownership and rehearse cutover.
- Run the browser journey and API smoke cases, including retrying the same plan/cart idempotency keys, before reopening traffic.
