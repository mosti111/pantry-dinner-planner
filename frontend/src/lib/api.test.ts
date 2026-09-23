import { beforeEach, describe, expect, it, vi } from "vitest";

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

describe("API security and retry contracts", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.restoreAllMocks();
  });

  it("creates a guest only when the current secure session is absent", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(json({ code: "GUEST_SESSION_REQUIRED", detail: "missing" }, 401))
      .mockResolvedValueOnce(json({ token: "csrf-token", headerName: "X-XSRF-TOKEN" }))
      .mockResolvedValueOnce(json({ id: "guest-1" }, 201));
    vi.stubGlobal("fetch", fetchMock);
    const { ensureGuestSession } = await import("./api");

    await ensureGuestSession();

    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock.mock.calls[2][1]).toMatchObject({ credentials: "include", method: "POST" });
    expect(fetchMock.mock.calls[2][1].headers).toMatchObject({ "X-XSRF-TOKEN": "csrf-token" });
  });

  it("sends CSRF and a unique idempotency key when creating a plan", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(json({ token: "csrf-token", headerName: "X-XSRF-TOKEN" }))
      .mockResolvedValueOnce(json({ id: "plan-1" }, 201));
    vi.stubGlobal("fetch", fetchMock);
    const { createPlan } = await import("./api");

    await createPlan({
      householdSize: 4,
      dinnerCount: 3,
      budget: { amount: "1500.00", currency: "TRY" },
      maxCookingMinutes: null,
      priority: "BALANCED",
      dietaryRules: [],
      allergens: [],
      excludedIngredients: [],
      requirementsReviewed: true,
    });

    const headers = fetchMock.mock.calls[1][1].headers as Record<string, string>;
    expect(headers["X-XSRF-TOKEN"]).toBe("csrf-token");
    expect(headers["Idempotency-Key"]).toMatch(/^[0-9a-f-]{36}$/);
  });
});
