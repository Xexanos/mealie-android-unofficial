# Test Automation Summary

## Generated Tests

### E2E Tests (WireMock Black-Box - Instrumented)
- [x] `app/src/androidTest/java/dev/xexanos/mealie/e2e/ServerUrlE2eTest.kt` - Server URL flow
- [x] `app/src/androidTest/java/dev/xexanos/mealie/e2e/StartupAuthE2eTest.kt` - Silent token refresh (Story 1-6)

#### Architecture
- App runs UNMODIFIED (real Koin, real OkHttp, real DataStore)
- WireMock standalone runs on host machine (port 8080), Gradle manages lifecycle
- Emulator reaches host via `10.0.2.2`
- Per-test stub configuration via WireMock admin API
- AndroidX Test Orchestrator with `clearPackageData` for test isolation

#### StartupAuth Test Cases (Story 1-6)
| Test | DataStore State | WireMock Setup | Assertion |
|------|----------------|----------------|-----------|
| `whenNoCredentialsStored_thenNavigatesToServerUrlScreen` | Empty (fresh install) | None | ServerUrlScreen appears |
| `whenRefreshSucceeds_thenNavigatesToMain` | Token + credentials stored | Refresh 200 | PostAuthRoute appears |
| `whenRefreshFailsAndReAuthSucceeds_thenNavigatesToMain` | Token + credentials stored | Refresh 401, Auth 200 | PostAuthRoute appears |
| `whenBothFail_thenNavigatesToCredentialScreen` | Token + credentials stored | Refresh 401, Auth 401 | CredentialScreen appears |
| `whenRefreshInProgress_thenShowsLoadingIndicator` | Token + credentials stored | Refresh 200 (3s delay) | Loading indicator visible |

#### ServerUrl Test Cases (existing)
| Test | WireMock Setup | Assertion |
|------|----------------|-----------|
| `whenServerReturnsOldVersion_thenShowsNotMealieError` | 200 + version 1.2.0 | Error: "Not a Mealie server" |
| `whenServerUnreachable_thenShowsNetworkError` | None (dead port 9999) | Error: "Could not reach server" |
| `whenEmptyUrlSubmitted_thenShowsValidationError` | None | Error: "Enter a valid URL..." |
| `whenProbing_thenShowsLoadingAndDisablesInput` | 3s delay stub | Spinner visible, fields disabled |

### Infrastructure Created
- [x] `feature/auth/src/main/java/.../StartupTestTags.kt` - Test tags for StartupScreen
- [x] `app/src/main/java/.../navigation/PostAuthTestTags.kt` - Test tags for PostAuthRoute
- [x] `app/src/androidTest/java/.../e2e/StartupStateRule.kt` - JUnit 4 TestRule for DataStore pre-population
- [x] `app/src/androidTest/java/.../e2e/WireMockRule.kt` - Extended with refresh endpoint stubs

### Production Code Modified
- `StartupScreen.kt` - Added `testTag()` to CircularProgressIndicator
- `AppNavGraph.kt` - Added `testTag()` to PostAuthRoute Box

### StartupStateRule Design
- Annotation-driven (`@WithStoredAuth`) for per-test DataStore configuration
- Runs between WireMock (order=0) and Activity launch (order=2)
- Pre-populates: server URL, credentials, token, HTTP warning acknowledgment
- Resets UseCase cached result via reflection for test isolation
- Cleans up all stores after each test

## Coverage

- **Startup auth flow:** 5/5 acceptance criteria paths covered (AC 1, 2, 3+4, 5, 6)
- **Auth flow (Server URL entry):** 4/4 scenarios covered
- **Offline scenario (AC 7):** Covered by unit tests (requires network simulation beyond WireMock capability)
- **Mutex concurrency (AC 8):** Covered by unit tests (not observable at E2E level)

## How to Run

```bash
# Requires a connected device or running emulator
# WireMock starts/stops automatically via Gradle tasks
./gradlew :app:connectedDebugAndroidTest
```

## Build Verification

- [x] E2E test sources compile successfully
- [x] All unit tests pass (no regressions from testTag additions)
- [x] ktlintCheck passes
- [x] detekt passes

## Next Steps

- Run E2E tests on emulator to verify end-to-end behavior
- Add E2E tests for future screens as they're implemented
- Consider adding Gradle Managed Devices for CI-based testing
