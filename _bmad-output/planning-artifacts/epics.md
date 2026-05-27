---
stepsCompleted: [1, 2, 3]
inputDocuments:
  - _bmad-output/planning-artifacts/prds/prd-mealie-android-2026-05-21/prd.md
  - _bmad-output/planning-artifacts/architecture.md
  - _bmad-output/planning-artifacts/ux-design-specification.md
---

# mealie-android-unofficial - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for mealie-android-unofficial, decomposing the requirements from the PRD, UX Design, and Architecture requirements into implementable stories.

## Requirements Inventory

### Functional Requirements

FR-1: The app presents a setup screen on first launch when no Mealie Instance URL is stored; the setup screen is not shown on subsequent launches when a valid Stored Token exists; the setup screen is accessible from Settings for re-configuration at any time.

FR-2: The user enters a server URL; the app probes the URL before accepting it; HTTP URLs show a one-time security warning per URL before proceeding; unreachable URLs show an inline connectivity error; non-Mealie URLs are distinguished from connectivity failures; trailing slashes are normalized automatically; both HTTPS and HTTP URLs are accepted after confirmation.

FR-3: After a valid server URL is confirmed, the user enters a username and password; the app calls POST /api/auth/token, stores the returned Access Token as the Stored Token, and stores the Stored Credentials via DataStore + Android Keystore; on success the user goes directly to the main screen; on failure the password field is cleared and the server URL is retained; the password field uses Android's standard masked input type.

FR-4: On launch (post-setup), the app calls GET /api/auth/refresh using the Stored Token before making other API calls; on success the new Access Token replaces the Stored Token; on 401 the app silently attempts POST /api/auth/token with Stored Credentials; if Stored Credentials also fail a re-authentication prompt appears; if offline the app proceeds in offline-only mode using Local Store content.

FR-5: An OkHttp Authenticator intercepts HTTP 401 responses, calls GET /api/auth/refresh, and retries the original request with the new Access Token; at most one concurrent refresh is issued regardless of simultaneous 401 triggers; if refresh fails the Authenticator attempts POST /api/auth/token with Stored Credentials; if both fail a re-authentication prompt is surfaced exactly once, no retry loop.

FR-6: If both the Stored Token and Stored Credentials are invalid, the app shows a re-authentication screen with the server URL pre-populated; this prompt is only shown when Stored Credentials are invalid, not on routine token expiry; Local Store data is not cleared on re-authentication; after successful re-authentication the Sync Queue is flushed; the re-authentication screen has no server URL field.

FR-10: The user can view all Shopping Lists available to their Household (GET /api/households/shopping/lists) with pagination; the roster is served from Local Store on open and refreshed in the background when connected; additional lists load on scroll.

FR-11: The user can open a Shopping List and view all its items whether connected or not; items are read from Local Store; items are grouped (unchecked at top, checked below); each item shows label, quantity, and unit where available.

FR-12: The user can check or uncheck a Shopping List Item; the change is applied immediately to the Local Store and added to the Sync Queue (optimistic update); if connected the Sync Queue is flushed within 5 seconds; if offline the item shows a Sync Status Badge until server confirmation; checking moves the item to the checked section without deleting it.

FR-13: The user can add a new Shopping List Item by entering a label (quantity and unit optional); the new item appears at the top of the unchecked section immediately with a Sync Status Badge until server confirmation; when connected the item is created via POST /api/households/shopping/items.

FR-14: The user can delete a Shopping List Item; deletion is applied to the UI immediately; deletion is queued and sent via DELETE /api/households/shopping/items (bulk endpoint) when connected; if the item was created offline and deleted before any sync, no server call is issued.

FR-15: When connectivity is restored and the active Sync Network Mode permits it, all pending Sync Queue entries are sent via a WorkManager job; WorkManager network constraint reflects the active Sync Network Mode (CONNECTED or UNMETERED); before syncing a lightweight probe against GET /api/app/about is performed; conflict resolution uses last-write-wins via updated_at; sync operations are idempotent; after successful sync Sync Status Badges are cleared; transient failures retry with exponential backoff; unrecoverable server errors for specific items are surfaced once after sync completes and reconciled to server state.

FR-16: When the device cannot reach the Mealie Instance, the Offline Indicator is visible on all screens; it appears within 3 seconds of connectivity loss and disappears within 3 seconds of restoration; it is non-blocking and does not cover content or require dismissal; Offline Indicator reflects API reachability, not device network state.

FR-17: Any Shopping List Item with a pending unconfirmed change in the Sync Queue displays a Sync Status Badge; the badge is visible on the item row without disrupting the primary label; the badge clears automatically when the server confirms the change; the badge visually distinguishes "queued/syncing" from "sync error" states; tapping a sync-error badge offers two options: retry or discard and reconcile to server state.

FR-18: The user can configure which network types trigger background sync - two options: All Networks (default, CONNECTED constraint) and Wi-Fi Only (UNMETERED constraint); the active mode is persisted and survives app restarts; changing the setting reschedules pending WorkManager jobs with the new constraint.

FR-19: The user can navigate to a "Report a Bug" option in Settings; tapping it opens the project's GitHub Issues page in the device browser with device context pre-filled (Android version, app version name and code, device manufacturer and model); no third-party SDK required; nothing is sent automatically; if no browser is available the URL is copyable.

FR-20: The user can update the server URL and/or credentials from Settings without reinstalling; the app re-validates the server URL using the same probe as FR-2; HTTP URLs trigger the one-time security warning again; on successful re-authentication the Stored Token and Stored Credentials are replaced and Local Store data and Sync Queue are preserved; if the server URL changes the Local Store is cleared and the Sync Queue is discarded; if re-validation or re-authentication fails the previous configuration remains active.

### NonFunctional Requirements

NFR-1 (Performance): Cold start to interactive state must be less than 3 seconds on a mid-range Android device (API 26+).

NFR-2 (Performance): All Local Store read operations must complete within 100 ms.

NFR-3 (Performance): All Local Store write operations (check, add, delete, Sync Queue writes) must complete within 100 ms.

NFR-4 (Performance): Sync Queue drain time - all entries confirmed within 30 seconds of connectivity restoration.

NFR-5 (Performance): Offline Indicator must appear and disappear within 3 seconds of connectivity state change to/from the Mealie Instance.

NFR-6 (Security): Stored Token and Stored Credentials persisted using DataStore backed by Android Keystore encryption (`datastore-tink` with `AeadSerializer`). Note: `setUnlockedDeviceRequired(true)` is not achievable via `AndroidKeysetManager`; encryption at rest covers the actual threat model.

NFR-7 (Security): Access Token held in memory only during a session; the Stored Token is the on-disk equivalent.

NFR-8 (Security): Neither the Access Token nor the password must appear in logcat, crash reports, or any application log.

NFR-9 (Security): No sensitive data written to external storage.

NFR-10 (Security - Hard Constraint): No analytics SDK, telemetry library, or automatic crash-reporting SDK is included in the app. No data is transmitted to any third party without an explicit user action.

NFR-11 (Security): Password field must use Android's standard password input type (masked, excluded from clipboard history suggestions).

NFR-12 (Reliability): Sync Queue is durable - pending items survive app kill, crash, and device restart (stored in Room, not in memory).

NFR-13 (Reliability): Sync operations are idempotent - duplicate delivery of the same mutation has no observable side effect.

NFR-14 (Reliability): The app must not crash on network loss mid-request, unexpected server response codes, or an empty Local Store on first offline launch.

NFR-15 (Reliability): TOKEN_TIME varies per Mealie instance (default 48 hours, range 1-9,600 hours). The app must handle any token lifetime gracefully without hardcoding assumptions about expiry windows.

NFR-16 (Compatibility): Minimum Android API 26 (Android 8.0 Oreo); target SDK: current stable at time of first release; architectures: arm64-v8a, armeabi-v7a, x86_64.

NFR-17 (Compatibility): Monochrome adaptive icon supported from API 33 (`android:monochromeIcon`) with graceful fallback on earlier API levels.

NFR-18 (Observability): Debug builds only: verbose API call logging to logcat with `Authorization` header redacted; no logging in release builds.

NFR-19 (Accessibility): WCAG AA minimum contrast (4.5:1 body text, 3:1 large text); all interactive element tap targets minimum 48dp x 48dp; TalkBack support; Reduce Motion support.

NFR-20 (Localization): All user-facing strings externalized to Android string resources in :core:ui. v1 ships with English (default) and German. App follows system locale; no in-app language toggle. Additional languages contributable via locale-qualified resource directories without code changes.

### Additional Requirements

- **Starter/Template**: Project created via Android Studio → New Project → Empty Activity (Min SDK: API 26, Language: Kotlin, Build config: Kotlin DSL). No Android CLI scaffold - the 7-module structure is established manually in the first implementation stories.

- **7-Module Structure**: `:app` (thin shell: MainActivity, NavHost, Koin wiring), `:feature:auth` (setup screen, re-auth screen), `:feature:shopping` (shopping list + settings screens), `:core:data` (Room, entities, DAOs, repositories, domain Use Cases), `:core:network` (OkHttp client, Retrofit, API services, Authenticator, ConnectivityMonitor), `:core:sync` (WorkManager workers, sync queue logic), `:core:ui` (shared Compose components: OfflineIndicator, SyncStatusBadge, MealieTheme, NavigationManager). Gradle dependency declarations enforce module boundaries at compile time.

- **Dependency Injection**: Koin (not Hilt). One Koin module per Gradle module, all wired in `:app`. Kotlin DSL, runtime DI graph.

- **Dependency Management**: `gradle/libs.versions.toml` version catalog with type-safe accessors as single source of truth for all dependency versions.

- **JSON Serialization**: Kotlinx Serialization (`@Serializable` on all API model classes); integrated with Retrofit 3.0.x via `retrofit2-kotlinx-serialization-converter`.

- **DataStore Split**: Two DataStore instances - (1) encrypted via `AeadSerializer`: token store + credentials store; (2) regular: shopping mode state + timestamp, per-list sort preferences, Sync Network Mode, HTTP warning confirmation.

- **Encryption**: `androidx.datastore:datastore-tink` with `AeadSerializer` (AES-256-GCM, Android Keystore-backed master key). The deprecated `security-crypto`/`MasterKey` approach is NOT used.

- **Error Propagation**: Custom sealed `ApiResult<T>` in `:core:network` with `Success`, `NetworkError`, `AuthError`, and `HttpError` variants. Repositories unwrap to domain-level sealed types for ViewModels. 401 is intercepted by OkHttp `Authenticator` before reaching the Repository layer.

- **Navigation**: Type-safe Navigation Compose 2.8+ with `@Serializable` route objects. `NavHost` in `:app`; each feature module exposes `NavGraphBuilder.xGraph()` extension. Cross-feature navigation via `NavigationManager` singleton in `:core:ui`.

- **UiState Pattern**: Auth/setup screens use sealed class (`Loading`, `AwaitingInput`, `Error`, `Success`). Shopping list screens use data class `ShoppingUiState` with nested `Overlay` sealed interface. All ViewModels expose `StateFlow<UiState>` collected via `collectAsStateWithLifecycle()`. One-shot events via `Channel<Event>`.

- **Testing Strategy**: JUnit 5 + MockK + Turbine for unit tests in `:core:*` and `:feature:*` modules; JUnit 4 + `ui-test-junit4` for androidTest in `:app`. Room in-memory database for Repository + DAO integration tests. `TestListenableWorkerBuilder` for WorkManager tests (never mock WorkManager directly). MockWebServer + JSON fixtures for API contract tests.

- **CI/CD**: GitHub Actions with four jobs: `build` (assembleDebug on push/PR), `test` (all unit tests on push/PR), `lint` (ktlint + Detekt + Android Lint on push/PR), `release` (assembleRelease + APK signing on tag push `v*`, attached to GitHub Release).

- **Code Quality**: ktlint (formatting via `.editorconfig`), Detekt (`config/detekt/detekt.yml`), Android Lint (per-module `lint.xml`). All three run in CI on every PR.

- **ConnectivityMonitor**: Probes `GET /api/app/about` (no auth required) to determine Mealie instance reachability (not device network state). Uses `ConnectivityManager.NetworkCallback` for network state changes, then probes API. Exposes `StateFlow<ConnectivityState>` with states: `Online`, `Offline`, `SyncError`. An explicit probe must be issued in `MealieApplication.onCreate()` via `applicationScope` coroutine to set initial state (GAP-2 resolution).

- **TokenManager + Concurrent Refresh Safety**: `TokenManager` Koin singleton in `:core:network` holds a `Mutex` for coroutine-level concurrent refresh safety. OkHttp `Authenticator` is called serially by OkHttp (one call per host at a time) providing additional safety.

- **AppOrchestrator / Connectivity-to-Sync Wiring**: `ConnectivityMonitor` (`:core:network`) cannot call `SyncScheduler` (`:core:sync`) directly due to module boundary rules. `:app` must collect `ConnectivityState` flow via an `AppOrchestrator` class or directly in `MainActivity`, and call `SyncScheduler.scheduleSyncIfNeeded()` when state transitions to `Online` (GAP-1 resolution - must be implemented as specified).

- **Model Layer Separation**: Four distinct model types - `{Name}Dto` (`@Serializable`, `:core:network`), `{Name}` domain (plain data class, `:core:data`), `{Name}Entity` (`@Entity`, `:core:data`), `{Name}Ui` (`:feature:*`). Mapper extension functions co-located with source type.

- **Date Handling**: API response ISO-8601 String → Domain `kotlinx-datetime` `Instant?` → Room `Long?` (epoch ms) → UI formatted String. Never use `java.util.Date`.

- **Room Migration Strategy**: Auto-migrations by default (`@AutoMigration`) for additive changes; v1 schema designed additive-only (no column renames or removals planned).

- **Settings Screen**: `SettingsScreen.kt` + `SettingsViewModel.kt` added to `:feature:shopping/ui/` with a settings route in `ShoppingNavGraph.kt` (GAP-5 resolution).

- **Build Variants**: `debug` (applicationIdSuffix ".debug", Timber + HttpLoggingInterceptor, no obfuscation) and `release` (production applicationId, R8 with explicit ProGuard keep rules for Retrofit, Kotlinx Serialization, Room, Koin).

- **Single `MealieApplication.kt`**: `DebugApplication.kt` in `debug/` sourceset is redundant; Timber `DebugTree` and `HttpLoggingInterceptor` are gated on `BuildConfig.DEBUG` in `MealieApplication` (GAP-4 resolution).

- **Distribution**: v1 direct APK via GitHub Releases; architecture is F-Droid compatible from day one. Release keystore as GitHub Actions secrets (never committed to repo).

- **Localization**: All user-facing strings centralized in `:core:ui` module's `res/values/strings.xml` (English) and `res/values-de/strings.xml` (German). Feature modules reference strings via `stringResource(R.string.xyz)`. String names use snake_case prefixed by screen/component. No hardcoded user-facing text in Kotlin/Compose code. App follows system locale.

### UX Design Requirements

UX-DR1: Planning mode (default) and Shopping mode (execution) are two distinct visual states of the Shopping List screen. Shopping mode is implemented as a `ModalBottomSheet` overlay (`skipPartiallyExpanded = true`, `containerColor = surfaceContainerHigh`, built-in `DragHandle` pill). The planning view is physically beneath the sheet in Z-order - this encodes the spatial metaphor structurally. Entry: "Start Shopping" `FilledButton` (text-only, no icon). Exit: "Done Shopping" `FilledButton` in sheet top bar or swipe-down gesture (plus `BackHandler` for back gesture = dismiss overlay, not navigate back).

UX-DR2: Shopping mode state is persisted to DataStore as `ShoppingModePrefs(active: Boolean, lastInteractionAt: Long)`. Restore rule: restore Shopping mode only if `active == true` AND `(now - lastInteractionAt) < 12 hours`. `lastInteractionAt` updated on every user action within Shopping mode. On cold start, render splash until first DataStore emission resolves to avoid Planning-mode flash. `LaunchedEffect` on Shopping List screen entry reads value and calls `sheetState.show()` if active.

UX-DR3: Single `ShoppingListItem` composable handles both density modes via `shoppingMode: Boolean` parameter. Planning mode: 56dp row height, 16dp horizontal padding, `titleMedium` 16sp, `onSurface` color. Shopping mode: 72dp row height, 20dp horizontal padding, `titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)`, `onSurface` color. No separate components, no duplication.

UX-DR4: Checked item visual state: 40% opacity + strikethrough + animation to checked section, all within 200ms. No undo prompt for check-off - tapping again reverses. The item fades, does not disappear. All three visual cues (opacity + strikethrough + move) applied together for visibility under store lighting conditions.

UX-DR5: Quick-add text field present in both Planning and Shopping modes with identical styling: `surfaceContainerHighest` container, `ShapeDefaults.Large` shape (16dp corners), 56dp height, placeholder "Add an item...". Done IME action submits and clears; keyboard stays open. Validation on submission only. Adding a forgotten item in-store is a valid use case - do not require exiting Shopping mode.

UX-DR6: `OfflineIndicator` custom component: 24dp height strip (using `wrapContentHeight()` at 1.3x font scale) pinned below `TopAppBar`. States: Connected = hidden (no layout space); Offline = `tertiaryContainer` background, `onTertiaryContainer` color, `Icons.Outlined.CloudOff` 16dp + `labelSmall` text "Working offline - changes will sync when connected"; Sync error = `errorContainer` background, `onErrorContainer` color, "Some changes couldn't sync". `AnimatedVisibility` with vertical slide 200ms; instant when `LocalReduceMotion.current` is true. `liveRegion = LiveRegionMode.Polite` on semantics.

UX-DR7: `SyncStatusBadge` custom component: 8dp dot badge in trailing slot of `ShoppingListItem`. Amber (`tertiary`) = pending/syncing; Red (`error`) = failed. Hidden when synced. `contentDescription` = "Sync pending" or "Sync error". Not interactive. Aggregate sync state surfaced at list level (via `OfflineIndicator` strip), not per-item during normal operation. Per-item badge only for items requiring explicit user action (sync error resolution).

UX-DR8: Material 3 color scheme seeded from `#E58325` (Mealie brand orange). Dynamic Color (`dynamicColorScheme`) on API 31+; static `lightColorScheme`/`darkColorScheme` fallback for API 26-30. Rule: never use `Color(0xFFE58325)` directly in code - always reference `MaterialTheme.colorScheme.primary`. Dark mode respects system setting; no in-app toggle. `surfaceContainerLow` for Planning mode list; `surfaceContainerHigh` for Shopping mode sheet container.

UX-DR9: Typography: `titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)` for Shopping mode item text. Quick-add field container uses `MaterialTheme.shapes.large` (16dp). Buttons use `CircleShape` (M3 pill default). No hardcoded hex colors or raw dp constants in component code - M3 tokens only. Sort chips use `ShapeDefaults.Small` (8dp).

UX-DR10: Shopping mode sort options via `FilterChip` row: Unsorted (server order), Alphabetical, By Category. Sort preference selected once before shopping, not ongoing. Drag-to-reorder via `SwipeToDismissBox` in Planning mode only (long-press or visible drag handle). Independent sort preferences per mode per list persisted as `ShoppingListSortPrefs(planningModeSortOrder: SortOrder, shoppingModeSortOrder: SortOrder)` in DataStore.

UX-DR11: Setup flow: all errors are inline adjacent to the field (no toasts, no dialogs for validation). Loading state: spinner on button only, not full-screen overlay. URL field: forgiving input normalization (bare IP with port, with or without scheme, trailing slash - all normalized silently before submission). Password cleared on auth failure; username retained. Two-screen setup: URL/connection screen first, credentials screen second.

UX-DR12: HTTP security warning is informational and inline (not a modal blocker). Framed for Sam: "Connecting over your local network - this is common for self-hosted setups." Not alarming. One-time per server URL (remembered in `AppPreferencesStore`); not repeated on subsequent launches for the same URL.

UX-DR13: TalkBack semantics: `ShoppingListItem` rows must have `role = Role.Checkbox` + `stateDescription = if (item.checked) "Checked" else "Unchecked"` + full `contentDescription` including label, quantity, unit. `OfflineIndicator` must have `liveRegion = LiveRegionMode.Polite`. All icon-only `IconButton` controls must have `contentDescription`. `ModalBottomSheet` focus trap is handled by M3 natively.

UX-DR14: All transitions gated via `LocalReduceMotion.current`. When enabled: `snap<Float>()` animation spec (instant). Affected: `OfflineIndicator` `AnimatedVisibility`, checked item opacity transition, Shopping mode sheet (M3 `ModalBottomSheet` respects `LocalReduceMotion` natively in Compose 1.3.0+).

UX-DR15: Responsive layout: `Modifier.widthIn(max = 600.dp)` on Shopping List content and Shopping mode sheet for `WindowWidthSizeClass.MEDIUM` and `EXPANDED`. No `NavigationBar` or `NavigationRail` in v1 - introduced in v2 when recipe browsing adds a second primary destination. `TopAppBar` + Settings `IconButton` pattern applies across all window size classes.

UX-DR16: Font scale validation at 1.0x and 1.3x system scale. Planning mode 56dp rows allow two-line wrapping at 1.3x (no truncation). `OfflineIndicator` uses `wrapContentHeight()` not fixed 24dp. Shopping mode 72dp rows accommodate 18sp Bold at 1.3x (~23sp effective) without overflow. Use `sp` for text only; `dp` for icon sizes, row heights, and padding (these must not scale with font size).

UX-DR17: `Snackbar` with `SnackbarHost` and 5-second undo action for item add and item delete operations. No `Snackbar` for errors (inline only). No undo prompt for check-off (second tap reverses). Confirmations/success states are silent - the result is the feedback.

UX-DR18: `MediumTopAppBar` on Shopping List (Planning mode) with Settings `IconButton`. `TopAppBar` (small) on Settings screen. No bottom `NavigationBar` in v1. Settings screen accessible via the `IconButton` in the `TopAppBar` only.

UX-DR19: `SwipeToDismissBox` swipe-to-delete enabled in Planning mode only; disabled in Shopping mode (ModalBottomSheet vertical drag disambiguation conflict + deletion is a Planning activity). Pull-to-refresh enabled in Planning mode when connected. Delete requires exiting Shopping mode first - accepted friction.

### FR Coverage Map

```
FR-1:  Epic 1 - First-launch setup screen routing
FR-2:  Epic 1 - Server URL validation + HTTP warning
FR-3:  Epic 1 - Credential entry + encrypted DataStore storage
FR-4:  Epic 1 - Silent token refresh on launch + offline fallback
FR-5:  Epic 1 - OkHttp Authenticator 401 interception (Mutex, concurrency AC)
FR-6:  Epic 1 - Re-authentication screen (last-resort prompt)
FR-20: Epic 1 - Credential/URL update from Settings (:feature:settings)
FR-10: Epic 2 Phase 1 - Shopping List roster from Local Store
FR-11: Epic 2 Phase 1 - Shopping List item view (offline-capable)
FR-12: Epic 2 Phase 1 - Check/uncheck with optimistic update + Sync Queue
FR-13: Epic 2 Phase 1 - Add item offline-capable
FR-14: Epic 2 Phase 1 - Delete item offline-capable
FR-15: Epic 2 Phase 2 - WorkManager Sync Queue flush + ConflictResolver
FR-16: Epic 2 Phase 2 - Global Offline Indicator (ConnectivityMonitor + API probe)
FR-17: Epic 2 Phase 2 - Per-item Sync Status Badge

Deferred (post-v1): FR-7, FR-8, FR-9 (Recipe Browsing), FR-18 (Sync Network Mode), FR-19 (Bug Reporting)
```

## Epic List

### Epic 1: Secure App Setup & Silent Authentication

Users can install the app, connect it to their Mealie instance, stay signed in automatically despite token expiry, and update their credentials from Settings if anything changes.

**FRs covered:** FR-1, FR-2, FR-3, FR-4, FR-5, FR-6, FR-20

**Modules:** `:app`, `:feature:auth`, `:feature:settings`, `:core:data`, `:core:network`, `:core:ui`

**Story structure (high-level):**
- Story 1a: Build wiring completion (libs.versions.toml, build.gradle.kts files, Koin modules, CI workflows) - checklist PR, CI green = done
- Story 1b: MealieTheme + NavigationManager (unit-testable components)
- Stories for setup flow (URL validation, HTTP warning, credentials, DataStore encryption)
- Stories for silent auth (TokenManager with Mutex + explicit concurrency AC, OkHttp Authenticator, launch flow)
- Story for re-auth screen (FR-6)
- Story for credential update in `:feature:settings` (FR-20)

**Implementation notes:**
- Scaffold story split: wiring (no tests, CI green = done) vs. testable components (NavigationManager has unit tests)
- TokenManager AC must include: "given N simultaneous 401s, refreshToken() called exactly once, all N requests retried"
- Clock injection in any time-dependent component from day one
- All stories from 1-5 onward: user-facing strings must use `stringResource(R.string.xyz)` from `:core:ui`; English and German entries required in acceptance criteria

### Epic 2: Offline-First Shopping List

Users can view and manage their household shopping list from anywhere - including in the supermarket with no signal. Changes sync automatically when connectivity returns, and sync state is always visible but never in the way.

**FRs covered:** FR-10, FR-11, FR-12, FR-13, FR-14, FR-15, FR-16, FR-17

**Modules:** `:feature:shopping`, `:core:data`, `:core:network`, `:core:sync`, `:core:ui`, `:app`

**Phase 1 - Shopping List Interaction ("does it feel right"):**
- Room schema (ShoppingListEntity, ShoppingItemEntity, SyncQueueEntity)
- ShoppingRepository + ShoppingService (Retrofit)
- ShoppingListScreen with Planning/Shopping mode (ModalBottomSheet, density profiles)
- Check/uncheck with optimistic update, add/delete with Snackbar undo
- Sort preferences per mode, swipe-to-delete (Planning only)
- Basic online sync (immediate flush when connected)

**Phase 2 - Offline Resilience ("does it work when the world breaks"):**
- WorkManager SyncWorker with connectivity probe and exponential backoff
- ConflictResolver as extracted pure function in `:core:sync` (unit-testable, not buried in worker)
- ConnectivityMonitor (NetworkCallback + API probe) + AppOrchestrator wiring in `:app`
- OfflineIndicator composable
- SyncStatusBadge composable
- ShoppingModePrefs 12-hour auto-reset (with injected Clock)

**Implementation notes:**
- ConflictResolver: pure function, JUnit 5 unit test in < 5ms, wired into SyncWorker separately
- SyncWorker lives in `:core:sync`; tested via TestListenableWorkerBuilder
- Clock injection for ShoppingModePrefs auto-reset
- Phase 1 is demoable independently (online-only); Phase 2 adds the offline promise
- All stories: user-facing strings must use `stringResource(R.string.xyz)` from `:core:ui`; English and German entries required in acceptance criteria

## Module Boundary Rules

```
:app                 depends on all modules
:feature:auth        depends on :core:data, :core:network, :core:ui
:feature:shopping    depends on :core:data, :core:network, :core:ui, :core:sync
:feature:settings    depends on :core:data, :core:network, :core:ui
:core:data           depends on :core:network
:core:sync           depends on :core:data, :core:network
:core:ui             no core dependencies
:core:network        no core dependencies (leaf module)
```

## Epic 1: Secure App Setup & Silent Authentication

### Story 1.1: Multi-Module Build Infrastructure and CI/CD

As a developer,
I want a properly structured multi-module Gradle project with automated testing and build infrastructure,
So that all subsequent feature stories can integrate cleanly and CI/CD gates code quality.

**Acceptance Criteria:**

**Given** the project scaffold is created via Android Studio
**When** story is started
**Then** `:app`, `:core:network`, `:core:data`, `:core:sync`, `:core:ui` modules are created with minimal `build.gradle.kts` files

**Given** all core modules are scaffolded
**When** Gradle sync completes
**Then** no circular dependencies exist and the build succeeds

**Given** `gradle/libs.versions.toml` is created with type-safe accessors
**When** dependency versions are updated
**Then** all modules reference versions through the catalog (e.g., `libs.kotlin.stdlib`)
**And** there is a single source of truth for all versions

**Given** the GitHub Actions workflow files are created
**When** a commit is pushed to main or a PR is opened
**Then** `build.yml` runs `./gradlew assembleDebug` and succeeds

**Given** unit test CI workflow is configured
**When** a commit is pushed
**Then** `test.yml` runs `./gradlew test` for all `:core:*` and `:feature:*` modules

**Given** lint CI workflow is configured
**When** a commit is pushed
**Then** `lint.yml` runs ktlint, Detekt, and Android Lint; all pass

**Given** a tag matching `v*` is pushed (e.g., `v1.0.0`)
**When** the tag is created
**Then** `release.yml` runs `./gradlew assembleRelease` and creates a GitHub Release with the signed APK attached

**Given** all GitHub Actions workflow files are in `.github/workflows/`
**When** the first PR is created
**Then** all four workflows trigger and pass (green CI)

---

### Story 1.2: App Theme, Navigation Shell, and Application Class

As a developer,
I want a Material 3 themed application with a typed navigation container and Koin DI initialized,
So that all subsequent feature stories can wire into a consistent foundation.

**Acceptance Criteria:**

**Given** the project builds with `:feature:auth` scaffolded
**When** the app launches
**Then** the `MealieApplication` class initializes Koin with all current module declarations
**And** no crash occurs on cold start

**Given** the device is running API 31+
**When** the app renders any screen
**Then** Dynamic Color is applied from the system wallpaper seed

**Given** the device is running API 26-30
**When** the app renders any screen
**Then** the static Material 3 color scheme seeded from `#E58325` is applied

**Given** the `NavHost` is initialized in `MainActivity`
**When** any composable requests navigation via `NavigationManager`
**Then** the `SharedFlow<NavigationCommand>` emits and the `NavHost` responds correctly

**Given** `:feature:auth` module is scaffolded
**When** the Gradle sync completes
**Then** `:app` declares a dependency on `:feature:auth` and the build succeeds

---

### Story 1.3: Server URL Entry and Connection Validation

As a first-time user,
I want to enter my Mealie server address and have it verified,
So that I know the app can reach my server before I enter credentials.

**Acceptance Criteria:**

**Given** the user opens the app for the first time (no server URL stored)
**When** the app launches
**Then** `ServerUrlScreen` is displayed with an empty URL input field and a "Connect" button

**Given** the user enters a URL with a trailing slash (e.g. `https://mealie.example.com/`)
**When** they tap "Connect"
**Then** the trailing slash is silently stripped before the probe is sent
**And** the normalized URL is shown in the field

**Given** the user enters a bare hostname or IP with port (e.g. `192.168.1.100:9925` or `mealie.local`)
**When** they tap "Connect"
**Then** `https://` is prepended automatically before the probe is sent
**And** the normalized URL is shown in the field

**Given** the user enters a malformed string (invalid characters, not a recognizable host)
**When** they tap "Connect"
**Then** an inline error "Enter a valid URL (e.g. https://mealie.example.com)" is shown
**And** no network request is made

**Given** the user enters a well-formed URL
**When** they tap "Connect"
**Then** a loading indicator replaces the button
**And** the app sends `GET /api/app/about` to that URL

**Given** the probe returns HTTP 200 with a Mealie `version` field in the response body
**When** the response is parsed
**Then** the URL is persisted to Preferences DataStore
**And** the screen navigates to HTTP warning check (Story 1.4's destination route is declared here)

**Given** the probe fails (timeout, DNS failure, non-200, or no `version` field)
**When** the error is received
**Then** a specific error message is shown ("Could not reach server" / "Not a Mealie server")
**And** the input remains editable for correction

**Given** a server URL is already stored in DataStore
**When** the app launches
**Then** `ServerUrlScreen` is skipped and the app proceeds directly to auth flow

---

### Story 1.4: HTTP Security Warning for Non-HTTPS URLs

As a user connecting to a self-hosted server over HTTP,
I want a clear, non-alarming explanation of the connection type,
So that I can proceed confidently without being scared off by unnecessary warnings.

**Acceptance Criteria:**

**Given** the validated server URL uses the `https://` scheme
**When** the probe succeeds
**Then** no warning is shown and the app navigates directly to credential entry

**Given** the validated server URL uses the `http://` scheme
**When** the probe succeeds
**Then** an inline informational message is shown beneath the URL field: "Connecting over your local network - this is common for self-hosted setups."
**And** the message is styled as informational (not alarming, no error color)
**And** a "Continue" button allows the user to proceed to credential entry

**Given** the user taps "Continue" on the HTTP warning
**When** confirmed
**Then** the URL and a "warning acknowledged" flag are persisted to `AppPreferencesStore` keyed by the URL
**And** the app navigates to credential entry

**Given** the same `http://` URL is already stored with an acknowledged warning flag
**When** the app launches or the URL is re-validated
**Then** the inline warning is NOT shown again for that URL
**And** the app proceeds directly to credential entry

**Given** the server URL changes to a different `http://` URL
**When** the new URL is validated
**Then** the one-time warning is shown again (new URL, no prior acknowledgement)

---

### Story 1.4a: Externalize UI Strings to Centralized Resources

As a developer,
I want all existing user-facing strings extracted to `:core:ui` string resources with German translations,
So that the i18n pattern is established before further UI stories are implemented.

**Acceptance Criteria:**

**Given** stories 1-2, 1-3, and 1-4 contain hardcoded English strings in Compose code
**When** this story is completed
**Then** all user-facing strings are replaced with `stringResource(R.string.xyz)` references
**And** no hardcoded user-facing text remains in `:feature:auth` or `:core:ui` Compose code

**Given** `:core:ui` module's `src/main/res/` directory
**When** string resource files are created
**Then** `values/strings.xml` contains all English strings
**And** `values-de/strings.xml` contains German translations for all entries

**Given** string resource names are created
**When** naming is reviewed
**Then** all names use snake_case prefixed by screen or component (e.g., `setup_url_label`, `setup_button_connect`, `http_warning_message`)

**Given** the HTTP warning copy contains dynamic content (server URL)
**When** the string is externalized
**Then** a parameterized string is used (`%1$s`) rather than concatenation

**Given** the app is launched with the device locale set to German
**When** any screen from stories 1-2 through 1-4 is displayed
**Then** all UI chrome appears in German

**Given** the app is launched with a locale other than English or German
**When** any screen is displayed
**Then** the app falls back to English (default resource)

---

### Story 1.5: Credential Entry and Encrypted Storage

As a first-time user,
I want to enter my username and password securely and have them stored for later token refresh,
So that I can authenticate with the Mealie server without re-entering credentials manually.

**Acceptance Criteria:**

**Given** the server URL has been validated and stored
**When** the user navigates from `ServerUrlScreen`
**Then** `CredentialScreen` is displayed with username and password input fields and a "Login" button

**Given** the user leaves either field empty
**When** they tap "Login"
**Then** inline validation shows "Username and password required"
**And** no network request is made

**Given** the user enters valid username and password
**When** they tap "Login"
**Then** a POST `/api/auth/token` request is made with the credentials
**And** a loading indicator replaces the button

**Given** the token endpoint returns HTTP 200 with an `access_token` in the response
**When** the response is parsed
**Then** the username, password, and access token are all encrypted using `datastore-tink` with `AeadSerializer` (AES-256-GCM)
**And** all three values are persisted to encrypted Preferences DataStore
**And** the screen navigates to the shopping list (Story 1.6 wires this destination)

**Given** the credentials are invalid or the token endpoint returns HTTP 401
**When** the error is received
**Then** an error message "Invalid username or password" is displayed
**And** the input fields remain editable for correction

**Given** credentials have been stored in encrypted DataStore
**When** the app is killed and relaunched
**Then** the stored credentials are decrypted and available to `TokenManager` without user re-entry

---

### Story 1.6: Silent Token Refresh on App Launch

As the app,
I want to silently restore an authenticated session on launch,
So that users remain logged in across app restarts without manual re-entry.

**Acceptance Criteria:**

**Given** a token and credentials are stored in encrypted DataStore
**When** the app launches
**Then** `TokenManager` first attempts `GET /api/auth/refresh` with the stored Bearer token
**And** a loading indicator is shown during the attempt

**Given** the refresh request succeeds (HTTP 200 with new token)
**When** the response is received
**Then** the new token is encrypted and persisted to DataStore
**And** the app navigates to the main app screen (destination implemented in Epic 2)

**Given** the refresh request fails with HTTP 401 (token already expired)
**When** the 401 is received
**Then** `TokenManager` falls back to `POST /api/auth/token` with stored username, password, and `remember_me: true`

**Given** the credential re-authentication succeeds (HTTP 200 with new token)
**When** the response is received
**Then** the new token is encrypted and persisted to DataStore
**And** the app navigates to the main app screen

**Given** both refresh and credential re-authentication fail (credentials changed on server, network error)
**When** both attempts complete
**Then** all stored tokens are cleared from DataStore (credentials are kept)
**And** the app navigates to `CredentialScreen` for manual re-entry (Story 1.7 flow)

**Given** no credentials are stored in DataStore
**When** the app launches
**Then** both refresh and re-auth are skipped
**And** the app immediately navigates to `ServerUrlScreen`

**Given** stored credentials exist but the device is offline at launch
**When** `TokenManager` attempts the refresh request
**Then** the network call fails with a connectivity error (not a 401)
**And** the app skips credential re-authentication
**And** navigates to `ShoppingListScreen` in offline-only mode (Local Store content only)

**Given** multiple launch attempts occur concurrently
**When** `TokenManager` is invoked
**Then** the `Mutex` ensures only one refresh/re-auth cycle runs at a time

---

### Story 1.7: Automatic Mid-Session 401 Recovery

As a user,
I want the app to automatically recover when my token expires during use,
So that I can continue working without manual interruption unless my credentials are invalid.

**Acceptance Criteria:**

**Given** the user is authenticated and using the app
**When** an API request returns HTTP 401
**Then** the OkHttp `Authenticator` intercepts the response
**And** does NOT attempt `GET /api/auth/refresh` (token is already expired)

**Given** a 401 is intercepted
**When** `TokenManager` re-authenticates with stored credentials via `POST /api/auth/token` (username, password, `remember_me: true`)
**Then** if re-authentication succeeds, the new token is encrypted and persisted to DataStore
**And** the original request is automatically retried with the new token
**And** the user sees no error (the operation completes normally)

**Given** credential re-authentication fails (HTTP 401 from token endpoint - password changed on server)
**When** the failure is received
**Then** the `Authenticator` returns null (passes the 401 through)
**And** downstream code emits an "unauthenticated" event for Story 1.7 to handle

**Given** multiple API requests receive 401 simultaneously
**When** the `Authenticator` is triggered concurrently
**Then** the `Mutex` in `TokenManager` ensures only one re-authentication attempt runs
**And** all waiting requests reuse the same result (retry with new token or all fail together)

**Given** the `Authenticator` has already attempted re-authentication once and it failed
**When** the retried request also returns 401
**Then** no second re-authentication is attempted (prevent infinite loops)
**And** the 401 is passed through

---

### Story 1.8: Last-Resort Re-Authentication Screen

As a user,
I want to manually re-enter my credentials when automatic recovery fails,
So that I can regain access even if my password has changed or automatic refresh is broken.

**Acceptance Criteria:**

**Given** Story 1.5 (app launch recovery) fails or Story 1.6 (mid-session 401 recovery) fails
**When** the error is detected
**Then** the app navigates to `ReAuthScreen` with username and password input fields
**And** a message explains "Your session has expired. Please log in again."

**Given** the user opens `ReAuthScreen`
**When** the screen renders
**Then** the username field is pre-populated with the stored username (if available)
**And** the password field is empty

**Given** the user leaves either field empty
**When** they tap "Re-Authenticate"
**Then** inline validation shows "Username and password required"
**And** no network request is made

**Given** the user enters credentials
**When** they tap "Re-Authenticate"
**Then** `POST /api/auth/token` is called with the new credentials and `remember_me: true`
**And** a loading indicator replaces the button

**Given** re-authentication succeeds (HTTP 200 with new token)
**When** the response is received
**Then** the new username, password, and token are encrypted and persisted to DataStore
**And** the app navigates to the main app screen
**And** the user's session continues (no data loss)
**And** if connected, `SyncRepository.flushPendingChanges()` is called to flush any queued pending changes

**Given** re-authentication fails (HTTP 401)
**When** the error is received
**Then** an error message "Invalid credentials" is displayed
**And** the password field is cleared for another attempt

**Given** the user suspects the server URL is incorrect
**When** they tap an optional "Change Server" link
**Then** the app navigates back to `ServerUrlScreen` to allow reconfiguration

---

### Story 1.9: Credential and Server URL Update in Settings

As a user,
I want to update my server URL and credentials from a settings screen,
So that I can switch servers or change my password without reinstalling the app.

**Acceptance Criteria:**

**Given** the user navigates to Settings from the main app
**When** `SettingsScreen` is displayed
**Then** the `:feature:settings` module is scaffolded
**And** two updateable sections are shown: "Server Configuration" and "Account"

**Given** the user is on the Server Configuration section
**When** they tap "Change Server URL"
**Then** a text field shows the currently stored server URL
**And** an "Update" button is present

**Given** the user enters a new server URL
**When** they tap "Update"
**Then** the URL is validated (format check) and probed with `GET /api/app/about`
**And** a loading indicator is shown during the probe

**Given** the new server URL is valid and reachable
**When** the probe succeeds
**Then** the URL is persisted to DataStore
**And** a success message "Server updated" is shown
**And** stored credentials are retained (can be revalidated on next app launch)

**Given** the new server URL is invalid or unreachable
**When** the probe fails
**Then** an error message is shown ("Invalid URL" or "Could not reach server")
**And** the old URL remains unchanged

**Given** the user is on the Account section
**When** they tap "Change Password"
**Then** a text field for new username and a text field for new password are shown
**And** an "Update" button is present

**Given** the user enters a new username and password
**When** they tap "Update"
**Then** `POST /api/auth/token` is called with the new credentials and `remember_me: true`
**And** a loading indicator is shown

**Given** authentication with new credentials succeeds
**When** the response is received
**Then** the new username, password, and token are encrypted and persisted to DataStore
**And** a success message "Credentials updated" is shown

**Given** authentication with new credentials fails (HTTP 401)
**When** the error is received
**Then** an error message "Invalid credentials" is shown
**And** the old credentials remain unchanged

## Epic 2: Offline-First Shopping List

### Story 2.1: Shopping List Roster Screen and App Entry Routing

As a user,
I want to see my shopping lists immediately after signing in,
So that I can start managing my household's shopping without unnecessary setup screens.

**Acceptance Criteria:**

**Given** the `:feature:shopping` module is scaffolded with its `build.gradle.kts` and Koin module
**When** Gradle sync completes
**Then** `:app` declares a dependency on `:feature:shopping` and the build succeeds

**Given** `ShoppingListEntity`, `ShoppingListDao`, `ShoppingListDto`, `ShoppingList` domain model, and `ShoppingService` (`GET /api/households/shopping/lists`) are created with mapper extensions
**When** the Room schema is compiled
**Then** the database version is set to 1 and the schema export file is generated

**Given** the app launches after successful authentication (Story 1.5)
**When** `ShoppingListScreen` is the registered destination for the main app route
**Then** a loading splash is shown until session state resolves (no flash of auth screens)

**Given** session check confirms the user is authenticated
**When** `ShoppingListScreen` is displayed
**Then** the roster is immediately populated from Local Store (zero network wait)
**And** a background refresh triggers `GET /api/households/shopping/lists` when connected
**And** new results are written to Room and reflected via `StateFlow`

**Given** the roster has more lists than fit on screen
**When** the user scrolls to the bottom
**Then** the next page is fetched and appended without replacing existing items

**Given** the device is offline and Local Store has cached lists
**When** `ShoppingListScreen` opens
**Then** the cached lists are shown with no error

**Given** the device is offline and Local Store is empty (first launch, never synced)
**When** `ShoppingListScreen` opens
**Then** an empty state message is shown ("No lists available - connect to sync")

**Given** `ShoppingListScreen` renders
**When** any window size class is active
**Then** a `MediumTopAppBar` is shown with a Settings `IconButton`
**And** content is constrained to `widthIn(max = 600.dp)` on medium and expanded screens

**Given** the app routes authenticated users to `ShoppingListScreen`
**When** the user presses the device back button on `ShoppingListScreen`
**Then** the app exits (back stack is clean, no auth screens remain)

---

### Story 2.2: Shopping List Detail and Item View

As a user,
I want to open a shopping list and see all its items grouped by status,
So that I can quickly see what still needs to be bought versus what's already in the cart.

**Acceptance Criteria:**

**Given** `ShoppingItemEntity`, `ShoppingItemDao`, `ShoppingItemDto`, `ShoppingItem` domain model, and a `GET /api/households/shopping/items` service method are created with mapper extensions
**When** the user taps a list in the roster
**Then** `ShoppingListDetailScreen` opens and items are immediately read from Local Store

**Given** the list has both checked and unchecked items
**When** the screen renders
**Then** unchecked items appear at the top of the list
**And** checked items appear below in a separate section
**And** each item row displays label, quantity, and unit where available

**Given** `ShoppingListItem` composable is created with a `shoppingMode: Boolean` parameter
**When** rendered in Planning mode (`shoppingMode = false`)
**Then** row height is 56dp, horizontal padding is 16dp, text uses `titleMedium` 16sp in `onSurface` color
**And** the `shoppingMode = true` density variant is declared but implemented in Story 2.5

**Given** the screen opens while connected
**When** Local Store items are displayed
**Then** a background refresh fetches the latest items from the server
**And** any changes are written to Room and reflected in the UI via `StateFlow`

**Given** the device is offline
**When** the user opens a list that has cached items
**Then** the cached items are shown with no error and no network call is attempted

**Given** a list has no items in Local Store
**When** the screen opens offline
**Then** an empty state message is shown ("No items - connect to load your list")

**Given** the user presses back from `ShoppingListDetailScreen`
**When** back is pressed
**Then** the app navigates back to the roster screen

---

### Story 2.3: Check/Uncheck with Optimistic Update

As a user,
I want checking and unchecking items to feel instant,
So that I can move through my shopping list quickly without waiting for the server.

**Acceptance Criteria:**

**Given** `SyncQueueEntity`, `SyncQueueDao` are created and `ShoppingItemEntity` gains a `syncStatus` field (`SYNCED`, `PENDING`, `ERROR`)
**When** the user taps a `ShoppingListItem` row
**Then** the item's `checked` state toggles immediately in Local Store (optimistic update)
**And** a `PENDING` Sync Queue entry is written to Room within 100ms

**Given** an item is checked
**When** the optimistic update is applied
**Then** the item animates to 40% opacity with strikethrough and moves to the checked section
**And** all three visual cues apply together within 200ms
**And** when `LocalReduceMotion.current` is true, the transition is instant (`snap<Float>()`)

**Given** an item is in the checked section
**When** the user taps it again
**Then** the item reverts to unchecked with full opacity and moves back to the top section
**And** no undo prompt is shown (second tap is the undo)

**Given** the device is connected and a Sync Queue entry is `PENDING`
**When** `SyncRepository.flushPendingChanges()` becomes available (Story 2.8)
**Then** the queued change is sent to the server and `syncStatus` is updated to `SYNCED`

**Given** the device is offline and a Sync Queue entry is `PENDING`
**When** the item row renders
**Then** a trailing slot placeholder marks the item as having a pending sync status
**And** the actual `SyncStatusBadge` composable is wired in Story 2.9

**Given** `ShoppingListItem` rows render
**When** TalkBack is active
**Then** each row has `role = Role.Checkbox`, `stateDescription = "Checked"` or `"Unchecked"`, and a full `contentDescription` including label, quantity, and unit

---

### Story 2.4: Add and Delete Shopping List Items

As a user,
I want to add and remove items while planning my shopping trip,
So that my list is accurate before I leave home.

**Acceptance Criteria:**

**Given** the quick-add text field is visible at the bottom of `ShoppingListDetailScreen`
**When** the screen renders in Planning mode
**Then** the field has `surfaceContainerHighest` container, `ShapeDefaults.Large` shape (16dp corners), 56dp height, and placeholder "Add an item..."

**Given** the user types a label and taps the Done IME action
**When** the submission is validated (non-empty label)
**Then** a new item appears immediately at the top of the unchecked section with `syncStatus = PENDING`
**And** a `POST /api/households/shopping/items` Sync Queue entry is created in Room
**And** the text field clears but the keyboard stays open for adding the next item

**Given** a new item was added
**When** the item appears in the list
**Then** a `Snackbar` with a 5-second "Undo" action is shown
**And** tapping "Undo" removes the item from Local Store and discards the Sync Queue entry

**Given** the user swipes an item row in Planning mode
**When** `SwipeToDismissBox` detects a full swipe
**Then** the item is immediately removed from the UI
**And** a `DELETE /api/households/shopping/items` Sync Queue entry is created
**And** a `Snackbar` with a 5-second "Undo" action is shown

**Given** the user taps "Undo" on a deleted item
**When** undo is triggered within 5 seconds
**Then** the item is restored in Local Store and the Sync Queue entry is discarded

**Given** an item was created offline and deleted before any sync occurs
**When** the deletion is processed
**Then** both the POST and DELETE Sync Queue entries are removed
**And** no server call is issued for either operation

---

### Story 2.5: Shopping Mode ModalBottomSheet

As a user,
I want to switch into a focused shopping mode when I'm in the store,
So that items are larger and easier to tap while I'm moving through the aisles.

**Acceptance Criteria:**

**Given** the user is on `ShoppingListDetailScreen` in Planning mode
**When** they tap the "Start Shopping" `FilledButton`
**Then** a `ModalBottomSheet` overlay appears with `skipPartiallyExpanded = true`, `containerColor = surfaceContainerHigh`, and a built-in `DragHandle` pill

**Given** Shopping mode is active
**When** `ShoppingListItem` renders with `shoppingMode = true`
**Then** row height is 72dp, horizontal padding is 20dp, text uses `titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)` in `onSurface` color

**Given** Shopping mode is active
**When** the sheet renders
**Then** a "Done Shopping" `FilledButton` is visible in the sheet top bar
**And** tapping it dismisses the sheet and returns to Planning mode

**Given** Shopping mode is active
**When** the user swipes the sheet down or triggers the back gesture
**Then** the sheet is dismissed and Planning mode is restored
**And** back gesture dismisses the overlay, it does NOT navigate back to the roster

**Given** Shopping mode is active
**When** the quick-add field renders inside the sheet
**Then** it uses identical styling to Planning mode (`surfaceContainerHighest`, `ShapeDefaults.Large`, 56dp, placeholder "Add an item...")
**And** Done IME action submits, clears the field, and keeps the keyboard open

**Given** Shopping mode is active
**When** `SwipeToDismissBox` would normally trigger on an item row
**Then** swipe-to-delete is disabled (deletion requires exiting Shopping mode)

---

### Story 2.6: Shopping Mode Persistence and Auto-Reset

As a user,
I want the app to remember I was shopping when I briefly switch apps,
So that I don't have to re-enter Shopping mode every time I check my phone mid-shop.

**Acceptance Criteria:**

**Given** `ShoppingModePrefs(active: Boolean, lastInteractionAt: Long)` is persisted to Preferences DataStore
**When** the user enters Shopping mode
**Then** `active = true` and `lastInteractionAt = now` are written to DataStore immediately

**Given** the user performs any action within Shopping mode (check item, add item, scroll)
**When** the action is registered
**Then** `lastInteractionAt` is updated to `now` in DataStore

**Given** a `Clock` interface is injected into the Shopping mode prefs logic
**When** the 12-hour expiry check runs
**Then** `Clock` provides the current time (replaceable in tests with a fake implementation)
**And** no call to `System.currentTimeMillis()` appears directly in the prefs logic

**Given** the user exits Shopping mode via "Done Shopping" or swipe-down
**When** the sheet is dismissed
**Then** `active = false` is written to DataStore

**Given** the app cold-starts and `ShoppingModePrefs` is in DataStore
**When** `ShoppingListDetailScreen` is entered
**Then** a `LaunchedEffect` reads the DataStore value before rendering
**And** if `active == true` AND `(now - lastInteractionAt) < 12 hours`
**Then** `sheetState.show()` is called and Shopping mode is restored

**Given** the app cold-starts and `lastInteractionAt` is more than 12 hours ago
**When** `ShoppingListDetailScreen` is entered
**Then** Shopping mode is NOT restored
**And** `active` is reset to `false` in DataStore

**Given** the cold-start DataStore read is in-flight
**When** `ShoppingListDetailScreen` first renders
**Then** a splash or neutral state is shown until the first DataStore emission resolves
**And** Planning mode does NOT flash before Shopping mode sheet appears

---

### Story 2.7: Sort Preferences per Mode

As a user,
I want to sort my shopping list differently when planning versus when shopping,
So that I can organize alphabetically at home and find items by category aisle in the store.

**Acceptance Criteria:**

**Given** the user is on `ShoppingListDetailScreen` in Planning mode
**When** the screen renders
**Then** a `FilterChip` row is visible with three options: "Unsorted", "Alphabetical", "By Category"
**And** chips use `ShapeDefaults.Small` (8dp corners) per UX-DR9

**Given** the user selects a sort chip in Planning mode
**When** the selection is made
**Then** the list reorders immediately to reflect the chosen sort
**And** the selected chip is visually active

**Given** the user switches to Shopping mode
**When** the sheet renders
**Then** the same `FilterChip` row is visible inside the sheet
**And** the Shopping mode sort selection is independent from the Planning mode selection

**Given** the user selects a sort in either mode
**When** the selection is confirmed
**Then** `ShoppingListSortPrefs(planningModeSortOrder: SortOrder, shoppingModeSortOrder: SortOrder)` is persisted to Preferences DataStore keyed per list ID

**Given** sort preferences are stored for a list
**When** the user reopens the list
**Then** both Planning and Shopping mode sort preferences are restored from DataStore

**Given** the user is in Planning mode with "Unsorted" active
**When** they long-press an item row or use a visible drag handle
**Then** drag-to-reorder is enabled via `SwipeToDismissBox` in Planning mode only

**Given** the user is in Shopping mode
**When** they attempt to drag-reorder
**Then** drag-to-reorder is disabled (reordering is a Planning-only activity)

---

### Story 2.8: ConflictResolver, Sync Flush, and Pull-to-Refresh

As a user,
I want to pull down on my shopping list to force a sync,
So that I can confirm my offline changes have reached the server before I leave home.

**Acceptance Criteria:**

**Given** `ConflictResolver` is implemented as a pure function in `:core:sync`
**When** a server response contains an `updated_at` timestamp newer than the local change
**Then** `ConflictResolver` applies last-write-wins: server state overwrites local state
**And** the Sync Queue entry is discarded
**And** `ConflictResolver` is independently unit-testable in under 5ms with no Android dependencies

**Given** `SyncRepository.flushPendingChanges()` is implemented in `:core:sync`
**When** called
**Then** it processes all `PENDING` Sync Queue entries sequentially
**And** sends the appropriate API call (POST, DELETE, or PATCH) for each entry
**And** on server confirmation, updates `syncStatus` to `SYNCED` and removes the Sync Queue entry

**Given** an unrecoverable server error occurs for a specific item (4xx other than 401)
**When** the error is received
**Then** the item's `syncStatus` is updated to `ERROR` in Room
**And** the Sync Queue entry is removed
**And** the error is surfaced to the user once after the full flush completes

**Given** the same Sync Queue entry is sent twice (duplicate delivery)
**When** the second request arrives
**Then** the result is idempotent and no observable side effect occurs

**Given** the user pulls down on `ShoppingListDetailScreen` in Planning mode while connected
**When** the pull-to-refresh gesture completes
**Then** `SyncRepository.flushPendingChanges()` is called immediately
**And** a refresh indicator is shown during the flush
**And** the list updates to reflect server-confirmed state after the flush

**Given** the user pulls down while offline
**When** the gesture completes
**Then** no flush is attempted
**And** an inline message "No connection - changes will sync automatically" is shown briefly

**Given** the user is in Shopping mode
**When** they attempt pull-to-refresh
**Then** the gesture is disabled (pull-to-refresh is a Planning mode action per UX-DR19)

---

### Story 2.9: WorkManager SyncWorker, ConnectivityMonitor, and AppOrchestrator

As the app,
I want to automatically detect when the Mealie server is reachable and trigger a background sync,
So that offline changes are flushed without any user action required.

**Acceptance Criteria:**

**Given** `ConnectivityMonitor` is implemented in `:core:network`
**When** the device's network state changes
**Then** `ConnectivityManager.NetworkCallback` detects the change
**And** immediately probes `GET /api/app/about` (no auth required) to confirm Mealie instance reachability
**And** exposes `StateFlow<ConnectivityState>` with states `Online`, `Offline`, and `SyncError`

**Given** `MealieApplication.onCreate()` runs
**When** the app starts
**Then** an explicit `ConnectivityMonitor` probe is issued via `applicationScope` coroutine
**And** the initial `ConnectivityState` is set before any screen renders (GAP-2 resolution)

**Given** `SyncWorker` is implemented in `:core:sync` as a `CoroutineWorker`
**When** the worker runs
**Then** it first probes `GET /api/app/about` to confirm the instance is reachable
**And** if the probe fails it returns `Result.retry()` without processing any queue entries
**And** if the probe succeeds it delegates to `SyncRepository.flushPendingChanges()`
**And** it is tested via `TestListenableWorkerBuilder` (WorkManager is never mocked directly)

**Given** a transient failure occurs during the worker run (timeout, 5xx)
**When** `SyncWorker` encounters the error
**Then** it returns `Result.retry()` with exponential backoff

**Given** `ConnectivityMonitor` is in `:core:network` and `SyncScheduler` is in `:core:sync`
**When** `ConnectivityState` transitions to `Online`
**Then** `:app`'s `AppOrchestrator` collects the `ConnectivityState` flow
**And** calls `SyncScheduler.scheduleSyncIfNeeded()` on the `Online` transition (GAP-1 resolution)
**And** `ConnectivityMonitor` does NOT reference `SyncScheduler` directly (module boundary enforced)

**Given** `SyncScheduler.scheduleSyncIfNeeded()` is called with pending Sync Queue entries
**When** `SyncWorker` runs
**Then** all pending entries are confirmed within 30 seconds of connectivity restoration

**Given** `ConnectivityState` transitions to `Offline` or `SyncError`
**When** the state changes
**Then** the `ConnectivityState` flow emits the new state within 3 seconds
**And** downstream consumers (Story 2.10's `OfflineIndicator`) can react to it

---

### Story 2.10: OfflineIndicator and SyncStatusBadge

As a user,
I want to see clearly when I'm offline and when individual items have unresolved sync errors,
So that I know the state of my data without it getting in the way of shopping.

**Acceptance Criteria:**

**Given** `OfflineIndicator` is implemented in `:core:ui` consuming `ConnectivityState` from Story 2.9
**When** `ConnectivityState` is `Online`
**Then** the indicator is hidden with no layout space reserved

**Given** `ConnectivityState` transitions to `Offline`
**When** the indicator appears
**Then** it renders as a 24dp strip (using `wrapContentHeight()` at 1.3x font scale) pinned below `TopAppBar`
**And** shows `tertiaryContainer` background, `Icons.Outlined.CloudOff` 16dp, `labelSmall` text "Working offline - changes will sync when connected"

**Given** `ConnectivityState` transitions to `SyncError`
**When** the indicator renders
**Then** it shows `errorContainer` background and `onErrorContainer` color with text "Some changes couldn't sync"

**Given** `OfflineIndicator` transitions between hidden and visible
**When** the animation runs
**Then** `AnimatedVisibility` uses a vertical slide of 200ms
**And** when `LocalReduceMotion.current` is true the transition is instant
**And** the strip has `liveRegion = LiveRegionMode.Polite` for TalkBack

**Given** `SyncStatusBadge` is implemented in `:core:ui` as an 8dp dot in the trailing slot of `ShoppingListItem`
**When** an item has `syncStatus = PENDING`
**Then** the badge renders in amber (`tertiary`) with `contentDescription = "Sync pending"`

**Given** an item has `syncStatus = ERROR`
**When** the badge renders
**Then** it renders in red (`error`) with `contentDescription = "Sync error"`
**And** tapping the item row presents two options: "Retry" or "Discard and sync from server"

**Given** the user taps "Retry" on a sync-error item
**When** the retry is triggered
**Then** a new Sync Queue entry is created and `SyncRepository.flushPendingChanges()` is called immediately if connected

**Given** the user taps "Discard and sync from server"
**When** confirmed
**Then** the local item state is replaced with the current server state
**And** the Sync Queue entry is removed
**And** the `SyncStatusBadge` clears

**Given** an item has `syncStatus = SYNCED`
**When** the item row renders
**Then** no badge is shown
