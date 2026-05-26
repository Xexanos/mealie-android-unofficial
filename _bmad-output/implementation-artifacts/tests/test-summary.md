# Test Automation Summary

## Generated Tests

### E2E Tests (WireMock Black-Box - Instrumented)
- [x] `app/src/androidTest/java/dev/xexanos/mealie/e2e/ServerUrlE2eTest.kt` - Server URL flow

#### Architecture
- App runs UNMODIFIED (real Koin, real OkHttp, real DataStore)
- WireMock standalone runs on host machine (port 8080), Gradle manages lifecycle
- Emulator reaches host via `10.0.2.2`
- Per-test stub configuration via WireMock admin API
- AndroidX Test Orchestrator with `clearPackageData` for test isolation

#### Test Cases
| Test | WireMock Setup | Assertion |
|------|----------------|-----------|
| `whenValidMealieServer_thenNavigatesAway` | 200 + version 3.0.0 | URL field disappears (navigated forward) |
| `whenServerReturnsOldVersion_thenShowsNotMealieError` | 200 + version 2.0.0 | Error: "Not a Mealie server" |
| `whenServerUnreachable_thenShowsNetworkError` | None (dead port 9999) | Error: "Could not reach server" |
| `whenEmptyUrlSubmitted_thenShowsValidationError` | None | Error: "Enter a valid URL..." |
| `whenProbing_thenShowsLoadingAndDisablesInput` | 3s delay stub | Spinner visible, fields disabled |

### Infrastructure Created
- [x] `feature/auth/src/main/java/.../ServerUrlTestTags.kt` - Semantic test tag constants
- [x] `app/src/androidTest/java/.../e2e/WireMockRule.kt` - JUnit 4 TestRule for WireMock admin API
- [x] `app/src/androidTest/resources/wiremock/` - WireMock root directory (response files)

### Dependencies Added
- `wiremock-standalone` (3.6.0) - Host-side WireMock JAR (Gradle runner config)
- `androidx-test-runner` (1.6.2) - AndroidX instrumentation runner
- `androidx-test-orchestrator` (1.5.1) - Test isolation via clearPackageData

### Production Code Modified
- `ServerUrlScreen.kt` - Added `testTag()` modifiers to TextField, Button, ErrorText, ProgressIndicator

## Coverage

- **Auth flow (Server URL entry):** 5/5 black-box E2E scenarios covered
- **Navigation:** Forward navigation on successful server probe
- **Error handling:** Validation errors, network errors, wrong server type
- **State transitions:** Loading state with disabled inputs

## How to Run

```bash
# Requires a connected device or running emulator
# WireMock starts/stops automatically via Gradle tasks
./gradlew :app:connectedDebugAndroidTest
```

## Build Verification

- [x] All 5 E2E tests pass (28.66s total)
- [x] `feature:auth` unit tests pass (no regressions)
- [x] `core:ui` unit tests pass

## Next Steps

- Add E2E tests for future screens (HttpWarningCheck, Login, Shopping List) as they're implemented
- Consider adding Gradle Managed Devices for CI-based testing
