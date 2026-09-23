# Architecture decision records

ADRs are append-only records of durable choices. A superseded decision remains in history and links to its replacement.

| ADR | Decision | Status |
|---|---|---|
| [0001](0001-modular-monolith.md) | Begin as a Spring Modulith modular monolith | Accepted |
| [0002](0002-ai-is-a-proposal-source.md) | Treat AI as an untrusted proposal source | Accepted |
| [0003](0003-retailer-adapter-boundary.md) | Isolate retailer integrations behind capability-aware adapters | Accepted |
| [0004](0004-reconstruct-from-deployed-demo.md) | Reconstruct behavior from the deployed demo | Accepted |
| [0005](0005-secure-stateless-scale-baseline.md) | Secure stateless scaling with PostgreSQL-first coordination | Accepted |

New ADRs use `NNNN-short-title.md` and include status, context, decision, alternatives, consequences, and follow-up triggers.
