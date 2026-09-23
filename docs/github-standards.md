# GitHub and engineering standards

## Repository layout

```text
pantry/
├── backend/                 Java 21 Spring Boot application
├── frontend/                Next.js TypeScript application
├── docs/                    product, architecture, runbooks, ADRs
├── infra/                   local/deployment definitions, no secrets
├── .github/                 workflows and templates
├── compose.yaml
├── README.md
├── CONTRIBUTING.md
├── SECURITY.md
└── LICENSE                  chosen before public release
```

A monorepo keeps contracts, documentation, demo data, and vertical changes together while the team is small.

## Branch and review policy

- Protected `main`; changes arrive through pull requests.
- Short-lived branches such as `feat/guest-session` or `fix/package-rounding`.
- Required CI, resolved review comments, and at least one review for public/team development.
- No force push to protected branches and no committed secrets.
- Squash merge by default so each PR is one coherent change; preserve separate commits only when they are independently valuable.

## Commit and release policy

Use Conventional Commits: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `build:`, `ci:`, `chore:`. Breaking changes are explicit. Releases use Semantic Versioning and generated changelogs, with human-edited release notes for user-visible changes.

## Pull-request checklist

- User outcome and scope are clear.
- Domain invariants and module boundaries remain intact.
- APIs/migrations are backward-compatible or migration is documented.
- Success, edge, concurrency, and failure cases are tested.
- No sensitive data is logged or committed.
- Accessibility and error/loading states are addressed.
- Observability and runbooks are updated where needed.
- ADR created when a durable architectural choice changes.
- Screenshots or recordings included for material UI changes.

## Clean-code rules

- Names express domain intent; avoid `Manager`, `Helper`, `Util`, and generic `Service` when a precise role exists.
- Small cohesive methods/classes, but do not fragment logic into indirection without a reason.
- Prefer immutable value objects and explicit state-transition methods.
- Use records for immutable boundary DTOs where appropriate.
- Constructor injection only; no service locator or field injection.
- Domain exceptions are meaningful; do not use exceptions for normal branch logic.
- No boolean parameters that obscure call meaning; use named commands/value types.
- Comments explain why, constraints, or external quirks—not what readable code already says.
- Delete dead code; never keep commented-out implementations.
- Map at module/adapter boundaries; never leak persistence or provider models.
- Optimize after measurement while keeping safety and correctness non-negotiable.

## GitHub artifacts before publication

- Public-safe README with architecture and screenshots.
- `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`, support policy.
- Issue forms for bugs/features and PR template.
- Dependabot/Renovate configuration and CodeQL.
- CI with least-privilege permissions, pinned action SHAs, concurrency cancellation, and artifact retention.
- Release workflow and container provenance/SBOM.
- Demo environment, fictional seed data, and documented test accounts without shared secrets.
- License chosen deliberately; do not publish under an accidental default.

## Documentation governance

- Product behavior belongs in the product specification.
- Durable architecture decisions belong in ADRs.
- Current system structure belongs in architecture/data/API docs.
- Operational procedures belong in runbooks.
- Every document carries status/date where staleness matters.
- CI checks links, formatting, generated API drift, and Mermaid syntax where practical.
