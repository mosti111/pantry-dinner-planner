# Frontend reconstruction record

## Goal

Reconstruct the lost frontend from the user's surviving deployed demo while keeping the new production backend authoritative. The deployed demo is the visual and behavioral source of truth; the recovered implementation is maintainable Next.js/TypeScript code and does not depend on the hosted site at runtime.

## Reconstruction status

Completed and verified at desktop and mobile breakpoints. The local application now uses the recovered Pantry typography, colors, kitchen artwork, character illustrations, meal imagery, paper surfaces, responsive layouts, copy hierarchy, and interaction patterns. The setup, dinner-plan, recipe, basket, product-alternative, completion, demo-scenario, and data-control views are connected to the Spring Boot API rather than a static mock.

## Preserved experience

- Warm, playful food identity with editorial headings and friendly microcopy.
- Three-step journey: household, dinners, basket.
- Guest-first flow with no forced account wall.
- Visible budget and full-package explanation.
- Requirement-review checkpoint before planning.
- Meal cards with localized title, time, servings, and rationale.
- Recipe view with ingredients, method, equipment, storage, leftovers, and limitations.
- Basket grouped into meal essentials and likely pantry staples.
- Package math and leftovers shown in human language.
- Product and dinner replacement flows with total impact.
- Clear simulated/live capability labels.

## Implemented route map

```text
/
/demo
/privacy
```

The main route keeps the three-step workflow in one accessible client surface so the recovered experience stays faithful to the demo. Plan and basket state is server-authoritative and belongs to the secure guest session. Reloading resumes the latest plan. Dialogs and sheets are presentation state only.

## Component structure

- `PantryApp`, `SiteHeader`, `Journey`, and `SiteFooter` own the restored shell and workflow navigation.
- `Setup`, `KitchenScene`, `Counter`, and `ChoiceGroup` own household and food-requirement input.
- `Dinners`, `RecipeDialog`, and the plan summary render validated server decisions.
- `Basket`, `BasketGroup`, `BasketLineRow`, and `ProductSheet` provide already-have review and product replacement.
- `Complete`, `DemoScenarioPage`, and `PrivacyPage` cover cart completion, recovery demonstrations, and guest-data deletion.

Keep business calculations out of components. The frontend formats server decisions and may calculate non-authoritative previews only when clearly identified.

## State and data

- A small typed API client talks to the versioned Spring Boot API with credentials and CSRF protection.
- The server owns plans, constraints, package math, matching, pricing, stock/recovery decisions, and lifecycle transitions.
- React owns transient form, dialog, tab, and current-step state; there is no global client store.
- The last plan identifier is stored locally only as a resume hint. The backend still checks guest-session ownership.
- Polling, SSE, and a query library remain unnecessary until a genuinely asynchronous provider workflow is introduced.

## Design recovery evidence

1. The surviving deployed demo was inspected screen by screen, including setup, dinner plan, recipe dialog, basket, completion, demo scenarios, and privacy/data controls.
2. Its stylesheet, fonts, and visual assets were recovered from the user's own deployment and stored locally so production does not depend on that deployment remaining online.
3. The markup was rebuilt in React around the recovered class system while preserving semantic headings, labels, dialogs, tabs, and buttons.
4. The complete browser journey was exercised against the real local API, including unchecking an already-owned product and creating an idempotent demo cart.
5. Desktop and narrow mobile layouts were visually inspected. Automated lint, type, component/API tests, production build, and Playwright E2E all pass.

## Accessibility and quality

- Semantic headings, forms, fieldsets, error summaries, and live regions for workflow progress.
- Full keyboard path for steppers, accordions, dialogs, tabs, alternatives, and basket checkboxes.
- Focus returns predictably after dialogs and route transitions.
- Color is never the sole status signal; minimum touch targets and reduced-motion support.
- Locale-aware TRY formatting and Turkish characters; architecture supports future locales.
- Axe checks in component and end-to-end tests, plus manual keyboard/screen-reader review.

## Failure UX

Users see whether Pantry is generating, validating, matching, repricing, recovering, or waiting for review. Recovery messages say what changed, why, the price impact, and whether user approval is required. Retailer unavailability preserves the plan and offers a shopping-list fallback when supported.
