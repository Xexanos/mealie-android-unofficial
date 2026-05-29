---
stepsCompleted: ['step-01-preflight-and-context', 'step-02-generation-mode', 'step-03-test-strategy', 'step-04c-aggregate', 'step-05-validate-and-complete']
lastStep: 'step-05-validate-and-complete'
lastSaved: '2026-05-28'
storyId: '1.5'
storyKey: '1-5-credential-entry-and-encrypted-storage'
storyFile: '_bmad-output/implementation-artifacts/1-5-credential-entry-and-encrypted-storage.md'
atddChecklistPath: '_bmad-output/test-artifacts/atdd-checklist-1-5-credential-entry-and-encrypted-storage.md'
generatedTestFiles:
  - 'feature/auth/src/test/java/dev/xexanos/mealie/feature/auth/ui/CredentialViewModelTest.kt'
  - 'core/network/src/test/java/dev/xexanos/mealie/core/network/api/AuthServiceContractTest.kt'
inputDocuments:
  - '_bmad-output/implementation-artifacts/1-5-credential-entry-and-encrypted-storage.md'
  - '_bmad-output/implementation-artifacts/1-4-http-security-warning-for-non-https-urls.md'
  - '_bmad-output/planning-artifacts/architecture.md'
---

# ATDD Checklist: Story 1.5 - Credential Entry and Encrypted Storage

## TDD Red Phase (Current)

All red-phase test scaffolds generated with `@Disabled` annotations (JUnit 5).

- Unit Tests (ViewModel): 11 tests (all disabled)
- Integration Tests (API Contract): 5 tests (all disabled)

Total: **16 tests** - all will FAIL once `@Disabled` is removed (classes don't exist yet).

## Acceptance Criteria Coverage

| AC | Description | Test Level | Test Count | Priority |
|----|-------------|-----------|------------|----------|
| AC1 | Initial state: AwaitingInput with empty fields | Unit (ViewModel) | 1 | P1 |
| AC2 | Empty field validation - no network call | Unit (ViewModel) | 3 | P0 |
| AC3 | POST /api/auth/token form-encoded request | Integration (MockWebServer) | 2 | P0 |
| AC3/4 | Successful auth emits NavigateToMain | Unit (ViewModel) | 2 | P0 |
| AC4 | 200 response parses access_token | Integration (MockWebServer) | 1 | P0 |
| AC5 | 401 clears password, retains username | Unit (ViewModel) | 1 | P0 |
| AC5 | 401 response code handling | Integration (MockWebServer) | 1 | P0 |
| AC6 | Network error retains both fields | Unit (ViewModel) | 1 | P1 |
| AC8 | Error messages use @StringRes IDs | Unit (ViewModel) | 2 | P2 |
| Edge | Double-tap guard (isSubmitting no-op) | Unit (ViewModel) | 1 | P1 |
| Edge | remember_me=true in request | Integration (MockWebServer) | 1 | P1 |
| Edge | Request NOT JSON-encoded | Integration (MockWebServer) | 1 | P0 |

## Generated Test Files

| File | Level | Tests | Status |
|------|-------|-------|--------|
| `feature/auth/src/test/java/.../CredentialViewModelTest.kt` | Unit | 11 | RED (all @Disabled) |
| `core/network/src/test/java/.../AuthServiceContractTest.kt` | Integration | 5 | RED (all @Disabled) |

## Test Infrastructure Used

- **Framework:** JUnit 5 + `@ExtendWith(MainDispatcherExtension::class)`
- **Async:** Turbine (`events.test {}`) + `kotlinx.coroutines.test.runTest`
- **Test Doubles:** `FakeAuthRepository` (needs `authResult` and `authenticateCallCount` additions)
- **API Contract:** MockWebServer + Retrofit
- **Annotations:** `@Disabled("Red phase - ...")` for TDD red phase

## Next Steps (Task-by-Task Activation)

During implementation of each task:

1. Implement the class/method referenced by the test
2. Remove `@Disabled` from the relevant test(s)
3. Run: `./gradlew :feature:auth:test` or `./gradlew :core:network:test`
4. Verify the activated test FAILS first (compilation error = expected for red phase)
5. Implement until the test PASSES (green phase)
6. Commit passing tests with the implementation

### Activation Order (recommended)

1. **Task 1 (AuthService):** Remove @Disabled from `AuthServiceContractTest` (5 tests)
2. **Task 2-3 (TokenStore + CredentialsStore):** No dedicated red-phase tests (tested via Repository integration)
3. **Task 4 (AuthRepository.authenticate):** Tested transitively via ViewModel tests
4. **Task 5-7 (CredentialViewModel + Screen + Navigation):** Remove @Disabled from `CredentialViewModelTest` (11 tests)

## Implementation Guidance

### API Endpoint

```
POST /api/auth/token
Content-Type: application/x-www-form-urlencoded
Body: username={}&password={}&remember_me=true
Response 200: {"access_token": "...", "token_type": "bearer"}
Response 401: {"detail": "Unauthorized"}
```

### FakeAuthRepository Additions Required

```kotlin
// Add to existing FakeAuthRepository.kt:
var authResult: AuthResult = AuthResult.Success
var authenticateCallCount = 0

override suspend fun authenticate(username: String, password: String): AuthResult {
    authenticateCallCount++
    return authResult
}
```

## Execution Report

- Execution Mode: SEQUENTIAL (adapted for Android/Kotlin)
- TDD Phase: RED
- All tests reference non-existent classes (will not compile until implemented)
- No placeholder assertions - all assert expected behavior
