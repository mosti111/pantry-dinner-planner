# ADR 0004: Reconstruct behavior from the deployed demo

- Status: accepted
- Date: 2026-09-21

## Context

The original demo source and local design files were lost. A deployed, functioning demonstration survives. It contains valuable product behavior but may include implementation shortcuts, fictional data, and assets whose provenance is not yet documented.

## Decision

Treat the deployed demo as a behavioral and visual reference. Record directly observed behavior separately from proposed production behavior. Rebuild the application from reviewed product, domain, API, and architecture specifications; do not attempt a line-for-line or asset-for-asset extraction.

Recreate the warm brand character with original or properly licensed assets. Preserve the key journey, explanations, and recovery behaviors. Improve accessibility, security, persistence, and failure handling as production requirements demand.

## Alternatives considered

- Rebuild from memory only: rejected because observable behavior remains available.
- Attempt to recover/minify/decompile deployed bundles into source: rejected as unreliable, difficult to maintain, and potentially unclear in asset provenance.
- Replace the product with a new generic UI: rejected because it discards validated interaction ideas.

## Consequences

The new codebase will be clean and intentional, but visual pixel parity is not guaranteed. The product specification and acceptance tests become the durable source of truth. Any recovered image/font/content asset must have known rights before inclusion in a public repository.
