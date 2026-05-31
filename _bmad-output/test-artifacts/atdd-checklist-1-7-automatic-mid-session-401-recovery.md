---
stepsCompleted: ['step-01-preflight-and-context', 'step-02-generation-mode', 'step-03-test-strategy', 'step-04c-aggregate']
lastStep: 'step-04c-aggregate'
lastSaved: '2026-05-31'
storyId: '1.7'
storyKey: '1-7-automatic-mid-session-401-recovery'
storyFile: '_bmad-output/implementation-artifacts/1-7-automatic-mid-session-401-recovery.md'
atddChecklistPath: '_bmad-output/test-artifacts/atdd-checklist-1-7-automatic-mid-session-401-recovery.md'
generatedTestFiles:
  - 'core/network/src/test/java/dev/xexanos/mealie/core/network/auth/MealieAuthenticatorTest.kt'
  - 'core/network/src/test/java/dev/xexanos/mealie/core/network/auth/TokenInterceptorTest.kt'
  - 'core/data/src/test/java/dev/xexanos/mealie/core/data/auth/AuthenticatorRefresherImplTest.kt'
  - 'core/network/src/test/java/dev/xexanos/mealie/core/network/auth/TokenRefreshIntegrationTest.kt'
inputDocuments:
  - '_bmad-output/implementation-artifacts/1-7-automatic-mid-session-401-recovery.md'
  - '_bmad/tea/config.yaml'
---

# ATDD Checklist: Story 1.7 - Automatic Mid-Session 401 Recovery

## TDD Red Phase (Current)

All tests are marked `@Disabled` (JUnit 5 equivalent of `test.skip()`) and will fail once enabled because the implementation classes do not yet exist.

- Unit Tests: 8 tests (all disabled)
- Integration Tests: 4 tests (all disabled)
- Total: 12 red-phase test scaffolds

## Generated Test Files

| File | Level | Tests | Module |
|------|-------|-------|--------|
| `core/network/src/test/.../auth/MealieAuthenticatorTest.kt` | Unit | 5 | :core:network |
| `core/network/src/test/.../auth/TokenInterceptorTest.kt` | Unit | 3 | :core:network |
| `core/data/src/test/.../auth/AuthenticatorRefresherImplTest.kt` | Unit | 3 | :core:data |
| `core/network/src/test/.../auth/TokenRefreshIntegrationTest.kt` | Integration | 4 | :core:network |

## Acceptance Criteria Coverage

| AC | Description | Tests Covering |
|----|-------------|----------------|
| AC 1 | Authenticator intercepts 401, does NOT call refresh | MealieAuthenticatorTest (implicit - no refresh call in design) |
| AC 2 | Re-auth succeeds: new token persisted, request retried | MealieAuthenticatorTest (2 tests), IntegrationTest (1 test) |
| AC 3 | Re-auth fails: returns null, signals auth failure | MealieAuthenticatorTest (2 tests), AuthenticatorRefresherImplTest (2 tests), IntegrationTest (1 test) |
| AC 4 | Concurrent 401s handled safely | IntegrationTest (covered by OkHttp serial guarantee) |
| AC 5 | Infinite loop prevention (responseCount > 1) | MealieAuthenticatorTest (1 test), IntegrationTest (1 test) |
| AC 6 | TokenInterceptor adds/skips Bearer header | TokenInterceptorTest (3 tests), IntegrationTest (1 test) |
| AC 7 | No new user-facing strings | N/A (no UI, verified by review) |

## Priority Distribution

- P0: 7 tests (infinite loop guard, auth success/failure, header injection)
- P1: 5 tests (empty token skip, refresher delegation, integration flows)
- P2: 0 tests (concurrency covered by framework guarantee)

## Next Steps (Task-by-Task Activation)

During implementation of each task:

1. Remove `@Disabled` from the tests relevant to the current task
2. Run tests: `./gradlew :core:network:test :core:data:test`
3. Verify the activated test fails (compile error = red phase confirmed)
4. Implement the class/method under test
5. Run tests again - verify they pass (green phase)
6. Refactor if needed, re-run tests
7. Commit passing tests with implementation

### Task-to-Test Mapping

| Task | Tests to Activate |
|------|-------------------|
| Task 1: AuthenticatorRefresher interface | (no test - interface only) |
| Task 2: AuthenticatorRefresherImpl | `AuthenticatorRefresherImplTest` (3 tests) |
| Task 3: TokenManager auth failure signal | `MealieAuthenticatorTest` signal test (1 test) |
| Task 4: TokenInterceptor | `TokenInterceptorTest` (3 tests) |
| Task 5: MealieAuthenticator | `MealieAuthenticatorTest` (5 tests) |
| Task 6: Wire into OkHttpClient | `TokenRefreshIntegrationTest` (4 tests) |

## Implementation Guidance

### Classes to Implement

- `core/network/.../auth/AuthenticatorRefresher.kt` - interface
- `core/network/.../auth/TokenInterceptor.kt` - OkHttp Interceptor
- `core/network/.../auth/MealieAuthenticator.kt` - OkHttp Authenticator
- `core/data/.../auth/AuthenticatorRefresherImpl.kt` - delegates to AuthRepository
- `TokenManager` additions: `signalAuthFailure()`, `authFailureEvent` SharedFlow

### Dependencies Required for Tests

- MockK (already in project)
- kotlinx-coroutines-test (already in project)
- OkHttp MockWebServer (already in project)
- No new test dependencies needed
