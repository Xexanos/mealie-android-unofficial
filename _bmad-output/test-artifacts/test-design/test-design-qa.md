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

- ~48 tests organized by feature area
- All tests must pass (100%) before merge
- **Total effort**: ~35-56 hours (~1-1.5 weeks with 1 developer)

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
    fun `when viewmodel initialized then items loaded from repository`() = runTest {
        val items = listOf(aShoppingItem(), aShoppingItem())
        every { repository.observeItems(any()) } returns flowOf(items)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(items.size, state.items.size)
        }
    }
}
```

**Instrumented test pattern (JUnit 4 + @DisplayName):**

```kotlin
@RunWith(AndroidJUnit4::class)
class EncryptedDataStoreTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    @DisplayName("when credentials stored then readable after app restart")
    fun whenCredentialsStoredThenReadableAfterRestart() {
        // ...
    }

    @Test
    @DisplayName("when datastore corrupted then re-auth triggered")
    fun whenDatastoreCorruptedThenReAuthTriggered() {
        // ...
    }
}
```

---

## Risk Assessment

**Full risk details in Architecture doc.** This section summarizes QA test coverage per risk.

### High-Priority Risks (Score >= 6)

| Risk ID | Category | Description | Score | QA Test Coverage |
| --- | --- | --- | --- | --- |
| **R-01** | SEC | datastore-tink alpha breaking changes | **6** | Encrypted read/write cycle on real device |
| **R-02** | SEC | Keystore key lost after backup restore | **6** | Simulate decryption failure, verify re-auth |

### Medium/Low-Priority Risks

| Risk ID | Category | Description | Score | QA Test Coverage |
| --- | --- | --- | --- | --- |
| R-03 | TECH | ConnectivityMonitor false positive | 4 | Indicator timing tests; mid-sync failure recovery |
| R-04 | DATA | Clock skew drops edits | 4 | Conflict resolution unit test |
| R-05 | PERF | 500+ items UI jank | 4 | Scroll benchmark |
| R-06 | OPS | No instrumented CI | 4 | Mitigated by local pass requirement |
| R-07 | TECH | Room auto-migration failure | 3 | Additive schema migration test |
| R-08 | BUS | Mealie API changes | 2 | MockWebServer fixture contract test |

---

## NFR Test Coverage Plan

| NFR Category | Requirement / Threshold | Planned Validation | Tool / Level | Evidence Artifact |
| --- | --- | --- | --- | --- |
| Security | Keystore encryption; no secrets in logcat | Encrypted DataStore cycle test + Timber log assertion | Instrumented + Unit | Test results + log scan |
| Performance | Cold start < 3s; local reads < 100ms | Startup benchmark + Room query timing | Macrobenchmark | Benchmark JSON |
| Reliability | Sync queue survives process death; drain < 30s | Process recreation test + timed sync drain | Integration | Test results |
| Maintainability | Line coverage >= 80% core, >= 70% feature | JaCoCo coverage gate in CI | CI report | HTML/XML report |

**Missing thresholds or evidence sources:** None - all NFR thresholds specified in PRD.

---

## Entry Criteria

- [ ] `CredentialStore` interface implemented with fake available
- [ ] MockWebServer JSON fixtures committed to test resources
- [ ] Room in-memory test setup verified working
- [ ] `MainDispatcherExtension` utility class available
- [ ] Feature code under test deployed to module (code exists to test)

## Exit Criteria

- [ ] All tests passing (100%)
- [ ] No open high-severity bugs
- [ ] Line coverage >= 80% on `:core:*` modules
- [ ] Line coverage >= 70% on `:feature:*` modules
- [ ] Instrumented security tests pass on 3+ device configurations

---

## Test Coverage Plan

Tests are organized by feature area. All tests carry equal weight - any failure blocks merge.

### Authentication & Security

| Test ID | Scenario | Test Level | Risk Link | Notes |
| --- | --- | --- | --- | --- |
| **AUTH-001** | Silent token refresh on launch (expired token) | Unit | R-01 | FakeTokenStore returns expired token |
| **AUTH-002** | Credential fallback when refresh fails | Unit | R-01, R-02 | Verify full fallback chain |
| **AUTH-003** | Encrypted DataStore read/write cycle | Instrumented | R-01, R-02 | Real Keystore on device |
| **AUTH-004** | Encrypted DataStore decryption failure triggers re-auth | Instrumented | R-02 | Simulate corrupted file |
| **AUTH-005** | OkHttp Authenticator intercepts 401 and refreshes | Unit | - | MockWebServer returns 401 then 200 |
| **AUTH-006** | Concurrent token refresh safety (Mutex) | Unit | - | Launch parallel refresh coroutines |
| **AUTH-007** | No secrets in logcat (release build) | Instrumented | - | Timber log capture assertion |
| **AUTH-008** | Login with valid credentials | Unit | - | MockWebServer returns token |
| **AUTH-009** | Login with invalid credentials shows error | Unit | - | MockWebServer returns 401 |

### Server Setup

| Test ID | Scenario | Test Level | Risk Link | Notes |
| --- | --- | --- | --- | --- |
| **SETUP-001** | Server URL validation (scheme, trailing slash, IPv6) | Unit | - | Edge cases from existing tests |
| **SETUP-002** | HTTP security warning flow | Unit | - | Warning shown for http:// URLs |

### Shopping List

| Test ID | Scenario | Test Level | Risk Link | Notes |
| --- | --- | --- | --- | --- |
| **SHOP-001** | Shopping list loads from Room (offline read) | Unit | - | In-memory Room with pre-seeded data |
| **SHOP-002** | Shopping list items display in correct order | Unit | - | ViewModel sorts by position |
| **SHOP-003** | Check/uncheck item (optimistic + sync) | Unit | - | Local update immediate; sync queued |
| **SHOP-004** | Add item (optimistic + sync) | Unit | - | Item appears locally; queued |
| **SHOP-005** | Delete item (optimistic + sync) | Unit | - | Item removed locally; queued |
| **SHOP-006** | Multiple lists - switch between lists | Unit | - | ViewModel handles list ID change |
| **SHOP-007** | Sort preferences persistence per list | Unit | - | DataStore keyed by list ID |
| **SHOP-008** | Shopping mode persistence (DataStore) | Unit | - | FakeDataStore |
| **SHOP-009** | Shopping mode auto-timeout | Unit | - | Time-based state transition |
| **SHOP-010** | Large list performance (500+ items, no jank) | Benchmark | R-05 | Macrobenchmark scroll test |
| **SHOP-011** | Error state UI (sync failure overlay) | Unit | - | ViewModel overlay state |

### Sync & Offline

| Test ID | Scenario | Test Level | Risk Link | Notes |
| --- | --- | --- | --- | --- |
| **SYNC-001** | Sync queue persists across process death | Integration | - | Write queue, recreate component, verify items |
| **SYNC-002** | SyncWorker flushes queue on connectivity | Integration | R-03 | TestListenableWorkerBuilder |
| **SYNC-003** | Conflict resolution (last-write-wins via updated_at) | Unit | R-04 | Pure function, deterministic timestamps |
| **SYNC-004** | SyncWorker respects Wi-Fi Only constraint | Unit | - | WorkManager constraint verification |
| **SYNC-005** | SyncWorker retry with exponential backoff | Unit | - | Verify Result.retry() on failure |
| **SYNC-006** | Sync idempotency (duplicate delivery no side effect) | Integration | - | Send same mutation twice |
| **SYNC-007** | Mid-sync network failure recovery | Integration | R-03 | MockWebServer drops mid-request |
| **SYNC-008** | Sync status badge shows pending/failed/synced per item | Unit | - | ViewModel maps queue state |

### Connectivity

| Test ID | Scenario | Test Level | Risk Link | Notes |
| --- | --- | --- | --- | --- |
| **CONN-001** | Offline indicator appears within 3s of connectivity loss | Unit | R-03 | FakeConnectivityMonitor emit Offline |
| **CONN-002** | Offline indicator disappears within 3s of restoration | Unit | R-03 | FakeConnectivityMonitor emit Online |

### Settings

| Test ID | Scenario | Test Level | Risk Link | Notes |
| --- | --- | --- | --- | --- |
| **SET-001** | Update credentials flow | Unit | - | New credentials stored; token refreshed |
| **SET-002** | Change server URL clears Local Store | Unit | - | Room tables cleared on URL change |

### Data Layer & Infrastructure

| Test ID | Scenario | Test Level | Risk Link | Notes |
| --- | --- | --- | --- | --- |
| **DATA-001** | Room DAO: insert, query, update, delete shopping items | Integration | - | In-memory Room |
| **DATA-002** | Repository maps Dto to Domain correctly | Unit | - | Pure mapper function test |
| **DATA-003** | API contract: MockWebServer fixture matches expected schema | Unit | R-08 | Deserialize fixture to Dto |
| **DATA-004** | Network timeout handling (10s connect, 30s read) | Unit | - | MockWebServer with delay |
| **DATA-005** | Koin module graph validates (checkModules) | Unit | - | Koin verify utility |
| **DATA-006** | Room TypeConverter Instant/Long round-trip | Unit | - | Pure function test |
| **DATA-007** | 422 validation error logging (not surfaced to user) | Unit | - | Verify no UiState change |
| **DATA-008** | Household info cached correctly | Unit | - | Repository caches API response |
| **DATA-009** | Room auto-migration additive schema change | Integration | R-07 | Test with Room testing library |

### App-Level & Accessibility

| Test ID | Scenario | Test Level | Risk Link | Notes |
| --- | --- | --- | --- | --- |
| **APP-001** | Navigation graph smoke test (all routes reachable) | Instrumented | - | NavHost test in :app |
| **APP-002** | Accessibility: TalkBack content descriptions | Instrumented | - | Semantics node assertions |
| **APP-003** | Accessibility: contrast ratio (theme validation) | Unit | - | Color constant assertions |
| **APP-004** | Cold start benchmark (< 3s target) | Benchmark | - | Macrobenchmark startup |
| **APP-005** | Bug report intent launches correctly | Instrumented | - | Intent assertion |

**Total:** ~48 tests

---

## Execution Strategy

**Philosophy:** Run everything in PRs unless there's significant infrastructure overhead. With Gradle parallel test execution, ~40 unit/integration tests complete in under 5 minutes.

### Every PR: Unit + Integration Tests (< 5 min)

All tests that run on JVM without a device:

- JUnit 5 unit tests (MockK, Turbine, MockWebServer)
- Room in-memory integration tests
- Koin checkModules validation
- Total: ~40 tests parallelized across modules

**Why run in PRs:** Fast feedback, no device/emulator required

### Pre-Release: Instrumented Tests (local, ~10 min)

Tests that require Android Keystore or Compose UI runtime:

- AUTH-003, AUTH-004, AUTH-007: Encrypted DataStore + log scan
- APP-001: Navigation graph smoke test
- APP-002: TalkBack accessibility
- APP-005: Intent test
- SHOP-010, APP-004: Benchmarks (scroll + cold start)

**Why defer:** Requires physical device or emulator; no emulator CI in v1

---

## QA Effort Estimate

| Area | Count | Effort Range |
| --- | --- | --- |
| Authentication & Security | 9 | ~10-16 hours |
| Shopping List | 11 | ~6-10 hours |
| Sync & Offline | 8 | ~8-14 hours |
| Data Layer & Infrastructure | 9 | ~5-8 hours |
| Setup + Connectivity + Settings + App | 9 | ~6-10 hours |
| **Total** | ~48 | **~35-56 hours (~1-1.5 weeks)** |

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

## Appendix A: Test Naming Convention & Code Examples

### Naming Convention: When-Then

All test names follow **When-Then** structure: `when <trigger/precondition> then <expected outcome>`.

**JVM tests (JUnit 5)** - use backtick syntax:

```kotlin
@Test
fun `when token expired then refreshes silently`() = runTest { ... }

@Test
fun `when sync fails mid-request then retries with backoff`() = runTest { ... }

@Test
fun `when server url has trailing slash then normalized before storage`() { ... }
```

**Instrumented tests (JUnit 4)** - camelCase method + `@DisplayName` annotation:

```kotlin
@Test
@DisplayName("when datastore corrupted then re-auth triggered")
fun whenDatastoreCorruptedThenReAuthTriggered() { ... }

@Test
@DisplayName("when talkback active then all items have content description")
fun whenTalkbackActiveThenAllItemsHaveContentDescription() { ... }
```

### Full Example: Auth Unit Test

```kotlin
@ExtendWith(MainDispatcherExtension::class)
class TokenRefreshTest {

    @Test
    fun `when token expired then refreshes using stored credentials`() = runTest {
        val fakeTokenStore = FakeTokenStore(storedToken = "expired-token")
        val mockServer = MockWebServer()
        mockServer.enqueue(MockResponse().setResponseCode(401))
        mockServer.enqueue(MockResponse().setBody("""{"access_token":"new-token"}"""))

        val authRepository = AuthRepositoryImpl(
            authService = createAuthService(mockServer.url("/")),
            tokenStore = fakeTokenStore
        )

        val result = authRepository.refreshToken()

        assertTrue(result is ApiResult.Success)
        assertEquals("new-token", fakeTokenStore.currentToken)
    }
}
```

### Full Example: Instrumented Test

```kotlin
@RunWith(AndroidJUnit4::class)
class NavigationGraphTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    @DisplayName("when app launched then all routes reachable from nav graph")
    fun whenAppLaunchedThenAllRoutesReachableFromNavGraph() {
        composeTestRule.onNodeWithTag("nav_host").assertExists()
        // verify routes...
    }
}
```

**Run all tests (CI default):**

```bash
./gradlew test
```

---

## Appendix B: Knowledge Base References

- **Risk Governance**: `risk-governance.md` - Risk scoring methodology (P x I, >= 6 = HIGH)
- **Test Levels Framework**: `test-levels-framework.md` - Unit/Integration/Instrumented selection
- **Test Quality**: `test-quality.md` - Definition of Done (no hard waits, focused assertions, < 1.5 min per test)
- **NFR Criteria**: `nfr-criteria.md` - NFR validation planning patterns

---

**Generated by:** BMad TEA Agent
**Workflow:** `bmad-testarch-test-design`
