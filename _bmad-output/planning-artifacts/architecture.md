---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8]
lastStep: 8
status: 'complete'
completedAt: '2026-05-22'
inputDocuments:
  - _bmad-output/planning-artifacts/prds/prd-mealie-android-2026-05-21/prd.md
  - _bmad-output/planning-artifacts/ux-design-specification.md
  - _bmad-output/planning-artifacts/briefs/brief-mealie-android-2026-05-21/brief.md
workflowType: 'architecture'
project_name: 'mealie-android-unofficial'
user_name: 'Xexanos'
date: '2026-05-22'
---

# Architecture Decision Document

_This document builds collaboratively through step-by-step discovery. Sections are appended as we work through each architectural decision together._

## Project Context Analysis

### Requirements Overview

**Functional Requirements:**

20 FRs across 6 feature areas. V1 scope includes 14 FRs (FR-1 through FR-6, FR-10 through FR-20);
recipe browsing (FR-7, FR-8, FR-9) is explicitly deferred to v2.

- **Server Setup & Auth (FR-1 to FR-6):** First-launch setup with URL validation and HTTP security
  warning, credential entry, silent token refresh on launch, OkHttp Authenticator for 401 interception,
  re-authentication without Local Store data loss. All credentials and tokens stored via DataStore +
  Android Keystore.
- **Shopping List (FR-10 to FR-15):** Full read/write access to Mealie Shopping Lists backed by Room.
  All mutations (check/uncheck, add, delete) are offline-capable: applied to Local Store immediately,
  queued in Sync Queue, flushed via WorkManager on reconnect. Conflict resolution: last-write-wins via
  `updated_at` timestamp.
- **Connectivity Awareness (FR-16 to FR-17):** Global Offline Indicator reflects Mealie instance
  reachability (not device network state). Per-item Sync Status Badge for pending/failed mutations,
  with user-resolvable error state on sync failure.
- **App Settings (FR-18 to FR-20):** Sync Network Mode (All Networks / Wi-Fi Only) via WorkManager
  constraint, in-app bug reporting via GitHub Issues URL intent, credential/server URL update with
  Local Store clear on server URL change.

**Non-Functional Requirements:**

- Cold start to interactive: < 3 seconds (mid-range device, API 26+)
- Local Store reads: < 100 ms; Sync Queue writes: < 100 ms
- Offline Indicator latency: appears/disappears within 3 seconds of connectivity state change
- Sync Queue drain time: all entries confirmed within 30 seconds of connectivity restoration
- Sync Queue durability: survives app kill, crash, device restart
- Sync idempotency: duplicate mutation delivery has no observable side effect
- Security: Android Keystore encryption with `setUnlockedDeviceRequired(true)`, no secrets in logcat
  or crash reports, no third-party analytics/telemetry/crash-reporting SDKs (hard constraint)
- Min SDK: API 26 (Android 8.0); target SDK: current stable; architectures: arm64-v8a, armeabi-v7a, x86_64
- WCAG AA accessibility (4.5:1 body contrast, 3:1 large text); TalkBack support; Reduce Motion support

**Scale & Complexity:**

- Primary domain: Native Android (Kotlin + Jetpack Compose)
- Complexity level: Medium
- Estimated architectural components: 6 distinct layers/modules (UI/Compose, ViewModel, Repository,
  Room data layer, Network/OkHttp layer, WorkManager sync layer) + 2 cross-cutting infrastructure
  modules (Auth/Keystore, Connectivity)

### Technical Constraints & Dependencies

**Platform:**
Kotlin + Jetpack Compose, MVVM + Repository pattern.

**Library Evaluation:**

| Library | Fit | Rationale |
| --- | --- | --- |
| Room | Strong | Flow-based queries drive Compose recomposition; durable Sync Queue as a Room table is idiomatic |
| WorkManager | Strong | Only Android API that provides durable, constraint-aware background work surviving process death |
| OkHttp + Authenticator | Strong | `Authenticator` interface provides serial 401 interception with built-in one-refresh-at-a-time guarantee |
| DataStore + Android Keystore | Strong | Keystore is non-negotiable for `setUnlockedDeviceRequired(true)`; DataStore is the correct modern replacement for SharedPreferences |
| Retrofit | Selected over Ktor | OkHttp already required; Retrofit adds minimal overhead; Ktor's multiplatform advantages are irrelevant for a single-platform app |

**Hard exclusion:** No analytics SDK, telemetry library, or crash-reporting SDK.

**API-level constraints:**
- Min API 26 - requires version-gating for Dynamic Color (API 31+), Predictive Back gesture support
  (API 33+, handled automatically by M3 ModalBottomSheet 1.3.0+), and monochrome adaptive icon (API 33+)
- TOKEN_TIME varies 1-9,600 hours per Mealie instance - app must never hardcode expiry assumptions
- Single Mealie instance and single Household per install (v1)

### Cross-Cutting Concerns Identified

1. **Offline-first data access** - Room is the single source of truth; all UI reads from Local Store;
   network responses update Local Store which triggers UI recomposition via Flow
2. **Auth token lifecycle** - Token refresh on launch, 401 interception mid-session, at-most-one
   concurrent refresh (mutex/synchronization required), credential fallback chain, last-resort
   re-auth prompt
3. **Sync queue durability and idempotency** - Room-backed queue survives process death;
   WorkManager handles retry/backoff; conflict resolution via `updated_at`; Mealie instance
   reachability probe before sync attempt
4. **Connectivity detection** - Must probe Mealie instance directly, not rely on device network
   state; `ConnectivityManager.NetworkCallback` plus periodic/event-driven API probe
5. **Security isolation** - Keystore-backed DataStore for tokens and credentials;
   `setUnlockedDeviceRequired(true)` means credentials unavailable when device locked;
   offline mode must handle this gracefully
6. **UI state persistence** - Shopping mode active/inactive + last-interaction timestamp,
   per-list sort preferences (separate for Planning/Shopping modes), all persisted to DataStore
7. **Accessibility** - WCAG AA contrast, TalkBack semantics on custom components (ShoppingListItem,
   OfflineIndicator, SyncStatusBadge), Reduce Motion gating on all transitions, font-scale
   resilience (sp for text, dp for structural sizes)

## Starter Template & Project Scaffolding

### Primary Technology Domain

Native Android (Kotlin + Jetpack Compose) - single-platform, focused scope, open-source.

### Starter Options Considered

| Option | Assessment |
| --- | --- |
| Android Studio "Empty Activity (Compose)" | Correct creation mechanism - no architectural opinions imposed |
| Now in Android (NiA) | Over-engineered: Hilt, 50+ modules, convention plugins - complexity without benefit at this scale |
| Architecture Blueprints (dev-compose branch) | Reference only - confirms MVVM + Repository + Room patterns; not a copyable template |

### Selected Approach: Android Studio Empty Activity + Manual Layer Setup

**Rationale:** No Android CLI scaffolds a full architecture. The project shell is created via
Android Studio; the layer structure is established manually in the first implementation stories,
using Architecture Blueprints as a reference pattern.

**Project Creation:**

```
Android Studio → New Project → Empty Activity
  Min SDK: API 26
  Language: Kotlin
  Build config: Kotlin DSL (build.gradle.kts)
```

### Module Structure

7-module split providing compile-time boundary enforcement without NiA-level complexity:

```
:app                → thin shell: MainActivity, NavHost, Koin module wiring
:feature:auth       → setup screen, re-auth screen, auth ViewModels
:feature:shopping   → shopping list screens, shopping ViewModels
:core:data          → Room database, entities, DAOs, repositories, domain Use Cases
:core:network       → OkHttp client, Retrofit, API service interfaces, Authenticator
:core:sync          → WorkManager workers, sync queue logic
:core:ui            → shared Compose components: OfflineIndicator, SyncStatusBadge, MealieTheme
```

**Boundary rules (enforced by Gradle dependency declarations):**
- `:feature:*` modules depend on `:core:*` modules, never on each other
- `:core:data` depends on `:core:network` for API calls within repositories
- `:core:sync` depends on `:core:data` (queue reads) and `:core:network` (API probe)
- `:app` depends on all modules to wire the DI graph and NavHost

**v2 expansion:** `:feature:recipe` is a new module added alongside `:feature:shopping` with no
structural changes to existing modules. The bridge Use Case
(`AddRecipeIngredientsToShoppingListUseCase`) lives in `:core:data` and is accessible to both
feature modules.

### Dependency Injection: Koin

Selected over Hilt. Kotlin DSL, no annotation processing overhead, runtime DI graph - errors
surface immediately in development. Compile-time guarantee not critical at this scale.
One Koin module per Gradle module, all wired in `:app`.

### Dependency Management: Gradle Version Catalog

`gradle/libs.versions.toml` with type-safe accessors. Single source of truth for all
dependency versions across all modules.

**Reference Versions (verify at project creation):**

| Library | Approx Version |
| --- | --- |
| Kotlin | 2.1.x |
| Compose BOM | 2025.05.x |
| Room | 2.8.x |
| WorkManager | 2.9.x |
| OkHttp | 4.12.x |
| Retrofit | 3.0.x |
| DataStore | 1.2.x |
| Koin | 4.x |

**Note:** Project initialization is the first implementation story. Layer structure (packages,
Repository interfaces, ViewModel setup, Koin modules) is established in the first few stories.

## Core Architectural Decisions

### Data Architecture

**JSON Serialization:** Kotlinx Serialization
- Kotlin-first, compile-time safe, no reflection, Retrofit converter via
  `kotlinx-serialization-json` + `retrofit2-kotlinx-serialization-converter`
- `@Serializable` on all API model classes

**DataStore Variant:** Preferences DataStore
- Two instances: one encrypted (token + credentials), one regular (shopping mode state,
  sort preferences, sync network mode, HTTP warning confirmation)
- Keystore encryption scoped only to where it is actually needed
- Thin typed wrapper classes over raw preference keys for type safety
- Multi-server future: server configurations migrate to Room (`ServerConfig` entity) when
  that feature is added; remaining DataStore entries (app-level preferences) are unaffected

**DataStore vs Room boundary:**
- DataStore: server URL, Stored Token (encrypted), Stored Credentials (encrypted), shopping
  mode state + timestamp, per-list sort preferences, Sync Network Mode, HTTP warning confirmation
- Room: Shopping Lists, Shopping List Items, Sync Queue, Household info (cached), *(v2: Recipe entities)*

**Room Migration Strategy:** Auto-migrations by default
- `@AutoMigration` for additive changes (new columns, new tables - the v2 recipe schema addition)
- Manual `Migration(from, to)` for breaking changes (column renames, type changes)
- v1 schema designed to be additive-only: no column renames or removals planned

**Data Flow Pattern:** Room → Repository → UseCase (where applicable) → ViewModel → UI
- Repository exposes `Flow<T>` from Room DAOs
- ViewModel collects as `StateFlow` via `stateIn(viewModelScope)`
- Compose collects via `collectAsStateWithLifecycle()`

### Authentication & Security

**Keystore Encryption:** `androidx.datastore:datastore-tink` with `AeadSerializer`
- `security-crypto` (`MasterKey`) fully deprecated (1.1.0, July 2025) - not used
- `datastore-tink` (`1.3.0-alpha09`) accepted as a dependency; actively developed Google
  Jetpack library with a clear stable path (estimated late 2026/2027)
- `AeadSerializer` wraps any `Serializer<T>` and encrypts/decrypts transparently using
  Tink AES-256-GCM with an Android Keystore-backed master key
- Protection level: **encrypted at rest** - covers the actual threat model for a shopping
  list app (file extraction, unencrypted backups, cross-app storage access)
- `setUnlockedDeviceRequired(true)` from the original PRD is not achievable via
  `AndroidKeysetManager` (known Tink gap, open since 2019) and is not required given
  the app's stated threat model ("device lock screen provides sufficient protection")
- Both sensitive DataStore instances (token store, credentials store) use `AeadSerializer`;
  the non-sensitive instance (shopping mode, sort prefs, sync mode) uses standard DataStore

**In-memory Token Management:** `TokenManager` Koin singleton in `:core:network`
- `suspend fun getToken(): String?`, `fun updateToken(token: String)`, `fun clearToken()`
- Holds `Mutex` for coroutine-level concurrent refresh safety
- Injected by both OkHttp `Authenticator` and `AuthRepository`

**Concurrent Refresh Safety:**
- OkHttp `Authenticator` is called serially by OkHttp (one call per host at a time)
- `TokenManager` `Mutex` provides additional guard for the credential fallback path
  running outside the OkHttp thread pool

**Error Propagation:** Custom sealed `ApiResult<T>` in `:core:network`

Based on the Mealie OpenAPI spec (FastAPI/Pydantic): only 422 `HTTPValidationError` is
explicitly documented; 401/403/404/500 occur at runtime. 401 is intercepted by OkHttp
`Authenticator` before reaching the Repository layer.

```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data object NetworkError : ApiResult<Nothing>()   // IOException, timeout, unreachable
    data object AuthError : ApiResult<Nothing>()      // 401 not resolved by Authenticator
    data class HttpError(                             // 403, 404, 422, 5xx
        val code: Int,
        val detail: String?                           // raw detail string for logging only
    ) : ApiResult<Nothing>()
}
```

422 `HTTPValidationError.detail` is `Array<ValidationError>` (`loc`, `msg`, `type` fields).
Parsed for logging only - it is a programming error and never surfaced to the user.
Repositories unwrap `ApiResult` and expose domain-level sealed types to ViewModels.

### API & Communication Patterns

**Service Interface Organisation:** Per-feature interfaces in `:core:network`
- `AuthService` - `/api/auth/*` endpoints
- `ShoppingService` - `/api/households/shopping/*` and `/api/households/self`
- `AppService` - `/api/app/about` (connectivity probe, no auth required)
- `RecipeService` added in v2 alongside `:feature:recipe`
- Single shared Retrofit + OkHttp instance; interface split only

**OpenAPI Spec Usage:** Reference and validation only - no code generation
- `docs/openapi.json` (Mealie v3.18.0) used as documentation when writing model classes
- OpenAPI Generator plugin not used: OpenAPI 3.1.0 `anyOf` nullable pattern (107 schemas)
  has poor generator support, hyphenated schema names (`ShoppingListItemOut-Output`) are
  invalid Kotlin identifiers, and a separate Room entity layer is required regardless
- Nullability read directly from spec: `anyOf: [{type: "X"}, {type: "null"}]` → `val field: X?`
- Spec kept in `docs/` as a living reference; periodic manual diff as Mealie evolves

**Model Nullability from Spec (key shopping list fields):**
- `ShoppingListItemOut`: `quantity`, `note`, `unit`, `food`, `label`, `foodId`, `labelId`,
  `unitId`, `createdAt`, `updatedAt` all nullable; `id`, `shoppingListId`, `groupId`,
  `householdId` required
- `ShoppingListSummary`: `name`, `createdAt`, `updatedAt` nullable; `id`, `groupId`,
  `householdId`, `userId` required

**Debug Logging:** `HttpLoggingInterceptor` + Timber
- `HttpLoggingInterceptor(Level.BODY)` with `redactHeader("Authorization")`
  added to OkHttp only when `BuildConfig.DEBUG`
- Timber for app-level structured logs (auth transitions, sync events); `DebugTree`
  planted in `Application.onCreate()` gated on `BuildConfig.DEBUG`
- No logging active in release builds

**Connectivity Probe:** `GET /api/app/about` (no auth required)
- `ConnectivityManager.NetworkCallback` detects device network state changes
- On network gained: probe `GET /api/app/about` to confirm Mealie instance reachability
- `ConnectivityState` exposed as `StateFlow<ConnectivityState>` from `:core:network`
- States: `Online`, `Offline`, `SyncError`
- WorkManager sync job independently probes before attempting sync (per FR-15)

**OkHttp Timeouts:** Connect: 10s, Read: 30s, Write: 15s

**Server URL Normalisation:** applied at input time (`:feature:auth`) and enforced in
`:core:network` base URL utility
- Strip trailing slashes, prepend `https://` if no scheme provided, lowercase scheme and host
- Store normalised form; all Retrofit calls use stored value

### Frontend Architecture

**Navigation:** Type-safe Navigation Compose 2.8+ with graph extension functions
- `@Serializable` route objects (Kotlinx Serialization already a dependency)
- `NavHost` in `:app`; each feature module exposes `NavGraphBuilder.xGraph()` extension
- Feature modules own their internal screens; `:app` wires the full graph
- Cross-feature navigation (e.g. auth failure interrupting shopping flow): `NavigationManager`
  singleton in `:core:ui` exposes a `SharedFlow<NavigationCommand>`; feature modules emit
  commands; `:app` collects and routes - avoids callback pyramids at 3+ features
- Custom `NavType<T>` required for any non-primitive route argument (e.g. UUID value classes)
- Navigation graph smoke test in `:app` androidTest verifies all routes reachable and
  back-stack behaviour is correct

**UiState Pattern:**
- Auth/setup screens: sealed class (`Loading`, `AwaitingInput`, `Error`, `Success`)
- Shopping list screens: data class with nested sealed `Overlay` for transient states:
  ```kotlin
  data class ShoppingUiState(
      val items: List<ShoppingListItemUi> = emptyList(),
      val syncState: SyncState = SyncState.Synced,
      val connectivityState: ConnectivityState = ConnectivityState.Online,
      val overlay: Overlay = Overlay.None
  ) {
      sealed interface Overlay {
          data object None : Overlay
          data class EditItem(val item: ShoppingListItemUi) : Overlay
          data class ConfirmDelete(val item: ShoppingListItemUi) : Overlay
          data class SyncError(val message: String) : Overlay
      }
  }
  ```
- All ViewModels expose `StateFlow<UiState>`, collected via `collectAsStateWithLifecycle()`

**Testing Strategy:**
- Framework split enforced by Gradle module dependency declarations:
  - `:core:*` and `:feature:*` modules: `testImplementation(junit-jupiter)` only;
    JUnit 4 physically unavailable, no Vintage engine added
  - `:app`: JUnit 5 for local unit tests; JUnit 4 + `ui-test-junit4` for androidTest
- Local unit tests: JUnit 5 + MockK + Turbine (ViewModels, Repositories, UseCases, TokenManager)
- Integration: Room in-memory database for Repository + DAO layer
- Compose UI: JUnit 4 + `ui-test-junit4` + `createAndroidComposeRule` in `:app`
- WorkManager: `TestListenableWorkerBuilder` with `WorkerFactory` for dependency injection;
  mocking WorkManager directly prohibited
- API contract tests: MockWebServer + JSON fixtures in `:core:network` test resources;
  fixtures versioned in git, updated when Mealie API version changes
- Offline/sync integration: MockWebServer + in-memory Room + real `SyncWorker` covering
  happy path, mid-sync network failure, and `updated_at` conflict resolution
- Test double strategy: `FakeXRepository` for ViewModel/UseCase tests; in-memory Room
  for Repository tests; never mixed
- StateFlow dispatcher: shared `MainDispatcherExtension` (JUnit 5 Extension) using
  `UnconfinedTestDispatcher` as default; `StandardTestDispatcher` explicitly opted into
  for timing-sensitive tests (debouncing, retry backoff)
- No screenshot tests for v1

**Image Loading:** Coil deferred to v2 (no images in v1 shopping list)

### Infrastructure & Deployment

**Version Control & Hosting:** GitHub (open source)

**CI/CD:** GitHub Actions
- `build` job: `./gradlew assembleDebug` on every push/PR
- `test` job: `./gradlew test` (all local unit tests, all modules) on every push/PR
- `lint` job: `./gradlew lint detekt ktlintCheck` on every push/PR
- `release` job: `./gradlew assembleRelease` + APK signing on tag push (`v*`),
  attached to GitHub Release
- Instrumented tests: run locally before tagging in v1; added to CI when contributors join

**Code Quality:**
- ktlint: formatting via `.editorconfig`; optional pre-commit auto-format hook
- Detekt: code smell + complexity; config at `config/detekt/detekt.yml`
- Android Lint: Android-specific issues; per-module `lint.xml`
- All three run in CI `lint` job on every PR

**Build Variants:**
- `debug`: `applicationIdSuffix ".debug"`, debug signing, Timber + HttpLoggingInterceptor, no obfuscation
- `release`: production applicationId, release signing, R8 with explicit ProGuard keep rules
  for Retrofit, Kotlinx Serialization, Room, Koin

**Signing:** Release keystore as GitHub Actions secrets; never committed to repo.
Variables: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

**Distribution:**
- v1: direct APK via GitHub Releases
- Post-v1: F-Droid (reproducible builds; all dependencies open source - already satisfied)
  and/or Play Store (requires privacy policy, compliant signing config)
- Architecture is F-Droid compatible as decided; no structural changes required

## Implementation Patterns & Consistency Rules

### Naming Conventions

| Concern | Pattern | Example |
| --- | --- | --- |
| ViewModel | `{Feature}ViewModel` | `ShoppingListViewModel` |
| Repository interface | `{Feature}Repository` | `ShoppingRepository` |
| Repository implementation | `{Feature}RepositoryImpl` | `ShoppingRepositoryImpl` |
| Use Case | `{Verb}{Noun}UseCase` | `AddItemToShoppingListUseCase` |
| Room Entity | `{Name}Entity` | `ShoppingItemEntity` |
| API DTO | `{Name}Dto` | `ShoppingListItemDto` |
| UI model | `{Name}Ui` | `ShoppingListItemUi` |
| Room DAO | `{Name}Dao` | `ShoppingItemDao` |
| Koin module | `{module}Module` | `networkModule`, `shoppingModule` |

### Model Layer Separation

Four distinct model types; no layer references a model from a higher layer:

| Layer | Type | Annotation | Location |
| --- | --- | --- | --- |
| Network | `{Name}Dto` | `@Serializable` | `:core:network` |
| Domain | `{Name}` (plain data class) | none | `:core:data` domain subpackage |
| Persistence | `{Name}Entity` | `@Entity` | `:core:data` database subpackage |
| UI | `{Name}Ui` | none | `:feature:*` ui subpackage |

**Mapper placement:** Extension functions co-located with the source type.
- `ShoppingListItemDto.toDomain()` lives in `:core:network` (network → domain)
- `ShoppingListItem.toEntity()` lives in `:core:data` domain package (domain → entity)
- `ShoppingListItem.toUi()` lives in `:feature:shopping` ui package (domain → ui)

### Date Handling

- API response: ISO-8601 `String` → Domain: `kotlinx-datetime` `Instant?` → Room: `Long?` (epoch ms) → UI: formatted `String`
- `kotlinx-datetime` `Instant` used in all domain models; never `java.util.Date`
- Room `TypeConverter` converts `Instant?` to/from `Long?` epoch milliseconds
- UI formatting via `android.text.format.DateUtils` for locale-aware display

### ViewModel Patterns

**One-shot events** (navigation, toasts, snackbars): `Channel<Event>` + `receiveAsFlow()`

```kotlin
private val _events = Channel<ShoppingUiEvent>(Channel.BUFFERED)
val events: Flow<ShoppingUiEvent> = _events.receiveAsFlow()
```

**Coroutine scope rules:**
- All ViewModel coroutines launched in `viewModelScope` - never `GlobalScope`
- `StateFlow` produced via `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState)`
- Background work launched with `viewModelScope.launch(Dispatchers.IO)`
- Never block the main thread from a ViewModel

**ApiResult → UiState mapping pattern:**

```kotlin
viewModelScope.launch {
    _uiState.update { it.copy(syncState = SyncState.Syncing) }
    when (val result = repository.syncShoppingList()) {
        is ApiResult.Success -> _uiState.update { it.copy(syncState = SyncState.Synced) }
        is ApiResult.NetworkError -> _uiState.update {
            it.copy(syncState = SyncState.Error, overlay = Overlay.SyncError("No connection"))
        }
        is ApiResult.AuthError -> _events.send(ShoppingUiEvent.NavigateToReAuth)
        is ApiResult.HttpError -> _uiState.update {
            it.copy(overlay = Overlay.SyncError("Server error ${result.code}"))
        }
    }
}
```

### Package Structure Within Modules

**`:feature:shopping`:**

```
src/main/kotlin/dev/xexanos/mealie/feature/shopping/
  ui/
    ShoppingListScreen.kt
    ShoppingListViewModel.kt
    ShoppingUiState.kt
    ShoppingUiEvent.kt
    components/
      ShoppingListItem.kt
      AddItemBottomSheet.kt
      SyncErrorDialog.kt
  domain/
    AddItemToShoppingListUseCase.kt
    CheckItemUseCase.kt
    DeleteItemUseCase.kt
  navigation/
    ShoppingNavGraph.kt
```

**`:core:data`:**

```
src/main/kotlin/dev/xexanos/mealie/core/data/
  database/
    MealieDatabase.kt
    entities/
      ShoppingListEntity.kt
      ShoppingItemEntity.kt
      SyncQueueEntity.kt
    dao/
      ShoppingListDao.kt
      ShoppingItemDao.kt
      SyncQueueDao.kt
    converters/
      InstantConverter.kt
  domain/
    ShoppingList.kt
    ShoppingListItem.kt
  repository/
    ShoppingRepository.kt
    ShoppingRepositoryImpl.kt
  di/
    DataModule.kt
```

**`:core:network`:**

```
src/main/kotlin/dev/xexanos/mealie/core/network/
  api/
    AuthService.kt
    ShoppingService.kt
    AppService.kt
  dto/
    ShoppingListDto.kt
    ShoppingListItemDto.kt
    AuthDto.kt
  auth/
    TokenManager.kt
    MealieAuthenticator.kt
  result/
    ApiResult.kt
  di/
    NetworkModule.kt
```

## Project Structure & Boundaries

### Requirements-to-Module Mapping

| FR Category | Module(s) |
| --- | --- |
| Server Setup & Auth (FR-1 to FR-6) | `:feature:auth`, `:core:network`, `:core:data` |
| Shopping List CRUD (FR-10 to FR-15) | `:feature:shopping`, `:core:data`, `:core:sync` |
| Connectivity Awareness (FR-16 to FR-17) | `:core:network`, `:core:ui`, `:core:data` |
| App Settings (FR-18 to FR-20) | `:feature:shopping` (settings screen), `:core:data`, `:core:sync` |
| Cross-cutting: Auth token lifecycle | `:core:network` (TokenManager, Authenticator) |
| Cross-cutting: Sync durability | `:core:sync` (SyncWorker + queue) |
| Cross-cutting: Theme / shared UI | `:core:ui` |

### Complete Project Directory Structure

```
mealie-android/
├── .github/
│   └── workflows/
│       ├── build.yml
│       ├── test.yml
│       ├── lint.yml
│       └── release.yml
├── config/
│   └── detekt/
│       └── detekt.yml
├── docs/
│   └── openapi.json
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       └── gradle-wrapper.properties
├── app/
│   ├── build.gradle.kts
│   ├── lint.xml
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/dev/xexanos/mealie/
│       │       ├── MealieApplication.kt
│       │       ├── MainActivity.kt
│       │       └── navigation/
│       │           └── AppNavGraph.kt
│       ├── debug/
│       │   └── kotlin/dev/xexanos/mealie/
│       │       └── DebugApplication.kt
│       └── androidTest/
│           └── kotlin/dev/xexanos/mealie/
│               └── navigation/
│                   └── NavGraphTest.kt
├── feature/
│   ├── auth/
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/dev/xexanos/mealie/feature/auth/
│   │       ├── ui/
│   │       │   ├── ServerSetupScreen.kt
│   │       │   ├── ServerSetupViewModel.kt
│   │       │   ├── AuthUiState.kt
│   │       │   ├── AuthUiEvent.kt
│   │       │   ├── ReAuthScreen.kt
│   │       │   └── ReAuthViewModel.kt
│   │       ├── navigation/
│   │       │   └── AuthNavGraph.kt
│   │       └── di/
│   │           └── AuthFeatureModule.kt
│   └── shopping/
│       ├── build.gradle.kts
│       └── src/
│           ├── main/kotlin/dev/xexanos/mealie/feature/shopping/
│           │   ├── ui/
│           │   │   ├── ShoppingListScreen.kt
│           │   │   ├── ShoppingListViewModel.kt
│           │   │   ├── ShoppingUiState.kt
│           │   │   ├── ShoppingUiEvent.kt
│           │   │   └── components/
│           │   │       ├── ShoppingListItem.kt
│           │   │       ├── AddItemBottomSheet.kt
│           │   │       ├── ShoppingModeOverlay.kt
│           │   │       └── SyncErrorDialog.kt
│           │   ├── domain/
│           │   │   ├── AddItemToShoppingListUseCase.kt
│           │   │   ├── CheckItemUseCase.kt
│           │   │   └── DeleteItemUseCase.kt
│           │   ├── navigation/
│           │   │   └── ShoppingNavGraph.kt
│           │   └── di/
│           │       └── ShoppingFeatureModule.kt
│           └── test/kotlin/dev/xexanos/mealie/feature/shopping/
│               ├── ShoppingListViewModelTest.kt
│               ├── AddItemUseCaseTest.kt
│               └── CheckItemUseCaseTest.kt
├── core/
│   ├── data/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/kotlin/dev/xexanos/mealie/core/data/
│   │       │   ├── database/
│   │       │   │   ├── MealieDatabase.kt
│   │       │   │   ├── entities/
│   │       │   │   │   ├── ShoppingListEntity.kt
│   │       │   │   │   ├── ShoppingItemEntity.kt
│   │       │   │   │   └── SyncQueueEntity.kt
│   │       │   │   ├── dao/
│   │       │   │   │   ├── ShoppingListDao.kt
│   │       │   │   │   ├── ShoppingItemDao.kt
│   │       │   │   │   └── SyncQueueDao.kt
│   │       │   │   └── converters/
│   │       │   │       └── InstantConverter.kt
│   │       │   ├── datastore/
│   │       │   │   ├── TokenStore.kt
│   │       │   │   ├── CredentialsStore.kt
│   │       │   │   └── AppPreferencesStore.kt
│   │       │   ├── domain/
│   │       │   │   ├── ShoppingList.kt
│   │       │   │   ├── ShoppingListItem.kt
│   │       │   │   └── SyncQueueEntry.kt
│   │       │   ├── repository/
│   │       │   │   ├── ShoppingRepository.kt
│   │       │   │   ├── ShoppingRepositoryImpl.kt
│   │       │   │   ├── AuthRepository.kt
│   │       │   │   └── AuthRepositoryImpl.kt
│   │       │   └── di/
│   │       │       └── DataModule.kt
│   │       └── test/kotlin/dev/xexanos/mealie/core/data/
│   │           ├── repository/
│   │           │   └── ShoppingRepositoryImplTest.kt
│   │           └── dao/
│   │               └── ShoppingItemDaoTest.kt
│   ├── network/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/kotlin/dev/xexanos/mealie/core/network/
│   │       │   ├── api/
│   │       │   │   ├── AuthService.kt
│   │       │   │   ├── ShoppingService.kt
│   │       │   │   └── AppService.kt
│   │       │   ├── dto/
│   │       │   │   ├── ShoppingListDto.kt
│   │       │   │   ├── ShoppingListItemDto.kt
│   │       │   │   └── AuthDto.kt
│   │       │   ├── auth/
│   │       │   │   ├── TokenManager.kt
│   │       │   │   └── MealieAuthenticator.kt
│   │       │   ├── result/
│   │       │   │   └── ApiResult.kt
│   │       │   ├── connectivity/
│   │       │   │   └── ConnectivityMonitor.kt
│   │       │   └── di/
│   │       │       └── NetworkModule.kt
│   │       └── test/kotlin/dev/xexanos/mealie/core/network/
│   │           ├── fixtures/
│   │           │   ├── shopping_list.json
│   │           │   └── auth_token.json
│   │           ├── ShoppingServiceTest.kt
│   │           └── TokenManagerTest.kt
│   ├── sync/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/kotlin/dev/xexanos/mealie/core/sync/
│   │       │   ├── SyncWorker.kt
│   │       │   ├── SyncScheduler.kt
│   │       │   └── di/
│   │       │       └── SyncModule.kt
│   │       └── test/kotlin/dev/xexanos/mealie/core/sync/
│   │           └── SyncWorkerTest.kt
│   └── ui/
│       ├── build.gradle.kts
│       └── src/main/kotlin/dev/xexanos/mealie/core/ui/
│           ├── theme/
│           │   ├── MealieTheme.kt
│           │   ├── Color.kt
│           │   └── Type.kt
│           ├── components/
│           │   ├── OfflineIndicator.kt
│           │   └── SyncStatusBadge.kt
│           └── navigation/
│               └── NavigationManager.kt
├── build.gradle.kts
├── settings.gradle.kts
├── .editorconfig
└── .gitignore
```

### Architectural Boundaries

**Module dependency graph (enforced by Gradle):**

```
:app                 depends on all modules
:feature:auth        depends on :core:data, :core:network, :core:ui
:feature:shopping    depends on :core:data, :core:network, :core:ui, :core:sync
:core:data           depends on :core:network
:core:sync           depends on :core:data, :core:network
:core:ui             no core dependencies
:core:network        no core dependencies (leaf module)
```

Features never depend on each other. Cross-feature navigation flows through `NavigationManager` in `:core:ui`.

**Data boundaries:**

| Store | Contents | Encryption |
| --- | --- | --- |
| `TokenStore` (DataStore) | Access token | AeadSerializer (AES-256-GCM) |
| `CredentialsStore` (DataStore) | Username, password, server URL | AeadSerializer (AES-256-GCM) |
| `AppPreferencesStore` (DataStore) | Shopping mode state, sort prefs, sync network mode, HTTP warning ack | None |
| `MealieDatabase` (Room) | Shopping lists, items, sync queue | None (device full-disk encryption) |

**Integration points:**

- `ConnectivityMonitor` (`:core:network`) emits `StateFlow<ConnectivityState>` - consumed by `:feature:shopping` ViewModel and `:core:sync` SyncWorker
- `SyncScheduler` (`:core:sync`) called from `:feature:shopping` ViewModel on mutation and from `ConnectivityMonitor` on network-gained event
- `NavigationManager` (`:core:ui`) emits `SharedFlow<NavigationCommand>` - collected in `MainActivity` (`:app`)
- `TokenManager` (`:core:network`) injected into `MealieAuthenticator` and `AuthRepositoryImpl`

## Architecture Validation Results

### Coherence Validation

**Decision Compatibility:** All library choices compatible. No version conflicts identified.
Kotlinx Serialization integrates with Retrofit 3.0.x via `retrofit2-kotlinx-serialization-converter`;
`datastore-tink` extends DataStore 1.2.x; Navigation Compose 2.8+ ships in Compose BOM 2025.05.x;
MockK + Turbine both support JUnit 5.

**Pattern Consistency:** MVVM + Repository + Domain applied uniformly. `StateFlow` + `collectAsStateWithLifecycle`,
`Channel` for one-shot events, `ApiResult<T>` for error propagation, and type-safe navigation routes
are consistent across all modules.

**Structure Alignment:** 7-module dependency graph matches declared rules. Package layouts match naming
conventions. JUnit 5 in `test/`, JUnit 4 in `androidTest/` matches testing strategy.

### Requirements Coverage Validation

All 20 FRs and all NFRs are architecturally covered. See Requirements-to-Module Mapping for traceability.

### Gap Analysis Results

**GAP-1 (Important - Resolved): `ConnectivityMonitor` → `SyncScheduler` dependency direction**

`ConnectivityMonitor` (`:core:network`) cannot call `SyncScheduler` (`:core:sync`) directly - that
would create an illegal cross-dependency between peer modules. Resolution: `ConnectivityMonitor`
emits `StateFlow<ConnectivityState>` only. `:app` (which depends on both modules) collects this
flow via an `AppOrchestrator` class or directly in `MainActivity`, and calls
`SyncScheduler.scheduleSyncIfNeeded()` when the state transitions to `Online`. `:app` is the
correct and only legal wiring point.

**GAP-2 (Important - Resolved): Initial connectivity state on app start**

`NetworkCallback` only fires on state *changes*. If the app starts offline, no callback fires
and `ConnectivityState` never transitions from its initial value. Resolution: issue an explicit
`GET /api/app/about` probe in `MealieApplication.onCreate()` via a `applicationScope` coroutine
to set the initial `ConnectivityState` before any UI renders.

**GAP-3 (Minor - Resolved): `kotlinx-datetime` missing from version catalog**

Add `kotlinx-datetime = "0.6.x"` to `libs.versions.toml` reference versions table.

**GAP-4 (Minor - Resolved): `DebugApplication.kt` redundant**

`HttpLoggingInterceptor` is already gated on `BuildConfig.DEBUG` in `NetworkModule`. Timber
`DebugTree` can be planted in `MealieApplication` with the same gate. Remove `DebugApplication.kt`
from the `debug/` sourceset; use a single `MealieApplication.kt`.

**GAP-5 (Minor - Resolved): Settings screen missing from project structure**

FR-18 to FR-20 have no corresponding screen file. Add `SettingsScreen.kt` + `SettingsViewModel.kt`
to `:feature:shopping/ui/` and a settings route to `ShoppingNavGraph.kt`.

### Architecture Completeness Checklist

**Requirements Analysis**
- [x] Project context thoroughly analyzed
- [x] Scale and complexity assessed
- [x] Technical constraints identified
- [x] Cross-cutting concerns mapped

**Architectural Decisions**
- [x] Critical decisions documented with versions
- [x] Technology stack fully specified
- [x] Integration patterns defined
- [x] Performance considerations addressed

**Implementation Patterns**
- [x] Naming conventions established
- [x] Structure patterns defined
- [x] Communication patterns specified
- [x] Process patterns documented

**Project Structure**
- [x] Complete directory structure defined
- [x] Component boundaries established
- [x] Integration points mapped
- [x] Requirements to structure mapping complete

### Architecture Readiness Assessment

**Overall Status:** READY FOR IMPLEMENTATION

All 16 checklist items verified. No Critical Gaps remain. Important gaps (GAP-1, GAP-2) are
resolved by clarification above and must be implemented as specified.

**Confidence Level:** High

**Key Strengths:**
- Offline-first architecture with Room as single source of truth is robust for the stated threat model
- Multi-module boundary enforcement prevents feature coupling from day one
- JUnit 4/5 split is pragmatically correct and enforced structurally by Gradle
- `datastore-tink` covers the actual security threat model without undue alpha-library risk
- F-Droid compatible from day one; no structural changes needed for distribution expansion

**Areas for Future Enhancement:**
- Multi-server support: migrate server URL + credentials to a Room `ServerConfig` entity
- Recipe browsing (v2): add `:feature:recipe` module; no changes to existing modules required
- Screenshot testing: add Paparazzi or Roborazzi once the component library stabilises
- F-Droid release metadata: add `metadata/` directory with fastlane-compatible structure

### Implementation Handoff

**AI Agent Guidelines:**
- Follow all architectural decisions exactly as documented
- Use implementation patterns consistently across all components
- Respect module dependency boundaries - Gradle compilation will fail if boundaries are violated
- Cross-reference `docs/openapi.json` for field nullability when writing DTO model classes
- GAP-1 resolution (AppOrchestrator/MainActivity wiring in `:app`) must be in place before
  `ConnectivityMonitor` is connected to `SyncScheduler`

**First Implementation Priority:**

Android Studio → New Project → Empty Activity (Min SDK API 26, Kotlin, Kotlin DSL).
Then: `settings.gradle.kts` module declarations, `libs.versions.toml`, root `build.gradle.kts`,
per-module `build.gradle.kts` files with correct dependency declarations enforcing the boundary rules.
This scaffolding is Story 1 of the implementation backlog.
