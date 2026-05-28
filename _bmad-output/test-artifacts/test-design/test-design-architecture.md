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

# Test Design for Architecture: Mealie Android v1

**Purpose:** Architectural concerns, testability gaps, and NFR requirements for review by the development team. Serves as a contract on what must be addressed before test development begins.

**Date:** 2026-05-25
**Author:** Xexanos
**Status:** Architecture Review Pending
**Project:** mealie-android-unofficial
**PRD Reference:** `_bmad-output/planning-artifacts/prds/prd-mealie-android-2026-05-21/prd.md`
**Architecture Reference:** `_bmad-output/planning-artifacts/architecture.md`

---

## Executive Summary

**Scope:** Full v1 product - server setup, authentication, shopping list CRUD with offline support, connectivity awareness, app settings. Recipe browsing deferred to v2.

**Architecture:**

- Kotlin + Jetpack Compose, MVVM + Repository pattern
- 7 Gradle modules: `:app`, `:feature:auth`, `:feature:shopping`, `:feature:settings`, `:core:data`, `:core:network`, `:core:sync`, `:core:ui`
- Koin DI, Room (single source of truth), WorkManager (durable sync), OkHttp Authenticator (auth)
- `datastore-tink` with `AeadSerializer` for encrypted credential storage

**Risk Summary:**

- **Total risks**: 9
- **High-priority (score >= 6)**: 2 risks (R-01, R-02) - both in encrypted storage
- **Test effort**: ~53 tests (~1-1.5 weeks for 1 developer)

---

## Quick Guide

### BLOCKERS - Team Must Decide

**Pre-Implementation Critical Path** - these must be completed before test development for auth/security:

1. **R-02: Backup restore key loss** - Implement detection of Keystore decryption failure and auto-clear + re-auth flow (owner: Dev)
2. **ASR-1: CredentialStore abstraction** - Define `CredentialStore` interface so encrypted DataStore can be faked in unit tests (owner: Dev)

**What we need:** Complete these 2 items during auth implementation or security tests are blocked.

---

### HIGH PRIORITY - Team Should Validate

1. **R-01: datastore-tink alpha stability** - Pin to `1.3.0-alpha09`; verify no breaking changes before stable release. Dev should approve interface abstraction approach.
2. **R-03: ConnectivityMonitor false positives** - Validate that SyncWorker's independent probe is sufficient mitigation. Dev should approve the dual-probe pattern.
3. **R-04: Clock skew conflict resolution** - Confirm that sourcing `updated_at` from server response (not device clock) is acceptable for v1.

**What we need:** Review and approve (or suggest alternatives).

---

### INFO ONLY - Solutions Provided

1. **Test strategy**: Unit-heavy (JUnit 5 + MockK + Turbine), integration for Room/Sync, instrumented for security/UI
2. **Tooling**: JUnit 5, MockK, Turbine, Room in-memory, MockWebServer, TestListenableWorkerBuilder, Macrobenchmark
3. **Execution**: All unit/integration tests in CI on every PR (< 5 min); instrumented tests pre-release locally
4. **Coverage**: ~53 test scenarios organized by feature area with risk links
5. **Quality gates**: 100% pass rate; >= 80% line coverage on `:core:*`

**What we need:** Acknowledge only.

---

## Risk Assessment

**Total risks identified**: 9 (2 high-priority >= 6, 5 medium, 2 low)

### High-Priority Risks (Score >= 6)

| Risk ID | Category | Description | P | I | Score | Mitigation | Owner | Timeline |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **R-01** | **SEC** | `datastore-tink` alpha may have breaking API changes before stable | 2 | 3 | **6** | Pin version; `CredentialStore` interface abstraction allows swap | Dev | Before auth impl |
| **R-02** | **SEC** | Encrypted DataStore unreadable after Android backup restore (Keystore key lost) | 2 | 3 | **6** | Detect decryption failure; auto-clear credentials + trigger re-auth | Dev | Before auth impl |

### Medium-Priority Risks (Score 3-5)

| Risk ID | Category | Description | P | I | Score | Mitigation | Owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| R-03 | TECH | ConnectivityMonitor false-positive (probe succeeds but sync fails) | 2 | 2 | 4 | SyncWorker independently probes before sync | Dev |
| R-04 | DATA | Conflict resolution drops edits when device clocks are skewed | 2 | 2 | 4 | Source `updated_at` from server response only | Dev |
| R-05 | PERF | Large shopping list (500+ items) causes UI jank | 2 | 2 | 4 | LazyColumn + benchmark test to catch regressions | Dev |
| R-06 | OPS | No instrumented CI - regressions caught late | 2 | 2 | 4 | Local instrumented pass before merge; CI added when team grows | Dev |
| R-07 | TECH | Room auto-migration fails on v2 schema addition | 1 | 3 | 3 | Manual migration test added for v2 transition | Dev |

### Low-Priority Risks (Score 1-2)

| Risk ID | Category | Description | P | I | Score | Action |
| --- | --- | --- | --- | --- | --- | --- |
| R-08 | BUS | Mealie API changes break client silently | 1 | 2 | 2 | MockWebServer fixtures versioned; update on Mealie version bump |
| R-09 | OPS | demo.mealie.io unavailable causes silent E2E coverage loss | 2 | 1 | 2 | Nightly cron hard-fails if unreachable; PR pipeline skips with annotation |

---

### NFR Testability Requirements

| NFR Category | Threshold | Current Design Support | Gap / Decision Needed | Planned Evidence |
| --- | --- | --- | --- | --- |
| Security | Keystore encryption; no secrets in logcat | Supported (datastore-tink + redactHeader) | `CredentialStore` interface needed for testability | Instrumented test + log scan |
| Performance | Cold start < 3s; local reads < 100ms | Supported (Room + LazyColumn) | None | Macrobenchmark JSON |
| Reliability | Sync survives process death; drain < 30s | Supported (WorkManager + Room queue) | None | Integration test results |
| Maintainability | >= 80% core coverage, >= 70% feature coverage | Supported (JaCoCo) | None | CI coverage report |

**Unknown thresholds:** None - all NFR thresholds specified in PRD.

---

### Testability Concerns and Architectural Gaps

**ACTIONABLE CONCERNS**

#### Blockers to Fast Feedback

| Concern | Impact | What Architecture Must Provide | Owner | Timeline |
| --- | --- | --- | --- | --- |
| **Encrypted DataStore not unit-testable** | Cannot test token/credential flows without real Keystore | `CredentialStore` interface with fake implementation for tests | Dev | Auth story implementation |

#### Architectural Improvements Needed

1. **CredentialStore interface extraction**
   - **Current problem**: `datastore-tink` `AeadSerializer` requires Android Keystore - unavailable in JVM unit tests
   - **Required change**: Define `CredentialStore` / `TokenStore` interfaces in `:core:network`; implementation wraps DataStore; tests use in-memory fake
   - **Impact if not fixed**: All auth-related unit tests require instrumented execution (10x slower, no CI in v1)
   - **Owner**: Dev
   - **Timeline**: Auth feature implementation (Epic 1, Stories 1.1-1.4)

---

### Testability Assessment Summary

#### What Works Well

- Room-backed offline architecture: in-memory DB for fast integration tests with Flow assertions via Turbine
- OkHttp Authenticator serial guarantee: simplifies concurrent refresh testing (no race conditions in test)
- ConflictResolver as pure function: trivially unit-testable with deterministic inputs
- Clear 4-layer model separation (Dto/Domain/Entity/Ui): all mappers are pure functions
- Koin `checkModules()` validates full DI graph without running app
- MockWebServer JSON fixtures: contract tests verify API compatibility without live server

#### Accepted Trade-offs

- **No instrumented CI in v1** - acceptable for solo developer; local pass required before merge
- **No screenshot tests for v1** - acceptable given small UI surface; add with contributors
- **datastore-tink alpha dependency** - accepted risk; interface abstraction provides escape hatch
- **E2E Live tests skip gracefully in PR pipeline** - when demo.mealie.io is unreachable, PR pipeline skips live E2E with a warning annotation (not a failure). Nightly cron job hard-fails to prevent silent erosion.

---

### Risk Mitigation Plans (High-Priority Risks >= 6)

#### R-01: datastore-tink Alpha Stability (Score: 6)

**Mitigation Strategy:**

1. Pin to `1.3.0-alpha09` in version catalog
2. Extract `CredentialStore` interface - implementation detail hidden behind abstraction
3. Monitor Jetpack release notes; upgrade path is drop-in replacement of the serializer

**Owner:** Dev
**Timeline:** Auth story implementation
**Status:** Planned
**Verification:** Unit tests pass with fake; one instrumented smoke test validates real encryption

#### R-02: Backup Restore Key Loss (Score: 6)

**Mitigation Strategy:**

1. Wrap DataStore reads in try/catch for `GeneralSecurityException` / `IOException`
2. On decryption failure: clear corrupted DataStore file, emit `AuthError` to ViewModel
3. ViewModel navigates to re-auth screen; user re-enters credentials
4. Document behavior: backup restore requires one-time re-authentication

**Owner:** Dev
**Timeline:** Auth story implementation
**Status:** Planned
**Verification:** Instrumented test simulates corrupted DataStore file; verifies re-auth flow triggers

---

### Assumptions and Dependencies

#### Assumptions

1. Solo developer workflow - no parallel test development; one person writes both code and tests
2. Mealie API remains backward-compatible within v3.18.x for shopping list endpoints
3. `datastore-tink` alpha API surface remains stable through v1 development (3-4 months)
4. demo.mealie.io remains publicly accessible with a demo account for E2E testing

#### Dependencies

1. `CredentialStore` interface - required before auth unit tests can be written
2. MockWebServer JSON fixtures derived from Mealie v3.18.0 OpenAPI spec - required before API contract tests
3. demo.mealie.io demo account credentials - required for live E2E tests (stored as CI secrets)

#### Risks to Plan

- **Risk**: Mealie ships breaking API change mid-development
  - **Impact**: MockWebServer fixtures and DTO classes need update
  - **Contingency**: Fixtures are small and isolated; update is < 1 hour of work
- **Risk**: demo.mealie.io goes permanently offline or removes demo accounts
  - **Impact**: Live E2E tests cannot run; coverage limited to WireMock-only
  - **Contingency**: Self-hosted Mealie instance as fallback (Docker); nightly cron detects within 24h

---

## Dual-Backend E2E Architecture

### Overview

E2E tests run in two modes against two different backends:

| Mode | Backend | Purpose | Deterministic | CI Trigger |
| --- | --- | --- | --- | --- |
| **WireMock** | Local WireMock (host:8080) | Fast, deterministic, full scenario control | Yes | Every PR |
| **Live** | demo.mealie.io | Real API validation, drift detection | No | Every PR (skip if unreachable) + Nightly hard-fail |

Both modes exercise the same UI-driven test flows. The app binary is identical - no test-only build variants or DI overrides.

### Configuration Mechanism

Backend selection via **instrumentation arguments** passed at test launch:

```
-Pandroid.testInstrumentationRunnerArguments.e2eBackend=wiremock|live
-Pandroid.testInstrumentationRunnerArguments.e2eBaseUrl=http://10.0.2.2:8080|https://demo.mealie.io
-Pandroid.testInstrumentationRunnerArguments.e2eUsername=<demo-user>
-Pandroid.testInstrumentationRunnerArguments.e2ePassword=<demo-password>
```

A shared base class reads these arguments and exposes them to test methods:

```kotlin
abstract class E2ETestBase {
    protected val backend: E2EBackend by lazy {
        val args = InstrumentationRegistry.getArguments()
        val mode = args.getString("e2eBackend", "wiremock")
        E2EBackend.from(mode, args)
    }

    @Before
    fun assumeBackendReachable() {
        if (backend is E2EBackend.Live) {
            val reachable = runBlocking { backend.healthCheck() }
            Assume.assumeTrue("Live backend unreachable", reachable)
        }
    }
}
```

### Test Categorization

Not all E2E tests can run in both modes:

| Test ID | WireMock | Live | Reason |
| --- | --- | --- | --- |
| E2E-001 | Yes | Yes | Happy path - works against any backend |
| E2E-002 | Yes | Yes | Invalid credentials - universal behavior |
| E2E-003 | Yes | Yes* | Check/uncheck - requires test item cleanup on live |
| E2E-004 | Yes | Yes* | Add item - requires cleanup on live |
| E2E-005 | Yes | **No** | Offline test - requires stopping the server |

*Live-mode tests that mutate data must clean up after themselves (delete created items in `@After`).

### CI/CD Execution Strategy

#### PR Pipeline (on every push)

```
1. Run WireMock E2E tests (always, deterministic)
2. Health-check: curl https://demo.mealie.io/api/app/about
3. If reachable: run Live E2E tests
4. If unreachable: skip with annotation warning ("Live E2E skipped: demo.mealie.io unreachable")
```

PR result is green if WireMock tests pass, regardless of live skip. The skip annotation ensures visibility.

#### Nightly Cron Job

```yaml
schedule:
  - cron: '23 2 * * *'   # 02:23 UTC nightly

steps:
  - Guard: check for commits in last 24h (git log --since="24 hours ago")
    - No commits: skip entire job (exit 0, no failure)
    - Commits found: continue

  - Health-check: curl https://demo.mealie.io/api/app/about
    - Unreachable: HARD FAIL (not skip) - prevents silent erosion
    - Reachable: continue

  - Run Live E2E tests against demo.mealie.io
    - Failure: HARD FAIL - real regression detected
```

The nightly cron is the **erosion guard**: it ensures that live E2E coverage is validated at least daily during active development. If demo.mealie.io is persistently down, the developer is notified within 24 hours via a failed nightly run.

### Credentials Management

- **WireMock mode**: Hardcoded test credentials (`test@example.com` / `password123`) - stubs accept anything
- **Live mode**: Real demo.mealie.io credentials stored as **CI secrets** (`E2E_LIVE_USERNAME`, `E2E_LIVE_PASSWORD`)
- Local development: `.env.e2e.local` file (gitignored) for running live tests manually

### Data Isolation on Live Backend

Tests that create or modify data on demo.mealie.io must be self-cleaning:

1. **Test setup**: Create test-specific items with a recognizable prefix (e.g., `[E2E-<timestamp>]`)
2. **Test teardown** (`@After`): Delete all items with the test prefix via API
3. **Fallback**: If teardown fails (network issue), prefixed items are identifiable for manual cleanup
4. **Read-only tests** (E2E-001, E2E-002): No cleanup needed

### Timeout Configuration

| Mode | Connect Timeout | Read Timeout | UI waitUntil |
| --- | --- | --- | --- |
| WireMock | 5s | 5s | 5s |
| Live | 10s | 30s | 15s |

Live mode uses more generous timeouts to account for network latency and server load.

---

**End of Architecture Document**

**Next Steps:**

1. Review Quick Guide - prioritize the `CredentialStore` interface extraction
2. Validate assumptions about `datastore-tink` stability
3. Refer to companion QA doc (`test-design-qa.md`) for test scenarios and implementation details
