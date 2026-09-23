import { expect, test, type Page, type Route } from "@playwright/test";
import type { Plan } from "../src/lib/types";

const planId = "11111111-1111-4111-8111-111111111111";
const lineOne = "22222222-2222-4222-8222-222222222222";
const lineTwo = "33333333-3333-4333-8333-333333333333";

test("plans dinners, reviews already-owned items, and creates a recoverable cart", async ({ page }) => {
  let plan = fixturePlan();
  await mockApi(page, request => {
    const url = new URL(request.request().url());
    if (url.pathname.endsWith(`/basket/lines/${lineTwo}`) && request.request().method() === "PUT") {
      const body = request.request().postDataJSON() as { selected: boolean };
      plan = withLineSelection(plan, lineTwo, body.selected);
    }
    if (url.pathname.endsWith("/basket/finalize")) {
      plan = { ...plan, status: "CART_READY", basket: { ...plan.basket, status: "CREATED", cartReference: "DEMO-E2E" } };
    }
    return plan;
  });

  const response = await page.goto("/");
  expect(response?.headers()["content-security-policy"]).toContain("frame-ancestors 'none'");
  await expect(page.getByRole("heading", { name: "A little prep." })).toBeVisible();
  await page.getByText("I’ve reviewed my food requirements.").click();
  await page.getByRole("button", { name: "Plan my dinners" }).click();

  await expect(page.getByRole("heading", { name: "Good food ahead." })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Lentil skillet" })).toBeVisible();
  await page.getByRole("button", { name: "Review my basket" }).click();

  await expect(page.getByRole("heading", { name: "A little list. A lot of dinner." })).toBeVisible();
  await page.getByRole("checkbox", { name: "Buy Pantry salt" }).click();
  await expect(page.getByRole("checkbox", { name: "Buy Pantry salt" })).not.toBeChecked();
  await expect(page.getByText("1 of 2 products selected")).toBeVisible();
  await page.getByRole("button", { name: "Create demo cart" }).click();

  await expect(page.getByRole("heading", { name: "All set for 3 dinners." })).toBeVisible();
  await expect(page.getByText("Cart reference: DEMO-E2E")).toBeVisible();
});

async function mockApi(page: Page, currentPlan: (route: Route) => Plan) {
  await page.route("http://localhost:8080/api/v1/**", async route => {
    const url = new URL(route.request().url());
    const headers = {
      "access-control-allow-origin": "http://127.0.0.1:3000",
      "access-control-allow-credentials": "true",
      "access-control-allow-headers": "Content-Type, Idempotency-Key, X-Pantry-Request, X-XSRF-TOKEN",
      "access-control-allow-methods": "GET, POST, PUT, DELETE, OPTIONS",
    };
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers });
    if (url.pathname.endsWith("/csrf")) {
      return route.fulfill({ json: { token: "e2e-csrf", headerName: "X-XSRF-TOKEN" }, headers });
    }
    if (url.pathname.endsWith("/guest-sessions/current")) {
      return route.fulfill({ json: { id: "44444444-4444-4444-8444-444444444444", expiresAt: "2026-09-29T00:00:00Z" }, headers });
    }
    return route.fulfill({ json: currentPlan(route), headers });
  });
}

function withLineSelection(plan: Plan, id: string, selected: boolean): Plan {
  const lines = plan.basket.lines.map(line => line.id === id ? { ...line, selected } : line);
  const selectedLines = lines.filter(line => line.selected);
  const toBuy = selectedLines.reduce((total, line) => total + Number(line.lineTotal.amount), 0);
  return {
    ...plan,
    basket: {
      ...plan.basket,
      lines,
      selectedCount: selectedLines.length,
      toBuy: { amount: toBuy.toFixed(2), currency: "TRY" },
      alreadyAtHome: { amount: (105 - toBuy).toFixed(2), currency: "TRY" },
    },
  };
}

function fixturePlan(): Plan {
  const meal = {
    slot: "DINNER_1", title: "Lentil skillet", localizedTitle: "Mercimek tava",
    description: "A weeknight dinner.", minutes: 30, servings: 4, dietaryLabel: "Plant-based",
    ingredients: [{ key: "green-lentils", name: "Green lentils", quantity: { value: "400", unit: "GRAM" as const } }],
    method: ["Cook the lentils."], reasons: ["Within your budget"],
  };
  return {
    id: planId, status: "REVIEW_READY", householdSize: 4, dinnerCount: 3,
    budget: { amount: "1500.00", currency: "TRY" }, total: { amount: "105.00", currency: "TRY" },
    remaining: { amount: "1395.00", currency: "TRY" }, meals: [meal, { ...meal, slot: "DINNER_2", title: "Bean bowls" }, { ...meal, slot: "DINNER_3", title: "Tomato rice" }],
    basket: {
      id: "55555555-5555-4555-8555-555555555555", status: "DRAFT", completeTotal: { amount: "105.00", currency: "TRY" },
      alreadyAtHome: { amount: "0.00", currency: "TRY" }, toBuy: { amount: "105.00", currency: "TRY" }, selectedCount: 2,
      lines: [
        {
          id: lineOne, ingredientKey: "green-lentils", ingredientName: "Green lentils", sku: "LENTILS-500", brand: "Anadolu Table",
          productName: "Green lentils 500 g", required: { value: "400", unit: "GRAM" }, packageSize: { value: "500", unit: "GRAM" },
          packageCount: 1, leftover: { value: "100", unit: "GRAM" }, lineTotal: { amount: "95.00", currency: "TRY" }, selected: true,
          pantryStaple: false, estimatedWeight: false, availability: "AVAILABLE", alternatives: [],
        },
        {
          id: lineTwo, ingredientKey: "salt", ingredientName: "Salt", sku: "SALT-50", brand: "Pantry",
          productName: "Pantry salt", required: { value: "4", unit: "GRAM" }, packageSize: { value: "50", unit: "GRAM" },
          packageCount: 1, leftover: { value: "46", unit: "GRAM" }, lineTotal: { amount: "10.00", currency: "TRY" }, selected: true,
          pantryStaple: true, estimatedWeight: false, availability: "AVAILABLE", alternatives: [],
        },
      ],
    },
    notices: [], updatedAt: "2026-09-22T09:00:00Z", version: 1,
  };
}
