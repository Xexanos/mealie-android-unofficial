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

### Coverage Matrix (~53 tests, organized by feature area)

- **AUTH-***: 9 tests (token refresh, credential fallback, encryption, Authenticator, login)
- **SETUP-***: 2 tests (URL validation, HTTP warning)
- **SHOP-***: 11 tests (offline read, CRUD, modes, sort, performance)
- **SYNC-***: 8 tests (queue persistence, flush, conflict resolution, retry, idempotency)
- **CONN-***: 2 tests (offline indicator timing)
- **SET-***: 2 tests (credential update, server URL change)
- **DATA-***: 9 tests (Room DAO, mappers, API contract, timeouts, Koin, TypeConverter)
- **APP-***: 5 tests (navigation, accessibility, benchmark, intent)
- **E2E-***: 5 tests (setup flow, shopping list, offline - WireMock black-box)

See `test-design-qa.md` for full test scenario tables.

### NFR Coverage and Evidence Plan

| NFR Category | Planned Validation | Tool/Level | Evidence Artifact |
| --- | --- | --- | --- |
| Security | AUTH-003, AUTH-004, AUTH-007 + Timber log assertion | Instrumented + Unit | Test results + log scan report |
| Performance | APP-004 cold start, SHOP-010 scroll benchmark | Macrobenchmark | Benchmark JSON output |
| Reliability | SYNC-001, SYNC-002, SYNC-006, SYNC-007 | Integration | Test results |
| Maintainability | JaCoCo coverage gates | CI | Coverage HTML/XML report |

### Execution Strategy

- **PR**: All unit + integration tests (~40 tests, < 5 min with Gradle parallel)
- **Pre-release**: Instrumented tests + benchmarks (run locally on device before tagging)

No nightly/weekly cadence needed - solo developer, small test suite, no expensive infrastructure.

### Resource Estimates

| Area | Count | Effort Range |
| --- | --- | --- |
| Authentication & Security | 9 | ~10-16 hours |
| Shopping List | 11 | ~6-10 hours |
| Sync & Offline | 8 | ~8-14 hours |
| Data Layer & Infrastructure | 9 | ~5-8 hours |
| Setup + Connectivity + Settings + App | 9 | ~6-10 hours |
| E2E (WireMock black-box) | 5 | ~6-10 hours |
| **Total** | ~53 | **~41-68 hours (~1-2 weeks full-time)** |

### Quality Gates

| Gate | Threshold | Enforcement |
| --- | --- | --- |
| All tests pass | 100% | CI blocks merge |
| Line coverage (:core:*) | >= 80% | CI reports; enforced before first release |
| Line coverage (:feature:*) | >= 70% | CI reports; ViewModels are the coverage target |
| High-risk mitigations (R-01, R-02) | Complete before release | Instrumented test suite passes on 3+ device configs |
| No secrets in logs | Verified | Automated test in CI |
| NFR validation evidence | Identified per category | Full PASS/CONCERNS/FAIL deferred to nfr-assess |
