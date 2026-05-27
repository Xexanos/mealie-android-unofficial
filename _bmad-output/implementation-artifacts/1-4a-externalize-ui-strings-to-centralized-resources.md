# Story 1.4a: Externalize UI Strings to Centralized Resources

Status: ready-for-dev

## Story

As a developer,
I want all existing user-facing strings extracted to `:core:ui` string resources with German translations,
so that the i18n pattern is established before further UI stories are implemented.

## Acceptance Criteria

1. **Given** stories 1-2, 1-3, and 1-4 contain hardcoded English strings in Compose code
   **When** this story is completed
   **Then** all user-facing strings are replaced with `stringResource(R.string.xyz)` references
   **And** no hardcoded user-facing text remains in `:feature:auth` or `:core:ui` Compose code

2. **Given** `:core:ui` module's `src/main/res/` directory
   **When** string resource files are created
   **Then** `values/strings.xml` contains all English strings
   **And** `values-de/strings.xml` contains German translations for all entries

3. **Given** string resource names are created
   **When** naming is reviewed
   **Then** all names use snake_case prefixed by screen or component (e.g., `setup_url_label`, `setup_button_connect`, `http_warning_message`)

4. **Given** the HTTP warning copy contains dynamic content (server URL)
   **When** the string is externalized
   **Then** a parameterized string is used (`%1$s`) rather than concatenation

5. **Given** the app is launched with the device locale set to German
   **When** any screen from stories 1-2 through 1-4 is displayed
   **Then** all UI chrome appears in German

6. **Given** the app is launched with a locale other than English or German
   **When** any screen is displayed
   **Then** the app falls back to English (default resource)

## Tasks / Subtasks

- [ ] Task 1: Create string resource files in `:core:ui` (AC: 2, 3)
  - [ ] Create `core/ui/src/main/res/values/strings.xml` with all English strings
  - [ ] Create `core/ui/src/main/res/values-de/strings.xml` with German translations
  - [ ] Verify naming follows `screen_component_purpose` snake_case convention

- [ ] Task 2: Refactor `ServerUrlUiState` to use `@StringRes` for error messages (AC: 1)
  - [ ] Change `InputError(message: String, ...)` to `InputError(@StringRes messageResId: Int, ...)`
  - [ ] Add `import androidx.annotation.StringRes` to the file

- [ ] Task 3: Update `ServerUrlViewModel` to emit resource IDs instead of literal strings (AC: 1)
  - [ ] Replace `"Enter a valid URL (e.g. https://mealie.example.com)"` with `R.string.setup_url_error_invalid`
  - [ ] Replace `"Could not reach server"` with `R.string.setup_url_error_unreachable`
  - [ ] Replace `"Not a Mealie server"` with `R.string.setup_url_error_not_mealie`
  - [ ] Add `import dev.xexanos.mealie.core.ui.R` to imports

- [ ] Task 4: Update `ServerUrlScreen` composable to use `stringResource()` (AC: 1)
  - [ ] Replace `"Server URL"` with `stringResource(R.string.setup_url_label)`
  - [ ] Replace `"Connect"` with `stringResource(R.string.setup_button_connect)`
  - [ ] Resolve `inputError.messageResId` via `stringResource(inputError.messageResId)`
  - [ ] Add `import androidx.compose.ui.res.stringResource` and `import dev.xexanos.mealie.core.ui.R`

- [ ] Task 5: Update `HttpWarningCheckScreen` composable to use `stringResource()` (AC: 1, 4)
  - [ ] Replace `"Connecting over your local network..."` with `stringResource(R.string.http_warning_message)`
  - [ ] Replace `"Continue"` with `stringResource(R.string.http_warning_button_continue)`
  - [ ] Add `import androidx.compose.ui.res.stringResource` and `import dev.xexanos.mealie.core.ui.R`

- [ ] Task 6: Externalize app name in manifest (AC: 1)
  - [ ] Add `app_name` string to `core/ui/src/main/res/values/strings.xml`
  - [ ] Add German translation for app name in `values-de/strings.xml`
  - [ ] Update `app/src/main/AndroidManifest.xml` to use `android:label="@string/app_name"`

- [ ] Task 7: Update unit tests for `ServerUrlViewModel` (AC: 1)
  - [ ] Change assertions from string equality to resource ID equality
  - [ ] `assertEquals(R.string.setup_url_error_unreachable, state.messageResId)`
  - [ ] `assertEquals(R.string.setup_url_error_not_mealie, state.messageResId)`
  - [ ] Add `import dev.xexanos.mealie.core.ui.R` to test file

- [ ] Task 8: Verify build, tests, and lint pass
  - [ ] `./gradlew assembleDebug` - BUILD SUCCESSFUL
  - [ ] `./gradlew :feature:auth:test` - all tests pass
  - [ ] `./gradlew ktlintCheck detekt lint` - all pass

## Dev Notes

### String Inventory (Exhaustive)

All hardcoded user-facing strings currently in the codebase:

| # | Current String | File | Line | Resource Name |
|---|---|---|---|---|
| 1 | `"Server URL"` | `ServerUrlScreen.kt` | 87 | `setup_url_label` |
| 2 | `"Connect"` | `ServerUrlScreen.kt` | 131 | `setup_button_connect` |
| 3 | `"Enter a valid URL (e.g. https://mealie.example.com)"` | `ServerUrlViewModel.kt` | 40 | `setup_url_error_invalid` |
| 4 | `"Could not reach server"` | `ServerUrlViewModel.kt` | 54 | `setup_url_error_unreachable` |
| 5 | `"Not a Mealie server"` | `ServerUrlViewModel.kt` | 60 | `setup_url_error_not_mealie` |
| 6 | `"Connecting over your local network - this is common for self-hosted setups."` | `HttpWarningCheckScreen.kt` | 83 | `http_warning_message` |
| 7 | `"Continue"` | `HttpWarningCheckScreen.kt` | 100 | `http_warning_button_continue` |
| 8 | `"Mealie"` | `AndroidManifest.xml` | 7 | `app_name` |

### ViewModel Error String Pattern

The `ServerUrlViewModel` produces error messages in non-Composable context. `stringResource()` is only available in `@Composable` functions. The correct pattern:

1. Change `ServerUrlUiState.InputError` to carry `@StringRes Int` instead of `String`
2. Resolve the resource ID to a string in the Composable layer via `stringResource()`

**Before:**
```kotlin
data class InputError(val message: String, val lastUrl: String = "") : ServerUrlUiState()
```

**After:**
```kotlin
data class InputError(@StringRes val messageResId: Int, val lastUrl: String = "") : ServerUrlUiState()
```

**ViewModel usage (after):**
```kotlin
_uiState.value = ServerUrlUiState.InputError(
    messageResId = R.string.setup_url_error_invalid,
    lastUrl = rawUrl,
)
```

**Screen usage (after):**
```kotlin
if (inputError != null) {
    Text(
        text = stringResource(inputError.messageResId),
        ...
    )
}
```

The `R` import MUST reference `:core:ui`'s generated R class: `import dev.xexanos.mealie.core.ui.R`

### AC 4 - Parameterized Strings

The current implementation shows the server URL as a separate `Text` element above the warning message - the URL is NOT embedded in the warning string. The AC about `%1$s` is a guardrail for future strings that include dynamic content. The current `http_warning_message` does NOT need a parameter.

If the developer wants to combine URL + message into a single string in a future story, they would use:
```xml
<string name="http_warning_message_with_url">Connecting to %1$s over your local network - this is common for self-hosted setups.</string>
```
and resolve with `stringResource(R.string.http_warning_message_with_url, serverUrl)`.

For THIS story: keep the existing UI structure (separate URL display + static warning message).

### String Resource Files - Complete Content

**`core/ui/src/main/res/values/strings.xml`:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Mealie</string>

    <!-- Server URL Setup Screen -->
    <string name="setup_url_label">Server URL</string>
    <string name="setup_button_connect">Connect</string>
    <string name="setup_url_error_invalid">Enter a valid URL (e.g. https://mealie.example.com)</string>
    <string name="setup_url_error_unreachable">Could not reach server</string>
    <string name="setup_url_error_not_mealie">Not a Mealie server</string>

    <!-- HTTP Warning Screen -->
    <string name="http_warning_message">Connecting over your local network - this is common for self-hosted setups.</string>
    <string name="http_warning_button_continue">Continue</string>
</resources>
```

**`core/ui/src/main/res/values-de/strings.xml`:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Mealie</string>

    <!-- Server URL Setup Screen -->
    <string name="setup_url_label">Server-URL</string>
    <string name="setup_button_connect">Verbinden</string>
    <string name="setup_url_error_invalid">Gib eine gueltige URL ein (z.B. https://mealie.example.com)</string>
    <string name="setup_url_error_unreachable">Server nicht erreichbar</string>
    <string name="setup_url_error_not_mealie">Kein Mealie-Server</string>

    <!-- HTTP Warning Screen -->
    <string name="http_warning_message">Verbindung ueber das lokale Netzwerk - das ist bei selbst gehosteten Setups ueblich.</string>
    <string name="http_warning_button_continue">Weiter</string>
</resources>
```

**Note on German umlauts:** Use proper Unicode characters in the XML (ue -> u with umlaut, etc.). The strings above use ASCII for readability in this document. The actual XML MUST use: `Gib eine gueltige` -> `Gib eine g\u00FCltige` or directly `Gib eine g&#252;ltige` or simply the UTF-8 character `u` (the file must be UTF-8 encoded, which Android Studio handles by default).

Correct German with proper characters:
- `Gib eine gueltige` -> `Gib eine g\u00FCltige` -> actually just use `gultige` with u-umlaut: `g\u00FCltige`
- `ueber` -> `\u00FCber`
- `ueblich` -> `\u00FCblich`

Android resource XML files are UTF-8 by default. Just use the proper Unicode characters directly: `u` (U+00FC), `o` (U+00F6), `a` (U+00E4).

### AndroidManifest.xml Update

**Before:**
```xml
android:label="Mealie"
```

**After:**
```xml
android:label="@string/app_name"
```

Since `:app` depends on `:core:ui`, it has access to `:core:ui`'s string resources.

### Test Updates Required

`ServerUrlViewModelTest.kt` currently asserts on literal strings:
```kotlin
assertEquals("Could not reach server", state.message)
assertEquals("Not a Mealie server", state.message)
```

**After refactoring** - assert on resource IDs:
```kotlin
assertEquals(R.string.setup_url_error_unreachable, state.messageResId)
assertEquals(R.string.setup_url_error_not_mealie, state.messageResId)
```

The `InputError` type check test (`assert(state is ServerUrlUiState.InputError)`) remains unchanged.

### What NOT to Do in This Story

- Do NOT create a `UiText` sealed class - `@StringRes Int` is sufficient for this project's scale
- Do NOT create string resources in feature modules - ALL go in `:core:ui` per architecture
- Do NOT add plurals or quantity strings - not needed for current strings
- Do NOT add `stringResource()` to `HttpWarningCheckViewModel` - the warning message is rendered directly in the Composable, not produced by the ViewModel
- Do NOT change the HTTP warning screen layout or behavior - only externalize the strings
- Do NOT touch `ServerUrlViewModel.normalizeUrl()` or any logic - only the string literals
- Do NOT create Spacing/design token files - that is a separate concern

### Project Structure Notes

- Source directories are `java/` not `kotlin/` (consistent with all existing code)
- Resource directories: `core/ui/src/main/res/values/` and `core/ui/src/main/res/values-de/`
- Package for `:core:ui` R class: `dev.xexanos.mealie.core.ui.R`
- Feature module imports `:core:ui` R class, NOT its own R class for strings
- No `res/` directory currently exists in `:core:ui` - create `src/main/res/values/` fresh
- The `build.gradle.kts` for `:core:ui` already has `namespace = "dev.xexanos.mealie.core.ui"` so R class generation works automatically

### Previous Story Intelligence

From Story 1-4 review findings:
- **[Deferred]** "Hard-coded UI strings instead of string resources [HttpWarningCheckScreen.kt]" - THIS story resolves that deferred item
- **Pattern established:** MVI with Channel events, `collectAsStateWithLifecycle()`, test with `FakeAuthRepository`
- **Double-tap fix:** `HttpWarningCheckViewModel.onContinue()` transitions state to Loading before launching coroutine - preserve this behavior
- **Import convention:** Use full qualified R import `dev.xexanos.mealie.core.ui.R` (not wildcard)

### References

- [Source: epics.md#Story 1.4a] - Full acceptance criteria and user story
- [Source: architecture.md#Additional Requirements - Localization] - NFR-20: All user-facing strings externalized to `:core:ui`, snake_case prefixed by screen/component, English + German, system locale
- [Source: architecture.md#Module Structure] - `:core:ui` is shared Compose components module, feature modules depend on it
- [Source: architecture.md#Frontend Architecture - UiState Pattern] - Sealed class UiState with StateFlow, one-shot events via Channel
- [Source: story 1-4 Dev Notes] - File list of modified files, source dirs are java/, design token note
- [Source: story 1-4 Review Findings] - Deferred hard-coded UI strings finding that this story resolves

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
