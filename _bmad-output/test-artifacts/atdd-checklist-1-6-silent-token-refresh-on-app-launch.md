---
stepsCompleted: ['step-01-preflight-and-context', 'step-02-generation-mode', 'step-03-test-strategy', 'step-04c-aggregate', 'step-05-validate-and-complete']
lastStep: 'step-05-validate-and-complete'
lastSaved: '2026-05-29'
storyId: '1.6'
storyKey: '1-6-silent-token-refresh-on-app-launch'
storyFile: '_bmad-output/implementation-artifacts/1-6-silent-token-refresh-on-app-launch.md'
atddChecklistPath: '_bmad-output/test-artifacts/atdd-checklist-1-6-silent-token-refresh-on-app-launch.md'
generatedTestFiles:
  - 'core/network/src/test/java/dev/xexanos/mealie/core/network/api/AuthServiceRefreshContractTest.kt'
  - 'core/data/src/test/java/dev/xexanos/mealie/core/data/domain/StartupAuthUseCaseTest.kt'
  - 'feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/ui/StartupAuthViewModelTest.kt'
inputDocuments:
  - '_bmad-output/implementation-artifacts/1-6-silent-token-refresh-on-app-launch.md'
  - '_bmad/tea/config.yaml'
---

# ATDD Checklist: Story 1.6 - Silent Token Refresh on App Launch

## TDD Red Phase (Current)

All test scaffolds generated with `@Disabled` - will fail to compile until implementation exists.

- Contract Tests: 4 tests (AuthService refresh endpoint)
- Unit Tests (UseCase): 11 tests (StartupAuthUseCase logic)
- Unit Tests (ViewModel): 4 tests (StartupAuthViewModel navigation events)
- **Total: 19 red-phase test scaffolds**

## Acceptance Criteria Coverage

| AC | Description | Test Level | Tests |
|----|-------------|-----------|-------|
| AC1 | Token refresh attempt on launch | Contract, Unit | RefreshRequestFormat (2), RefreshSuccess (2) |
| AC2 | Refresh success persists token, navigates to main | Contract, Unit, ViewModel | SuccessResponse (1), RefreshSuccess (2), NavigateToMain (1) |
| AC3 | Refresh 401 triggers credential fallback | Contract, Unit | UnauthorizedResponse (1), FallbackReAuthSuccess (2) |
| AC4 | Re-auth success persists token, navigates to main | Unit, ViewModel | FallbackReAuthSuccess (2), NavigateToMain (1) |
| AC5 | Both fail - clears token, navigates to credentials | Unit, ViewModel | BothFail (3), NavigateToCredentials (1) |
| AC6 | No credentials - navigates to setup | Unit, ViewModel | NoCredentials (2), NavigateToSetup (1) |
| AC7 | Offline - skips re-auth, navigates to main | Unit, ViewModel | Offline (2), NavigateToMainOffline (1) |
| AC8 | Mutex ensures single concurrent execution | Unit | MutexConcurrency (1) |
| AC9 | String resources (i18n) | Build/Lint | Not unit-testable |

## Generated Test Files

### Contract Tests (`:core:network`)

**File:** `core/network/src/test/java/dev/xexanos/mealie/core/network/api/AuthServiceRefreshContractTest.kt`

| Priority | Test | AC |
|----------|------|-----|
| P0 | refreshToken sends GET with Authorization Bearer header | AC1 |
| P0 | refreshToken sends no request body | AC1 |
| P0 | 200 response parses access_token and token_type | AC2 |
| P0 | 401 response returns unsuccessful response | AC3 |

### Unit Tests - UseCase (`:core:data`)

**File:** `core/data/src/test/java/dev/xexanos/mealie/core/data/domain/StartupAuthUseCaseTest.kt`

| Priority | Test | AC |
|----------|------|-----|
| P0 | stored token exists and refresh succeeds - returns Success | AC1/2 |
| P0 | refresh success persists new token | AC2 |
| P0 | refresh returns 401, re-auth succeeds - returns Success | AC3/4 |
| P0 | re-auth success persists new token | AC4 |
| P1 | both fail - returns CredentialsInvalid | AC5 |
| P1 | both fail - clears stored token | AC5 |
| P1 | both fail - does NOT clear credentials | AC5 |
| P1 | no credentials stored - returns NoCredentials immediately | AC6 |
| P1 | no credentials - skips both refresh and re-auth | AC6 |
| P1 | refresh throws IOException - returns Offline | AC7 |
| P1 | offline - does NOT attempt credential re-auth | AC7 |
| P2 | concurrent calls only execute one refresh cycle | AC8 |

### Unit Tests - ViewModel (`:feature:auth`)

**File:** `feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/ui/StartupAuthViewModelTest.kt`

| Priority | Test | AC |
|----------|------|-----|
| P0 | Success result emits NavigateToMain event | AC2/4 |
| P1 | Offline result emits NavigateToMain | AC7 |
| P1 | CredentialsInvalid result emits NavigateToCredentials | AC5 |
| P1 | NoCredentials result emits NavigateToSetup | AC6 |

## Fixture/Fake Needs (to create during implementation)

- `FakeTokenProvider` - in-memory token store for UseCase tests
- `FakeCredentialProvider` - in-memory credential store for UseCase tests
- `FakeStartupAuthRepository` - configurable AuthResult responses
- `FakeStartupAuthUseCase` - configurable StartupAuthResult for ViewModel tests
- `StartupAuthResult` sealed class (production code)
- `StartupAuthEvent` sealed class (production code)

## Next Steps (Task-by-Task Activation)

During implementation of each task:

1. Remove `@Disabled` from the relevant test class/nested class
2. Create minimum production stubs to make the test compile
3. Run tests: `./gradlew :core:network:test` / `:core:data:test` / `:feature:auth:test`
4. Verify the activated test FAILS first (red), then implement to make it PASS (green)
5. Refactor if needed (refactor phase)
6. Commit passing tests with implementation

### Suggested activation order:

1. **Task 1 (AuthService):** Activate `AuthServiceRefreshContractTest`
2. **Task 2-3 (TokenManager/Repository):** Activate `StartupAuthUseCaseTest`
3. **Task 4-5 (ViewModel/Screen):** Activate `StartupAuthViewModelTest`

## Implementation Guidance

### Production classes to create:

| Class | Module | Package |
|-------|--------|---------|
| `AuthService.refreshToken()` | `:core:network` | `dev.xexanos.mealie.core.network.api` |
| `TokenProvider` interface | `:core:network` | `dev.xexanos.mealie.core.network.auth` |
| `CredentialProvider` interface | `:core:network` | `dev.xexanos.mealie.core.network.auth` |
| `StartupAuthUseCase` | `:core:data` | `dev.xexanos.mealie.core.data.domain` |
| `StartupAuthResult` sealed class | `:core:data` | `dev.xexanos.mealie.core.data.domain` |
| `StartupAuthViewModel` | `:feature:auth` | `dev.xexanos.mealie.feature.auth.ui` |
| `StartupAuthEvent` sealed class | `:feature:auth` | `dev.xexanos.mealie.feature.auth.ui` |
| `StartupScreen` composable | `:feature:auth` | `dev.xexanos.mealie.feature.auth.ui` |
