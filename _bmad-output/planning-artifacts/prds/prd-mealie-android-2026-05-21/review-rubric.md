---
title: PRD Quality Review - Mealie Android
reviewed: 2026-05-21
reviewer: Claude Code PRD Reviewer
verdict: pass
---

# PRD Quality Review: Mealie Android

## Verdict: PASS (with minor gaps noted)

---

## 1. Decision-Readiness

**Strong.** A UX designer can proceed directly: the Information Architecture section specifies bottom nav structure, screen hierarchy, and primary entry points. FR-11 defines Shopping List grouping (unchecked/checked). FR-16 delegates visual treatment to UX explicitly - appropriate division of responsibility. FR-3 specifies the security dialog behavior (one-time warning for HTTP). An architect has enough: stack choices are named (Room, WorkManager, OkHttp Authenticator, DataStore + Keystore, Retrofit or Ktor), API endpoints cited per FR, conflict strategy defined (last-write-wins on updated_at).

**Gap:** FR-6 says "show prompt" on re-auth but the Info Architecture section does not clarify whether this prompt is a full screen or a dialog. This is a small ambiguity that will surface during UX design. Similarly FR-8 mentions "debounced, 2-char minimum" but does not specify debounce delay - a designer will need a number to prototype correctly (300ms is industry standard but not stated).

---

## 2. Substance

**Strong overall.** Most FRs are testable:
- FR-2: "probe before accepting" - testable, endpoint implied.
- FR-4/FR-5: Auth fallback chain is explicit and deterministic (refresh -> stored credentials -> prompt; offline -> proceed). Each branch is independently testable.
- FR-12: "flush within 5s if connected" - measurable.
- FR-15: Conflict resolution strategy (last-write-wins, updated_at, log losers) is precise enough to implement and verify.
- FR-16: "appears within 3s of loss, disappears within 3s of restore" - measurable.
- FR-19: Pre-filled context fields are enumerated (Android version, app version, manufacturer, model).

**Weak spots:**
- FR-7: "Graceful offline fallback" for recipe list is vague. Does it show cached results from a previous session? Show an empty state? Show the last fetched page? This is not addressed anywhere and is a testability gap - there is no recipe caching defined for v1, so "graceful fallback" may mean "show empty state with offline indicator" but that should be explicit.
- FR-10: "Background refresh" for Shopping List roster is mentioned without specifying the trigger (on-launch? periodic? on-connectivity-restore?). The WorkManager flush in FR-15 handles mutations, but pull refresh is not covered.
- SM-4 "sync drain <30s" - what constitutes a "drain"? All items in queue? A representative test queue of N items? Unmeasurable without a definition.

---

## 3. Strategic Coherence

**Strong.** The product story is tight: the two pain points (silent logout, offline failure) are named in the vision, mapped to user journeys (UJ-2, UJ-3), addressed by specific FRs (FR-4/FR-5 for auth, FR-10 through FR-15 for offline Shopping List), and measured by SM-1 and SM-2. The Passenger persona drives the auth resilience requirement; the supermarket scenario drives the offline-first Shopping List design. The non-goals correctly exclude features that would dilute the v1 focus (offline recipes, real-time sync, multi-server). The v2 items listed in Section 6 are logically the natural next step once core flows are stable.

**Minor tension:** The primary metrics SM-1 and SM-2 are binary (100%) - effectively pass/fail acceptance criteria, not adoption metrics. SM-5 (household adoption over PWA) is the only forward-looking adoption signal, but it has no baseline, no measurement mechanism, and no target percentage or timeline. For an open-source project this may be acceptable, but it means there is no numeric definition of "success" beyond shipping a working app.

---

## 4. Completeness

**Good, with identifiable gaps:**

- **Error states for sync failures:** FR-15 says "unrecoverable errors surfaced once" - but what is the UI? A snackbar? A persistent error state on the item row? The FR-17 Sync Status Badge distinguishes "queued/syncing" from "sync error" but does not define what the user can do with a sync error. Can they retry manually? Dismiss? This is an actionability gap.
- **Recipe list offline:** FR-7 says "graceful offline fallback" but v1 has no recipe caching (offline recipes are post-v1). If the user is offline and opens Recipes, the expected behavior is undefined. An empty state with an error message? The Shopping List correctly references Local Store as source of truth; Recipes has no equivalent. This needs an explicit statement even if the answer is "show error state, no cached data."
- **Token expiry edge case:** FR-4 handles expiry on launch, FR-5 handles mid-session 401. But what happens if TOKEN_TIME is very short (e.g., 1 hour, which is within the 1-9600h range stated in Cross-Cutting NFRs) and Stored Credentials have changed on the server (password reset)? The fallback chain will eventually reach "prompt" - that path is covered - but the UX for "your password was changed, please log in" vs. "network error" is not differentiated. Low priority but worth a note.
- **Passenger first setup:** UJ-1 is Operator setup. UJ-3 is Passenger offline. But there is no UJ for Passenger first setup (the §2.2 JTBD mentions "Get a new household member set up without explaining Mealie infrastructure"). FR-1 covers the setup screen but the Passenger-specific path (no server URL knowledge) is not elaborated. §5 Non-Goals excludes QR onboarding, which is fine, but the Passenger setup flow remains underspecified relative to the persona promise.
- **Settings credential update:** The Info Architecture lists "credentials update" in Settings but there is no FR for it. This is a functional gap - it exists in the nav but has no defined behavior, validation, or security treatment.

---

## 5. Glossary Discipline

**Good.** The glossary in §3 is substantive and anchors key technical terms. Terms used in FRs are consistent with their glossary definitions. "Stored Token" and "Stored Credentials" are distinct and used accurately. "Local Store" and "Sync Queue" are used consistently. "Sync Status Badge" is defined in §3 (implicitly, by use in FR-12/FR-13/FR-17) and FR-17 elaborates it.

**Issues:**
- "Offline Indicator" appears in both §3 (as a glossary term) and as a section header (§4.5), but "Offline Indicator" and "Sync Status Badge" are two distinct UI elements that are only disambiguated in FR-17. A brief note in the glossary clarifying that these are different components (one global, one per-item) would prevent confusion for a UX designer reading the glossary before the FRs.
- "Connectivity probe" appears in FR-15 and FR-16 but is not in the glossary. FR-16 specifies it means "API reachability, not just network state" and FR-15 specifies the endpoint (GET /api/app/about) - enough context to infer the definition, but it should be in the glossary since it is a meaningful architectural term.
- "Sync Network Mode" is in both the glossary and FR-18 - consistent.
- TOKEN_TIME appears twice in the glossary entry (listed as a term and then again at the end of the glossary list) - minor duplication.

---

## 6. Non-Goals

**Strong.** The non-goals list in §5 is specific and comprehensive. Each item is named with enough precision to be used as a scope boundary in a design review. "No API token auth" is a notable explicit exclusion that prevents a common edge case from sneaking in. "No analytics/telemetry/crash reporting" appears in both §5 and Cross-Cutting NFRs - the repetition is appropriate given the security sensitivity.

**Minor issue:** "No multi-Household v1" is listed in §5 non-goals, but the MVP Scope (§6) says "multi-Household picker (post-v1)" - slightly inconsistent framing. §5 implies it is never in scope; §6 implies it is deferred. This should be harmonized: is it post-v1 or a deliberate permanent exclusion?

---

## 7. Metrics

**Acceptable, but thin on secondary metrics.**

SM-1 and SM-2 (100% auth resilience, 100% offline Shopping List availability) are the right primary metrics and are clearly tied to FR-4/FR-5 and FR-10 through FR-15 respectively. They are binary acceptance criteria effectively, which is honest for an open-source v1.

SM-3 (setup completion >90%) is measurable in principle but requires an instrumented funnel - and the PRD explicitly excludes analytics/telemetry. How will this be measured? Manual user testing? Issue volume? The metric is good in concept but has no measurement pathway defined.

SM-4 (sync drain <30s) has the definitional problem noted above. Needs a reference queue size or scenario (e.g., "10 pending mutations over cellular connection").

SM-5 (household adoption over PWA) has no baseline, no target, no timeframe, and no measurement mechanism. It reads as an aspiration, not a metric.

SM-C1 (battery impact) and SM-C2 (sync queue integrity) are listed as counter-metrics but have no thresholds. "No measurable background battery drain above 5% daily" would be a counter-metric; "battery impact" is a concern. Similarly "sync queue integrity" needs a definition - zero data loss? Zero silent failures?

The metrics section would benefit from attaching each secondary metric to the FR it validates. That mapping is implicit (SM-4 maps to FR-15, SM-C2 maps to FR-14/FR-15) but making it explicit would strengthen the traceability chain.

---

## Summary Table

| Dimension | Rating | Key Finding |
|---|---|---|
| Decision-readiness | Strong | Re-auth dialog vs. screen ambiguous; debounce delay missing |
| Substance | Good | FR-7 offline fallback vague; FR-10 refresh trigger undefined |
| Strategic coherence | Strong | Vision-to-FR-to-metric chain is tight; SM-5 is an aspiration not a metric |
| Completeness | Adequate | Sync error UX not actionable; Settings credential update has no FR; Passenger setup underspecified |
| Glossary discipline | Good | Connectivity probe missing; Offline Indicator vs. Sync Status Badge needs disambiguation in glossary |
| Non-goals | Strong | Multi-Household framing inconsistency between §5 and §6 |
| Metrics | Adequate | Secondary metrics lack measurement pathways; counter-metrics lack thresholds |

---

## Top Issues to Address Before UX/Architecture Handoff

1. **Add FR for Settings credential update** - it is in the nav but has no defined behavior.
2. **Define Recipe list offline behavior explicitly** - "graceful fallback" is not implementable; state whether it is an error state with no cached data.
3. **Define sync error UX in FR-15/FR-17** - what can the user do with a sync error item? Retry? Dismiss? This gates the UX design for the item row.
4. **Add Passenger first setup journey** - UJ for a new user who does not know the server URL; clarify what "setup without explaining Mealie infrastructure" means in v1 without QR codes.
5. **Sharpen SM-3, SM-4, SM-C1, SM-C2** - either define measurement pathways or demote to qualitative acceptance criteria.
