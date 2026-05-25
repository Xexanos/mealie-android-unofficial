---
workflowStatus: 'completed'
totalSteps: 5
stepsCompleted: ['step-01-detect-mode', 'step-02-load-context', 'step-03-risk-and-testability', 'step-04-coverage-plan', 'step-05-generate-output']
lastStep: 'step-05-generate-output'
nextStep: ''
lastSaved: '2026-05-25'
workflowType: 'testarch-test-design'
inputDocuments:
  - _bmad-output/planning-artifacts/prds/prd-mealie-android-2026-05-21/prd.md
  - _bmad-output/planning-artifacts/architecture.md
  - _bmad-output/planning-artifacts/epics.md
---

# Test Design for QA: Mealie Android v1

**Purpose:** Test execution recipe. Defines what to test, how to test it, and what infrastructure is needed.

**Date:** 2026-05-25
**Author:** Xexanos
**Status:** Draft
**Project:** mealie-android-unofficial

**Related:** See Architecture doc (`test-design-architecture.md`) for testability concerns and risk mitigation plans.

---

## Executive Summary

**Scope:** Full v1 product scope - server setup/auth, shopping list CRUD with offline support, connectivity awareness, app settings. Recipe browsing deferred to v2.

**Risk Summary:**

- Total Risks: 8 (2 high-priority score >= 6, 4 medium, 2 low)
- Critical Categories: SEC (encrypted storage), TECH (connectivity)

**Coverage Summary:**

- P0 tests: ~10 (auth, security, offline core)
- P1 tests: ~20 (feature flows, sync, connectivity)
- P2 tests: ~15 (edge cases, accessibility, performance guard)
- P3 tests: ~3 (benchmarks, migration, misc)
- **Total**: ~48 tests (~1-1.5 weeks with 1 developer)

---

## Not in Scope

| Item | Reasoning | Mitigation |
| --- | --- | --- |
| **Recipe browsing (FR-7, FR-8, FR-9)** | Explicitly deferred to v2 in PRD | No testing needed until v2 |
| **Multi-server support** | Not in v1 architecture | Single-instance assumption documented |
| **Screenshot/visual regression tests** | Small UI surface; solo developer | Manual visual check before release |
| **E2E tests against live Mealie instance** | Requires server infrastructure | MockWebServer covers API contract |

---

## Dependencies & Test Blockers

### Architecture Dependencies (Pre-Implementation)

1. **CredentialStore interface** - Dev - Auth story implementation
   - QA needs a fakeable interface to test auth flows without Android Keystore
   - Blocks all auth-related unit tests

2. **MockWebServer JSON fixtures** - Dev - Network module setup
   - QA needs fixtures matching Mealie v3.18.0 OpenAPI spec
   - Blocks API contract tests

### QA Infrastructure Setup

1. **Test Data Factories**
   - `ShoppingListItemEntity` factory with randomized fields
   - `ShoppingListDto` factory matching API response shape
   - Room in-memory DB setup/teardown per test class

2. **Test Utilities**
   - `MainDispatcherExtension` (JUnit 5) - `UnconfinedTestDispatcher` default
   - `FakeCredentialStore` / `FakeTokenStore` - in-memory implementations
   - `FakeConnectivityMonitor` - controllable `StateFlow<ConnectivityState>`
   - MockWebServer base class with fixture loading helpers

**Example test pattern (Kotlin + JUnit 5 + MockK + Turbine):**

```kotlin
@ExtendWith(MainDispatcherExtension::class)
class ShoppingListViewModelTest {

    private val repository = mockk<ShoppingRepository>()
    private val viewModel by lazy { ShoppingListViewModel(repository) }

    @Test
    fun `loads items from repository on init`() = runTest {
        val items = listOf(aShoppingItem(), aShoppingItem())
        every { repository.observeItems(any()) } returns flowOf(items)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(items.size, state.items.size)
        }
    }
}
```

---

## Risk Assessment

**Full risk details in Architecture doc.** This section summarizes QA test coverage per risk.

### High-Priority Risks (Score >= 6)

| Risk ID | Category | Description | Score | QA Test Coverage |
| --- | --- | --- | --- | --- |
| **R-01** | SEC | datastore-tink alpha breaking changes | **6** | P0-003: encrypted read/write cycle on real device |
| **R-02** | SEC | Keystore key lost after backup restore | **6** | P0-004: simulate decryption failure, verify re-auth |

### Medium/Low-Priority Risks

| Risk ID | Category | Description | Score | QA Test Coverage |
| --- | --- | --- | --- | --- |
| R-03 | TECH | ConnectivityMonitor false positive | 4 | P1-009/P1-010: indicator timing; P2-013: mid-sync failure |
| R-04 | DATA | Clock skew drops edits | 4 | P0-009: conflict resolution unit test |
| R-05 | PERF | 500+ items UI jank | 4 | P2-001: scroll benchmark |
| R-06 | OPS | No instrumented CI | 4 | Mitigated by local pass requirement |
| R-07 | TECH | Room auto-migration failure | 3 | P3-002: additive schema migration test |
| R-08 | BUS | Mealie API changes | 2 | P1-017: MockWebServer fixture contract test |

---

## NFR Test Coverage Plan

| NFR Category | Requirement / Threshold | Planned Validation | Tool / Level | Evidence Artifact | Priority |
| --- | --- | --- | --- | --- | --- |
| Security | Keystore encryption; no secrets in logcat | Encrypted DataStore cycle test + Timber log assertion | Instrumented + Unit | Test results + log scan | P0 |
| Performance | Cold start < 3s; local reads < 100ms | Startup benchmark + Room query timing | Macrobenchmark | Benchmark JSON | P3/P2 |
| Reliability | Sync queue survives process death; drain < 30s | Process recreation test + timed sync drain | Integration | Test results | P0 |
| Maintainability | Line coverage >= 80% core, >= 70% feature | JaCoCo coverage gate in CI | CI report | HTML/XML report | - |

**Missing thresholds or evidence sources:** None - all NFR thresholds specified in PRD.

---

## Entry Criteria

- [ ] `CredentialStore` interface implemented with fake available
- [ ] MockWebServer JSON fixtures committed to test resources
- [ ] Room in-memory test setup verified working
- [ ] `MainDispatcherExtension` utility class available
- [ ] Feature code under test deployed to module (code exists to test)

## Exit Criteria

- [ ] All P0 tests passing (100%)
- [ ] All P1 tests passing (100%)
- [ ] All P2 tests passing (100%)
- [ ] No open high-severity bugs
- [ ] Line coverage >= 80% on `:core:*` modules
- [ ] Line coverage >= 70% on `:feature:*` modules
- [ ] Instrumented security tests pass on 3+ device configurations

---

## Test Coverage Plan

**IMPORTANT:** P0/P1/P2/P3 = **priority and risk level** (what to focus on if time-constrained), NOT execution timing. See "Execution Strategy" for when tests run.

### P0 (Critical)

**Criteria:** Blocks core functionality + High risk (>= 6) + No workaround + Affects majority of users

| Test ID | Requirement | Test Level | Risk Link | Notes |
| --- | --- | --- | --- | --- |
| **P0-001** | Silent token refresh on launch (expired token) | Unit | R-01 | FakeTokenStore returns expired token |
| **P0-002** | Credential fallback when refresh fails | Unit | R-01, R-02 | Verify full fallback chain |
| **P0-003** | Encrypted DataStore read/write cycle | Instrumented | R-01, R-02 | Real Keystore on device |
| **P0-004** | Encrypted DataStore decryption failure triggers re-auth | Instrumented | R-02 | Simulate corrupted file |
| **P0-005** | OkHttp Authenticator intercepts 401 and refreshes | Unit | - | MockWebServer returns 401 then 200 |
| **P0-006** | Shopping list loads from Room (offline read) | Unit | - | In-memory Room with pre-seeded data |
| **P0-007** | Sync queue persists across process death | Integration | - | Write queue, recreate component, verify items |
| **P0-008** | SyncWorker flushes queue on connectivity | Integration | R-03 | TestListenableWorkerBuilder |
| **P0-009** | Conflict resolution (last-write-wins via updated_at) | Unit | R-04 | Pure function, deterministic timestamps |
| **P0-010** | No secrets in logcat (release build) | Instrumented | - | Timber log capture assertion |

**Total P0:** ~10 tests

---

### P1 (High)

**Criteria:** Important features + Medium risk (3-4) + Common workflows

| Test ID | Requirement | Test Level | Risk Link | Notes |
| --- | --- | --- | --- | --- |
| **P1-001** | Server URL validation (scheme, trailing slash, IPv6) | Unit | - | Edge cases from existing tests |
| **P1-002** | HTTP security warning flow | Unit | - | Warning shown for http:// URLs |
| **P1-003** | Login with valid credentials | Unit | - | MockWebServer returns token |
| **P1-004** | Login with invalid credentials shows error | Unit | - | MockWebServer returns 401 |
| **P1-005** | Shopping list items display in correct order | Unit | - | ViewModel sorts by position |
| **P1-006** | Check/uncheck item (optimistic + sync) | Unit | - | Local update immediate; sync queued |
| **P1-007** | Add item (optimistic + sync) | Unit | - | Item appears locally; queued |
| **P1-008** | Delete item (optimistic + sync) | Unit | - | Item removed locally; queued |
| **P1-009** | Offline indicator appears within 3s of connectivity loss | Unit | R-03 | FakeConnectivityMonitor emit Offline |
| **P1-010** | Offline indicator disappears within 3s of restoration | Unit | R-03 | FakeConnectivityMonitor emit Online |
| **P1-011** | Sync status badge shows pending/failed/synced per item | Unit | - | ViewModel maps queue state |
| **P1-012** | SyncWorker respects Wi-Fi Only constraint | Unit | - | WorkManager constraint verification |
| **P1-013** | SyncWorker retry with exponential backoff | Unit | - | Verify Result.retry() on failure |
| **P1-014** | Concurrent token refresh safety (Mutex) | Unit | - | Launch parallel refresh coroutines |
| **P1-015** | Room DAO: insert, query, update, delete shopping items | Integration | - | In-memory Room |
| **P1-016** | Repository maps Dto to Domain correctly | Unit | - | Pure mapper function test |
| **P1-017** | API contract: MockWebServer fixture matches expected schema | Unit | R-08 | Deserialize fixture to Dto |
| **P1-018** | Shopping mode persistence (DataStore) | Unit | - | FakeDataStore |
| **P1-019** | Settings: update credentials flow | Unit | - | New credentials stored; token refreshed |
| **P1-020** | Settings: change server URL clears Local Store | Unit | - | Room tables cleared on URL change |

**Total P1:** ~20 tests

---

### P2 (Medium)

**Criteria:** Secondary features + Low risk (1-2) + Edge cases + Regression prevention

| Test ID | Requirement | Test Level | Risk Link | Notes |
| --- | --- | --- | --- | --- |
| **P2-001** | Large list performance (500+ items, no jank) | Benchmark | R-05 | Macrobenchmark scroll test |
| **P2-002** | Multiple lists - switch between lists | Unit | - | ViewModel handles list ID change |
| **P2-003** | Sort preferences persistence per list | Unit | - | DataStore keyed by list ID |
| **P2-004** | Network timeout handling (10s connect, 30s read) | Unit | - | MockWebServer with delay |
| **P2-005** | Koin module graph validates (checkModules) | Unit | - | Koin verify utility |
| **P2-006** | Navigation graph smoke test (all routes reachable) | Instrumented | - | NavHost test in :app |
| **P2-007** | Shopping mode auto-timeout | Unit | - | Time-based state transition |
| **P2-008** | Sync idempotency (duplicate delivery no side effect) | Integration | - | Send same mutation twice |
| **P2-009** | Room TypeConverter Instant/Long round-trip | Unit | - | Pure function test |
| **P2-010** | Accessibility: TalkBack content descriptions | Instrumented | - | Semantics node assertions |
| **P2-011** | Accessibility: contrast ratio (theme validation) | Unit | - | Color constant assertions |
| **P2-012** | Error state UI (sync failure overlay) | Unit | - | ViewModel overlay state |
| **P2-013** | Mid-sync network failure recovery | Integration | R-03 | MockWebServer drops mid-request |
| **P2-014** | 422 validation error logging (not surfaced to user) | Unit | - | Verify no UiState change |
| **P2-015** | Household info cached correctly | Unit | - | Repository caches API response |

**Total P2:** ~15 tests

---

### P3 (Low)

**Criteria:** Nice-to-have + Exploratory + Performance benchmarks

| Test ID | Requirement | Test Level | Risk Link | Notes |
| --- | --- | --- | --- | --- |
| **P3-001** | Cold start benchmark (< 3s target) | Benchmark | - | Macrobenchmark startup |
| **P3-002** | Room auto-migration additive schema change | Integration | R-07 | Test with Room testing library |
| **P3-003** | Bug report intent launches correctly | Instrumented | - | Intent assertion |

**Total P3:** ~3 tests

---

## Execution Strategy

**Philosophy:** Run everything in PRs unless there's significant infrastructure overhead. With Gradle parallel test execution, ~45 unit/integration tests complete in under 5 minutes.

### Every PR: Unit + Integration Tests (< 5 min)

**All functional tests** (P0 through P3, excluding instrumented):

- JUnit 5 unit tests (MockK, Turbine, MockWebServer)
- Room in-memory integration tests
- Koin checkModules validation
- Total: ~40 tests parallelized across modules

**Why run in PRs:** Fast feedback, no device/emulator required

### Pre-Release: Instrumented Tests (local, ~10 min)

**Device-dependent tests** (require Android Keystore or Compose UI):

- P0-003, P0-004: Encrypted DataStore (real Keystore)
- P0-010: Log scan (release build)
- P2-006: Navigation graph smoke test
- P2-010: TalkBack accessibility
- P3-003: Intent test
- Benchmarks: P2-001 (scroll), P3-001 (cold start)

**Why defer:** Requires physical device or emulator; no emulator CI in v1

---

## QA Effort Estimate

| Priority | Count | Effort Range | Notes |
| --- | --- | --- | --- |
| P0 | ~10 | ~15-25 hours | Complex setup (security, sync integration) |
| P1 | ~20 | ~12-20 hours | Standard ViewModel/Repository tests |
| P2 | ~15 | ~8-12 hours | Edge cases, accessibility |
| P3 | ~3 | ~2-4 hours | Benchmarks, migration |
| **Total** | ~48 | **~35-56 hours (~1-1.5 weeks)** | **1 developer, full-time** |

**Assumptions:**

- Includes test design, implementation, debugging, CI integration
- Excludes ongoing maintenance (~10% effort per sprint)
- Assumes test infrastructure (fakes, fixtures, utilities) built alongside feature code
- Tests written incrementally per story, not as a separate phase

---

## Implementation Planning Handoff

| Work Item | Owner | Dependencies/Notes |
| --- | --- | --- |
| `MainDispatcherExtension` utility | Dev | First; all ViewModel tests depend on it |
| `FakeCredentialStore` / `FakeTokenStore` | Dev | After `CredentialStore` interface exists |
| `FakeConnectivityMonitor` | Dev | After `ConnectivityMonitor` interface exists |
| MockWebServer base class + fixtures | Dev | After `:core:network` API services defined |
| Room test factory functions | Dev | After entity classes defined |
| JaCoCo CI integration | Dev | After first tests exist; add to `test` CI job |

---

## Appendix A: Code Examples & Tagging

**JUnit 5 Test Tagging for Selective Execution:**

```kotlin
@Tag("P0")
@ExtendWith(MainDispatcherExtension::class)
class TokenRefreshTest {

    @Test
    fun `refreshes expired token silently on launch`() = runTest {
        // Arrange
        val fakeTokenStore = FakeTokenStore(storedToken = "expired-token")
        val mockServer = MockWebServer()
        mockServer.enqueue(MockResponse().setResponseCode(401))
        mockServer.enqueue(MockResponse().setBody("""{"access_token":"new-token"}"""))

        val authRepository = AuthRepositoryImpl(
            authService = createAuthService(mockServer.url("/")),
            tokenStore = fakeTokenStore
        )

        // Act
        val result = authRepository.refreshToken()

        // Assert
        assertTrue(result is ApiResult.Success)
        assertEquals("new-token", fakeTokenStore.currentToken)
    }
}
```

**Run specific tags:**

```bash
# Run only P0 tests
./gradlew test --tests "*" -Djunit.jupiter.tag.include=P0

# Run P0 + P1 tests
./gradlew test --tests "*" -Djunit.jupiter.tag.include="P0|P1"

# Run all tests (default in CI)
./gradlew test
```

---

## Appendix B: Knowledge Base References

- **Risk Governance**: `risk-governance.md` - Risk scoring methodology (P x I, >= 6 = HIGH)
- **Test Priorities Matrix**: `test-priorities-matrix.md` - P0-P3 classification criteria
- **Test Levels Framework**: `test-levels-framework.md` - Unit/Integration/Instrumented selection
- **Test Quality**: `test-quality.md` - Definition of Done (no hard waits, focused assertions, < 1.5 min per test)
- **NFR Criteria**: `nfr-criteria.md` - NFR validation planning patterns

---

**Generated by:** BMad TEA Agent
**Workflow:** `bmad-testarch-test-design`
