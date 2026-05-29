# Deferred Work

## Deferred from: code review of 1-1-multi-module-build-infrastructure-and-ci-cd (2026-05-24)

- Timber as debugImplementation - no logging abstraction in place; any release code calling Timber.* will crash with NoClassDefFoundError. Address when adding first real logging calls.
- `v*` tag release trigger has no branch or actor guard - any collaborator can push a v* tag from any branch and trigger a signed release. Consider adding a branch-protection check or restricting tag creation.
- CI `needs: build` sequential coupling - test and lint jobs each do a full recompile on separate runners without artifact sharing. Consider Gradle remote build cache or artifact upload/restore to speed up CI.
- Missing secrets produce unsigned APK silently - acknowledged as expected in spec dev notes; address when configuring repository secrets before first release.

## Deferred from: code review of 1-2-app-theme-navigation-shell-and-application-class (2026-05-24)

- `applicationScope` not registered in Koin DI - the CoroutineScope in MealieApplication is a local property not exposed through DI. Expose via Koin when a consumer needs it in later stories.
- `core:ui` no explicit coroutines main dependency - NavigationManager uses MutableSharedFlow but only gets kotlinx.coroutines transitively through koin.android. Make explicit in a dependency cleanup pass.

## Deferred from: code review of 1-4-http-security-warning-for-non-https-urls (2026-05-26)

- Hard-coded UI strings instead of string resources - Warning message and button label are inline English strings rather than Android string resources. Address when adding localization support.
- Unbounded growth of acknowledged URLs set - The DataStore set of acked URLs grows indefinitely with no pruning mechanism. Realistically bounded (1-3 servers) but could be capped if multi-server usage patterns emerge.

## Deferred from: code review of 1-4a-externalize-ui-strings-to-centralized-resources (2026-05-27)

- E2E tests use hardcoded English strings - `ServerUrlE2eTest.kt` matches rendered text by literal English content (e.g., `onNodeWithText("Not a Mealie server")`). Now that German translations exist, these tests are locale-sensitive and will fail on non-English devices. Use `activity.getString(R.string.*)` or match by test tag instead.
- `when` on `UrlProbeResult` is a statement, not exhaustive - The `when` block in `ServerUrlViewModel.onConnect()` is a statement, so Kotlin does not enforce exhaustiveness. If a new case is added to `UrlProbeResult`, the UI would silently remain in `Probing` state. Convert to expression (`val _ = when(...)`) or add an `else` branch with a clear crash.

## Deferred from: code review of 1-5-credential-entry-and-encrypted-storage (2026-05-28)

- DataStore delegate inside class risks IllegalStateException on multiple instantiation - `by dataStore(...)` declared as member extension property; creating a second instance of TokenStore/CredentialsStore throws. Mitigated by Koin `single{}` scope; move to top-level file scope when adding instrumented tests.
- Retrofit instance created per authenticate() call - `createAuthService()` builds fresh Retrofit on each call. Low performance impact; same pattern exists in `probeServerUrl`. Cache when auth frequency increases.
- AeadConfig.register() called redundantly in both stores - Tink's register() is idempotent but scattered across lazy blocks. Centralize in Application.onCreate or a Koin initializer when adding more Tink consumers.
- No error handling for KeyStoreException when Android Keystore unavailable - `AndroidKeysetManager.Builder().build()` can throw on biometric reset, hardware failure, or key invalidation. Requires app-level recovery strategy (clear data + re-auth); address in Story 1-8.
- HTTP 403/429/5xx not differentiated from NetworkError - spec only defines 401 and network error cases. All non-401 errors map to "Could not reach server". Expand AuthResult when real-world Mealie deployments reveal additional error scenarios.
- No concurrency guard in AuthRepositoryImpl.authenticate() - currently single-caller via ViewModel's isSubmitting guard. Add Mutex or similar when Story 1-6 (silent token refresh) introduces a second caller.
- Process death during isSubmitting loses typed credentials - ViewModel uses MutableStateFlow without SavedStateHandle; process death resets to empty fields. SavedStateHandle integration is nice-to-have for credential screen.
- Raw dp/sp literals instead of Spacing design tokens - `CredentialScreen.kt` uses `.padding(16.dp)` and `.widthIn(max = 600.dp)` as raw literals. Pre-existing debt from earlier stories (same pattern in ServerUrlScreen, HttpWarningCheckScreen). Address in a design-tokens cleanup pass.
- Redaction regex bypassed by URL-encoded special chars in passwords - `NetworkModule.kt` regex `password=[^&\s]+` stops at `&` or whitespace; encoded `%26` in password truncates the redacted segment. DEBUG-only, low risk.
- Empty access_token string accepted and stored without validation - `AuthRepositoryImpl.kt` saves `body.accessToken` without checking for blank. Extremely unlikely from real Mealie server; add guard when hardening for production.
- Silent credential/token wipe on DataStore file corruption - `ReplaceFileCorruptionHandler` resets both stores to empty defaults with no user notification. User appears silently logged out. Add Timber/Crashlytics logging and surface a re-login prompt.
