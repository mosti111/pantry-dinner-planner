# Contributing

Read [docs/github-standards.md](docs/github-standards.md) and the relevant domain/API documents before changing behavior.

## Local development

1. Copy `.env.example` to `.env` and use local-only credentials.
2. Start PostgreSQL with `docker compose up postgres -d`.
3. Run `backend/mvnw verify` from `backend`.
4. Run `pnpm install` and `pnpm dev` from `frontend`.

Alternatively, run the full stack with `docker compose up --build`.

## Pull requests

Keep changes small and vertical. Include the behavior, tests, documentation, migrations, accessibility states, and operational effects in one coherent pull request. Use Conventional Commits and never commit secrets or real customer/retailer data.
