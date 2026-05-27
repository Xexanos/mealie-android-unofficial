# Story 1.5: Credential Entry and Encrypted Storage

Status: ready-for-dev

## Story

As a first-time user,
I want to enter my username and password securely and have them stored for later token refresh,
so that I can authenticate with the Mealie server without re-entering credentials manually.

## Acceptance Criteria

1. **Given** the server URL has been validated and stored
   **When** the user navigates from `HttpWarningCheckScreen`
   **Then** `CredentialScreen` is displayed with username and password input fields and a "Sign In" button

2. **Given** the user leaves either field empty
   **When** they tap "Sign In"
   **Then** inline validation shows "Username and password required" below the password field
   **And** no network request is made

3. **Given** the user enters valid username and password
   **When** they tap "Sign In"
   **Then** a POST `/api/auth/token` request is made with the credentials
   **And** a loading indicator replaces the button text (spinner on button, not full-screen overlay)

4. **Given** the token endpoint returns HTTP 200 with an `access_token` in the response
   **When** the response is parsed
   **Then** the username, password, and access token are all encrypted using `datastore-tink` with `AeadSerializer` (AES-256-GCM)
   **And** all three values are persisted to encrypted DataStore
   **And** the screen navigates to the post-auth destination (placeholder route for Epic 2)

5. **Given** the credentials are invalid or the token endpoint returns HTTP 401
   **When** the error is received
   **Then** an error message "Incorrect username or password" is displayed below the password field
   **And** the password field is cleared
   **And** the username field retains its value
   **And** the fields remain editable for correction

6. **Given** a network error occurs (timeout, DNS failure, unreachable)
   **When** the error is received
   **Then** an error message "Could not reach server" is displayed
   **And** both fields retain their values

7. **Given** credentials have been stored in encrypted DataStore
   **When** the app is killed and relaunched
   **Then** the stored credentials are decrypted and available without user re-entry (consumed by Story 1-6's TokenManager)

8. **Given** all user-facing strings on `CredentialScreen`
   **When** string resources are reviewed
   **Then** all strings use `stringResource(R.string.xyz)` from `:core:ui`
   **And** English and German translations exist in `values/strings.xml` and `values-de/strings.xml`

## Tasks / Subtasks

- [ ] Task 1: Create `AuthService` Retrofit interface in `:core:network` (AC: 3, 4, 5)
  - [ ] Create `core/network/src/main/java/dev/xexanos/mealie/core/network/api/AuthService.kt`
  - [ ] Create `core/network/src/main/java/dev/xexanos/mealie/core/network/dto/AuthTokenDto.kt`
  - [ ] Register `AuthService` in `NetworkModule.kt`

- [ ] Task 2: Create `TokenStore` encrypted DataStore in `:core:data` (AC: 4, 7)
  - [ ] Create `core/data/src/main/java/dev/xexanos/mealie/core/data/datastore/TokenStore.kt`

- [ ] Task 3: Create `CredentialsStore` encrypted DataStore in `:core:data` (AC: 4, 7)
  - [ ] Create `core/data/src/main/java/dev/xexanos/mealie/core/data/datastore/CredentialsStore.kt`

- [ ] Task 4: Extend `AuthRepository` interface and implementation (AC: 3, 4, 5, 6, 7)
  - [ ] Add `authenticate(username, password)` to `AuthRepository`
  - [ ] Add credential getter/setter methods to `AuthRepository`
  - [ ] Implement in `AuthRepositoryImpl` using `AuthService`, `TokenStore`, `CredentialsStore`
  - [ ] Register new dependencies in `DataModule.kt`

- [ ] Task 5: Create `CredentialViewModel` with UiState and UiEvent (AC: 1-6)
  - [ ] Create `feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/CredentialUiState.kt`
  - [ ] Create `feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/CredentialUiEvent.kt`
  - [ ] Create `feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/CredentialViewModel.kt`

- [ ] Task 6: Create `CredentialScreen` composable (AC: 1-6, 8)
  - [ ] Create `feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/CredentialScreen.kt`
  - [ ] Create `feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/ui/CredentialTestTags.kt`

- [ ] Task 7: Wire navigation and Koin (AC: 1, 4)
  - [ ] Replace `CredentialRoute` placeholder in `AuthNavGraph.kt` with real `CredentialScreen`
  - [ ] Define post-auth destination route (placeholder composable for Epic 2)
  - [ ] Register `CredentialViewModel` in `AuthFeatureModule.kt`

- [ ] Task 8: Add string resources (AC: 8)
  - [ ] Add English strings to `core/ui/src/main/res/values/strings.xml`
  - [ ] Add German strings to `core/ui/src/main/res/values-de/strings.xml`

- [ ] Task 9: Write unit tests (AC: 2-6)
  - [ ] Update `FakeAuthRepository` with new credential methods
  - [ ] Create `feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/ui/CredentialViewModelTest.kt`
  - [ ] Create `core/data/src/test/java/dev/xexanos/mealie/core/data/datastore/TokenStoreTest.kt` (if testable without Android context)
  - [ ] Create `core/network/src/test/java/dev/xexanos/mealie/core/network/api/AuthServiceTest.kt` (MockWebServer contract test)

- [ ] Task 10: Verify build, tests, and lint pass
  - [ ] `./gradlew assembleDebug` - BUILD SUCCESSFUL
  - [ ] `./gradlew :feature:auth:test :core:data:test :core:network:test` - all tests pass
  - [ ] `./gradlew ktlintCheck detekt lint` - all pass

## Dev Notes

### Mealie Auth API Contract

Mealie uses FastAPI's OAuth2 password flow. The token endpoint accepts **form-encoded** data, NOT JSON:

```
POST /api/auth/token
Content-Type: application/x-www-form-urlencoded

username=user%40example.com&password=secret&remember_me=true
```

Response (200 OK):
```json
{
  "access_token": "eyJhbG...",
  "token_type": "bearer"
}
```

Response (401 Unauthorized):
```json
{
  "detail": "Unauthorized"
}
```

**Critical:** Retrofit requires `@FormUrlEncoded` + `@Field` annotations for this endpoint, NOT `@Body` with a JSON class.

### AuthService.kt

```kotlin
package dev.xexanos.mealie.core.network.api

import dev.xexanos.mealie.core.network.dto.AuthTokenDto
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface AuthService {
    @FormUrlEncoded
    @POST("api/auth/token")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("remember_me") rememberMe: Boolean = true,
    ): Response<AuthTokenDto>
}
```

Return `Response<AuthTokenDto>` (not raw `AuthTokenDto`) to distinguish 401 from network errors without exceptions.

### AuthTokenDto.kt

```kotlin
package dev.xexanos.mealie.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthTokenDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
)
```

### Encrypted DataStore with datastore-tink

**Architecture requirement:** Two encrypted DataStore instances - `TokenStore` (access token) and `CredentialsStore` (username + password). Both use `AeadSerializer` with AES-256-GCM, backed by Android Keystore.

**Key implementation pattern:**

```kotlin
package dev.xexanos.mealie.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.tink.AeadSerializer
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class StoredCredentials(
    val username: String = "",
    val password: String = "",
)

class CredentialsStore(private val context: Context) {
    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, "credentials_keyset", "credentials_keyset_prefs")
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://mealie_credentials_master_key")
            .build()
            .keysetHandle
            .getPrimitive(Aead::class.java)
    }

    private val Context.credentialsDataStore: DataStore<StoredCredentials> by dataStore(
        fileName = "credentials.pb",
        serializer = AeadSerializer(
            aead = aead,
            wrapped = StoredCredentialsSerializer,
        ),
    )

    fun getCredentials(): Flow<StoredCredentials> =
        context.credentialsDataStore.data

    suspend fun saveCredentials(username: String, password: String) {
        context.credentialsDataStore.updateData {
            StoredCredentials(username = username, password = password)
        }
    }

    suspend fun clearCredentials() {
        context.credentialsDataStore.updateData { StoredCredentials() }
    }
}
```

**CRITICAL implementation notes:**

1. `AeadSerializer` wraps a custom `Serializer<T>` - you must create `StoredCredentialsSerializer` and `StoredTokenSerializer` implementing `androidx.datastore.core.Serializer<T>`. Use Kotlinx Serialization JSON for the serialization format.

2. `TokenStore` follows the same pattern but stores `StoredToken(accessToken: String)`.

3. Each store must use a **different** master key URI and keyset preference name to avoid key collision.

4. `AeadConfig.register()` must be called once before creating AEAD instances. Call in the `lazy` initializer.

5. The `by dataStore(...)` delegate must use a **unique file name** per store (`credentials.pb`, `token.pb`).

6. **Do NOT use** the deprecated `security-crypto` / `MasterKey` approach.

### TokenStore.kt

Same pattern as CredentialsStore:
- Master key URI: `"android-keystore://mealie_token_master_key"`
- Keyset pref name: `"token_keyset"`, shared pref file: `"token_keyset_prefs"`
- File name: `"token.pb"`
- Data class: `StoredToken(accessToken: String = "")`
- Methods: `getToken(): Flow<StoredToken>`, `saveToken(accessToken: String)`, `clearToken()`

### AuthRepository - New Methods

Add to `AuthRepository.kt` interface:

```kotlin
sealed class AuthResult {
    data object Success : AuthResult()
    data object InvalidCredentials : AuthResult()
    data object NetworkError : AuthResult()
}

suspend fun authenticate(username: String, password: String): AuthResult
fun getStoredCredentials(): Flow<StoredCredentials>
fun getStoredToken(): Flow<StoredToken>
```

Implementation in `AuthRepositoryImpl`:
- Creates `AuthService` via Retrofit (same pattern as `probeServerUrl` creates AppService dynamically)
- On success: stores token via `TokenStore`, stores credentials via `CredentialsStore`
- On 401: returns `AuthResult.InvalidCredentials`
- On IOException/network failure: returns `AuthResult.NetworkError`

**Important:** `AuthRepositoryImpl` constructor must be updated to accept `TokenStore` and `CredentialsStore` as dependencies. Update `DataModule.kt` to provide them.

### CredentialUiState.kt

```kotlin
package dev.xexanos.mealie.feature.auth.ui

import androidx.annotation.StringRes

sealed class CredentialUiState {
    data object Loading : CredentialUiState()
    data class AwaitingInput(
        val username: String = "",
        val password: String = "",
        @StringRes val errorResId: Int? = null,
        val isSubmitting: Boolean = false,
    ) : CredentialUiState()
}
```

### CredentialUiEvent.kt

```kotlin
package dev.xexanos.mealie.feature.auth.ui

sealed class CredentialUiEvent {
    data object NavigateToMain : CredentialUiEvent()
}
```

### CredentialViewModel.kt

Key behaviors:
- Initial state: `AwaitingInput()` with empty fields
- `onUsernameChanged(value)` / `onPasswordChanged(value)` update state
- `onSignIn()`:
  - Validate: both fields non-empty, otherwise show error (no network call)
  - Set `isSubmitting = true`
  - Call `authRepository.authenticate(username, password)`
  - On `Success`: emit `NavigateToMain` event
  - On `InvalidCredentials`: clear password, set error `R.string.credential_error_invalid`
  - On `NetworkError`: keep both fields, set error `R.string.credential_error_network`

**Guard pattern from Story 1-4 review findings:** Transition state to prevent double-tap before launching coroutine.

### CredentialScreen.kt - UX Requirements

Per UX-DR11 and the UX consistency patterns:
- **Inline errors only** - no toasts, no dialogs
- **Spinner on button** - not a full-screen overlay
- **Password field:** use `KeyboardType.Password` + `VisualTransformation.Password` (trailing visibility toggle)
- **Password cleared on auth failure, username retained**
- **Validation on submission only** - no real-time validation while typing
- Primary button label: "Sign In" (per UX button hierarchy table)
- Use `Spacing.ScreenPadding` (16.dp) and `widthIn(max = 600.dp)` for responsive layout
- `rememberSaveable` for field values to survive configuration changes

```kotlin
@Composable
fun CredentialScreen(
    onNavigateToMain: () -> Unit,
    viewModel: CredentialViewModel = koinViewModel(),
) {
    // Collect state + events (same pattern as HttpWarningCheckScreen)
}
```

### Navigation Update

In `AuthNavGraph.kt`:
- Replace the empty `Box` placeholder at `CredentialRoute` with `CredentialScreen`
- Add a post-auth route: `@Serializable object PostAuthRoute` (placeholder for Epic 2)
- `CredentialScreen.onNavigateToMain` navigates from `CredentialRoute` to `PostAuthRoute` with `popUpTo(AuthGraph) { inclusive = true }` to clear the auth back stack

In `AppNavGraph.kt` (if needed):
- Ensure `PostAuthRoute` renders something (empty Box or placeholder text) until Epic 2 wires `ShoppingListScreen`

### String Resources

Add to `core/ui/src/main/res/values/strings.xml`:
```xml
<!-- Credential Screen -->
<string name="credential_label_username">Username</string>
<string name="credential_label_password">Password</string>
<string name="credential_button_sign_in">Sign In</string>
<string name="credential_error_empty">Username and password required</string>
<string name="credential_error_invalid">Incorrect username or password</string>
<string name="credential_error_network">Could not reach server</string>
```

Add to `core/ui/src/main/res/values-de/strings.xml`:
```xml
<!-- Credential Screen -->
<string name="credential_label_username">Benutzername</string>
<string name="credential_label_password">Passwort</string>
<string name="credential_button_sign_in">Anmelden</string>
<string name="credential_error_empty">Benutzername und Passwort erforderlich</string>
<string name="credential_error_invalid">Benutzername oder Passwort falsch</string>
<string name="credential_error_network">Server nicht erreichbar</string>
```

### Testing Pattern

**CredentialViewModelTest.kt** - test cases:
1. Initial state is AwaitingInput with empty fields
2. Empty fields on sign-in shows validation error, no network call
3. Successful auth emits NavigateToMain event
4. Invalid credentials (401) clears password, shows error, retains username
5. Network error shows error, retains both fields
6. Double-tap guard: sign-in while `isSubmitting` is a no-op

Use `@ExtendWith(MainDispatcherExtension::class)` + Turbine for event testing.

**AuthServiceTest.kt** (MockWebServer contract test):
1. Login request sends form-encoded body with correct fields
2. Successful response parses `access_token` and `token_type`
3. 401 response returns error status code

### What NOT to Create in This Story

- No `TokenManager` singleton in `:core:network` - Story 1-6
- No `MealieAuthenticator` (OkHttp Authenticator) - Story 1-7
- No `GET /api/auth/refresh` endpoint - Story 1-6
- No silent token refresh on launch - Story 1-6
- No re-authentication screen - Story 1-8
- No Room database - Story 2-1
- No ConnectivityMonitor - Story 2-9
- Do NOT modify `ServerUrlScreen` or `HttpWarningCheckScreen` - they work correctly

### Project Structure Notes

- Source directories are `java/` not `kotlin/` (consistent with all existing project files)
- Package: `dev.xexanos.mealie.feature.auth.ui` for screen/viewmodel/state/event
- Package: `dev.xexanos.mealie.core.data.datastore` for TokenStore, CredentialsStore
- Package: `dev.xexanos.mealie.core.network.api` for AuthService
- Package: `dev.xexanos.mealie.core.network.dto` for AuthTokenDto
- `MainDispatcherExtension` already exists at `feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/testutil/`
- `FakeAuthRepository` already exists at `feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/ui/`
- Design tokens: use `Spacing.ScreenPadding`, `Spacing.Medium`, `Spacing.Large` from `:core:ui` (already created in earlier stories)
- `@StringRes` pattern established in Story 1-4a: UiState holds resource IDs, screen resolves via `stringResource()`

### Retrofit Service Creation Pattern

Story 1-3's `AuthRepositoryImpl` creates Retrofit services dynamically because the base URL isn't known at compile time:

```kotlin
private fun createAuthService(baseUrl: String): AuthService {
    val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
    return retrofit.create(AuthService::class.java)
}
```

Follow this same pattern for `AuthService`. Read the stored server URL from `AppPreferencesStore` to build the base URL.

**Note on form-encoded:** Retrofit handles `@FormUrlEncoded` natively - the `kotlinx-serialization-converter` is only for JSON body requests. Form-encoded fields use Retrofit's built-in converter. No additional converter factory needed.

### Previous Story Intelligence

From Story 1-4 review findings:
- **Double-tap guard:** Transition state before launching coroutine (set `isSubmitting = true` synchronously before `viewModelScope.launch`)
- **CancellationException:** Do not swallow in catch blocks
- **`rememberSaveable`:** Use for surviving configuration changes (not `remember`)
- **`flowWithLifecycle`:** Use when collecting one-shot events in Compose

From Story 1-4a:
- **`@StringRes` pattern:** UiState holds `Int?` resource IDs for error messages
- **No hardcoded strings:** All UI text via `stringResource()`

### References

- [Source: epics.md#Story 1.5] - Acceptance criteria and user story
- [Source: architecture.md#Authentication & Security] - Mealie token model, AeadSerializer, datastore-tink, no security-crypto
- [Source: architecture.md#Data Architecture] - Two DataStore instances (encrypted for token+credentials, regular for prefs)
- [Source: architecture.md#API & Communication Patterns] - AuthService, per-feature Retrofit interface
- [Source: architecture.md#Frontend Architecture] - Sealed UiState, Channel for events, StateFlow
- [Source: architecture.md#Package Structure] - `:core:data/datastore/`, `:core:network/api/`, `:core:network/dto/`
- [Source: ux-design-specification.md#UJ-1] - Credentials screen flow, inline errors, spinner on button
- [Source: ux-design-specification.md#Button Hierarchy] - Primary button: "Sign In"
- [Source: ux-design-specification.md#Form Patterns] - Password trailing visibility toggle, cleared on failure, username retained
- [Source: ux-design-specification.md#Feedback Patterns] - Errors inline only, no toasts, no dialogs
- [Source: story 1-4 Review Findings] - Double-tap guard, CancellationException handling, rememberSaveable
- [Source: story 1-4a] - @StringRes pattern for error messages, string resource naming conventions

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
