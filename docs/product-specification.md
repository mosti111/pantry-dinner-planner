# Product and reconstruction specification

Status: baseline derived from the deployed demo and project decisions on 2026-09-21.

## Evidence policy

Because the original local source was lost, this document separates:

- **Observed**: behavior directly visible in the deployed demo.
- **Committed**: behavior explicitly chosen for the production product.
- **Proposed**: implementation detail that may change through an ADR.

Screens, copy, recipes, and fictional catalog content may be recreated rather than copied exactly. The important assets to preserve are the flow, product intent, state transitions, and recognizable visual personality.

## Observed demo

### Household step

- Household-size stepper, defaulting to four people.
- Dinner-count stepper, defaulting to three dinners.
- Whole-plan grocery budget in Turkish lira, including full packages and pantry basics while excluding delivery fees.
- Preference priority: balanced, lower cost, quicker cooking, or protein-rich ingredients.
- Maximum cooking time options.
- Dietary selections: vegan, vegetarian, pescatarian, gluten-free.
- Fourteen named allergy selections.
- A large canonical ingredient exclusion list.
- Explicit confirmation that food requirements were reviewed before planning.
- Guest-first copy: no account and no required pantry inventory.

### Dinner plan step

- Progress navigation: household, dinners, basket.
- Several dinners scaled to the household.
- Each meal shows description, localized title, dietary label, time, servings, and a requirements-fit indicator.
- Users can inspect or replace each dinner.
- The plan shows complete-basket cost, budget remaining, and consolidated grocery-line count.
- A downloadable plan record exists.

### Recipe detail

- Ingredient quantities scaled for the selected household.
- Preparation/cooking time, equipment, and ordered method steps.
- Storage/leftover guidance and explicit nutrition limitations.
- Explanation of why the meal was selected, including constraint fit and ingredient reuse.
- Safety disclaimer to check real product labels.

### Basket step

- Recipe demand is consolidated across all dinners.
- Products show brand, package quantity, required quantity, leftover amount, offer/price, and weighted-price estimates.
- Staples likely to be at home are separated visually.
- Unchecking an item means the household already has enough; meals do not change.
- Users can select alternative products and preview the changed total.
- Summary distinguishes complete basket, already at home, and amount to buy.
- Basket refresh and demo-cart creation are available.

### Recovery demonstration

Observed scenarios include one SKU unavailable, all SKUs for an ingredient unavailable, a selected-SKU price increase, expired promotions, retailer unavailable, and cart-transfer failure. Integration levels range from a demo cart through simulated partnership capabilities. The stated recovery order is same-ingredient SKU, reviewed ingredient alternative, then affected-dinner replacement.

### Demo boundaries

- Products and prices are fictional.
- No real order or payment is placed.
- Guest-session controls affect only the current guest session.

## Production scope

### MVP

- Guest session with durable, expiring server-side state.
- Household requirements and consent/review checkpoint.
- Curated recipe catalog plus optional AI proposal generation.
- Deterministic safety and time validation.
- Ingredient normalization and retailer-product matching.
- Full-package pricing, budget enforcement, and leftover visibility.
- Already-have review and recalculation.
- Fake/reference retailer adapter with controlled failure scenarios.
- Downloadable plan record.
- Responsive accessible frontend preserving the demo's warm, playful identity.

### Beta

- Optional account creation and guest-session claiming.
- Saved households, plans, preferences, and reusable pantry declarations.
- At least one permitted live catalog/price integration or honest shopping-list handoff.
- Background catalog refresh and incomplete-cart reconciliation.
- Audit trail, rate limiting, operational dashboards, and support tooling.

### Later

- Multiple retailer comparison.
- Meal calendars, favorites, ratings, and feedback-driven ranking.
- Nutrition calculations based on authoritative data.
- Mobile client using the same API.
- Authorized retailer cart mutation and checkout handoff.

### Explicitly out of scope initially

- Payment processing.
- Medical claims or personalized medical nutrition advice.
- Scraping or automating retailer checkout without permission.
- Microservices, Kafka, Elasticsearch, or Redis without measured need.
- A fully automatic pantry inventory requirement.

## Primary journeys

1. Guest defines household requirements, confirms them, receives a valid plan, reviews the basket, marks owned items, and receives a demo/reference checkout handoff.
2. Guest replaces a dinner and sees demand, packages, price, and budget recalculate.
3. Guest changes a product and sees the total and leftover impact before applying.
4. A product becomes unavailable during finalization; Pantry recovers in the approved order and explains the change.
5. A returning guest resumes an unexpired plan.
6. A guest creates an account later and claims existing plans without duplication.

## Non-functional requirements

- Safety checks fail closed on unknown ingredients or allergen data.
- Money uses decimal arithmetic and explicit currency.
- Public mutations are idempotent where retries are realistic.
- A stale plan or basket cannot overwrite a newer version.
- Finalization always rechecks price and stock.
- Core planning remains usable when the AI provider is unavailable by using curated recipes.
- API errors use stable machine-readable codes and correlation IDs.
- Accessibility target: WCAG 2.2 AA.
- Initial performance targets: p95 normal API reads under 300 ms; planning request accepted under 500 ms and completed asynchronously; no external call inside a database transaction.
- Guest data has a documented retention period and deletion mechanism.

## Success measures

- Plan completion rate.
- Constraint-validation failure and manual-review rates.
- Percentage of ingredients matched automatically at acceptable confidence.
- Basket within-budget rate and average budget variance.
- Stock/price recovery success rate.
- Time from request to reviewable plan.
- Percentage of users who complete already-have review.
- Unsafe-plan escapes: target zero.

## Open product decisions

- Launch geography, currency, and first authorized retailer.
- Exact guest-session retention period.
- Whether recipe content is editorial, licensed, generated, or mixed.
- Legal review for allergy messaging and retailer integrations.
- Account identity provider.
- Repository license and whether the finished project is public-source or source-available.
