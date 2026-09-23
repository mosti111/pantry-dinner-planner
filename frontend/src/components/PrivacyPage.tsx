"use client";

import Link from "next/link";
import { useState } from "react";
import { deleteGuestSession } from "@/lib/api";

export function PrivacyPage() {
  const [status, setStatus] = useState("");
  const [busy, setBusy] = useState(false);

  async function clear() {
    setBusy(true);
    try {
      await deleteGuestSession();
      window.localStorage.removeItem("pantry.currentPlanId");
      setStatus("Your saved guest-session data has been cleared.");
    } catch (problem) {
      setStatus(problem instanceof Error ? problem.message : "Your saved data could not be cleared.");
    } finally {
      setBusy(false);
    }
  }

  return <main className="shell static-page">
    <Link href="/">← Return to planner</Link>
    <h1>Your plan. Your data.</h1>
    <section className="setup-panel">
      <h2>What this demo saves</h2>
      <p>We save your household inputs, food requirements, dinners, basket and demo cart so you can return to them in this browser. A browser session cookie controls access. No account, address or payment details are required.</p>
      <p style={{ marginTop: 16 }}>Food requirements are included in your private plan record. General usage events record actions, such as generating a plan, without those requirements. This app does not send them to a third-party analytics service.</p>
      <h2 style={{ marginTop: 28 }}>Retention and deletion</h2>
      <p style={{ marginTop: 16 }}>Saved plans become unavailable after seven days without an update. Expired records are cleaned up by scheduled maintenance. You can clear this session’s saved data now.</p>
      <button className="secondary" style={{ marginTop: 24 }} disabled={busy} onClick={clear}>{busy ? "Clearing…" : "Clear my saved data"}</button>
      <output style={{ display: "block", marginTop: 16 }}>{status}</output>
    </section>
  </main>;
}
