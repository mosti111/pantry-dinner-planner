"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { simulateRecovery } from "@/lib/api";

const scenarios: Record<string, string> = {
  normal: "",
  "sku-unavailable": "SKU_UNAVAILABLE",
  "ingredient-unavailable": "INGREDIENT_UNAVAILABLE",
  "price-increase": "PRICE_INCREASE",
  "promotion-expired": "PROMOTIONS_EXPIRED",
  "retailer-unavailable": "RETAILER_UNAVAILABLE",
  "transfer-failed": "CART_TRANSFER_FAILURE",
};

export function DemoScenarioPage() {
  const [scenario, setScenario] = useState("normal");
  const [level, setLevel] = useState("0");
  const [status, setStatus] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    const planId = window.localStorage.getItem("pantry.currentPlanId");
    if (!planId) {
      setStatus("Create a dinner plan first, then return here to test recovery.");
      return;
    }
    if (!scenarios[scenario]) {
      setStatus("Normal operation selected. No failure is active.");
      return;
    }
    setBusy(true);
    try {
      await simulateRecovery(planId, scenarios[scenario]);
      setStatus("Scenario applied. Return to your basket to see how Pantry recovered.");
    } catch (problem) {
      setStatus(problem instanceof Error ? problem.message : "The scenario could not be applied.");
    } finally {
      setBusy(false);
    }
  }

  return <main className="shell static-page">
    <Link href="/">← Return to planner</Link>
    <div className="eyebrow" style={{ marginTop: 30 }}>RETAILER DEMONSTRATION</div>
    <h1>Test the unexpected.</h1>
    <p style={{ margin: "20px 0" }}>These controls affect only your guest session. Use them to demonstrate recovery and integration capabilities. Every level remains simulated.</p>
    <form className="setup-panel" onSubmit={submit}>
      <div className="field"><label htmlFor="scenario">Scenario</label><select id="scenario" value={scenario} onChange={event => setScenario(event.target.value)}><option value="normal">Normal operation</option><option value="sku-unavailable">One SKU unavailable</option><option value="ingredient-unavailable">All SKUs for one ingredient unavailable</option><option value="price-increase">Selected SKU price increases by ₺100</option><option value="promotion-expired">Promotions expired</option><option value="retailer-unavailable">Retailer unavailable</option><option value="transfer-failed">Cart transfer fails</option></select></div>
      <div className="field"><label htmlFor="target">Affected product from your latest plan</label><select id="target" disabled><option>No product selected</option></select></div>
      <div className="field"><label htmlFor="level">Integration level</label><select id="level" value={level} onChange={event => setLevel(event.target.value)}><option value="0">0 · Demo cart</option><option value="1">1 · Catalog and shopping list</option><option value="2">2 · Live-capability simulation and shopping list</option><option value="3">3 · Cart transfer simulation</option><option value="4">4 · Partnership-capability simulation</option></select></div>
      <button className="primary full" disabled={busy}>{busy ? "Applying scenario…" : "Apply demo scenario"}</button>
      <output style={{ marginTop: 20, display: "block" }}>{status}</output>
    </form>
    <p className="hint" style={{ marginTop: 20 }}>Recovery: refresh basket for same-ingredient SKUs; choose a reviewed ingredient alternative when available; otherwise replace the affected dinner. Reset to normal operation when finished.</p>
  </main>;
}
