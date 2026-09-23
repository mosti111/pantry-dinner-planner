# Data model

This is the logical relational baseline. Physical columns, indexes, and constraints will be finalized with each Flyway migration. Tables are module-owned; foreign-key references across modules use IDs without cross-module ORM navigation.

## Identity and household

- `guest_session`: token hash, status, created/last-seen/expires timestamps, claimed account.
- `account`: external auth subject/provider, status, timestamps.
- `household`: owner type/id, locale, currency, store context, member count, version.
- `household_member`: household, display label, constraint metadata.
- `household_dietary_rule`: household/member, controlled rule code, severity.
- `household_allergy`: household/member, allergen code, severity, cross-contact policy.
- `planning_preferences`: household, priority, meal count, max duration, budget amount/currency, leftover preference.
- `ingredient_exclusion`: household, ingredient ID, reason.

An ownership check constraint permits exactly one active owner form. Guest tokens are random high-entropy values; only hashes are persisted.

## Ingredient and recipe

- `ingredient`: canonical key/name, category, dimension, dietary/allergen metadata, status.
- `ingredient_alias`: ingredient, locale, normalized phrase, confidence, provenance, review status.
- `unit`: symbol, dimension, base-unit factor, precision policy.
- `recipe`: stable identity, editorial status, provenance.
- `recipe_version`: recipe, version number, localized titles/descriptions, servings, preparation/cook duration, instructions, safety metadata, immutable timestamp.
- `recipe_ingredient`: recipe version, ingredient, decimal quantity, unit, optional flag, preparation note, substitution policy.
- `recipe_step`: recipe version, sequence, instruction.

Unique indexes protect canonical keys and recipe/version pairs. Alias search uses normalized text plus locale and a GIN/GiST trigram index.

## Catalog

- `retailer`: key, display name, status, declared capabilities.
- `store_context`: retailer, external location/channel key, region metadata.
- `product`: retailer, external SKU, title, brand, category, package quantity/unit, weighted flag, status.
- `product_ingredient_mapping`: product, canonical ingredient, confidence, provenance, approval state.
- `offer_snapshot`: product, store context, price/currency, promotion, stock state, observed/expires timestamps, source revision.

The current offer is a query/projection; history remains append-only where useful. Uniqueness is retailer plus external SKU.

## Planning and basket

- `meal_plan`: owner, household, request snapshot JSON, state, proposal provenance, budget, current total, version, timestamps.
- `planned_meal`: plan, slot, recipe version, servings, selection reason.
- `plan_validation`: plan/version, rule code, severity, outcome, details.
- `plan_transition`: plan, from/to state, reason, actor, timestamp.
- `basket`: plan, store context, state, amount/currency, offer verification timestamp, version.
- `ingredient_demand`: basket, ingredient, required/base unit, source-meal references.
- `pantry_declaration`: basket, ingredient, available quantity/unit, confirmation timestamp.
- `basket_line`: basket, demand, product, offer snapshot, package count, required/supplied/leftover quantity, unit price, line total, selected flag.
- `match_decision`: demand, candidate product, score components, decision/rejection reason, model/rule version.

Basket lines retain a commercial snapshot so history remains understandable after catalog changes.

## Integration and AI

- `cart_attempt`: idempotency key, basket/version, payload hash, adapter, state, external reference/link, retry metadata.
- `cart_line_result`: attempt, basket line, requested/accepted quantity, status, failure code.
- `integration_call`: provider, operation, correlation ID, sanitized request/response metadata, outcome, duration.
- `ai_proposal`: plan, schema/prompt versions, provider/model, response ID/hash, structured payload, parse status, usage, latency.
- `event_publication`: managed by Spring Modulith for durable completion tracking.

## Required database constraints

- Nonnegative money and quantity checks.
- Positive package count, package size, servings, and requested dinner count.
- ISO 4217 currency code and consistent currency within calculations.
- Unique idempotency key plus request fingerprint semantics.
- Unique slot per plan and sequence per recipe version.
- State values restricted through enums/check constraints.
- Foreign keys inside module-owned data; intentional ID references documented across modules.
- Optimistic-lock version on mutable aggregates.

## Migration rules

- Flyway migrations are immutable after merge.
- Prefer expand/migrate/contract for breaking changes.
- Production migrations are backward-compatible with the previous application version during rolling deployment.
- Seed/demo data is separate from structural migrations.
- Every migration is tested on an empty database and an upgrade fixture in CI.
