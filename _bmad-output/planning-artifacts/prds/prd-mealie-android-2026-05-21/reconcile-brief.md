# Brief-to-PRD Reconciliation: Mealie Android

**Source:** brief-mealie-android-2026-05-21/brief.md
**Target:** prd-mealie-android-2026-05-21/prd.md
**Date:** 2026-05-21

---

## Gaps Found

### 1. Dropped: "Feels exactly like Mealie's web interface" vision language
The brief's vision section states the app "should feel exactly like Mealie's web interface, with one silent addition." This qualitative design constraint - fidelity to the web experience as a deliberate goal, not a coincidence - is absent from the PRD. It has UX implications: the app should not introduce foreign UI patterns but mirror the web where reasonable.

### 2. Dropped: Share-to-import mid-flow auth context
The brief describes a specific failure mode: sharing a recipe URL to the PWA while not logged in loses the share target after authentication. The PRD lists recipe URL import as post-v1 (correct) but drops the motivation entirely. The background re-authentication requirement for the share flow is the reason silent re-auth must be lossless - that context is gone from the PRD.

### 3. Weakly covered: Family/household adoption as a distinct success signal
The brief explicitly names household adoption - a family member abandoning Mealie for a notes app - as both the failure state and the benchmark for success. The PRD captures SM-5 ("Household adoption over PWA") as a one-line counter but strips the human story. The brief's framing that success means "they use the app instead of the PWA because it genuinely works better, not because they were asked to" is absent.

### 4. Dropped: Strategic aspiration and community ownership language
The brief says the project should earn endorsement, not plan for it: "not because it was planned that way, but because it solved a real problem well enough that the community made it their own." The PRD's vision paragraph says "the aspiration is to become the native Android client the Mealie project can point to" but removes the earned/organic framing. This affects how the project presents itself to open-source contributors.

### 5. Dropped: Connectivity transparency principle
The brief states offline "is not a special state - it is just how the app works when there is no signal" and that the long-term goal is that "connectivity is no longer a constraint." The PRD's vision repeats the "offline is not a special mode" phrase for the Shopping List but scopes it explicitly to v1 features. The broader principle - that the app should eventually make connectivity invisible across all features, not just the list - is not captured as a design principle or post-v1 direction.

---

## No Contradictions Found

The PRD does not contradict the brief on any factual or scope point. All explicit v1 scope items from the brief are present. Post-v1 items (offline recipe browsing, URL import, QR onboarding) are correctly deferred.
