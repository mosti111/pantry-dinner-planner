"use client";

import Image from "next/image";
import Link from "next/link";
import { FormEvent, ReactNode, useEffect, useState } from "react";
import {
  createPlan,
  ensureGuestSession,
  finalizeBasket,
  getPlan,
  refreshBasket,
  replaceMeal,
  selectBasketLine,
  selectProduct,
} from "@/lib/api";
import type { BasketLine, Meal, Plan, PlanRequest, Quantity } from "@/lib/types";

type Stage = "setup" | "plan" | "basket" | "done";

const diets = ["VEGAN", "VEGETARIAN", "PESCATARIAN", "GLUTEN_FREE"];
const allergens = ["MILK", "EGGS", "GLUTEN", "SOY", "PEANUT", "TREE_NUT", "SESAME", "FISH", "SHELLFISH", "CELERY", "MUSTARD", "SULPHITES", "LUPIN"];

const initialRequest: PlanRequest = {
  householdSize: 4,
  dinnerCount: 3,
  budget: { amount: "1500.00", currency: "TRY" },
  maxCookingMinutes: null,
  priority: "BALANCED",
  dietaryRules: [],
  allergens: [],
  excludedIngredients: [],
  requirementsReviewed: false,
};

export function PantryApp() {
  const [stage, setStage] = useState<Stage>("setup");
  const [request, setRequest] = useState<PlanRequest>(initialRequest);
  const [plan, setPlan] = useState<Plan | null>(null);
  const [recipe, setRecipe] = useState<Meal | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [sessionReady, setSessionReady] = useState(false);

  useEffect(() => {
    let active = true;
    async function restore() {
      try {
        await ensureGuestSession();
        const savedPlanId = window.localStorage.getItem("pantry.currentPlanId");
        if (savedPlanId) {
          try {
            const saved = await getPlan(savedPlanId);
            if (!active) return;
            setPlan(saved);
            setRequest(requestFrom(saved));
            setStage(saved.status === "CART_READY" ? "done" : "plan");
          } catch {
            window.localStorage.removeItem("pantry.currentPlanId");
          }
        }
        if (active) setSessionReady(true);
      } catch {
        if (active) setError("The Pantry service is not available yet. Start the backend and try again.");
      }
    }
    void restore();
    return () => { active = false; };
  }, []);

  async function run<T>(label: string, operation: () => Promise<T>): Promise<T | undefined> {
    if (busy) return undefined;
    setBusy(label);
    setError(null);
    try {
      return await operation();
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Pantry could not complete that request.");
      return undefined;
    } finally {
      setBusy(null);
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!request.requirementsReviewed) return;
    const result = await run("Finding dinners and checking your basket", () => createPlan(request));
    if (!result) return;
    window.localStorage.setItem("pantry.currentPlanId", result.id);
    setPlan(result);
    setStage("plan");
  }

  async function toggleLine(lineId: string, selected: boolean) {
    if (!plan) return;
    const previous = plan;
    const lines = plan.basket.lines.map(line => line.id === lineId ? { ...line, selected } : line);
    setPlan(withOptimisticBasket(plan, lines));
    const updated = await run("Updating your shopping total", () => selectBasketLine(plan.id, lineId, selected));
    setPlan(updated ?? previous);
  }

  async function changeProduct(lineId: string, sku: string) {
    if (!plan) return;
    const updated = await run("Checking alternative package quantities", () => selectProduct(plan.id, lineId, sku));
    if (updated) setPlan(updated);
  }

  async function swapMeal(slot: string) {
    if (!plan) return;
    const updated = await run("Finding another dinner", () => replaceMeal(plan.id, slot));
    if (updated) setPlan(updated);
  }

  async function refresh() {
    if (!plan) return;
    const updated = await run("Refreshing basket prices", () => refreshBasket(plan.id));
    if (updated) setPlan(updated);
  }

  async function finish() {
    if (!plan) return;
    const updated = await run("Checking and creating your demo cart", () => finalizeBasket(plan.id));
    if (updated) {
      setPlan(updated);
      setStage("done");
    }
  }

  function go(next: Stage) {
    if (busy) return;
    setRecipe(null);
    setStage(next);
  }

  return (
    <>
      <a href="#main" className="skip-link">Skip to content</a>
      <SiteHeader />
      <main id="main" className={`shell stage-${stage}`}>
        <Journey stage={stage} hasPlan={Boolean(plan)} busy={Boolean(busy)} onStage={go} />
        {error && <section role="alert" className="notice error"><div><strong>{error}</strong></div><button aria-label="Dismiss error" onClick={() => setError(null)}>×</button></section>}
        {busy && <output className="notice loading"><span className="spin">◌</span><span>{busy}…</span></output>}
        {stage === "setup" && <Setup request={request} setRequest={setRequest} submit={submit} busy={Boolean(busy) || !sessionReady} hasPlan={Boolean(plan)} onReturn={() => go("plan")} />}
        {stage !== "setup" && plan && <ResultsHeading stage={stage} plan={plan} busy={Boolean(busy)} onEdit={() => { setRequest(requestFrom(plan)); go("setup"); }} />}
        {stage === "plan" && plan && <Dinners plan={plan} busy={Boolean(busy)} onRecipe={setRecipe} onReplace={swapMeal} onBasket={() => go("basket")} />}
        {stage === "basket" && plan && <Basket plan={plan} busy={Boolean(busy)} onToggle={toggleLine} onProduct={changeProduct} onRefresh={refresh} onFinish={finish} onDinners={() => go("plan")} />}
        {stage === "done" && plan && <Complete plan={plan} onDinners={() => go("plan")} />}
        <SiteFooter plan={plan} />
      </main>
      {recipe && plan && <RecipeDialog meal={recipe} plan={plan} onClose={() => setRecipe(null)} />}
    </>
  );
}

function SiteHeader() {
  return <header className="site-header">
    <Link className="brand" href="/" aria-label="Pantry home"><span className="brand-icon">♨</span>pantry<span className="brand-dot">.</span></Link>
    <span className="header-note">A little planning. A better dinner.</span>
    <span className="demo-pill"><span /> Demo supermarket</span>
  </header>;
}

function Journey({ stage, hasPlan, busy, onStage }: { stage: Stage; hasPlan: boolean; busy: boolean; onStage: (stage: Stage) => void }) {
  const steps: Array<[Stage, string]> = [["setup", "Your household"], ["plan", "Your dinners"], ["basket", "Your basket"]];
  const current = stage === "done" ? 3 : steps.findIndex(([value]) => value === stage);
  return <nav className="journey" aria-label="Planning progress">{steps.map(([value, label], index) =>
    <button key={value} className={stage === value ? "current" : ""} disabled={busy || (!hasPlan && value !== "setup")} onClick={() => onStage(value)} aria-current={stage === value ? "step" : undefined}>
      <span>{current > index ? "✓" : index + 1}</span>{label}
    </button>)}</nav>;
}

function Setup({ request, setRequest, submit, busy, hasPlan, onReturn }: { request: PlanRequest; setRequest: (value: PlanRequest) => void; submit: (event: FormEvent) => void; busy: boolean; hasPlan: boolean; onReturn: () => void }) {
  function toggle(collection: "dietaryRules" | "allergens", value: string) {
    const values = request[collection];
    setRequest({ ...request, [collection]: values.includes(value) ? values.filter(item => item !== value) : [...values, value], requirementsReviewed: false });
  }
  return <div className="setup-grid">
    <section className="setup-copy">
      <div className="eyebrow"><span className="live-dot" /> YOUR EVENINGS, REIMAGINED</div>
      <h1 tabIndex={-1}>A little prep.<br/><span>A lot of yum.</span></h1>
      <p className="intro">Simple dinner planning for real life.</p>
      <KitchenScene people={request.householdSize} dinners={request.dinnerCount} busy={busy} />
      <div className="trust-row"><span>✓</span><p>Full packages. Clear prices.<br/><span>Review everything before creating a demo cart.</span></p></div>
    </section>
    <section className="setup-panel">
      <div className="panel-heading"><span className="eyebrow">SET YOUR TABLE <span className="step-tag">01 / 03</span></span><h2>Good food starts here.</h2><p>A few details, then we’ll do the planning.</p></div>
      <form onSubmit={submit}>
        <div className="two-cols">
          <Counter id="people" label="People" value={request.householdSize} min={1} max={8} onChange={value => setRequest({ ...request, householdSize: value })} />
          <Counter id="meals" label="Dinners" value={request.dinnerCount} min={1} max={7} onChange={value => setRequest({ ...request, dinnerCount: value })} />
        </div>
        <div className="field">
          <label htmlFor="budget">Grocery budget <span>for the whole plan</span></label>
          <div className="currency-input"><span>₺</span><input id="budget" aria-label="Grocery budget for the whole plan" type="number" required min="1" max="100000" step="0.01" value={request.budget.amount} onChange={event => setRequest({ ...request, budget: { ...request.budget, amount: event.target.value } })}/></div>
          <input className="budget-slider" aria-label="Adjust grocery budget" type="range" min="100" max="5000" step="50" value={Math.min(5000, Math.max(100, Number(request.budget.amount) || 100))} onChange={event => setRequest({ ...request, budget: { ...request.budget, amount: Number(event.target.value).toFixed(2) } })}/>
          <div className="range-labels"><span>₺100</span><span>₺5,000</span></div>
          <p className="hint">Includes full packages and pantry basics. Delivery fees are separate.</p>
        </div>
        <details className="preferences">
          <summary><span>♧ Preferences &amp; food requirements</span><span>⌄</span></summary>
          <div className="two-cols">
            <div className="field"><label htmlFor="preference">What matters most?</label><select id="preference" value={request.priority} onChange={event => setRequest({ ...request, priority: event.target.value as PlanRequest["priority"] })}><option value="BALANCED">A little of everything</option><option value="LOWER_COST">Lower grocery cost</option><option value="QUICKER">Quicker cooking</option><option value="PROTEIN_RICH">Protein-rich ingredients</option></select></div>
            <div className="field"><label htmlFor="time">Maximum cooking time</label><select id="time" value={request.maxCookingMinutes ?? ""} onChange={event => setRequest({ ...request, maxCookingMinutes: event.target.value ? Number(event.target.value) : null })}><option value="">No time limit</option><option value="45">45 minutes</option><option value="50">50 minutes</option><option value="60">60 minutes</option><option value="90">90 minutes</option></select></div>
          </div>
          <ChoiceGroup legend="Dietary preferences" values={diets} selected={request.dietaryRules} onToggle={value => toggle("dietaryRules", value)} />
          <ChoiceGroup legend="Allergies" values={allergens} selected={request.allergens} onToggle={value => toggle("allergens", value)} />
          <div className="field"><label htmlFor="excluded">Exclude a food</label><input id="excluded" value={request.excludedIngredients.join(", ")} placeholder="mushrooms, tofu" onChange={event => setRequest({ ...request, excludedIngredients: event.target.value.split(",").map(value => value.trim().toLowerCase().replaceAll(" ", "-")).filter(Boolean), requirementsReviewed: false })}/></div>
        </details>
        <label className="review-check"><input type="checkbox" required checked={request.requirementsReviewed} onChange={event => setRequest({ ...request, requirementsReviewed: event.target.checked })}/><span>I’ve reviewed my food requirements.{request.dietaryRules.length + request.allergens.length + request.excludedIngredients.length === 0 && <small>No restrictions selected.</small>}</span></label>
        <button className="primary full" disabled={busy || !request.requirementsReviewed} type="submit">Plan my dinners <span>→</span></button>
        <p className="form-footnote">No account. No pantry inventory. Just dinner.</p>
        {hasPlan && <button className="text-button" type="button" disabled={busy} onClick={onReturn}>Return to my saved plan →</button>}
      </form>
    </section>
  </div>;
}

function KitchenScene({ people, dinners, busy = false }: { people: number; dinners: number; busy?: boolean }) {
  return <div className={`kitchen-scene ${busy ? "is-busy" : ""}`}>
    <div className="kitchen-art"><div className="kitchen-stage">
      <Image className="kitchen-background" src="/kitchen/stage.webp" width={1536} height={1024} alt="A blue floral bowl in a sunny kitchen" priority />
      <div className="scene-tomato"><Image src="/kitchen/tomato.webp" width={700} height={700} alt="Playful tomato character" /></div>
      <div className="scene-aubergine"><Image src="/kitchen/aubergine.webp" width={700} height={700} alt="Aubergine character holding a wooden spoon" /></div>
    </div><span className="kitchen-note">Good food<br/>happens here<span>♡</span></span></div>
    <span className="table-feedback" aria-live="polite">{people} at the table · {dinners} good evenings</span>
  </div>;
}

function Counter({ id, label, value, min, max, onChange }: { id: string; label: string; value: number; min: number; max: number; onChange: (value: number) => void }) {
  return <div className="field"><label htmlFor={id}>{label}</label><div className="stepper"><button type="button" disabled={value <= min} aria-label={`Fewer ${label.toLowerCase()}`} onClick={() => onChange(value - 1)}>−</button><input id={id} aria-label={label === "People" ? "People at the table" : "Number of dinners"} type="number" min={min} max={max} value={value} onChange={event => onChange(Math.min(max, Math.max(min, Number(event.target.value))))}/><button type="button" disabled={value >= max} aria-label={`More ${label.toLowerCase()}`} onClick={() => onChange(value + 1)}>+</button></div></div>;
}

function ChoiceGroup({ legend, values, selected, onToggle }: { legend: string; values: string[]; selected: string[]; onToggle: (value: string) => void }) {
  return <fieldset><legend>{legend}</legend><div className="chips">{values.map(value => <label key={value} className="chip"><input type="checkbox" checked={selected.includes(value)} onChange={() => onToggle(value)}/>{pretty(value)}</label>)}</div></fieldset>;
}

function ResultsHeading({ stage, plan, busy, onEdit }: { stage: Stage; plan: Plan; busy: boolean; onEdit: () => void }) {
  return <><div className="results-heading"><div><div className="eyebrow">{stage === "done" ? "READY FOR THE TABLE" : stage === "basket" ? "ONE LIST. EVERY DINNER." : "YOUR EVENINGS, PLANNED"}</div><h1 tabIndex={-1}>{stage === "done" ? "Your demo cart is ready." : stage === "basket" ? "A little list. A lot of dinner." : "Good food ahead."}</h1><p>{plan.dinnerCount} dinners <span className="separator">/</span> {plan.householdSize} people <span className="separator">/</span> {formatMoney(plan.budget)} budget</p></div><button className="secondary" disabled={busy} onClick={onEdit}>Edit requirements</button></div><div className="kitchen-character" aria-hidden="true"><Image src="/kitchen/tomato.webp" width={300} height={300} alt="" /></div></>;
}

function Dinners({ plan, busy, onRecipe, onReplace, onBasket }: { plan: Plan; busy: boolean; onRecipe: (meal: Meal) => void; onReplace: (slot: string) => void; onBasket: () => void }) {
  return <div className="plan-layout"><section className="meal-list">{plan.meals.map((meal, index) => <article className="meal-card" key={meal.slot}>
    <div className="meal-art"><Image src={mealImage(meal)} width={600} height={600} alt="Illustrative serving suggestion" /><span className="meal-art-caption">Serving inspiration</span><div className="meal-number">{String(index + 1).padStart(2, "0")}<span>DINNER</span></div></div>
    <div className="meal-content"><div className="meal-tags"><span>{meal.dietaryLabel}</span><span>◷ {meal.minutes} min</span></div><h2>{meal.title}</h2><p className="turkish-title">{meal.localizedTitle}</p><p>{meal.description}</p><div className="meal-meta"><span>♧ {meal.servings} servings</span><span>✓ Fits your requirements</span></div><div className="meal-actions"><button className="text-button" onClick={() => onRecipe(meal)}>View recipe ⌄</button><button className="text-button" disabled={busy} onClick={() => onReplace(meal.slot)}>↻ Replace dinner</button></div></div>
  </article>)}</section><aside className="plan-summary"><Image src="/dinner-table.webp" width={1200} height={800} alt="An illustrative shared dinner table" /><div><span className="eyebrow">THE WHOLE PICTURE</span><h2>{formatMoney(plan.total)}</h2><meter className="sr-only" aria-label="Grocery budget used" value={Number(plan.total.amount)} min={0} max={Number(plan.budget.amount)} /><div className="budget-meter" aria-hidden="true"><span style={{ width: `${Math.min(100, Number(plan.total.amount) / Number(plan.budget.amount) * 100)}%` }} /></div><p>Complete grocery basket</p><div className="budget-line">✓ {formatMoney(plan.remaining)} remaining</div><hr/><p>{plan.basket.lines.length} grocery lines, combined across your dinners.</p><p className="hint">Already have oil or salt? Uncheck them in your basket.</p><button className="primary full" onClick={onBasket}>Review my basket →</button></div></aside></div>;
}

function Basket({ plan, busy, onToggle, onProduct, onRefresh, onFinish, onDinners }: { plan: Plan; busy: boolean; onToggle: (id: string, selected: boolean) => void; onProduct: (id: string, sku: string) => void; onRefresh: () => void; onFinish: () => void; onDinners: () => void }) {
  const [changing, setChanging] = useState<BasketLine | null>(null);
  const dinners = plan.basket.lines.filter(line => !line.pantryStaple);
  const pantry = plan.basket.lines.filter(line => line.pantryStaple);
  return <><div className="basket-layout"><section><div className="basket-explainer"><span aria-hidden="true">♙</span><p><strong>Tick what you need to buy.</strong><br/>Have enough at home? Uncheck it. Your dinners stay the same.</p></div>{plan.notices.length > 0 && <div className="notice" aria-live="polite">{plan.notices.at(-1)}</div>}<BasketGroup title="For your dinners" lines={dinners} busy={busy} onToggle={onToggle} onChange={setChanging}/><BasketGroup title="You may already have these" lines={pantry} busy={busy} onToggle={onToggle} onChange={setChanging}/></section><aside className="checkout-summary"><span className="eyebrow">YOUR SHOPPING TOTAL</span><h2>{formatMoney(plan.basket.toBuy)}</h2><p>{plan.basket.selectedCount} of {plan.basket.lines.length} products selected</p><dl><div><dt>Complete basket</dt><dd>{formatMoney(plan.basket.completeTotal)}</dd></div><div><dt>Already at home</dt><dd>−{formatMoney(plan.basket.alreadyAtHome)}</dd></div><div><dt>To buy</dt><dd>{formatMoney(plan.basket.toBuy)}</dd></div></dl><p className="hint">Simulated prices. No delivery fees or payment. Weighted-product totals are estimates.</p><button className="primary full" disabled={busy || plan.basket.selectedCount === 0} onClick={onFinish}>Create demo cart →</button><button className="text-button" disabled={busy} onClick={onRefresh}>↻ Refresh basket</button><button className="text-button" onClick={onDinners}>← Back to dinners</button></aside></div>{changing && <ProductSheet line={changing} busy={busy} onClose={() => setChanging(null)} onSelect={async sku => { await onProduct(changing.id, sku); setChanging(null); }} />}</>;
}

function BasketGroup({ title, lines, busy, onToggle, onChange }: { title: string; lines: BasketLine[]; busy: boolean; onToggle: (id: string, selected: boolean) => void; onChange: (line: BasketLine) => void }) {
  return <section className="basket-group"><h2>{title}</h2>{lines.map(line => <article className={`basket-item ${line.selected ? "" : "owned"}`} key={line.id}><label className="buy-toggle"><input aria-label={`Buy ${line.productName}`} type="checkbox" checked={line.selected} disabled={busy} onChange={event => onToggle(line.id, event.target.checked)}/></label><div className="basket-product"><span className="brand-label">{line.brand}</span><h3>{line.productName}</h3><p>{line.estimatedWeight ? `${line.packageSize.value} ${unit(line.packageSize)} requested · estimated price` : packageText(line.packageCount, line.packageSize)} <span className="separator">/</span> {quantityText(line.required)} needed</p>{Number(line.leftover.value) > 0 && <p className="hint">{quantityText(line.leftover)} left from this purchase · can be kept for another meal.</p>}<button className="text-button" disabled={busy || line.alternatives.length === 0} onClick={() => onChange(line)}>Change product</button></div><div className="line-price"><strong>{formatMoney(line.lineTotal)}</strong>{!line.selected && <span>At home</span>}</div></article>)}</section>;
}

function ProductSheet({ line, busy, onClose, onSelect }: { line: BasketLine; busy: boolean; onClose: () => void; onSelect: (sku: string) => Promise<void> }) {
  return <div className="sheet-overlay" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) onClose(); }}><section className="change-sheet product-sheet" role="dialog" aria-modal="true" aria-labelledby="product-sheet-title"><button className="sheet-close" aria-label="Close product choices" onClick={onClose}>×</button><h2 id="product-sheet-title">Choose a product</h2><p>Choose another package or brand for {line.ingredientName}.</p><div className="product-alternatives">{line.alternatives.length === 0 ? <p>No safe alternative is currently available.</p> : line.alternatives.map(alternative => <button key={alternative.sku} className="alternative-card" disabled={busy} onClick={() => onSelect(alternative.sku)}><span><strong>{alternative.brand}</strong><small>{alternative.productName}</small></span><b>{formatMoney(alternative.packagePrice)}</b></button>)}</div><p className="hint">The basket total and package quantity will be recalculated before the change is applied.</p></section></div>;
}

function Complete({ plan, onDinners }: { plan: Plan; onDinners: () => void }) {
  return <section className="success-card"><KitchenScene people={plan.householdSize} dinners={plan.dinnerCount} /><div className="success-mark">✓</div><h2>All set for {plan.dinnerCount} dinners.</h2><p>Demo cart created. No order or payment was placed.</p><div className="success-total">{formatMoney(plan.basket.toBuy)}<span>{plan.basket.selectedCount} selected products</span></div><p className="hint">Cart reference: {plan.basket.cartReference}</p><div className="button-row"><button className="secondary" onClick={() => downloadJson(plan.basket, "pantry-demo-cart.json")}>Save demo cart</button><button className="primary" onClick={onDinners}>Back to my dinners →</button></div></section>;
}

function RecipeDialog({ meal, plan, onClose }: { meal: Meal; plan: Plan; onClose: () => void }) {
  const [tab, setTab] = useState<"ingredients" | "method">("ingredients");
  return <div className="dialog-overlay" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) onClose(); }}><section className="recipe-view" data-slot="dialog-content" role="dialog" aria-modal="true" aria-labelledby="recipe-title"><header className="recipe-view-header"><button className="text-button" onClick={onClose}>← Your dinners</button><span className="wordmark">pantry<span>′</span></span><span>Demo</span></header><div className="recipe-scroll"><div className="recipe-hero"><Image src={mealImage(meal)} width={900} height={600} alt="Illustrative serving suggestion" /><span>Serving inspiration</span></div><div className="recipe-body"><h2 className="recipe-view-title" id="recipe-title">{meal.title}</h2><p className="recipe-description">{meal.description}</p><div className="recipe-facts"><span>◷ {meal.minutes} min</span><span>♧ {plan.householdSize} servings</span></div><div className="recipe-tabs"><div data-slot="tabs-list" role="tablist" aria-label="Recipe information"><button data-slot="tabs-trigger" data-active={tab === "ingredients" ? "" : undefined} role="tab" aria-selected={tab === "ingredients"} onClick={() => setTab("ingredients")}>Ingredients</button><button data-slot="tabs-trigger" data-active={tab === "method" ? "" : undefined} role="tab" aria-selected={tab === "method"} onClick={() => setTab("method")}>Method</button></div>{tab === "ingredients" ? <div data-slot="tabs-content" role="tabpanel" aria-label="Ingredients"><ul className="ingredient-list">{meal.ingredients.map(item => <li key={item.key}><span>{item.name}</span><strong>{quantityText(item.quantity)}</strong></li>)}</ul><p className="hand-note">Scaled for your table.</p></div> : <div data-slot="tabs-content" role="tabpanel" aria-label="Method"><ol className="method-list">{meal.method.map((instruction, index) => <li key={instruction}><span>{index + 1}</span>{instruction}</li>)}</ol></div>}</div><details className="food-notes"><summary>Storage, leftovers and nutrition</summary><p>Cool leftovers promptly, refrigerate in a covered container and follow the storage guidance for the actual products you purchase.</p></details><details className="food-notes"><summary>Why this dinner?</summary><ul>{meal.reasons.map(reason => <li key={reason}>{reason}</li>)}</ul></details><p className="hint">Demo recipe · structurally validated, not kitchen-tested. Check actual product labels before cooking. Imagery is illustrative.</p></div></div><footer className="recipe-view-footer"><button className="secondary full" onClick={onClose}>← Back to dinners</button></footer></section></div>;
}

function SiteFooter({ plan }: { plan: Plan | null }) {
  return <footer className="site-footer"><span>PROJECT PANTRY <span className="separator">/</span> A meal-to-basket demonstration</span><span>Fictional products &amp; prices · No real orders</span><Link href="/demo">Demo scenarios</Link><Link href="/privacy">Your data</Link>{plan && <button className="text-button" onClick={() => downloadJson(plan, "pantry-plan-record.json")}>Download plan record</button>}</footer>;
}

function withOptimisticBasket(plan: Plan, lines: BasketLine[]): Plan {
  const selected = lines.filter(line => line.selected);
  const toBuy = selected.reduce((total, line) => total + Number(line.lineTotal.amount), 0);
  const complete = Number(plan.basket.completeTotal.amount);
  return { ...plan, basket: { ...plan.basket, lines, selectedCount: selected.length, toBuy: { amount: toBuy.toFixed(2), currency: plan.basket.toBuy.currency }, alreadyAtHome: { amount: (complete - toBuy).toFixed(2), currency: plan.basket.alreadyAtHome.currency } } };
}

function requestFrom(plan: Plan): PlanRequest {
  return { ...initialRequest, householdSize: plan.householdSize, dinnerCount: plan.dinnerCount, budget: plan.budget, requirementsReviewed: true };
}

function mealImage(meal: Meal) {
  if (/lentil/i.test(meal.title)) return "/kitchen/lentils.webp";
  if (/chickpea/i.test(meal.title)) return "/kitchen/chickpeas.webp";
  if (/tofu/i.test(meal.title)) return "/kitchen/tofu.webp";
  return "/dinner-table.webp";
}

function downloadJson(value: unknown, filename: string) {
  const url = URL.createObjectURL(new Blob([JSON.stringify(value, null, 2)], { type: "application/json" }));
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

function formatMoney(value: { amount: string; currency: string }) { return new Intl.NumberFormat("tr-TR", { style: "currency", currency: value.currency }).format(Number(value.amount)); }
function unit(value: Quantity) { return value.unit === "GRAM" ? "g" : value.unit === "MILLILITER" ? "ml" : "item"; }
function quantityText(value: Quantity) { return `${value.value} ${unit(value)}`; }
function packageText(count: number, size: Quantity) { return `${count} × ${quantityText(size)}`; }
function pretty(value: string) { return value.toLowerCase().split("_").map(word => word[0].toUpperCase() + word.slice(1)).join(" "); }

export function StaticPageShell({ children }: { children: ReactNode }) {
  return <><SiteHeader/><main className="shell static-page">{children}</main></>;
}
