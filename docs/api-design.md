# API design

Base path: `/api/v1`. The reviewed OpenAPI 3.1 contract is `backend/src/main/resources/openapi/pantry-v1.yaml` and a test prevents implemented routes/protocol guards from disappearing silently. Internal IDs are UUIDs serialized as opaque strings.

## Implemented reference contract

The portfolio MVP currently exposes this synchronous, persisted contract. Every resource lookup is scoped through the guest cookie.

| Method | Path | Purpose |
|---|---|---|
| GET | `/csrf` | Issue/read the browser CSRF token |
| POST | `/guest-sessions` | Create an opaque guest session |
| GET | `/guest-sessions/current` | Validate/resume the current session |
| DELETE | `/guest-sessions/current` | Delete the guest and cascaded plan/cart data |
| POST | `/meal-plans` | Create or replay a plan; requires `Idempotency-Key` |
| GET | `/meal-plans/{planId}` | Read an owned plan |
| PUT | `/meal-plans/{planId}/basket/lines/{lineId}` | Mark a line needed/already at home |
| PUT | `/meal-plans/{planId}/basket/lines/{lineId}/product` | Choose an approved same-ingredient SKU |
| POST | `/meal-plans/{planId}/meals/{slot}/replace` | Replace a meal and recalculate its basket |
| POST | `/meal-plans/{planId}/basket/refresh` | Revalidate the reference offer snapshot |
| POST | `/meal-plans/{planId}/basket/finalize` | Idempotently create/replay a demo cart result |
| POST | `/meal-plans/{planId}/basket/recovery-demo` | Run an allow-listed failure scenario; available only when demo mode is enabled |

The remainder of this document defines the target contract as provider integrations, asynchronous work, accounts, and richer household/catalog persistence are introduced. Target-only routes are not present in the reviewed implemented contract.

## Conventions

- JSON over HTTPS; UTF-8.
- `Idempotency-Key` required for plan generation, basket finalization, and cart attempts.
- `ETag`/`If-Match` or explicit version fields protect mutable resources.
- RFC 9457 Problem Details for errors.
- Cursor pagination for potentially growing collections.
- Dates/times use ISO 8601; money is `{ "amount": "413.99", "currency": "TRY" }`.
- Quantities are `{ "value": "800", "unit": "g" }`.
- No JPA entity is serialized directly.

## Guest and account endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/guest-sessions` | Create an opaque guest session |
| GET | `/guest-sessions/current` | Resume current session summary |
| DELETE | `/guest-sessions/current` | Delete guest data |
| POST | `/guest-sessions/current/claim` | Attach session to authenticated account idempotently |

The browser receives a secure, HttpOnly, SameSite cookie. The raw token is not exposed through normal API responses.

## Household endpoints

| Method | Path | Purpose |
|---|---|---|
| PUT | `/households/{id}` | Create/update household requirements with version check |
| GET | `/households/{id}` | Read household and preferences |
| GET | `/ingredients?query=&locale=` | Search canonical ingredients for exclusion selection |
| GET | `/dietary-rules` | Supported controlled rules |
| GET | `/allergens` | Supported allergen vocabulary |

## Planning endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/meal-plans` | Accept planning request and return `202 Accepted` |
| GET | `/meal-plans/{id}` | Read state, validation, meals, and totals |
| GET | `/meal-plans/{id}/events` | Optional SSE progress stream; polling remains supported |
| POST | `/meal-plans/{id}/meals/{slot}/replacement-options` | Calculate valid replacements |
| PUT | `/meal-plans/{id}/meals/{slot}` | Apply selected replacement |
| POST | `/meal-plans/{id}/retry` | Resume a retryable failed workflow |
| DELETE | `/meal-plans/{id}` | Cancel/delete within ownership rules |
| GET | `/meal-plans/{id}/record` | Download human/machine-readable plan record |

The create response includes plan ID, current state, polling URL, and retry guidance. Planning does not hold one HTTP request open while providers run.

## Basket endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/meal-plans/{id}/basket` | Read consolidated basket |
| PUT | `/baskets/{id}/pantry-declarations` | Confirm already-have quantities/selections |
| POST | `/baskets/{id}/lines/{lineId}/alternatives` | Rank eligible alternative products |
| PUT | `/baskets/{id}/lines/{lineId}/selection` | Apply selected product |
| POST | `/baskets/{id}/refresh` | Refresh offers/stock and recalculate |
| POST | `/baskets/{id}/finalize` | Verify and start retailer handoff |
| GET | `/cart-attempts/{id}` | Read/reconcile handoff result |

Alternative responses include score explanations, package/leftover impact, price delta, freshness, and safety eligibility.

## Operations endpoints

- `/actuator/health/liveness` and `/actuator/health/readiness` expose minimal status.
- Metrics require protected operational access.
- Demo failure controls exist only in a demo profile/admin-protected test environment, never as anonymous production endpoints.

## Contract examples

Planning request:

```json
{
  "dinnerCount": 3,
  "householdSize": 4,
  "budget": { "amount": "1500.00", "currency": "TRY" },
  "maxCookingMinutes": 45,
  "priority": "BALANCED",
  "dietaryRules": ["VEGETARIAN"],
  "allergens": ["PEANUT"],
  "excludedIngredients": ["mushrooms"],
  "requirementsReviewed": true
}
```

The server reconstructs and validates authoritative household constraints; clients cannot bypass safety by omitting saved rules.
