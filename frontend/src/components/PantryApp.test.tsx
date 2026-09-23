import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { PantryApp } from "./PantryApp";

vi.mock("@/lib/api", () => ({
  ensureGuestSession: vi.fn().mockResolvedValue(undefined),
  getPlan: vi.fn(),
  createPlan: vi.fn(),
  selectBasketLine: vi.fn(),
  selectProduct: vi.fn(),
  replaceMeal: vi.fn(),
  simulateRecovery: vi.fn(),
  refreshBasket: vi.fn(),
  finalizeBasket: vi.fn(),
}));

describe("PantryApp", () => {
  it("presents the guest-first planning form", async () => {
    render(<PantryApp />);
    expect(screen.getByRole("heading", { name: /a little prep/i })).toBeInTheDocument();
    expect(await screen.findByRole("button", { name: /plan my dinners/i })).toBeDisabled();
    expect(screen.getByText(/no account/i)).toBeInTheDocument();
  });
});
