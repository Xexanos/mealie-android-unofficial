---
title: "PRD: Mealie Android"
status: final
created: 2026-05-21
updated: 2026-05-22
---

# PRD: Mealie Android

## 0. Document Purpose

This PRD is for the project maintainer (developer/PM), downstream UX and architecture work, and eventual open-source contributors. It is structured with Glossary-anchored vocabulary (§3), features grouped with FRs nested (§4), and assumptions tagged inline and indexed in §8. The product brief at `_bmad-output/planning-artifacts/briefs/brief-mealie-android-2026-05-21/brief.md` is the primary upstream input; this PRD extends it into implementable requirements without duplicating its narrative.

---

## 1. Vision

Mealie is a powerful self-hosted recipe manager with a household Shopping List feature. Its PWA wrapper on Android fails at the two moments that matter most: it silently logs users out when the auth token expires — the default lifetime is 48 hours, so infrequent users face a login screen without warning — and it returns a blank screen anywhere mobile data is unreliable. These are not edge cases - they are the core use cases.

The token lifetime is a deliberate Mealie security design. The app does not extend it. Instead, it stores credentials securely on-device so that when the token expires, re-authentication happens silently in the background — the user never sees a login screen.

Mealie Android is a native Android shopping list app that uses a self-hosted Mealie instance as its backend. It is not a general Mealie client - it is a focused tool for one job: a shopping list that is simply there when you need it. Authentication happens once: credentials are stored securely on-device and token refresh runs silently in the background. Offline is not a special mode - the Shopping List is fully editable without signal, changes queue locally, and sync happens automatically when connectivity returns. Recipe browsing is post-v1.

The design north star: the app should feel like the most reliable shopping list you have ever used. It is there in the supermarket basement with no signal. It is there after a week of not opening the app without a login prompt. It is there on the first tap of the day, already showing the right list. That reliability is invisible when everything works, and total when it does not.

The app is open source and built for the Android Mealie community. The near-term audience is small - people who already run Mealie and have felt these frustrations - but the aspiration is to become the endorsed Android shopping companion the Mealie project can point to. Not because it was planned that way, but because it solved a real problem well enough that the community made it their own.

---

## 2. Target User

### 2.1 Primary Persona

**Mealie Household Users** - people who use a self-hosted Mealie instance as part of their household's shopping workflow. They are comfortable with self-hosted software, use Android, and have experienced both the auth interruption (silent logout after days of not opening the app, when the short-lived token expires) and the offline failure (blank screen in the supermarket). They do not want to think about their app's infrastructure - they want their list when they need it.

Two sub-types share identical ongoing needs but have different setup paths:
- **Operator** - runs the Mealie server, configures the app themselves.
- **Passenger** - uses someone else's Mealie instance; needs a setup path that does not require technical knowledge. In v1, the Passenger receives the server URL from the Operator out-of-band (verbally or via message) and enters it manually during first-launch setup. Streamlined onboarding via QR code or setup link is post-v1.

### 2.2 Jobs To Be Done

- Open the Shopping List in the supermarket and trust it will be there, even without signal.
- Open the app after days of not using it without being forced through a login flow.
- Add an item to the Shopping List that syncs to the rest of the household.
- Get a new household member set up without explaining Mealie infrastructure.

### 2.3 Non-Users (v1)

- People who do not already run a Mealie instance - the app requires an existing Mealie server for authentication and sync; it is not a standalone service.
- iOS users.
- Mealie admins managing server configuration - the app exposes no admin surface.

### 2.4 Key User Journeys

**UJ-1. Operator sets up the app for the first time.**
- **Persona + context:** Alex, running a home Mealie server, installing the app fresh on a new device.
- **Entry state:** App installed, never launched; no existing session.
- **Path:** (1) Opens app; (2) Sees first-launch server setup screen; (3) Enters server URL and credentials; (4) App validates connection and authenticates; (5) Lands on main screen.
- **Climax:** Main screen loads with household data - Alex knows the connection is live.
- **Resolution:** Stored Token and Stored Credentials persisted securely. Future launches skip setup entirely.
- **Edge case:** Invalid URL or wrong credentials - inline error is shown; fields remain editable; no crash; no data loss.

**UJ-2. Passenger opens the app after the token has expired.**
- **Persona + context:** Sam (Alex's partner), hasn't opened the app in a few days; her Stored Token has expired (Mealie's default token lifetime is 48 hours).
- **Entry state:** App previously configured; Stored Token expired due to inactivity.
- **Path:** (1) Opens app; (2) App calls `GET /api/auth/refresh` with the Stored Token; (3) Token is expired — app silently calls `POST /api/auth/token` with Stored Credentials; (4) Returns a fresh Access Token; (5) Content loads normally.
- **Climax:** The app opens to content with no login screen visible - the token expiry was invisible.
- **Resolution:** New Access Token in memory; Stored Token updated on disk; session continues uninterrupted.
- **Edge case:** A manual prompt appears only if Stored Credentials are also invalid (e.g. Sam changed her Mealie password). The 48-hour token lifetime is a deliberate Mealie security choice; the app does not extend it.

**UJ-3. Passenger uses the Shopping List in the supermarket (no signal).**
- **Persona + context:** Sam, in a supermarket basement with no mobile data.
- **Entry state:** App was last synced at home; Shopping List is available in Local Store.
- **Path:** (1) Opens app offline; (2) Offline Indicator appears; (3) Opens Shopping List; (4) Checks off items; (5) Adds a forgotten item; (6) Exits the supermarket, signal restored; (7) Sync Queue flushes automatically in the background.
- **Climax:** All changes appear in the Mealie web interface without her taking any action.
- **Resolution:** Local Store and server are reconciled; Offline Indicator disappears; Sync Status Badges clear.
- **Edge case:** If the same item was edited on two devices while offline, the change with the more recent `updated_at` timestamp wins; no data is silently dropped.

**UJ-4. Operator browses recipes to plan the week. [POST-V1]**
- **Persona + context:** Alex, at home on Wi-Fi, planning meals for the week.
- **Entry state:** Authenticated and connected.
- **Path:** (1) Opens app; (2) Navigates to Recipes; (3) Browses or searches list; (4) Taps a recipe to view details.
- **Climax:** Full recipe detail is readable - title, ingredients, instructions, yield, timing.
- **Resolution:** Alex decides what to cook. No further in-app action required in v1.
- **Edge case:** If signal drops while on recipe detail, already-loaded content remains visible; navigating to a new recipe fails gracefully with a non-crashing error state.
- **Note:** Recipe browsing is deferred to v2. This journey is documented here for continuity with the v2 product scope.

---

## 3. Glossary

- **Mealie Instance** - A self-hosted Mealie server identified by a URL. In v1, one Mealie Instance is configured per app install.
- **Operator** - A user who administers the Mealie Instance they connect to.
- **Passenger** - A user who connects to a Mealie Instance they do not administer.
- **Access Token** - A JWT issued by `POST /api/auth/token` or `GET /api/auth/refresh`. Expires after TOKEN_TIME (default 48 hours; configurable per Mealie instance via the `TOKEN_TIME` environment variable, range 1-9,600 hours). Used as the Bearer token on all authenticated API calls. Held in memory during a session.
- **Stored Token** - A persisted copy of the most recently issued Access Token, written to DataStore backed by Android Keystore encryption. Used on app launch to call `GET /api/auth/refresh` and obtain a fresh Access Token without user interaction. Expires when the Access Token it represents expires.
- **Stored Credentials** - The username and password entered during setup, persisted using DataStore backed by Android Keystore encryption. Used only as a silent fallback when the Stored Token is expired. Never logged or transmitted except to `POST /api/auth/token`.
- **Local Store** - The on-device Room database. Single source of truth for all UI reads.
- **Sync Queue** - The set of local mutations (creates, updates, deletes) not yet confirmed by the server. Persisted in the Local Store.
- **Shopping List** - A named collection of Shopping List Items scoped to a Mealie Household. Corresponds to `/api/households/shopping/lists/{id}`.
- **Shopping List Item** - A single row within a Shopping List. Carries a label, checked state, optional quantity, unit, and an `updated_at` timestamp used for conflict resolution.
- **Household** - The Mealie organizational unit containing users, recipes, and Shopping Lists. Retrieved from `/api/households/self`.
- **Offline Indicator** - The persistent visual element shown on all screens when the device cannot reach the Mealie Instance.
- **Sync Status Badge** - A per-item visual indicator shown when a Shopping List Item has a mutation in the Sync Queue not yet confirmed by the server.
- **Sync Network Mode** - A user-configurable setting controlling which network types trigger background sync: All Networks (default) or Wi-Fi Only.
- **TOKEN_TIME** - The Mealie server-side configuration parameter controlling Access Token lifetime. Default 48 hours; set via environment variable on the Mealie Instance. The app has no visibility into this value and must handle variable token lifetimes gracefully.

---

## 4. Features

### 4.1 Server Setup and First Launch

**Description:** On first launch, the app presents a setup screen prompting for a Mealie Instance URL and credentials. This is the only time credentials are required; all subsequent launches authenticate silently using the Stored Token. The setup screen is also reachable from Settings for re-configuration. Realizes UJ-1.

**Functional Requirements:**

#### FR-1: First-launch setup screen

The app presents a setup screen on first launch when no Mealie Instance URL is stored. The screen is not shown on subsequent launches when a valid Stored Token exists.

**Consequences:**
- Setup screen appears exactly once per fresh install.
- Setup screen is accessible from Settings for re-configuration at any time.
- Completing setup stores the server URL, Stored Token, and Stored Credentials; the setup screen is never shown again unless the user explicitly triggers re-configuration.

#### FR-2: Server URL validation

The user enters a server URL. The app probes the URL before accepting it.

**Consequences:**
- If the user enters an HTTP (non-TLS) URL, the app displays a security warning before proceeding: credentials will be transmitted over an unencrypted connection. The user must explicitly confirm to continue. The confirmation is remembered per server URL - the warning is not repeated on subsequent launches for the same URL.
- If the URL is unreachable, an inline error indicates a connectivity failure; the user remains on the setup screen.
- If the URL responds but is not a Mealie Instance, an inline error distinguishes this from a connectivity failure.
- Both HTTPS and HTTP URLs are accepted after confirmation.
- Trailing slashes in the entered URL are normalized automatically.

#### FR-3: Credential entry and initial authentication

After a valid server URL is confirmed, the user enters a username and password. The app calls `POST /api/auth/token`, stores the returned Access Token as the Stored Token, and stores the Stored Credentials - all using DataStore backed by Android Keystore encryption.

**Consequences:**
- On success, the user is taken directly to the main screen; the setup screen is dismissed permanently.
- On failure (invalid credentials), an inline error is shown; the password field is cleared; the server URL field is retained.
- Stored Credentials (username and password) are persisted encrypted with `setUnlockedDeviceRequired(true)`.
- The in-memory Access Token and the Stored Token are the same JWT; the in-memory copy is used for API calls, the on-disk copy is used for session resumption on next launch.

**Feature-specific NFRs:**
- The password field must use Android's standard password input type (masked, excluded from clipboard history suggestions).

---

### 4.2 Authentication

**Description:** After setup, authentication is fully automatic. On launch, the Stored Token is used to call `GET /api/auth/refresh`, yielding a fresh Access Token. An OkHttp Authenticator intercepts 401 responses and refreshes silently. Only if both the Stored Token and Stored Credentials fail does the app surface a login prompt. Realizes UJ-2.

**Functional Requirements:**

#### FR-4: Silent token refresh on app launch

On launch (post-setup), the app calls `GET /api/auth/refresh` using the Stored Token before making other API calls.

**Consequences:**
- On success, the app proceeds to the main screen without user interaction. The new Access Token replaces the Stored Token on disk.
- On failure while the device is online, the app silently attempts re-authentication using Stored Credentials via `POST /api/auth/token`. If that succeeds, the new Access Token is stored and the app proceeds normally.
- If Stored Credentials also fail (e.g. the user changed their Mealie password), a non-disruptive re-authentication prompt appears (credentials only; server URL is pre-filled, not editable).
- On failure while the device is offline, the app proceeds in offline-only mode; Local Store content is accessible without re-authentication.

#### FR-5: Silent 401 interception

An OkHttp Authenticator intercepts HTTP 401 responses, calls `GET /api/auth/refresh`, and retries the original request with the new Access Token.

**Consequences:**
- No 401 error reaches the UI layer when the Stored Token is valid.
- If `GET /api/auth/refresh` fails, the Authenticator attempts `POST /api/auth/token` with Stored Credentials before surfacing an error.
- If both attempts fail, the Authenticator stops and surfaces a re-authentication prompt exactly once - no retry loop.
- At most one concurrent refresh request is issued, regardless of how many simultaneous requests trigger a 401.

#### FR-6: Re-authentication without data loss

If both the Stored Token and Stored Credentials are invalid, the app shows a re-authentication screen with the server URL pre-populated.

**Consequences:**
- This prompt is only shown when Stored Credentials are also invalid - not on routine token expiry.
- Local Store data (Shopping Lists, cached content) is not cleared on re-authentication.
- After successful re-authentication, the new Access Token is stored and the Sync Queue is flushed.
- The re-authentication screen is visually distinct from the first-launch setup screen (no server URL field).

**Feature-specific NFRs:**
- Stored Token and Stored Credentials must be stored using DataStore backed by Android Keystore encryption with `setUnlockedDeviceRequired(true)`.
- Neither the Access Token nor the password must appear in logcat, crash reports, or any application log.

---

### 4.3 Recipe Browsing (Online) [POST-V1]

> **This feature is out of scope for v1.** Recipe browsing is deferred to v2. FR-7, FR-8, and FR-9 are retained here as the v2 requirements baseline. Nothing in this section is built in v1.

**Description:** When connected, the user can browse the full recipe library of their Mealie Household and view recipe details. The list supports search. Offline recipe caching is also post-v1. Realizes UJ-4.

**Functional Requirements:**

#### FR-7: Recipe list

The user can view a paginated list of recipes from their Mealie Household while connected, using `GET /api/recipes` with `page` and `perPage` parameters.

**Consequences:**
- Each list row shows at minimum: recipe title and thumbnail image (if available).
- Additional recipes load on scroll (pagination via `page`/`perPage`).
- If the device goes offline while the list is visible, already-loaded rows remain visible; further pagination attempts show an inline error message ("No connection - previously loaded recipes are shown") and do not crash.

#### FR-8: Recipe search

The user can search recipes by keyword while connected, using the `queryFilter` parameter on `GET /api/recipes`.

**Consequences:**
- Results update as the user types (debounced; minimum 2 characters before a request fires).
- Clearing the search restores the full list.

#### FR-9: Recipe detail view

The user can tap a recipe in the list to view its full detail via `GET /api/recipes/{slug}`.

**Consequences:**
- All fields present in the Mealie web interface for a recipe are exposed: title, description, ingredients (with quantities and units), instructions, yield, prep time, cook time.
- Images load asynchronously; text content is readable before images arrive.
- Within a session, navigating away from and back to the same recipe uses a cached response - no redundant re-fetch.

**Feature-specific NFRs:**
- Recipe list initial load must display first results within 2 seconds on a typical home broadband connection.

**Notes:**
- "Add recipe ingredients to Shopping List" is explicitly post-v1. [NOTE FOR PM: this is the natural next step from UJ-4 and is the highest-value v2 recipe feature.]

---

### 4.4 Shopping List

**Description:** Full read and write access to Mealie Shopping Lists, backed by the Local Store so all operations work without connectivity. Changes made offline are queued and synced automatically on reconnect. This is the primary v1 value feature. Realizes UJ-3.

**Functional Requirements:**

#### FR-10: Shopping list roster

The user can view all Shopping Lists available to their Household, with pagination support.

**Consequences:**
- All lists returned by `GET /api/households/shopping/lists` are shown.
- The roster is served from Local Store on open; a background refresh updates it when connected.
- Roster supports pagination; additional lists load on scroll.

#### FR-11: Shopping list item view (offline-capable)

The user can open a Shopping List and view all its items, whether connected or not.

**Consequences:**
- Items are read from Local Store; no network call is required.
- Items are grouped: unchecked at top, checked below.
- Each item shows label, quantity, and unit where available.

#### FR-12: Check / uncheck item (offline-capable)

The user can check or uncheck a Shopping List Item. The change is applied immediately to the Local Store and added to the Sync Queue.

**Consequences:**
- UI reflects the change instantly (optimistic update).
- If connected, the Sync Queue is flushed within 5 seconds of the change.
- If offline, the item shows a Sync Status Badge until the server confirms the change.
- Checking an item moves it to the checked section; it is not deleted.

#### FR-13: Add item (offline-capable)

The user can add a new Shopping List Item by entering a label. Quantity and unit are optional.

**Consequences:**
- New item appears at the top of the unchecked section immediately.
- New item carries a Sync Status Badge until server confirmation.
- When connected, item is created via `POST /api/households/shopping/items`.

#### FR-14: Delete item (offline-capable)

The user can delete a Shopping List Item.

**Consequences:**
- Item is removed from the UI immediately.
- Deletion is queued and sent via `DELETE /api/households/shopping/items` (bulk endpoint) when connected.
- If the item was created offline and deleted before any sync, no server call is issued.

#### FR-15: Sync Queue flush on reconnect

When connectivity is restored and the active Sync Network Mode permits it (see FR-18), all pending Sync Queue entries are sent to the server via a WorkManager job.

**Consequences:**
- WorkManager network constraint reflects the active Sync Network Mode: `CONNECTED` for All Networks, `UNMETERED` for Wi-Fi Only.
- Before syncing, the job performs a lightweight connectivity probe against the Mealie Instance (`GET /api/app/about`). If the probe fails, the job exits immediately and WorkManager retries later.
- Conflict resolution uses the `updated_at` field on Shopping List Items. When a local mutation conflicts with a server-side change made while the device was offline, the change with the more recent `updated_at` wins (last-write-wins). No data is silently discarded - losing-side values are logged locally for potential post-v1 conflict surfacing.
- Sync operations are idempotent: duplicate delivery of the same mutation has no observable side effect.
- After successful sync, Sync Status Badges are cleared.
- Transient sync failures are retried with exponential backoff; individual retry attempts are not surfaced to the user.
- Unrecoverable server errors for a specific item (e.g. item deleted by another user) are surfaced once after sync completes; the local item is reconciled to server state. The user can dismiss the error notification; no manual recovery step is required.

**Feature-specific NFRs:**
- All Local Store write operations (check, add, delete) must complete within 100 ms.
- The Sync Queue must survive app kill, crash, and device restart (stored in Room, not in memory).

---

### 4.5 Offline Indicator

**Description:** A persistent, unobtrusive global element communicates connectivity state on every screen. Per-item Sync Status Badges communicate pending mutations on the Shopping List. Users always know what is synced and what is pending, but are never interrupted by it. Realizes UJ-3.

**Functional Requirements:**

#### FR-16: Global Offline Indicator

When the device cannot reach the Mealie Instance, the Offline Indicator is visible on all screens.

**Consequences:**
- Indicator appears within 3 seconds of connectivity loss to the Mealie Instance.
- Indicator disappears within 3 seconds of connectivity restoration.
- Indicator is non-blocking: it does not cover content or require dismissal.
- Exact visual treatment (chip, banner, icon) is a UX design decision.

#### FR-17: Sync Status Badge

Any Shopping List Item with a pending unconfirmed change in the Sync Queue displays a Sync Status Badge.

**Consequences:**
- Badge is visible on the item row without disrupting the primary label.
- Badge clears automatically when the server confirms the change.
- Badge visually distinguishes "queued / syncing" from "sync error" states.
- Tapping a sync-error badge offers the user two options: retry the sync for that item, or discard the local change and reconcile to server state.

**Feature-specific NFRs:**
- Offline Indicator state reflects API reachability, not merely device network state. A device on WiFi that cannot reach the Mealie Instance shows the Offline Indicator.

---

### 4.6 App Settings

**Description:** A Settings screen reachable from the main navigation. Covers server re-configuration, sync behaviour, and in-app support access.

**Functional Requirements:**

#### FR-18: Sync Network Mode setting

The user can configure which network types trigger background sync.

**Consequences:**
- Two options: **All Networks** (default) and **Wi-Fi Only**.
- All Networks applies a `CONNECTED` WorkManager constraint; Wi-Fi Only applies `UNMETERED`.
- The active mode is persisted and survives app restarts.
- Default is All Networks - the primary use case (supermarket) requires sync over mobile data.
- Changing the setting takes effect on the next sync cycle; any pending WorkManager jobs are rescheduled with the new constraint.

**Notes:**
- Wi-Fi Only mode is intended for users whose Mealie Instance is not publicly reachable - sync is only meaningful when the device is on the home network. The connectivity probe in FR-15 provides additional protection against wasted sync attempts regardless of this setting.

#### FR-19: In-app bug reporting (v1)

The user can navigate to a "Report a Bug" option in Settings. Tapping it opens the project's GitHub Issues page in the device browser, with a new-issue URL pre-filled with device context in the issue body template.

**Consequences:**
- No third-party SDK is required; the feature is a single URL intent.
- Device context included in the pre-filled template: Android version, app version name and code, device manufacturer and model.
- The user sees the pre-filled issue draft in their browser before submitting; nothing is sent automatically.
- If no browser is available, the URL is copyable from an error state.

**Out of Scope (v1):**
- Automatic crash capture and local log collection - deferred to post-v1 (see §6.2).

**Feature-specific NFRs:**
- No data is transmitted without explicit user action.

#### FR-20: Credential update in Settings

The user can update the server URL and/or credentials from Settings without reinstalling or triggering a full first-launch setup flow.

**Consequences:**
- The app re-validates the server URL using the same probe as FR-2 before accepting changes.
- If the user enters an HTTP URL, the one-time security warning (FR-2) is shown again regardless of prior confirmation.
- On successful re-authentication with new credentials, the Stored Token and Stored Credentials are replaced; Local Store data and the Sync Queue are preserved.
- If the server URL changes, the Local Store is cleared and the Sync Queue is discarded - data from a different Mealie Instance cannot be merged.
- If re-validation or re-authentication fails, the previous server URL and credentials remain active; no data is lost.

---

## 5. Non-Goals (Explicit)

- **No recipe browsing (v1)** - all recipe access (online and offline) is deferred to v2. The app ships as a shopping list tool; Mealie is the sync backend, not the content focus.
- **No recipe URL import (v1)** - sharing a URL from another app to trigger recipe import is v2.
- **No QR code / setup link onboarding (v1)** - first-launch setup requires manual URL and credential entry.
- **No multi-server support (v1)** - one Mealie Instance per app install; server switching is post-v1.
- **No multi-Household support (v1)** - the app always uses `/api/households/self`; a Household picker is post-v1.
- **No "add recipe to Shopping List" action (v1)** - recipe browsing and Shopping List are not yet connected.
- **No recipe creation or editing** - the app is a consumer of Mealie content, not an authoring tool.
- **No meal planning** - out of scope for v1 and not currently planned.
- **No push notifications** - the app does not notify users of Shopping List changes made by other household members in real time.
- **No iOS support** - native Android only.
- **No server administration** - the app does not expose Mealie server configuration.
- **No API token authentication** - API tokens require Mealie admin privilege and are unsuitable for Passengers; the standard JWT flow is the only supported authentication method.
- **No analytics, telemetry, or automatic crash reporting** - no data is ever transmitted to any third party without explicit user action.

---

## 6. MVP Scope

### 6.1 In Scope

- First-launch setup: server URL entry and validation (with HTTP security warning), credential entry and initial authentication (FR-1, FR-2, FR-3)
- Persistent authentication with silent token refresh and 401 interception (FR-4, FR-5, FR-6)
- Shopping List roster (paginated), item view, check/uncheck, add, and delete - all offline-capable (FR-10 through FR-14)
- Background Sync Queue flush via WorkManager with connectivity probe and `updated_at` conflict resolution (FR-15)
- Offline Indicator and per-item Sync Status Badges (FR-16, FR-17)
- App Settings: Sync Network Mode toggle, in-app bug reporting, credential and server URL update (FR-18, FR-19, FR-20)
- Monochrome adaptive icon (Android 13+ / API 33 `android:monochromeIcon`) using a derivative of the Mealie icon (AGPL-3.0)

### 6.2 Out of Scope for MVP

- Recipe browsing (FR-7, FR-8, FR-9) - v2; all recipe access is deferred. v1 ships as a focused shopping list tool.
- Offline recipe browsing - v2; requires full local recipe library caching.
- Recipe URL import via Android Share Intent - v2
- QR code / setup link Passenger onboarding - v2
- "Add recipe ingredients to Shopping List" action - v2; the natural bridge between the two main features
- Multi-server profile switching - post-v2
- Multi-Household picker - post-v1; add a selector when the user belongs to more than one Household on a Mealie Instance
- User-resolvable sync conflict surfacing - post-v1; losing-side values are logged locally in v1 to enable this without data loss
- In-app crash capture with local log and share-sheet report - post-v1; complements FR-19's GitHub Issues link once the app has a broader user base
- F-Droid / Play Store distribution requirements (privacy policy, signing config, no closed-source SDKs) - required before any public store listing
- Real-time multi-device sync (WebSocket / push) - deferred indefinitely; WorkManager pull-on-reconnect model is sufficient

---

## 7. Success Metrics

**Primary**
- **SM-1:** Silent re-authentication on token expiry - opening the app after the Stored Token has expired displays main content without showing a login screen, using silent credential fallback. Target: 100% of launches on a device with valid Stored Credentials. Validates FR-4, FR-5.
- **SM-2:** Shopping List accessible offline - opening a previously synced Shopping List with no connectivity succeeds. Target: 100% on any device where the list was synced at least once. Validates FR-11, FR-15.

**Secondary**
- **SM-3:** Setup completion rate - users who open the app and complete server setup reach the main screen. Target: >90%. Validates FR-1 through FR-3.
- **SM-4:** Sync queue drain time - all Sync Queue entries are confirmed by the server within 30 seconds of connectivity restoration. Validates FR-15.
- **SM-5:** Household adoption - household members use the native app for Shopping List access instead of the Mealie PWA. Validates overall product value; measured by observation.

**Counter-metrics (do not optimize)**
- **SM-C1:** Background sync battery impact - WorkManager sync jobs must not appear as notable battery consumers in Android's battery stats. Counterbalances SM-4: do not increase sync frequency or reduce batching to chase drain time.
- **SM-C2:** Sync Queue integrity - do not discard pending Sync Queue items that fail one or more retry attempts. A pending item must be retried until confirmed or explicitly resolved, never silently dropped. Counterbalances SM-4.

---

## 8. Assumptions Index

All assumptions have been resolved. No open assumptions remain.

---

## Cross-Cutting NFRs

### Performance
- Cold start to interactive state: < 3 seconds on a mid-range Android device (API 26+).
- All Local Store read operations: < 100 ms.

### Security
- Stored Token and Stored Credentials persisted using DataStore backed by Android Keystore encryption with `setUnlockedDeviceRequired(true)` - inaccessible while the device is locked.
- No app-level biometric or PIN gate required; the device lock screen provides sufficient protection for a recipe/shopping list app. Adding a biometric prompt on every open would directly harm the primary use case (supermarket, quick access). Deliberate trade-off.
- Access Token held in memory only during a session; the Stored Token is the on-disk equivalent.
- Password must not appear in logcat, crash reports, or any application log.
- No sensitive data written to external storage.
- **Hard constraint:** No analytics SDK, telemetry library, or automatic crash-reporting SDK is included in the app. No data is transmitted to any third party without an explicit user action.

### Reliability
- Sync Queue is durable: pending items survive app kill, crash, and device restart.
- Sync operations are idempotent: duplicate delivery of the same mutation has no observable side effect.
- The app must not crash on: network loss mid-request, unexpected server response codes, or an empty Local Store on first offline launch.
- TOKEN_TIME varies per Mealie instance (default 48 hours, range 1-9,600 hours). The app must handle any token lifetime gracefully without hardcoding assumptions about expiry windows.

### Compatibility
- Minimum: Android API 26 (Android 8.0 Oreo). Revisit if a specific v2 feature requires a higher floor.
- Target SDK: current Android stable at time of first release.
- Architectures: arm64-v8a, armeabi-v7a, x86_64.
- Monochrome adaptive icon: supported from API 33 (`android:monochromeIcon`); graceful fallback on earlier API levels.

### Observability
- Debug builds only: verbose API call logging to logcat (token values and passwords excluded).
- No analytics, telemetry, or crash reporting sent to third parties (hard constraint - see Security above).

---

## Platform

- **Platform:** Native Android - Kotlin + Jetpack Compose.
- **API floor:** API 26 (Android 8.0). Revisit per feature if needed.
- **Distribution (v1):** Direct APK. F-Droid and/or Play Store in later iterations (requires privacy policy, compliant signing config, no closed-source SDKs).
- **Reference architecture:** MVVM + Repository pattern. Room (Local Store), WorkManager (background sync), OkHttp with Authenticator interceptor (network layer), DataStore backed by Android Keystore encryption (token and credential storage), Retrofit or Ktor Client (API layer).
- **Icon:** Derivative of the Mealie icon (AGPL-3.0); monochrome variant included for Android 13+ adaptive icon theming.

---

## Information Architecture (v1)

No bottom navigation in v1. The Shopping List is the main screen. Settings is accessible via an icon in the TopAppBar.

- **Shopping List** (main screen): Shopping List roster → Shopping List detail (with Planning and Shopping modes)
- **Settings** (TopAppBar icon): Sync Network Mode, server re-configuration, credentials update, bug reporting, app version

No deep-link or Share Intent handling in v1 (recipe URL import is post-v1).
No NavigationBar or NavigationRail in v1 — these are introduced in v2 when recipe browsing adds a second primary destination.
