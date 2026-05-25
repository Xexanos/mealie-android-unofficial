---
workflowStatus: 'completed'
totalSteps: 5
stepsCompleted: ['step-01-detect-mode', 'step-02-load-context', 'step-03-risk-and-testability', 'step-04-coverage-plan', 'step-05-generate-output']
lastStep: 'step-05-generate-output'
nextStep: ''
lastSaved: '2026-05-25'
---

# Test Design Progress

## Step 1: Mode Detection & Prerequisites

**Mode Selected:** System-Level

**Rationale:** PRD, architecture document, and sprint-status all available. System-level provides comprehensive coverage of the full v1 product scope before breaking down into per-epic test plans.

**Prerequisites Verified:**
- PRD: `_bmad-output/planning-artifacts/prds/prd-mealie-android-2026-05-21/prd.md` (20 FRs, 14 in v1 scope)
- Architecture: `_bmad-output/planning-artifacts/architecture.md` (complete, status: ready for implementation)
- Sprint status: `_bmad-output/implementation-artifacts/sprint-status.yaml` (exists)

## Step 2: Context Loading

**Artifacts Loaded:**
- PRD (20 FRs, 19 NFRs; 14 FRs in v1 scope)
- Architecture document (MVVM + Repository, Kotlin + Jetpack Compose, 7 modules)
- Epics (2 epics, 19 stories)
- Knowledge fragments: risk-governance, test-levels-framework, nfr-criteria, test-quality, adr-quality-readiness-checklist

**Stack Detection:**
- Platform: Native Android (Kotlin + Jetpack Compose)
- Testing stack: JUnit 5 + MockK + Turbine (unit), Room in-memory (integration), JUnit 4 + Compose ui-test-junit4 (androidTest), MockWebServer + JSON fixtures, TestListenableWorkerBuilder
- DI: Koin (not Hilt)

## Step 3: Testability & Risk Assessment

### Testability Concerns

| Concern | Impact | Recommendation |
| --- | --- | --- |
| Encrypted DataStore (datastore-tink alpha) | Cannot unit-test serializer in isolation without Keystore | Interface-based abstraction (`CredentialStore`) tested via fake; one instrumented smoke test on real device |
| ConnectivityMonitor flakiness | Probe-based approach (`GET /api/app/about`) introduces timing sensitivity | Inject fake `ConnectivityState` Flow in ViewModel/Repository tests; integration test uses MockWebServer with controlled delays |
| WorkManager timing | SyncWorker retry/backoff hard to assert deterministically | `TestListenableWorkerBuilder` for synchronous execution; verify output/Result only, not timing |
| No instrumented CI in v1 | Compose UI tests and device-dependent tests run locally only | Acceptable for solo dev; add emulator CI when contributors join |
| Shopping mode persistence race | DataStore write from background (timeout) vs. foreground (user toggle) | Atomic `updateData {}` in DataStore; test via concurrent coroutine scenario |

### Testability Assessment Summary

- Room-backed offline architecture is highly testable (in-memory DB, Flow assertions via Turbine)
- OkHttp Authenticator serial guarantee simplifies auth testing
- ConflictResolver as pure function (last-write-wins) is trivially unit-testable
- Clear model layer separation (Dto/Domain/Entity/Ui) means mappers are pure functions
- Koin modules testable via `checkModules()` utility

### Risk Assessment

| Risk ID | Category | Description | P | I | Score | Mitigation |
| --- | --- | --- | --- | --- | --- | --- |
| R-01 | SEC | datastore-tink alpha may have breaking changes before stable | 2 | 3 | **6** | Pin version; interface abstraction allows swap to stable crypto |
| R-02 | SEC | Encrypted DataStore unreadable after Android backup restore (Keystore key lost) | 2 | 3 | **6** | Detect decryption failure; auto-clear + re-auth prompt |
| R-03 | TECH | ConnectivityMonitor false-positive (probe succeeds but sync fails) | 2 | 2 | 4 | SyncWorker independently probes before sync; separate error state |
| R-04 | DATA | Conflict resolution drops valid edits when clocks are skewed | 2 | 2 | 4 | Document limitation; `updated_at` sourced from server response |
| R-05 | PERF | Large shopping list (500+ items) causes UI jank | 2 | 2 | 4 | LazyColumn already used; add benchmark test for 500-item list |
| R-06 | OPS | No instrumented CI - regressions caught late | 2 | 2 | 4 | Local instrumented pass required before merge; CI added when team grows |
| R-07 | TECH | Room auto-migration fails on v2 schema addition | 1 | 3 | 3 | v1 schema additive-only; manual migration test for v2 transition |
| R-08 | BUS | Mealie API changes break client silently | 1 | 2 | 2 | MockWebServer fixtures versioned; bump when Mealie version changes |

### NFR Planning

| NFR Category | Threshold | Evidence Source |
| --- | --- | --- |
| Security | Keystore encryption, no secrets in logcat | Instrumented test + Timber log assertion |
| Performance | Cold start < 3s, local reads < 100ms | Macrobenchmark (pre-release) |
| Reliability | Sync queue survives process death; drain < 30s | Integration test with process recreation |
| Maintainability | Line coverage >= 80% core, >= 70% feature | JaCoCo CI report |

## Step 4: Coverage Plan & Execution Strategy

### Coverage Matrix

#### P0 (Critical) - ~10 tests

| Test ID | Requirement | Test Level | Risk Link |
| --- | --- | --- | --- |
| P0-001 | Silent token refresh on launch (expired token) | Unit | R-01 |
| P0-002 | Credential fallback when refresh fails | Unit | R-01, R-02 |
| P0-003 | Encrypted DataStore read/write cycle | Instrumented | R-01, R-02 |
| P0-004 | Encrypted DataStore decryption failure triggers re-auth | Instrumented | R-02 |
| P0-005 | OkHttp Authenticator intercepts 401 and refreshes | Unit | - |
| P0-006 | Shopping list loads from Room (offline read) | Unit | - |
| P0-007 | Sync queue persists across process death | Integration | - |
| P0-008 | SyncWorker flushes queue on connectivity | Integration | R-03 |
| P0-009 | Conflict resolution (last-write-wins via updated_at) | Unit | R-04 |
| P0-010 | No secrets in logcat (release build) | Instrumented | - |

#### P1 (High) - ~20 tests

| Test ID | Requirement | Test Level | Risk Link |
| --- | --- | --- | --- |
| P1-001 | Server URL validation (scheme, trailing slash, IPv6) | Unit | - |
| P1-002 | HTTP security warning flow | Unit | - |
| P1-003 | Login with valid credentials | Unit | - |
| P1-004 | Login with invalid credentials shows error | Unit | - |
| P1-005 | Shopping list items display in correct order | Unit | - |
| P1-006 | Check/uncheck item (optimistic + sync) | Unit | - |
| P1-007 | Add item (optimistic + sync) | Unit | - |
| P1-008 | Delete item (optimistic + sync) | Unit | - |
| P1-009 | Offline indicator appears within 3s of connectivity loss | Unit | R-03 |
| P1-010 | Offline indicator disappears within 3s of restoration | Unit | R-03 |
| P1-011 | Sync status badge shows pending/failed/synced per item | Unit | - |
| P1-012 | SyncWorker respects Wi-Fi Only constraint | Unit | - |
| P1-013 | SyncWorker retry with exponential backoff | Unit | - |
| P1-014 | Concurrent token refresh safety (Mutex) | Unit | - |
| P1-015 | Room DAO: insert, query, update, delete shopping items | Integration | - |
| P1-016 | Repository maps Dto to Domain correctly | Unit | - |
| P1-017 | API contract: MockWebServer fixture matches expected schema | Unit | R-08 |
| P1-018 | Shopping mode persistence (DataStore) | Unit | - |
| P1-019 | Settings: update credentials flow | Unit | - |
| P1-020 | Settings: change server URL clears Local Store | Unit | - |

#### P2 (Medium) - ~15 tests

| Test ID | Requirement | Test Level | Risk Link |
| --- | --- | --- | --- |
| P2-001 | Large list performance (500+ items, no jank) | Benchmark | R-05 |
| P2-002 | Multiple lists - switch between lists | Unit | - |
| P2-003 | Sort preferences persistence per list | Unit | - |
| P2-004 | Network timeout handling (10s connect, 30s read) | Unit | - |
| P2-005 | Koin module graph validates (checkModules) | Unit | - |
| P2-006 | Navigation graph smoke test (all routes reachable) | Instrumented | - |
| P2-007 | Shopping mode auto-timeout | Unit | - |
| P2-008 | Sync idempotency (duplicate delivery no side effect) | Integration | - |
| P2-009 | Room TypeConverter Instant/Long round-trip | Unit | - |
| P2-010 | Accessibility: TalkBack content descriptions | Instrumented | - |
| P2-011 | Accessibility: contrast ratio (theme validation) | Unit | - |
| P2-012 | Error state UI (sync failure overlay) | Unit | - |
| P2-013 | Mid-sync network failure recovery | Integration | R-03 |
| P2-014 | 422 validation error logging (not surfaced to user) | Unit | - |
| P2-015 | Household info cached correctly | Unit | - |

#### P3 (Low) - ~3 tests

| Test ID | Requirement | Test Level | Risk Link |
| --- | --- | --- | --- |
| P3-001 | Cold start benchmark (< 3s target) | Benchmark | - |
| P3-002 | Room auto-migration additive schema change | Integration | R-07 |
| P3-003 | Bug report intent launches correctly | Instrumented | - |

### NFR Coverage and Evidence Plan

| NFR Category | Planned Validation | Tool/Level | Evidence Artifact |
| --- | --- | --- | --- |
| Security | P0-003, P0-004, P0-010 + Timber log assertion | Instrumented + Unit | Test results + log scan report |
| Performance | P3-001 cold start, P2-001 scroll benchmark | Macrobenchmark | Benchmark JSON output |
| Reliability | P0-007, P0-008, P2-008, P2-013 | Integration | Test results |
| Maintainability | JaCoCo coverage gates | CI | Coverage HTML/XML report |

### Execution Strategy

- **PR**: All unit + integration tests (~40 tests, < 5 min with Gradle parallel)
- **Pre-release**: Instrumented tests + benchmarks (run locally on device before tagging)

No nightly/weekly cadence needed - solo developer, small test suite, no expensive infrastructure.

### Resource Estimates

| Priority | Count | Effort Range |
| --- | --- | --- |
| P0 | ~10 | ~15-25 hours |
| P1 | ~20 | ~12-20 hours |
| P2 | ~15 | ~8-12 hours |
| P3 | ~3 | ~2-4 hours |
| **Total** | ~48 | **~35-56 hours (~1-1.5 weeks full-time)** |

### Quality Gates

| Gate | Threshold | Enforcement |
| --- | --- | --- |
| P0 pass rate | 100% | CI blocks merge |
| P1 pass rate | 100% | CI blocks merge |
| P2 pass rate | 100% | CI blocks merge |
| Line coverage (:core:*) | >= 80% | CI reports; enforced before first release |
| Line coverage (:feature:*) | >= 70% | CI reports; ViewModels are the coverage target |
| High-risk mitigations (R-01, R-02) | Complete before release | Instrumented test suite passes on 3+ device configs |
| ASR tests (ASR-1 through ASR-6) | All green | CI blocks merge |
| No secrets in logs | Verified | Automated test in CI |
| NFR validation evidence | Identified per category | Full PASS/CONCERNS/FAIL deferred to nfr-assess |
