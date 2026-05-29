# Story 1.6: Silent Token Refresh on App Launch

Status: ready-for-dev

## Story

As the app,
I want to silently restore an authenticated session on launch,
so that users remain logged in across app restarts without manual re-entry.

## Acceptance Criteria

1. **Given** a token and credentials are stored in encrypted DataStore
   **When** the app launches
   **Then** `TokenManager` first attempts `GET /api/auth/refresh` with the stored Bearer token
   **And** a loading indicator is shown during the attempt

2. **Given** the refresh request succeeds (HTTP 200 with new token)
   **When** the response is received
   **Then** the new token is encrypted and persisted to DataStore
   **And** the app navigates to the main app screen (destination implemented in Epic 2)

3. **Given** the refresh request fails with HTTP 401 (token already expired)
   **When** the 401 is received
   **Then** `TokenManager` falls back to `POST /api/auth/token` with stored username, password, and `remember_me: true`

4. **Given** the credential re-authentication succeeds (HTTP 200 with new token)
   **When** the response is received
   **Then** the new token is encrypted and persisted to DataStore
   **And** the app navigates to the main app screen

5. **Given** both refresh and credential re-authentication fail (credentials changed on server, network error)
   **When** both attempts complete
   **Then** all stored tokens are cleared from DataStore (credentials are kept)
   **And** the app navigates to `CredentialScreen` for manual re-entry (Story 1.8 flow)

6. **Given** no credentials are stored in DataStore
   **When** the app launches
   **Then** both refresh and re-auth are skipped
   **And** the app immediately navigates to `ServerUrlScreen`

7. **Given** stored credentials exist but the device is offline at launch
   **When** `TokenManager` attempts the refresh request
   **Then** the network call fails with a connectivity error (not a 401)
   **And** the app skips credential re-authentication
   **And** navigates to `PostAuthRoute` in offline-only mode (Local Store content only)

8. **Given** multiple launch attempts occur concurrently
   **When** `TokenManager` is invoked
   **Then** the `Mutex` ensures only one refresh/re-auth cycle runs at a time

9. **Given** all user-facing strings on the loading/splash screen
   **When** string resources are reviewed
   **Then** all strings use `stringResource(R.string.xyz)` from `:core:ui`
   **And** English and German translations exist in `values/strings.xml` and `values-de/strings.xml`

## Tasks / Subtasks

- [ ] Task 1: Add refresh endpoint to `AuthService` in `:core:network` (AC: 1, 2, 3)
  - [ ] Add `GET /api/auth/refresh` method with Bearer token header
  - [ ] Reuse existing `AuthTokenDto` for response parsing

- [ ] Task 2: Create `TokenManager` singleton in `:core:network` (AC: 1-5, 7, 8)
  - [ ] Create `core/network/src/main/java/dev/xexanos/mealie/core/network/auth/TokenManager.kt`
  - [ ] Implement `Mutex`-guarded refresh/re-auth logic
  - [ ] Register as `single {}` in `NetworkModule.kt`

- [ ] Task 3: Add `refreshToken()` and `reAuthenticate()` to `AuthRepository` (AC: 1-5, 7)
  - [ ] Add `suspend fun refreshToken(token: String): AuthResult` to interface
  - [ ] Add `suspend fun reAuthenticate(): AuthResult` to interface
  - [ ] Implement in `AuthRepositoryImpl` using `AuthService`
  - [ ] Add Mutex to `AuthRepositoryImpl` for concurrent access safety (deferred from 1-5)

- [ ] Task 4: Create `StartupAuthViewModel` or `StartupAuthUseCase` (AC: 1-7)
  - [ ] Orchestrate the startup auth flow: check stored state -> refresh -> fallback -> navigate
  - [ ] Expose sealed result state for the navigation layer

- [ ] Task 5: Create `StartupScreen` composable with loading indicator (AC: 1, 9)
  - [ ] Create `feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/StartupScreen.kt`
  - [ ] Show loading spinner while auth resolution is in progress
  - [ ] No user interaction possible - purely a transition screen

- [ ] Task 6: Wire startup navigation in `AppNavGraph` (AC: 1-7)
  - [ ] Add `StartupRoute` as the new start destination (replaces direct `AuthGraph`)
  - [ ] Route to `PostAuthRoute` on success (AC 2, 4, 7)
  - [ ] Route to `CredentialRoute` on auth failure (AC 5)
  - [ ] Route to `ServerUrlRoute`/`AuthGraph` when no credentials exist (AC 6)

- [ ] Task 7: Expose `applicationScope` via Koin (deferred from 1-2)
  - [ ] Register `applicationScope` as a named Koin singleton in `MealieApplication`
  - [ ] Use for any app-level coroutine launches (e.g., initial ConnectivityMonitor probe in future stories)

- [ ] Task 8: Add string resources (AC: 9)
  - [ ] Add English strings to `core/ui/src/main/res/values/strings.xml`
  - [ ] Add German strings to `core/ui/src/main/res/values-de/strings.xml`

- [ ] Task 9: Write unit tests (AC: 1-8)
  - [ ] TokenManager unit tests (refresh success, refresh 401 -> credential fallback, both fail, offline, Mutex concurrency)
  - [ ] StartupAuthViewModel/UseCase tests (routes correctly for each scenario)
  - [ ] AuthService contract test for refresh endpoint (MockWebServer)

- [ ] Task 10: Verify build, tests, and lint pass
  - [ ] `./gradlew assembleDebug`
  - [ ] `./gradlew :feature:auth:test :core:data:test :core:network:test`
  - [ ] `./gradlew ktlintCheck detekt lint`

## Dev Notes

### Mealie Auth Refresh API Contract

`GET /api/auth/refresh` uses the current Bearer token to obtain a fresh token. There is NO separate refresh token - the access token itself is used as authorization for the refresh call.

```
GET /api/auth/refresh
Authorization: Bearer <stored_access_token>
```

Response (200 OK):
```json
{
  "access_token": "eyJhbG...",
  "token_type": "bearer"
}
```

Response (401 Unauthorized) - token expired:
```json
{
  "detail": "Could not validate credentials"
}
```

The response DTO is identical to the login endpoint - reuse `AuthTokenDto`.

### TokenManager.kt

**Location:** `core/network/src/main/java/dev/xexanos/mealie/core/network/auth/TokenManager.kt`

**Architecture requirement:** Koin singleton in `:core:network`. Holds a `Mutex` for coroutine-level concurrent refresh safety. This is the central authority for token state.

```kotlin
package dev.xexanos.mealie.core.network.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TokenManager(
    private val tokenStore: TokenStore,
    private val credentialsStore: CredentialsStore,
    private val authRepository: AuthRepository,
) {
    private val mutex = Mutex()

    sealed class RefreshResult {
        data object Success : RefreshResult()
        data object CredentialsInvalid : RefreshResult()
        data object NoCredentials : RefreshResult()
        data object Offline : RefreshResult()
    }

    suspend fun ensureAuthenticated(): RefreshResult = mutex.withLock {
        // 1. Read stored token
        // 2. If no token/credentials -> NoCredentials
        // 3. Try refresh with stored token
        // 4. On 401 -> try re-auth with stored credentials
        // 5. On success -> save new token, return Success
        // 6. On network error -> Offline (skip re-auth)
        // 7. On both fail -> clear token, return CredentialsInvalid
    }
}
```

**CRITICAL dependency direction issue:** `TokenManager` lives in `:core:network` but needs `TokenStore` and `CredentialsStore` from `:core:data`. Since `:core:data` depends on `:core:network` (not the other way), `TokenManager` CANNOT directly import `TokenStore`/`CredentialsStore`.

**Resolution options (choose one):**
1. **Interface in `:core:network`, implementation in `:core:data`:** Define `TokenProvider`/`CredentialProvider` interfaces in `:core:network` that `TokenStore`/`CredentialsStore` implement. Wire via Koin.
2. **Move TokenManager to `:core:data`:** Since `:core:data` already depends on `:core:network`, TokenManager can access both layers. But architecture says TokenManager belongs in `:core:network`.
3. **Pass lambdas/flows via Koin:** TokenManager accepts `Flow<StoredToken>` and `suspend (String) -> Unit` for save, provided by Koin wiring in `:app`.

**Recommended:** Option 1 - define interfaces. This matches the architecture doc's placement (`core/network/auth/TokenManager.kt`) and keeps module boundaries clean:

```kotlin
// In :core:network
interface TokenProvider {
    fun getToken(): Flow<StoredToken>
    suspend fun saveToken(accessToken: String)
    suspend fun clearToken()
}

interface CredentialProvider {
    fun getCredentials(): Flow<StoredCredentials>
}
```

Then `TokenStore` implements `TokenProvider` and `CredentialsStore` implements `CredentialProvider`. Koin binds them.

### AuthService.kt - Refresh Endpoint Addition

Add to existing `AuthService`:

```kotlin
@GET("api/auth/refresh")
suspend fun refreshToken(
    @Header("Authorization") bearerToken: String,
): Response<AuthTokenDto>
```

**Why `@Header` parameter instead of OkHttp interceptor:** At this point no global Authenticator/Interceptor exists (that's Story 1-7). The refresh endpoint needs the stored token explicitly passed. Pass as `"Bearer $token"` from the caller.

### AuthRepository - New Methods

Add to `AuthRepository.kt` interface:

```kotlin
suspend fun refreshToken(token: String): AuthResult
suspend fun reAuthenticateWithStoredCredentials(): AuthResult
```

Implementation in `AuthRepositoryImpl`:
- `refreshToken(token)`: Creates AuthService, calls `refreshToken("Bearer $token")`, on 200 saves new token and returns Success, on 401 returns InvalidCredentials, on IOException returns NetworkError
- `reAuthenticateWithStoredCredentials()`: Reads credentials from `CredentialsStore`, calls existing `login()`, saves new token on success

**Concurrency:** Add a `Mutex` to `AuthRepositoryImpl` wrapping network calls (deferred from story 1-5 review). This prevents concurrent `authenticate()` + `refreshToken()` calls from racing.

### Startup Flow and Navigation

**Current flow:** `AppNavGraph` starts at `AuthGraph` which shows `ServerUrlScreen` -> `HttpWarningCheckScreen` -> `CredentialScreen` -> `PostAuthRoute`.

**New flow with Story 1-6:**
1. App starts -> shows `StartupRoute` (loading spinner)
2. `StartupAuthViewModel` calls `TokenManager.ensureAuthenticated()`
3. Based on result:
   - `Success` -> navigate to `PostAuthRoute` (pop startup from backstack)
   - `Offline` -> navigate to `PostAuthRoute` in offline mode (same destination, no connectivity)
   - `CredentialsInvalid` -> navigate to `CredentialRoute` directly (server URL is already stored)
   - `NoCredentials` -> navigate to `AuthGraph` (full setup flow starting at ServerUrlScreen)

**StartupRoute placement:** Add as a new route in `AppNavGraph.kt`, set as the new start destination of the NavHost. The `AuthGraph` nested graph remains unchanged.

```kotlin
@Serializable object StartupRoute

NavHost(startDestination = StartupRoute) {
    composable<StartupRoute> {
        StartupScreen(
            onNavigateToMain = { navController.navigate(PostAuthRoute) { popUpTo(StartupRoute) { inclusive = true } } },
            onNavigateToCredentials = { navController.navigate(CredentialRoute) { popUpTo(StartupRoute) { inclusive = true } } },
            onNavigateToSetup = { navController.navigate(AuthGraph) { popUpTo(StartupRoute) { inclusive = true } } },
        )
    }
    authGraph(onAuthComplete = { ... })
    composable<PostAuthRoute> { ... }
}
```

**Problem: `CredentialRoute` is inside `AuthGraph` nested graph.** You cannot navigate directly to a nested destination from outside. Two options:
1. Move `CredentialRoute` to a top-level destination accessible from StartupRoute
2. Navigate to `AuthGraph` but pass a "skip to credentials" flag

**Recommended:** Navigate to `AuthGraph` and add logic to skip `ServerUrlScreen` if server URL already exists. The existing `ServerUrlScreen` already has AC "Given a server URL is already stored in DataStore, When the app launches, Then ServerUrlScreen is skipped" from Story 1-3. So navigating to `AuthGraph` when credentials are invalid should auto-skip to the credential screen. Verify this behavior exists.

### StartupScreen.kt

Minimal composable - just a centered loading spinner. No user interaction.

```kotlin
@Composable
fun StartupScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToCredentials: () -> Unit,
    onNavigateToSetup: () -> Unit,
    viewModel: StartupAuthViewModel = koinViewModel(),
) {
    val event by viewModel.event.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(event) {
        when (event) {
            StartupAuthEvent.NavigateToMain -> onNavigateToMain()
            StartupAuthEvent.NavigateToCredentials -> onNavigateToCredentials()
            StartupAuthEvent.NavigateToSetup -> onNavigateToSetup()
            null -> {} // still loading
        }
    }

    // Full-screen centered CircularProgressIndicator
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
```

### StartupAuthViewModel.kt

```kotlin
class StartupAuthViewModel(
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _event = Channel<StartupAuthEvent>(Channel.BUFFERED)
    val event: Flow<StartupAuthEvent> = _event.receiveAsFlow()

    init {
        viewModelScope.launch {
            val result = tokenManager.ensureAuthenticated()
            val navEvent = when (result) {
                TokenManager.RefreshResult.Success -> StartupAuthEvent.NavigateToMain
                TokenManager.RefreshResult.Offline -> StartupAuthEvent.NavigateToMain
                TokenManager.RefreshResult.CredentialsInvalid -> StartupAuthEvent.NavigateToCredentials
                TokenManager.RefreshResult.NoCredentials -> StartupAuthEvent.NavigateToSetup
            }
            _event.send(navEvent)
        }
    }
}

sealed class StartupAuthEvent {
    data object NavigateToMain : StartupAuthEvent()
    data object NavigateToCredentials : StartupAuthEvent()
    data object NavigateToSetup : StartupAuthEvent()
}
```

### Expose applicationScope via Koin (Deferred from Story 1-2)

In `MealieApplication.kt`:
```kotlin
startKoin {
    androidContext(this@MealieApplication)
    modules(
        networkModule,
        dataModule,
        syncModule,
        uiModule,
        authFeatureModule,
        module {
            single(named("applicationScope")) { applicationScope }
        }
    )
}
```

This is needed for future stories (ConnectivityMonitor initial probe in Story 2-9). Do it now to establish the pattern.

### TokenProvider/CredentialProvider Interface Pattern

In `:core:network`, create:
```kotlin
// core/network/src/main/java/dev/xexanos/mealie/core/network/auth/TokenProvider.kt
package dev.xexanos.mealie.core.network.auth

import kotlinx.coroutines.flow.Flow

data class StoredTokenData(val accessToken: String = "")
data class StoredCredentialData(val username: String = "", val password: String = "")

interface TokenProvider {
    fun getToken(): Flow<StoredTokenData>
    suspend fun saveToken(accessToken: String)
    suspend fun clearToken()
}

interface CredentialProvider {
    fun getCredentials(): Flow<StoredCredentialData>
}
```

In `:core:data`, `TokenStore` implements `TokenProvider` and `CredentialsStore` implements `CredentialProvider`.

**Alternative simpler approach:** Since `TokenManager` needs an `AuthService` to call the refresh endpoint, and `AuthService` is already in `:core:network`, you can have `TokenManager` accept simple functional interfaces or directly the Flows. Koin can wire `get<TokenStore>()` cast to `TokenProvider` in `:app` since `:app` sees all modules.

### AuthService Creation for TokenManager

`TokenManager` needs to call `AuthService.refreshToken()` and `AuthService.login()`. It needs a Retrofit `AuthService` instance, which requires the stored server URL.

**Pattern from previous stories:** `AuthRepositoryImpl.createAuthService(baseUrl)` builds Retrofit dynamically. `TokenManager` needs the same pattern - it must read the server URL and create an `AuthService`.

Options:
1. `TokenManager` accepts an `AuthRepository` interface (but then `:core:network` depends on `:core:data` - illegal)
2. `TokenManager` builds its own `AuthService` given OkHttpClient + Json + a server URL provider
3. The startup logic lives in a UseCase in `:core:data` that orchestrates `TokenStore`, `CredentialsStore`, `AuthRepository`

**Recommended: Option 3 - Move orchestration to `:core:data`.**

Create `StartupAuthUseCase` in `:core:data` that:
- Reads stored token from `TokenStore`
- Reads stored credentials from `CredentialsStore`
- Calls `AuthRepository.refreshToken(token)` and `AuthRepository.reAuthenticate()`
- Applies the Mutex for concurrent safety

Then `TokenManager` in `:core:network` becomes a simpler in-memory token holder (holds current session token, provides it to OkHttp Authenticator in Story 1-7). The startup orchestration lives where it has access to all dependencies.

**Final recommended architecture:**

```
:core:network/auth/TokenManager.kt  - simple in-memory token holder + Mutex
:core:data/domain/StartupAuthUseCase.kt  - orchestrates refresh flow using AuthRepository + stores
:feature:auth/ui/StartupAuthViewModel.kt  - calls StartupAuthUseCase, emits navigation events
```

### Offline Detection

AC 7 specifies: "the network call fails with a connectivity error (not a 401)". This means:
- `IOException` from OkHttp (connect timeout, DNS failure, no route to host) = offline
- HTTP 401 = expired token, attempt credential re-auth
- Any other HTTP error = treat as offline (server unreachable or broken)

When offline, skip the credential fallback entirely. Navigate to main in offline-only mode. The rationale: if the server is unreachable, re-authenticating won't help either. Let the user see cached data.

### Existing CredentialScreen's `Loading` State

Story 1-5 explicitly kept `Loading` as a placeholder: "Unused `Loading` sealed class variant - kept as placeholder for Story 1-6 pre-loading". This was intended for a scenario where `CredentialScreen` would show a loading state during token refresh. However, with the separate `StartupScreen` approach, the `Loading` state in `CredentialUiState` remains unused by this story. It can be removed or kept for future use.

### Testing Strategy

**TokenManager / StartupAuthUseCase tests (JUnit 5 + MockK + Turbine):**
1. Stored token exists, refresh succeeds -> returns Success, saves new token
2. Stored token exists, refresh 401, credential re-auth succeeds -> returns Success
3. Stored token exists, refresh 401, credential re-auth fails -> clears token, returns CredentialsInvalid
4. Stored token exists, refresh throws IOException (offline) -> returns Offline, no re-auth attempted
5. No stored token/credentials -> returns NoCredentials
6. Concurrent calls: Mutex ensures single execution

**StartupAuthViewModel tests:**
1. Success result -> emits NavigateToMain
2. Offline result -> emits NavigateToMain
3. CredentialsInvalid -> emits NavigateToCredentials
4. NoCredentials -> emits NavigateToSetup

**AuthService contract test (MockWebServer):**
1. Refresh request sends correct Authorization header
2. Refresh 200 response parses `access_token`
3. Refresh 401 response detected correctly

### String Resources

Add to `core/ui/src/main/res/values/strings.xml`:
```xml
<!-- Startup Screen -->
<string name="startup_loading">Signing in...</string>
```

Add to `core/ui/src/main/res/values-de/strings.xml`:
```xml
<!-- Startup Screen -->
<string name="startup_loading">Anmeldung...</string>
```

### What NOT to Create in This Story

- No `MealieAuthenticator` (OkHttp Authenticator) - Story 1-7
- No re-authentication screen with UI - Story 1-8
- No ConnectivityMonitor - Story 2-9
- No Room database - Story 2-1
- No navigation to ShoppingListScreen (PostAuthRoute remains placeholder)
- Do NOT modify `ServerUrlScreen`, `HttpWarningCheckScreen`, or `CredentialScreen` unless strictly required for navigation wiring
- Do NOT add `GET /api/auth/refresh` to the global OkHttp interceptor chain - that's Story 1-7

### Project Structure Notes

- Source directories use `java/` not `kotlin/` (consistent with all existing project files)
- Package: `dev.xexanos.mealie.core.network.auth` for TokenManager
- Package: `dev.xexanos.mealie.core.data.domain` for StartupAuthUseCase
- Package: `dev.xexanos.mealie.feature.auth.ui` for StartupScreen, StartupAuthViewModel, StartupAuthEvent
- `MainDispatcherExtension` at `feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/testutil/`
- Design tokens: use `Spacing.*` from `:core:ui` for padding/margins
- Navigation pattern: `popUpTo(StartupRoute) { inclusive = true }` to clear startup from backstack

### Previous Story Intelligence

From Story 1-5:
- **AuthService uses `@FormUrlEncoded` for login** - NOT JSON body. The refresh endpoint is a GET so no body encoding needed.
- **Retrofit created dynamically** via `createAuthService(baseUrl)` since URL isn't compile-time known. TokenManager/StartupAuthUseCase must follow same pattern.
- **`Response<AuthTokenDto>`** return type for HTTP status code inspection. Follow same pattern for refresh.
- **Double-tap guard pattern:** Set state before launching coroutine.
- **`@StringRes` pattern:** UiState holds resource IDs, screen resolves with `stringResource()`.
- **ReplaceFileCorruptionHandler** on both stores - empty StoredToken/StoredCredentials means "no stored data" (check `accessToken.isNotEmpty()` to determine if a token exists).

From Story 1-5 deferred items to address:
- **Add Mutex to AuthRepositoryImpl** for concurrent access safety (new caller from TokenManager)
- **Expose applicationScope via Koin** (from Story 1-2 deferred)

### References

- [Source: epics.md#Story 1.6] - Acceptance criteria and user story
- [Source: architecture.md#Authentication & Security] - Mealie single-token model, TokenManager Mutex, concurrent refresh safety
- [Source: architecture.md#Core Architectural Decisions] - ApiResult sealed class, error propagation
- [Source: architecture.md#Frontend Architecture] - Channel for one-shot events, StateFlow, Navigation Compose
- [Source: architecture.md#Package Structure] - `:core:network/auth/`, `:core:data/domain/`
- [Source: architecture.md#Module Boundary Rules] - `:core:data` depends on `:core:network`, not vice versa
- [Source: story 1-5 Dev Notes] - AuthService contract, Retrofit creation pattern, encrypted DataStore pattern
- [Source: story 1-5 Review Findings] - Deferred Mutex for concurrent access, applicationScope exposure
- [Source: deferred-work.md] - applicationScope not in Koin (1-2), no concurrency guard (1-5)

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
