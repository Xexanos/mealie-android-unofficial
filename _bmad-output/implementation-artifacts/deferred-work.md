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
