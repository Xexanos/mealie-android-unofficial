# Story 1.4: HTTP Security Warning for Non-HTTPS URLs

Status: ready-for-dev

## Story

As a user connecting to a self-hosted server over HTTP,
I want a clear, non-alarming explanation of the connection type,
so that I can proceed confidently without being scared off by unnecessary warnings.

## Acceptance Criteria

1. **Given** the validated server URL uses the `https://` scheme
   **When** the probe succeeds
   **Then** no warning is shown and the app navigates directly to credential entry

2. **Given** the validated server URL uses the `http://` scheme
   **When** the probe succeeds
   **Then** an inline informational message is shown beneath the URL field: "Connecting over your local network - this is common for self-hosted setups."
   **And** the message is styled as informational (not alarming, no error color)
   **And** a "Continue" button allows the user to proceed to credential entry

3. **Given** the user taps "Continue" on the HTTP warning
   **When** confirmed
   **Then** the URL and a "warning acknowledged" flag are persisted to `AppPreferencesStore` keyed by the URL
   **And** the app navigates to credential entry

4. **Given** the same `http://` URL is already stored with an acknowledged warning flag
   **When** the app launches or the URL is re-validated
   **Then** the inline warning is NOT shown again for that URL
   **And** the app proceeds directly to credential entry

5. **Given** the server URL changes to a different `http://` URL
   **When** the new URL is validated
   **Then** the one-time warning is shown again (new URL, no prior acknowledgement)

## Tasks / Subtasks

- [ ] Task 1: Add HTTP warning acknowledgement methods to `AppPreferencesStore` (AC: 3, 4, 5)
  - [ ] Add `fun getHttpWarningAckedUrls(): Flow<Set<String>>` reading from `HTTP_WARNING_ACK_URLS_KEY`
  - [ ] Add `suspend fun acknowledgeHttpWarning(url: String)` writing to `HTTP_WARNING_ACK_URLS_KEY`

- [ ] Task 2: Extend `AuthRepository` interface and implementation (AC: 3, 4, 5)
  - [ ] Add `suspend fun isHttpWarningAcknowledged(url: String): Boolean` to `AuthRepository`
  - [ ] Add `suspend fun acknowledgeHttpWarning(url: String)` to `AuthRepository`
  - [ ] Implement both methods in `AuthRepositoryImpl`

- [ ] Task 3: Create `HttpWarningCheckViewModel` with UiState and UiEvent (AC: 1-5)
  - [ ] Create `feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/HttpWarningCheckUiState.kt`
  - [ ] Create `feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/HttpWarningCheckUiEvent.kt`
  - [ ] Create `feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/HttpWarningCheckViewModel.kt`

- [ ] Task 4: Create `HttpWarningCheckScreen` composable (AC: 1-5, UX-DR12)
  - [ ] Create `feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/HttpWarningCheckScreen.kt`
  - [ ] Create `feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/HttpWarningCheckTestTags.kt`

- [ ] Task 5: Update `AuthNavGraph` to wire the real screen and declare `CredentialRoute` (AC: 1, 3)
  - [ ] Replace `Box(modifier = Modifier.fillMaxSize())` placeholder with `HttpWarningCheckScreen`
  - [ ] Add `@Serializable object CredentialRoute` (Story 1.5 fills it in)
  - [ ] Wire navigation: `HttpWarningCheckScreen` navigates to `CredentialRoute` on skip or continue
  - [ ] Add placeholder composable for `CredentialRoute`

- [ ] Task 6: Update `AuthFeatureModule` to register `HttpWarningCheckViewModel` (AC: all)
  - [ ] Add `viewModel { HttpWarningCheckViewModel(get()) }` to `authFeatureModule`

- [ ] Task 7: Write unit tests for `HttpWarningCheckViewModel` (AC: 1-5)
  - [ ] Update `FakeAuthRepository` with new methods
  - [ ] Create `feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/ui/HttpWarningCheckViewModelTest.kt`
  - [ ] Test: HTTPS URL on init emits NavigateToCredentials immediately
  - [ ] Test: HTTP URL already acknowledged on init emits NavigateToCredentials immediately
  - [ ] Test: HTTP URL not acknowledged on init transitions to ShowWarning state
  - [ ] Test: onContinue acknowledges URL and emits NavigateToCredentials
  - [ ] Test: Different HTTP URL after previous ack still shows warning

- [ ] Task 8: Verify build and tests pass
  - [ ] `./gradlew assembleDebug` - BUILD SUCCESSFUL
  - [ ] `./gradlew :feature:auth:test` - all tests pass
  - [ ] `./gradlew ktlintCheck detekt lint` - all pass

## Dev Notes

### Navigation Flow

The `HttpWarningCheckRoute` already exists in `AuthNavGraph.kt` and `ServerUrlScreen` already navigates to it on successful probe. This story replaces the empty placeholder with a real screen.

Flow: `ServerUrlScreen` -> (probe success) -> `HttpWarningCheckScreen` -> (skip or continue) -> `CredentialRoute` (Story 1.5 placeholder)

The `HttpWarningCheckScreen` is a pass-through gate: it checks scheme + acknowledgement state on init, and either immediately navigates forward (HTTPS or already-acked HTTP) or shows the warning UI.

### `AppPreferencesStore.kt` - Additions

```kotlin
fun getHttpWarningAckedUrls(): Flow<Set<String>> =
    context.appPreferencesDataStore.data.map { it[HTTP_WARNING_ACK_URLS_KEY] ?: emptySet() }

suspend fun acknowledgeHttpWarning(url: String) {
    context.appPreferencesDataStore.edit { prefs ->
        val current = prefs[HTTP_WARNING_ACK_URLS_KEY] ?: emptySet()
        prefs[HTTP_WARNING_ACK_URLS_KEY] = current + url
    }
}
```

`HTTP_WARNING_ACK_URLS_KEY` is already declared as `stringSetPreferencesKey("http_warning_ack_urls")`.

### `AuthRepository.kt` - Interface Additions

```kotlin
suspend fun isHttpWarningAcknowledged(url: String): Boolean
suspend fun acknowledgeHttpWarning(url: String)
```

### `AuthRepositoryImpl.kt` - Implementation

```kotlin
override suspend fun isHttpWarningAcknowledged(url: String): Boolean {
    val ackedUrls = appPreferencesStore.getHttpWarningAckedUrls().first()
    return url in ackedUrls
}

override suspend fun acknowledgeHttpWarning(url: String) {
    appPreferencesStore.acknowledgeHttpWarning(url)
}
```

Import `kotlinx.coroutines.flow.first` (already used in this file via `AuthRepositoryImpl` dependency on the Flow API).

### `HttpWarningCheckUiState.kt`

```kotlin
package dev.xexanos.mealie.feature.auth.ui

sealed class HttpWarningCheckUiState {
    data object Loading : HttpWarningCheckUiState()
    data class ShowWarning(val serverUrl: String) : HttpWarningCheckUiState()
}
```

`Loading` shown during the initial DataStore check. `ShowWarning` shown when HTTP URL is not yet acknowledged.

No `AwaitingInput` needed - this screen either auto-navigates or shows the warning.

### `HttpWarningCheckUiEvent.kt`

```kotlin
package dev.xexanos.mealie.feature.auth.ui

sealed class HttpWarningCheckUiEvent {
    data object NavigateToCredentials : HttpWarningCheckUiEvent()
}
```

### `HttpWarningCheckViewModel.kt`

```kotlin
package dev.xexanos.mealie.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.xexanos.mealie.core.data.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class HttpWarningCheckViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<HttpWarningCheckUiState>(HttpWarningCheckUiState.Loading)
    val uiState: StateFlow<HttpWarningCheckUiState> = _uiState.asStateFlow()

    private val _events = Channel<HttpWarningCheckUiEvent>(Channel.BUFFERED)
    val events: Flow<HttpWarningCheckUiEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val serverUrl = authRepository.getStoredServerUrl().first() ?: return@launch
            if (serverUrl.startsWith("https://", ignoreCase = true)) {
                _events.send(HttpWarningCheckUiEvent.NavigateToCredentials)
            } else if (authRepository.isHttpWarningAcknowledged(serverUrl)) {
                _events.send(HttpWarningCheckUiEvent.NavigateToCredentials)
            } else {
                _uiState.value = HttpWarningCheckUiState.ShowWarning(serverUrl)
            }
        }
    }

    fun onContinue() {
        val state = _uiState.value
        if (state !is HttpWarningCheckUiState.ShowWarning) return
        viewModelScope.launch {
            authRepository.acknowledgeHttpWarning(state.serverUrl)
            _events.send(HttpWarningCheckUiEvent.NavigateToCredentials)
        }
    }
}
```

Key design decisions:
- Reads `getStoredServerUrl()` from DataStore (set by Story 1.3 on successful probe)
- Uses `first()` to get the current value (not a continuous collection)
- HTTPS check is case-insensitive (Story 1.3's normalizeUrl lowercases the scheme, but defensive coding)
- `onContinue()` is a no-op if state is not `ShowWarning` (guards against double-tap)

### `HttpWarningCheckScreen.kt` - UX Requirements (UX-DR12)

```kotlin
@Composable
fun HttpWarningCheckScreen(
    onNavigateToCredentials: () -> Unit,
    viewModel: HttpWarningCheckViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel.events, lifecycleOwner) {
        viewModel.events.flowWithLifecycle(lifecycleOwner.lifecycle).collect { event ->
            when (event) {
                HttpWarningCheckUiEvent.NavigateToCredentials -> onNavigateToCredentials()
            }
        }
    }

    when (val state = uiState) {
        is HttpWarningCheckUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize())
        }
        is HttpWarningCheckUiState.ShowWarning -> {
            HttpWarningContent(
                serverUrl = state.serverUrl,
                onContinue = viewModel::onContinue,
            )
        }
    }
}

@Composable
private fun HttpWarningContent(
    serverUrl: String,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 600.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Display the URL (read-only context)
            Text(
                text = serverUrl,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(HttpWarningCheckTestTags.SERVER_URL_TEXT),
            )

            Spacer(modifier = Modifier.height(Spacing.Medium))

            // Informational message - NOT error colored (UX-DR12)
            Text(
                text = "Connecting over your local network \u2013 this is common for self-hosted setups.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(HttpWarningCheckTestTags.WARNING_MESSAGE),
            )

            Spacer(modifier = Modifier.height(Spacing.Large))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag(HttpWarningCheckTestTags.CONTINUE_BUTTON),
            ) {
                Text("Continue")
            }
        }
    }
}
```

**Critical UX-DR12 requirements:**
- Message tone: informational, non-alarming. Use `onSurfaceVariant` color (neutral, not error/warning)
- No modal dialog - inline content on the screen
- "Continue" button proceeds to credential entry
- Use en-dash in message text, not em-dash (per project feedback memory)

**Note on Spacing references:** Use `Spacing.ScreenPadding` (16.dp), `Spacing.Medium` (12.dp), `Spacing.Large` (24.dp) from `:core:ui` design tokens. If these don't exist yet, use raw dp values with a TODO comment for Story 1.2's design system - but check first; Story 1.2 created `MealieTheme`.

### `HttpWarningCheckTestTags.kt`

```kotlin
package dev.xexanos.mealie.feature.auth.ui

object HttpWarningCheckTestTags {
    const val SERVER_URL_TEXT = "httpWarningCheck_serverUrlText"
    const val WARNING_MESSAGE = "httpWarningCheck_warningMessage"
    const val CONTINUE_BUTTON = "httpWarningCheck_continueButton"
}
```

### `AuthNavGraph.kt` - Updated

```kotlin
@Serializable object AuthGraph
@Serializable object ServerUrlRoute
@Serializable object HttpWarningCheckRoute
@Serializable object CredentialRoute  // Story 1.5 fills in the screen body

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
            HttpWarningCheckScreen(
                onNavigateToCredentials = {
                    navController.navigate(CredentialRoute) {
                        popUpTo(HttpWarningCheckRoute) { inclusive = true }
                    }
                }
            )
        }
        composable<CredentialRoute> {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
```

- `popUpTo(HttpWarningCheckRoute) { inclusive = true }`: removes HttpWarningCheck from back stack. User cannot navigate back to it after proceeding.
- `CredentialRoute` placeholder - Story 1.5 replaces it with a real screen.

### `AuthFeatureModule.kt` - Updated

```kotlin
val authFeatureModule = module {
    viewModel { ServerUrlViewModel(get()) }
    viewModel { HttpWarningCheckViewModel(get()) }
}
```

### Testing Pattern - `FakeAuthRepository` Updates

Add to existing `FakeAuthRepository.kt`:

```kotlin
class FakeAuthRepository(
    private var storedUrl: String? = null,
    private var probeResult: UrlProbeResult = UrlProbeResult.Success,
    private val ackedUrls: MutableSet<String> = mutableSetOf(),
) : AuthRepository {
    var probeCallCount = 0
    override fun getStoredServerUrl() = flowOf(storedUrl)
    override suspend fun probeServerUrl(url: String): UrlProbeResult {
        probeCallCount++
        return probeResult
    }
    override suspend fun saveServerUrl(url: String) { storedUrl = url }
    override suspend fun isHttpWarningAcknowledged(url: String): Boolean = url in ackedUrls
    override suspend fun acknowledgeHttpWarning(url: String) { ackedUrls.add(url) }
}
```

### `HttpWarningCheckViewModelTest.kt` - Test Cases

```kotlin
@ExtendWith(MainDispatcherExtension::class)
class HttpWarningCheckViewModelTest {

    @Test
    fun `init with HTTPS URL emits NavigateToCredentials immediately`() = runTest {
        val fake = FakeAuthRepository(storedUrl = "https://mealie.example.com")
        val vm = HttpWarningCheckViewModel(fake)
        vm.events.test {
            assertEquals(HttpWarningCheckUiEvent.NavigateToCredentials, awaitItem())
        }
    }

    @Test
    fun `init with HTTP URL already acknowledged emits NavigateToCredentials`() = runTest {
        val fake = FakeAuthRepository(
            storedUrl = "http://192.168.1.100:9925",
            ackedUrls = mutableSetOf("http://192.168.1.100:9925"),
        )
        val vm = HttpWarningCheckViewModel(fake)
        vm.events.test {
            assertEquals(HttpWarningCheckUiEvent.NavigateToCredentials, awaitItem())
        }
    }

    @Test
    fun `init with HTTP URL not acknowledged shows warning`() = runTest {
        val fake = FakeAuthRepository(storedUrl = "http://192.168.1.100:9925")
        val vm = HttpWarningCheckViewModel(fake)
        val state = vm.uiState.value
        assert(state is HttpWarningCheckUiState.ShowWarning)
        assertEquals("http://192.168.1.100:9925", (state as HttpWarningCheckUiState.ShowWarning).serverUrl)
    }

    @Test
    fun `onContinue acknowledges URL and emits NavigateToCredentials`() = runTest {
        val fake = FakeAuthRepository(storedUrl = "http://192.168.1.100:9925")
        val vm = HttpWarningCheckViewModel(fake)
        vm.events.test {
            vm.onContinue()
            assertEquals(HttpWarningCheckUiEvent.NavigateToCredentials, awaitItem())
        }
        assert(fake.isHttpWarningAcknowledged("http://192.168.1.100:9925"))
    }

    @Test
    fun `different HTTP URL still shows warning even if previous URL was acked`() = runTest {
        val fake = FakeAuthRepository(
            storedUrl = "http://10.0.0.5:9000",
            ackedUrls = mutableSetOf("http://192.168.1.100:9925"),
        )
        val vm = HttpWarningCheckViewModel(fake)
        val state = vm.uiState.value
        assert(state is HttpWarningCheckUiState.ShowWarning)
    }
}
```

Use `@ExtendWith(MainDispatcherExtension::class)` from `feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/testutil/MainDispatcherExtension.kt` (already exists from Story 1.3).

### What NOT to Create in This Story

- No `CredentialScreen` or credential entry logic - Story 1.5
- No `TokenStore`, `CredentialsStore`, `datastore-tink` usage - Story 1.5
- No `AuthService` (`/api/auth/token`, `/api/auth/refresh`) - Story 1.5+
- No `TokenManager`, `Mutex`, `MealieAuthenticator` - Stories 1.5-1.7
- No Room database - Story 2.1+
- No `ConnectivityMonitor` - Story 2.9
- Do NOT change `ServerUrlScreen` or `ServerUrlViewModel` - they already navigate to `HttpWarningCheckRoute` correctly

### Project Structure Notes

- Source directories are `java/` not `kotlin/` - consistent with all existing project files
- Package: `dev.xexanos.mealie.feature.auth.ui` for screen/viewmodel/state/event files
- Package: `dev.xexanos.mealie.core.data.datastore` for `AppPreferencesStore`
- Package: `dev.xexanos.mealie.core.data.repository` for `AuthRepository` interface + impl
- `MainDispatcherExtension` already exists at `feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/testutil/`
- `FakeAuthRepository` already exists at `feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/ui/`

### Design Token Usage

Check if `Spacing` constants exist in `:core:ui`. Story 1.2 created `MealieTheme` but may not have created spacing tokens yet. If `Spacing` object does not exist, use raw dp values (`16.dp`, `12.dp`, `24.dp`). Do NOT create the design token file in this story unless it already exists.

### References

- [Source: epics.md#Story 1.4] - Acceptance criteria and user story
- [Source: architecture.md#Data Architecture] - HTTP warning confirmation stored in unencrypted DataStore; `AppPreferencesStore` with `HTTP_WARNING_ACK_URLS_KEY`
- [Source: architecture.md#Frontend Architecture] - Sealed UiState pattern for auth screens, Channel for one-shot events, StateFlow from ViewModel, collectAsStateWithLifecycle()
- [Source: ux-design-specification.md#UX-DR12] - Inline informational message (not modal), non-alarming tone, one-time per server URL, remembered in AppPreferencesStore
- [Source: ux-design-specification.md#UX-DR11] - Setup flow: all errors inline, no toasts/dialogs; two-screen setup (URL first, credentials second)
- [Source: story 1-3 Dev Notes] - Navigation pattern with popUpTo inclusive, test pattern with FakeAuthRepository, MainDispatcherExtension, source dirs are java/, rememberSaveable not remember
- [Source: story 1-3 Review Findings] - CancellationException must not be swallowed, guard against concurrent actions, use rememberSaveable for surviving config changes

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List

core/data/src/main/java/dev/xexanos/mealie/core/data/datastore/AppPreferencesStore.kt
core/data/src/main/java/dev/xexanos/mealie/core/data/repository/AuthRepository.kt
core/data/src/main/java/dev/xexanos/mealie/core/data/repository/AuthRepositoryImpl.kt
feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/HttpWarningCheckUiState.kt
feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/HttpWarningCheckUiEvent.kt
feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/HttpWarningCheckViewModel.kt
feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/HttpWarningCheckScreen.kt
feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/HttpWarningCheckTestTags.kt
feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/navigation/AuthNavGraph.kt
feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/di/AuthFeatureModule.kt
feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/ui/FakeAuthRepository.kt
feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/ui/HttpWarningCheckViewModelTest.kt
