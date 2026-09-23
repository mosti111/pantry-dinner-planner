import type { Plan, PlanRequest } from "./types";

const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";
let csrf: Promise<{ token: string; headerName: string }> | null = null;

export class ApiError extends Error {
  constructor(message: string, public readonly code?: string) {
    super(message);
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  let securityHeaders: Record<string, string> = {};
  if (!["GET", "HEAD", "OPTIONS"].includes(method) && path !== "/csrf") {
    csrf ??= fetch(`${baseUrl}/csrf`, { credentials: "include" }).then(async response => {
      if (!response.ok) throw new ApiError("Could not establish a secure browser session.");
      return response.json();
    });
    const token = await csrf;
    securityHeaders = { [token.headerName]: token.token };
  }
  const response = await fetch(`${baseUrl}${path}`, {
    ...init,
    credentials: "include",
    headers: { "Content-Type": "application/json", "X-Pantry-Request": "web", ...securityHeaders, ...init.headers },
  });
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    throw new ApiError(problem.detail ?? "Pantry could not complete that request.", problem.code);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export async function createGuestSession(): Promise<void> {
  await request("/guest-sessions", { method: "POST", body: "{}" });
}

export async function ensureGuestSession(): Promise<void> {
  try {
    await request("/guest-sessions/current");
  } catch (problem) {
    if (!(problem instanceof ApiError) || problem.code !== "GUEST_SESSION_REQUIRED") throw problem;
    await createGuestSession();
  }
}

export async function deleteGuestSession(): Promise<void> {
  await request("/guest-sessions/current", { method: "DELETE" });
}

export function createPlan(input: PlanRequest): Promise<Plan> {
  return request("/meal-plans", {
    method: "POST", body: JSON.stringify(input), headers: { "Idempotency-Key": crypto.randomUUID() },
  });
}

export function getPlan(planId: string): Promise<Plan> {
  return request(`/meal-plans/${planId}`);
}

export function selectBasketLine(planId: string, lineId: string, selected: boolean): Promise<Plan> {
  return request(`/meal-plans/${planId}/basket/lines/${lineId}`, {
    method: "PUT", body: JSON.stringify({ selected }),
  });
}

export function selectProduct(planId: string, lineId: string, sku: string): Promise<Plan> {
  return request(`/meal-plans/${planId}/basket/lines/${lineId}/product`, {
    method: "PUT", body: JSON.stringify({ sku }),
  });
}

export function replaceMeal(planId: string, slot: string): Promise<Plan> {
  return request(`/meal-plans/${planId}/meals/${slot}/replace`, { method: "POST", body: "{}" });
}

export function simulateRecovery(planId: string, scenario: string): Promise<Plan> {
  return request(`/meal-plans/${planId}/basket/recovery-demo`, {
    method: "POST", body: JSON.stringify({ scenario }),
  });
}

export function refreshBasket(planId: string): Promise<Plan> {
  return request(`/meal-plans/${planId}/basket/refresh`, { method: "POST", body: "{}" });
}

export function finalizeBasket(planId: string): Promise<Plan> {
  return request(`/meal-plans/${planId}/basket/finalize`, {
    method: "POST", body: "{}", headers: { "Idempotency-Key": crypto.randomUUID() },
  });
}
