# Story 1.3: Server URL Entry and Connection Validation

Status: review

## Story

As a first-time user,
I want to enter my Mealie server address and have it verified,
so that I know the app can reach my server before I enter credentials.

## Acceptance Criteria

1. **Given** the user opens the app for the first time (no server URL stored)
   **When** the app launches
   **Then** `ServerUrlScreen` is displayed with an empty URL input field and a "Connect" button

2. **Given** the user enters a URL with a trailing slash (e.g. `https://mealie.example.com/`)
   **When** they tap "Connect"
   **Then** the trailing slash is silently stripped before the probe is sent
   **And** the normalized URL is shown in the field

3. **Given** the user enters a bare hostname or IP with port (e.g. `192.168.1.100:9925` or `mealie.local`)
   **When** they tap "Connect"
   **Then** `https://` is prepended automatically before the probe is sent
   **And** the normalized URL is shown in the field

4. **Given** the user enters a malformed string (invalid characters, not a recognizable host)
   **When** they tap "Connect"
   **Then** an inline error "Enter a valid URL (e.g. https://mealie.example.com)" is shown
   **And** no network request is made

5. **Given** the user enters a well-formed URL
   **When** they tap "Connect"
   **Then** a loading indicator replaces the button
   **And** the app sends `GET /api/app/about` to that URL

6. **Given** the probe returns HTTP 200 with a Mealie `version` field in the response body
   **When** the response is parsed
   **Then** the URL is persisted to Preferences DataStore
   **And** the screen navigates to the HTTP warning check route (Story 1.4's destination route is declared here)

7. **Given** the probe fails (timeout, DNS failure, non-200, or no `version` field)
   **When** the error is received
   **Then** a specific error message is shown ("Could not reach server" / "Not a Mealie server")
   **And** the input remains editable for correction

8. **Given** a server URL is already stored in DataStore
   **When** the app launches
   **Then** `ServerUrlScreen` is skipped and the app proceeds directly to the next auth route

## Tasks / Subtasks

- [x] Task 1: Create `ApiResult` sealed class in `:core:network` (AC: 5, 6, 7)
  - [x] Create `core/network/src/main/java/dev/xexanos/mealie/core/network/result/ApiResult.kt` with `Success<T>`, `NetworkError`, `AuthError`, `HttpError(code, detail)` variants (exact definition in Dev Notes)

- [x] Task 2: Create `AppAboutDto` and `AppService` in `:core:network` (AC: 5, 6, 7)
  - [x] Create `core/network/src/main/java/dev/xexanos/mealie/core/network/dto/AppAboutDto.kt`
  - [x] Create `core/network/src/main/java/dev/xexanos/mealie/core/network/api/AppService.kt`

- [x] Task 3: Update `NetworkModule` to provide `OkHttpClient` and `Json` (AC: 5)
  - [x] Add `buildFeatures { buildConfig = true }` to `core/network/build.gradle.kts`
  - [x] Replace the empty `networkModule` in `NetworkModule.kt` with a real implementation providing `OkHttpClient` and `Json` (see Dev Notes for exact code)

- [x] Task 4: Create `AppPreferencesStore` in `:core:data` (AC: 6, 8)
  - [x] Create `core/data/src/main/java/dev/xexanos/mealie/core/data/datastore/AppPreferencesStore.kt`
    - Uses unencrypted `datastore-preferences` (not tink)
    - Keys: `SERVER_URL_KEY`, `HTTP_WARNING_ACK_URLS_KEY` (declare key now; read/write for HTTP ack in Story 1.4)
    - Methods: `fun getServerUrl(): Flow<String?>`, `suspend fun setServerUrl(url: String)`
  - [x] Update `DataModule.kt`: add `single { AppPreferencesStore(androidContext()) }`

- [x] Task 5: Create `UrlProbeResult` and `AuthRepository` in `:core:data` (AC: 5, 6, 7, 8)
  - [x] Create `core/data/src/main/java/dev/xexanos/mealie/core/data/domain/UrlProbeResult.kt` (sealed class with `Success`, `NetworkError`, `NotMealieServer`)
  - [x] Create `core/data/src/main/java/dev/xexanos/mealie/core/data/repository/AuthRepository.kt` (interface — see Dev Notes)
  - [x] Create `core/data/src/main/java/dev/xexanos/mealie/core/data/repository/AuthRepositoryImpl.kt` (see Dev Notes for complete probe pattern)
  - [x] Update `DataModule.kt`: add `single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }`

- [x] Task 6: Create ViewModel, UiState, and UiEvent in `:feature:auth` (AC: 1–8)
  - [x] Add to `feature/auth/build.gradle.kts`: `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`, `testRuntimeOnly(libs.junit.platform.launcher)`, `testImplementation(kotlin("test"))`
  - [x] Create `feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/ServerUrlUiState.kt`
  - [x] Create `feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/ServerUrlUiEvent.kt`
  - [x] Create `feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/ServerUrlViewModel.kt` (see Dev Notes)
  - [x] Update `AuthFeatureModule.kt`: add `viewModel { ServerUrlViewModel(get()) }`

- [x] Task 7: Create `ServerUrlScreen` composable in `:feature:auth` (AC: 1–8, UX-DR11)
  - [x] Create `feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/ServerUrlScreen.kt` (see Dev Notes for layout and UX requirements)

- [x] Task 8: Update `AuthNavGraph` to wire `ServerUrlScreen` and declare `HttpWarningCheckRoute` (AC: 1, 6, 8)
  - [x] Delete `@Serializable object AuthPlaceholder` — replaced by `ServerUrlRoute`
  - [x] Add `@Serializable object ServerUrlRoute` and `@Serializable object HttpWarningCheckRoute`
  - [x] Update `authGraph()`: `startDestination = ServerUrlRoute`, wire `ServerUrlScreen`, add placeholder `composable<HttpWarningCheckRoute>` (Story 1.4 fills it in)
  - [x] Use `popUpTo(ServerUrlRoute) { inclusive = true }` when navigating forward so back does not return to URL screen

- [x] Task 9: Write unit tests for `ServerUrlViewModel` (AC: 2, 3, 4, 7, 8)
  - [x] Create `feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/testutil/MainDispatcherExtension.kt` (copy pattern from Story 1.2, do NOT import from `:core:ui`)
  - [x] Create `feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/ui/FakeAuthRepository.kt`
  - [x] Create `feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/ui/ServerUrlViewModelTest.kt`
    - `normalizeUrl`: trailing slash stripped, bare IP gets `https://`, blank input returns null
    - malformed input → `InputError` state, `FakeAuthRepository.probeServerUrl` NOT called
    - `UrlProbeResult.Success` → `NavigateToNext` event emitted
    - `UrlProbeResult.NetworkError` → `InputError("Could not reach server")`
    - `UrlProbeResult.NotMealieServer` → `InputError("Not a Mealie server")`
    - URL already stored on init → `NavigateToNext` event emitted without `onConnect` being called

- [x] Task 10: Verify build and tests pass
  - [x] `./gradlew assembleDebug` — BUILD SUCCESSFUL
  - [x] `./gradlew :feature:auth:test` — all tests pass
  - [x] `./gradlew ktlintCheck detekt lint` — all pass

## Dev Notes

### URL Normalization Algorithm

Apply in `ServerUrlViewModel.normalizeUrl(raw: String): String?`. Return `null` triggers `InputError` with no network call.

```kotlin
internal fun normalizeUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null

    val withScheme = when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        else -> "https://$trimmed"
    }

    val stripped = withScheme.trimEnd('/')

    return try {
        val uri = android.net.Uri.parse(stripped)
        if (uri.host.isNullOrBlank()) null else stripped
    } catch (e: Exception) {
        null
    }
}
```

`internal` visibility allows direct testing from `feature/auth` test source set without exposing as public API.

### `ApiResult.kt` — Complete Implementation

```kotlin
// core/network/src/main/java/dev/xexanos/mealie/core/network/result/ApiResult.kt
package dev.xexanos.mealie.core.network.result

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data object NetworkError : ApiResult<Nothing>()   // IOException, timeout, unreachable
    data object AuthError : ApiResult<Nothing>()      // 401 not resolved by Authenticator (Stories 1.5+)
    data class HttpError(
        val code: Int,
        val detail: String?
    ) : ApiResult<Nothing>()
}
```

`AuthError` is not used in this story but declared now for all future network interactions.

### `AppAboutDto.kt`

```kotlin
// core/network/src/main/java/dev/xexanos/mealie/core/network/dto/AppAboutDto.kt
package dev.xexanos.mealie.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AppAboutDto(
    val version: String? = null,
    val production: Boolean? = null,
)
```

`Json { ignoreUnknownKeys = true }` handles the full Mealie response. Only `version` is checked to confirm it's a Mealie server. All other fields can be added later if needed.

### `AppService.kt`

```kotlin
// core/network/src/main/java/dev/xexanos/mealie/core/network/api/AppService.kt
package dev.xexanos.mealie.core.network.api

import dev.xexanos.mealie.core.network.dto.AppAboutDto
import retrofit2.http.GET

interface AppService {
    @GET("api/app/about")
    suspend fun getAppAbout(): AppAboutDto
}
```

No auth header needed — this endpoint is public on all Mealie instances.

### `NetworkModule.kt` — Complete Replacement

```kotlin
// core/network/src/main/java/dev/xexanos/mealie/core/network/di/NetworkModule.kt
package dev.xexanos.mealie.core.network.di

import dev.xexanos.mealie.core.network.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val networkModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }
    single { buildOkHttpClient() }
}

private fun buildOkHttpClient(): OkHttpClient {
    val builder = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
    if (BuildConfig.DEBUG) {
        builder.addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
                redactHeader("Authorization")
            }
        )
    }
    return builder.build()
}
```

`BuildConfig.DEBUG` requires `buildFeatures { buildConfig = true }` in `core/network/build.gradle.kts` — add it to the `android { }` block. Library modules have `BuildConfig.DEBUG = true` when the consuming app is built as debug, `false` for release. This is correct behavior.

### `AppPreferencesStore.kt` — Complete Implementation

```kotlin
// core/data/src/main/java/dev/xexanos/mealie/core/data/datastore/AppPreferencesStore.kt
package dev.xexanos.mealie.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "app_preferences")

class AppPreferencesStore(private val context: Context) {
    companion object {
        val SERVER_URL_KEY = stringPreferencesKey("server_url")
        val HTTP_WARNING_ACK_URLS_KEY = stringSetPreferencesKey("http_warning_ack_urls")
    }

    fun getServerUrl(): Flow<String?> =
        context.appPreferencesDataStore.data.map { it[SERVER_URL_KEY] }

    suspend fun setServerUrl(url: String) {
        context.appPreferencesDataStore.edit { it[SERVER_URL_KEY] = url }
    }
}
```

`HTTP_WARNING_ACK_URLS_KEY` is declared here but Story 1.4 adds the read/write methods. Do not implement them in this story.

This is the **unencrypted** DataStore. Server URL is not a secret. The encrypted `CredentialsStore` (username, password, token) is a separate DataStore instance created in Story 1.5.

### `UrlProbeResult.kt`

```kotlin
// core/data/src/main/java/dev/xexanos/mealie/core/data/domain/UrlProbeResult.kt
package dev.xexanos.mealie.core.data.domain

sealed class UrlProbeResult {
    data object Success : UrlProbeResult()
    data object NetworkError : UrlProbeResult()
    data object NotMealieServer : UrlProbeResult()
}
```

Domain-level result type — ViewModels never see `ApiResult` directly (architecture pattern).

### `AuthRepository.kt` Interface

```kotlin
// core/data/src/main/java/dev/xexanos/mealie/core/data/repository/AuthRepository.kt
package dev.xexanos.mealie.core.data.repository

import dev.xexanos.mealie.core.data.domain.UrlProbeResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getStoredServerUrl(): Flow<String?>
    suspend fun probeServerUrl(url: String): UrlProbeResult
    suspend fun saveServerUrl(url: String)
}
```

This interface will grow in Stories 1.5+ (credential storage, token operations).

### `AuthRepositoryImpl.kt` — Probe Pattern

```kotlin
class AuthRepositoryImpl(
    private val appPreferencesStore: AppPreferencesStore,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) : AuthRepository {

    override fun getStoredServerUrl(): Flow<String?> =
        appPreferencesStore.getServerUrl()

    override suspend fun saveServerUrl(url: String) =
        appPreferencesStore.setServerUrl(url)

    override suspend fun probeServerUrl(url: String): UrlProbeResult {
        return try {
            val retrofit = Retrofit.Builder()
                .baseUrl("$url/")
                .client(okHttpClient)
                .addConverterFactory(
                    json.asConverterFactory("application/json".toMediaType())
                )
                .build()
            val service = retrofit.create(AppService::class.java)
            val about = service.getAppAbout()
            if (about.version != null) UrlProbeResult.Success
            else UrlProbeResult.NotMealieServer
        } catch (e: IOException) {
            UrlProbeResult.NetworkError
        } catch (e: Exception) {
            UrlProbeResult.NotMealieServer
        }
    }
}
```

**Imports for `asConverterFactory`:**
```kotlin
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
```

**`$url/`:** Retrofit requires base URLs to end with `/`. The normalized URL passed here has trailing slashes stripped by the ViewModel, so appending `/` is correct.

**Why a temporary Retrofit:** The server URL is user-supplied and not yet stored. Story 1.5 creates the persistent `MealieRetrofit` instance with the stored URL. This one-off Retrofit is only for the probe — it is not stored or reused.

**Exception mapping:** `IOException` = network unreachable/timeout → `NetworkError`. Any other exception (e.g. `SerializationException` from unexpected response shape) → `NotMealieServer`.

### `ServerUrlUiState.kt`

```kotlin
sealed class ServerUrlUiState {
    data object Loading : ServerUrlUiState()
    data object AwaitingInput : ServerUrlUiState()
    data class Probing(val normalizedUrl: String) : ServerUrlUiState()
    data class InputError(val message: String, val lastUrl: String = "") : ServerUrlUiState()
}
```

### `ServerUrlUiEvent.kt`

```kotlin
sealed class ServerUrlUiEvent {
    data object NavigateToNext : ServerUrlUiEvent()
}
```

### `ServerUrlViewModel.kt` — Complete Implementation

```kotlin
class ServerUrlViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ServerUrlUiState>(ServerUrlUiState.Loading)
    val uiState: StateFlow<ServerUrlUiState> = _uiState.asStateFlow()

    private val _events = Channel<ServerUrlUiEvent>(Channel.BUFFERED)
    val events: Flow<ServerUrlUiEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val storedUrl = authRepository.getStoredServerUrl().first()
            if (storedUrl != null) {
                _events.send(ServerUrlUiEvent.NavigateToNext)
            } else {
                _uiState.value = ServerUrlUiState.AwaitingInput
            }
        }
    }

    fun onConnect(rawUrl: String) {
        val normalized = normalizeUrl(rawUrl)
        if (normalized == null) {
            _uiState.value = ServerUrlUiState.InputError(
                message = "Enter a valid URL (e.g. https://mealie.example.com)",
                lastUrl = rawUrl
            )
            return
        }
        _uiState.value = ServerUrlUiState.Probing(normalized)
        viewModelScope.launch {
            when (authRepository.probeServerUrl(normalized)) {
                UrlProbeResult.Success -> {
                    authRepository.saveServerUrl(normalized)
                    _events.send(ServerUrlUiEvent.NavigateToNext)
                }
                UrlProbeResult.NetworkError -> {
                    _uiState.value = ServerUrlUiState.InputError(
                        message = "Could not reach server",
                        lastUrl = normalized
                    )
                }
                UrlProbeResult.NotMealieServer -> {
                    _uiState.value = ServerUrlUiState.InputError(
                        message = "Not a Mealie server",
                        lastUrl = normalized
                    )
                }
            }
        }
    }

    internal fun normalizeUrl(raw: String): String? { /* see algorithm above */ }
}
```

Use `kotlinx.coroutines.flow.first()` (not `.collect {}`) to get one emission for the init check.

### `ServerUrlScreen.kt` — UX Requirements (UX-DR11)

```kotlin
@Composable
fun ServerUrlScreen(
    onNavigateToNext: () -> Unit,
    viewModel: ServerUrlViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel.events, lifecycleOwner) {
        viewModel.events.flowWithLifecycle(lifecycleOwner.lifecycle).collect { event ->
            when (event) {
                ServerUrlUiEvent.NavigateToNext -> onNavigateToNext()
            }
        }
    }

    // ... layout
}
```

**Layout (per UX-DR11):**
- `Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = CenterHorizontally)`
- Content constrained to `Modifier.widthIn(max = 600.dp)` for large screens
- `OutlinedTextField`: full width, label "Server URL", `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go)`, `keyboardActions = KeyboardActions(onGo = { viewModel.onConnect(urlText) })`; `value` initialized from `uiState.lastUrl` when `is InputError`, else current user input
- Inline error text (not a toast, not a dialog): `Text("message", color = MaterialTheme.colorScheme.error)` shown when `uiState is InputError`
- `Button("Connect")` full width, min 48dp: shows `CircularProgressIndicator` instead of text when `uiState is Probing`; disabled when `Probing`
- When `uiState is Loading`: render empty `Box(Modifier.fillMaxSize())` - the init event fires quickly

**Tip:** When state transitions from `InputError` back to `AwaitingInput` (user edits the field), pre-fill the URL field with `uiState.lastUrl` so the user's input is not lost.

`koinViewModel()` requires `koin-androidx-compose` (already in `feature/auth/build.gradle.kts`).

### `AuthNavGraph.kt` — Updated

```kotlin
@Serializable object AuthGraph
@Serializable object ServerUrlRoute
@Serializable object HttpWarningCheckRoute   // Story 1.4 fills in the screen body

fun NavGraphBuilder.authGraph(navController: NavController) {
    navigation<AuthGraph>(startDestination = ServerUrlRoute) {
        composable<ServerUrlRoute> {
            ServerUrlScreen(
                onNavigateToNext = {
                    navController.navigate(HttpWarningCheckRoute) {
                        popUpTo(ServerUrlRoute) { inclusive = true }
                    }
                }
            )
        }
        composable<HttpWarningCheckRoute> {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
```

**Delete `@Serializable object AuthPlaceholder`** — it was a temporary Story 1.2 placeholder. `AppNavGraph.kt` references `AuthGraph` (unchanged) and `authGraph(navController)` (unchanged) — no edits needed there.

`popUpTo(ServerUrlRoute) { inclusive = true }`: removes `ServerUrlScreen` from the back stack. User cannot navigate back to URL entry after proceeding.

### `DataModule.kt` — Updated

```kotlin
val dataModule = module {
    single { AppPreferencesStore(androidContext()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
}
```

The three `get()` calls inject: `AppPreferencesStore` (this module), `OkHttpClient` (networkModule), `Json` (networkModule). `:core:data` depends on `:core:network`, so cross-module Koin injection is valid.

### `feature/auth/build.gradle.kts` Additions

Add to `dependencies`:
```kotlin
implementation(libs.lifecycle.viewmodel.compose)
implementation(libs.lifecycle.runtime.compose)
testRuntimeOnly(libs.junit.platform.launcher)
testImplementation(kotlin("test"))
```

### Testing Pattern

```kotlin
// FakeAuthRepository.kt (test source)
class FakeAuthRepository(
    private var storedUrl: String? = null,
    private var probeResult: UrlProbeResult = UrlProbeResult.Success,
) : AuthRepository {
    var probeCallCount = 0
    override fun getStoredServerUrl() = flowOf(storedUrl)
    override suspend fun probeServerUrl(url: String): UrlProbeResult {
        probeCallCount++
        return probeResult
    }
    override suspend fun saveServerUrl(url: String) { storedUrl = url }
}
```

Use `@ExtendWith(MainDispatcherExtension::class)` (create this class in `feature/auth/src/test/.../testutil/` — same pattern as Story 1.2's `core:ui` version, but a fresh copy since cross-module test source is not importable).

Use `runTest { viewModel.events.test { ... } }` (Turbine) for event assertions.

### What NOT to Create in This Story

- No `TokenStore`, `CredentialsStore`, `datastore-tink` usage — Story 1.5
- No `AuthService` (`/api/auth/token`, `/api/auth/refresh`) — Story 1.5+
- No `CredentialScreen`, `ReAuthScreen` — Stories 1.5, 1.8
- No `HttpWarningCheckScreen` body — Story 1.4 implements it
- No `MealieAuthenticator`, `TokenManager`, `Mutex` — Stories 1.5–1.7
- No `ConnectivityMonitor` — Story 2.9
- No Room database — Story 2.1+
- Do NOT implement `HTTP_WARNING_ACK_URLS_KEY` read/write methods — declare the key only
- Do NOT add a persistent `Retrofit` bean to `NetworkModule` — the app-wide Retrofit is Story 1.5 (needs stored base URL)
- Do NOT edit `AppNavGraph.kt` — `authGraph(navController)` call and `startDestination = AuthGraph` remain unchanged

### Project Structure Notes

- Source directories are `java/` not `kotlin/` — consistent with all existing project files
- Package: `dev.xexanos.mealie.feature.auth.ui` for screen/viewmodel/state files
- Package: `dev.xexanos.mealie.core.data.domain` for `UrlProbeResult`
- Package: `dev.xexanos.mealie.core.data.datastore` for `AppPreferencesStore`
- Package: `dev.xexanos.mealie.core.data.repository` for `AuthRepository` interface + impl
- Package: `dev.xexanos.mealie.core.network.result` for `ApiResult`
- `MainDispatcherExtension` for feature:auth tests: `feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/testutil/MainDispatcherExtension.kt`

### References

- [Source: epics.md#Story 1.3] - Acceptance criteria and user story
- [Source: architecture.md#Authentication & Security] - `ApiResult` definition, DataStore boundary rules
- [Source: architecture.md#Data Architecture] - `AppPreferencesStore` (unencrypted), HTTP warning ack stored per URL; `CredentialsStore` scope (Story 1.5)
- [Source: architecture.md#API & Communication Patterns] - `AppService`, single `OkHttpClient`, debug logging with `redactHeader("Authorization")`, OkHttp timeouts (10s/30s/15s), URL normalization applied at `feature:auth` input time
- [Source: architecture.md#Frontend Architecture] - Sealed `UiState` pattern for auth screens, `Channel` for one-shot events, `StateFlow` exposed from ViewModel, `collectAsStateWithLifecycle()`
- [Source: architecture.md#Naming Conventions] - Repository interface/impl naming, domain type in `core:data/domain/`
- [Source: ux-design-specification.md#UX-DR11] - Inline errors, spinner on button only, URL normalization, two-screen setup
- [Source: story 1-2 Dev Notes] - No `kotlin.android` plugin (AGP 9+), `Timber` as `implementation`, `MainDispatcherExtension` pattern, `junit-platform-launcher` required, source dirs are `java/`
- [Source: story 1-2 Debug Log] - `testRuntimeOnly(libs.junit.platform.launcher)` and `testImplementation(kotlin("test"))` required in test-enabled modules

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

- Used `java.net.URI` instead of `android.net.Uri` in `normalizeUrl` - `android.net.Uri` is not available in JVM unit tests (host always null), breaking `normalizeUrl` tests and Turbine event assertions.
- Added Retrofit, OkHttp, and kotlinx-serialization-json as explicit `implementation` dependencies to `core/data/build.gradle.kts` - these are declared as `implementation` (not `api`) in `:core:network`, so they are not transitively visible to `:core:data`.
- Extracted OkHttp timeout values into named constants to satisfy detekt `MagicNumber` rule.

### Completion Notes List

All 10 tasks and subtasks complete. 11 unit tests pass covering URL normalization, probe outcomes, and stored-URL init flow. `assembleDebug`, `test`, `ktlintCheck`, `detekt`, and `lint` all pass.

### File List

core/network/build.gradle.kts
core/network/src/main/java/dev/xexanos/mealie/core/network/result/ApiResult.kt
core/network/src/main/java/dev/xexanos/mealie/core/network/dto/AppAboutDto.kt
core/network/src/main/java/dev/xexanos/mealie/core/network/api/AppService.kt
core/network/src/main/java/dev/xexanos/mealie/core/network/di/NetworkModule.kt
core/data/build.gradle.kts
core/data/src/main/java/dev/xexanos/mealie/core/data/datastore/AppPreferencesStore.kt
core/data/src/main/java/dev/xexanos/mealie/core/data/domain/UrlProbeResult.kt
core/data/src/main/java/dev/xexanos/mealie/core/data/repository/AuthRepository.kt
core/data/src/main/java/dev/xexanos/mealie/core/data/repository/AuthRepositoryImpl.kt
core/data/src/main/java/dev/xexanos/mealie/core/data/di/DataModule.kt
feature/auth/build.gradle.kts
feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/ServerUrlUiState.kt
feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/ServerUrlUiEvent.kt
feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/ServerUrlViewModel.kt
feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/ServerUrlScreen.kt
feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/di/AuthFeatureModule.kt
feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/navigation/AuthNavGraph.kt
feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/testutil/MainDispatcherExtension.kt
feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/ui/FakeAuthRepository.kt
feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/ui/ServerUrlViewModelTest.kt


## Change Log

- Implemented story 1.3: network layer (ApiResult, AppAboutDto, AppService, NetworkModule), data layer (AppPreferencesStore, UrlProbeResult, AuthRepository/Impl, DataModule), auth feature (ServerUrlViewModel, ServerUrlScreen, AuthNavGraph with ServerUrlRoute/HttpWarningCheckRoute), 11 unit tests (Date: 2026-05-25)