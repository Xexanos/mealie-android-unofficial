---
stepsCompleted: ['step-01-preflight-and-context', 'step-02-generation-mode', 'step-03-test-strategy', 'step-04-generate-tests']
lastStep: 'step-04-generate-tests'
lastSaved: '2026-05-26'
storyId: '1.1, 1.2, 1.3'
storyKey: 'epic-1-stories-1-through-3'
storyFile:
  - '_bmad-output/implementation-artifacts/1-1-multi-module-build-infrastructure-and-ci-cd.md'
  - '_bmad-output/implementation-artifacts/1-2-app-theme-navigation-shell-and-application-class.md'
  - '_bmad-output/implementation-artifacts/1-3-server-url-entry-and-connection-validation.md'
atddChecklistPath: '_bmad-output/test-artifacts/atdd-checklist-epic-1-stories-1-through-3.md'
generatedTestFiles:
  - 'app/src/test/java/dev/xexanos/mealie/di/KoinModuleCheckTest.kt'
  - 'core/ui/src/test/java/dev/xexanos/mealie/core/ui/theme/ColorSchemeTest.kt'
  - 'core/data/src/test/java/dev/xexanos/mealie/core/data/repository/AuthRepositoryImplTest.kt'
inputDocuments:
  - '_bmad-output/test-artifacts/test-design/test-design-architecture.md'
  - '_bmad-output/test-artifacts/test-design/test-design-qa.md'
  - '_bmad-output/implementation-artifacts/1-1-multi-module-build-infrastructure-and-ci-cd.md'
  - '_bmad-output/implementation-artifacts/1-2-app-theme-navigation-shell-and-application-class.md'
  - '_bmad-output/implementation-artifacts/1-3-server-url-entry-and-connection-validation.md'
---

# ATDD Checklist: Epic 1 Stories 1.1-1.3

## Stack & Mode

- **Detected stack**: backend (Android/Kotlin, Gradle multi-module)
- **Generation mode**: AI Generation (no browser recording)
- **Test framework**: JUnit 5 + MockK + Turbine + MockWebServer
- **Integration test differentiation**: `@Tag("integration")` annotation

---

## Test Strategy Summary

| Story | AC Count | New Tests | Level | Priority |
|-------|----------|-----------|-------|----------|
| 1.1 - Build Infrastructure | 8 | 5 (Koin verify) | Integration | P0 |
| 1.2 - Theme, Nav, DI | 5 | 6 (Color scheme) | Unit | P1 |
| 1.3 - Server URL | 8 | 7 (MockWebServer) | Integration | P0 |

**Total new acceptance tests**: 18

---

## Generated Test Files

### 1. KoinModuleCheckTest (Story 1.1/1.2)

**File**: `app/src/test/java/dev/xexanos/mealie/di/KoinModuleCheckTest.kt`
**Tag**: `@Tag("integration")`
**Covers**: AC1.1-1/2 (modules resolve), AC1.2-1 (Koin init), AC1.2-5 (feature:auth wired)

| Test | AC | Status |
|------|----|--------|
| `when network module verified then all dependencies resolve` | 1.1-AC1 | PASS |
| `when ui module verified then all dependencies resolve` | 1.2-AC4 | PASS |
| `when sync module verified then all dependencies resolve` | 1.1-AC1 | PASS |
| `when data module verified then all dependencies resolve` | 1.1-AC2 | PASS |
| `when auth feature module verified then all dependencies resolve` | 1.2-AC1,5 | PASS |

### 2. ColorSchemeTest (Story 1.2)

**File**: `core/ui/src/test/java/dev/xexanos/mealie/core/ui/theme/ColorSchemeTest.kt`
**Tag**: none (unit test)
**Covers**: AC1.2-2/3 (color scheme from #E58325 seed)

| Test | AC | Status |
|------|----|--------|
| `when light scheme created then primary matches E58325 tonal palette` | 1.2-AC3 | PASS |
| `when dark scheme created then primary matches E58325 tonal palette` | 1.2-AC3 | PASS |
| `when light scheme created then not default Material 3 purple` | 1.2-AC3 | PASS |
| `when dark scheme created then not default Material 3 purple` | 1.2-AC3 | PASS |
| `when light scheme created then error color is standard M3 red` | 1.2-AC3 | PASS |
| `when light scheme created then on-primary is white for contrast` | 1.2-AC3 | PASS |

### 3. AuthRepositoryImplTest (Story 1.3)

**File**: `core/data/src/test/java/dev/xexanos/mealie/core/data/repository/AuthRepositoryImplTest.kt`
**Tag**: `@Tag("integration")`
**Covers**: AC1.3-5/6/7 (probe success/failure via real HTTP)

| Test | AC | Status |
|------|----|--------|
| `when probe receives valid Mealie response then returns Success` | 1.3-AC6 | PASS |
| `when probe receives version without 3 prefix then returns NotMealieServer` | 1.3-AC7 | PASS |
| `when probe receives null version then returns NotMealieServer` | 1.3-AC7 | PASS |
| `when probe receives non-JSON response then returns NotMealieServer` | 1.3-AC7 | PASS |
| `when probe receives 404 then returns NotMealieServer` | 1.3-AC7 | PASS |
| `when probe cannot connect then returns NetworkError` | 1.3-AC7 | PASS |
| `when probe receives valid response then GET request was sent` | 1.3-AC5 | PASS |

---

## Dependency Changes

### Version Catalog (`gradle/libs.versions.toml`)

Added:
- `koin-test` - Koin test utilities (verify, checkModules)
- `koin-test-junit5` - JUnit 5 extension for Koin
- `okhttp-mockwebserver` - MockWebServer for HTTP integration tests

### Build Files

- `app/build.gradle.kts`: added testImplementation for koin-test, koin-test-junit5, okhttp, kotlinx-serialization-json, junit-jupiter, kotlin-test; added useJUnitPlatform()
- `core/data/build.gradle.kts`: added testImplementation for okhttp-mockwebserver, kotlin-test; added testRuntimeOnly for junit-platform-launcher

---

## Coverage Gaps (Not Covered by These Tests)

| AC | Reason | Recommendation |
|----|--------|----------------|
| 1.2-AC2 (Dynamic Color API 31+) | Requires Robolectric or instrumented test to mock Build.VERSION | Add instrumented test in `app/src/androidTest/` |
| 1.3-AC1 (ServerUrlScreen displayed) | Compose UI test requires androidTest | Add Compose test rule in future |
| 1.3-AC8 (URL persisted to DataStore) | Requires Android context for real DataStore | Covered at ViewModel level by existing tests |

---

## Execution

```bash
# Run all tests (unit + integration)
./gradlew test

# Run specific module
./gradlew :core:data:test
./gradlew :app:test
./gradlew :core:ui:test
```

> **Note**: Integration tests use `@Tag("integration")` for documentation purposes.
> JUnit 5 tag filtering can be enabled per-module by adding
> `useJUnitPlatform { includeTags("integration") }` to the relevant
> `tasks.withType<Test>` block when selective execution is needed.

---

## Handoff Notes

All 18 new tests pass. These acceptance tests verify the implemented stories' ACs at unit and integration levels. The `@Tag("integration")` convention is established for tests using real components (MockWebServer, Koin graph resolution) vs mocked unit tests.
