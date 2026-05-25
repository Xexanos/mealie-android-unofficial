# Story 1.2: App Theme, Navigation Shell, and Application Class

Status: in-progress

## Story

As a developer,
I want a Material 3 themed application with a typed navigation container and Koin DI initialized,
so that all subsequent feature stories can wire into a consistent foundation.

## Acceptance Criteria

1. **Given** the project builds with `:feature:auth` scaffolded
   **When** the app launches
   **Then** the `MealieApplication` class initializes Koin with all current module declarations
   **And** no crash occurs on cold start

2. **Given** the device is running API 31+
   **When** the app renders any screen
   **Then** Dynamic Color is applied from the system wallpaper seed

3. **Given** the device is running API 26-30
   **When** the app renders any screen
   **Then** the static Material 3 color scheme seeded from `#E58325` is applied

4. **Given** the `NavHost` is initialized in `MainActivity`
   **When** any composable requests navigation via `NavigationManager`
   **Then** the `SharedFlow<NavigationCommand>` emits and the `NavHost` responds correctly

5. **Given** `:feature:auth` module is scaffolded
   **When** the Gradle sync completes
   **Then** `:app` declares a dependency on `:feature:auth` and the build succeeds

## Tasks / Subtasks

- [x] Task 1: Scaffold `:feature:auth` module (AC: 1, 5)
  - [x] Create `feature/auth/build.gradle.kts` - depends on `:core:data`, `:core:network`, `:core:ui`; see Dev Notes for exact plugin list
  - [x] Create `feature/auth/src/main/AndroidManifest.xml`
  - [x] Create `feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/di/AuthFeatureModule.kt` - empty `val authFeatureModule = module { }`
  - [x] Create `feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/navigation/AuthNavGraph.kt` - `NavGraphBuilder.authGraph()` extension with placeholder composable (see Dev Notes)
  - [x] Add `include(":feature:auth")` to `settings.gradle.kts`
  - [x] Add `implementation(project(":feature:auth"))` to `app/build.gradle.kts`

- [x] Task 2: Create `NavigationManager` in `:core:ui` (AC: 4)
  - [x] Create `core/ui/src/main/java/dev/xexanos/mealie/core/ui/navigation/NavigationManager.kt`
  - [x] Create `core/ui/src/main/java/dev/xexanos/mealie/core/ui/di/UiModule.kt` - `val uiModule = module { single { NavigationManager() } }`
  - [x] Add `testImplementation(libs.turbine)` and `testImplementation(libs.kotlinx.coroutines.test)` to `core/ui/build.gradle.kts`
  - [x] Create `core/ui/src/test/java/dev/xexanos/mealie/core/ui/testutil/MainDispatcherExtension.kt`
  - [x] Create `core/ui/src/test/java/dev/xexanos/mealie/core/ui/navigation/NavigationManagerTest.kt`

- [x] Task 3: Create `MealieTheme` in `:core:ui` (AC: 2, 3)
  - [x] Create `core/ui/src/main/java/dev/xexanos/mealie/core/ui/theme/Color.kt` - `LightColorScheme` and `DarkColorScheme` from seed `#E58325`; see Dev Notes for generation approach
  - [x] Create `core/ui/src/main/java/dev/xexanos/mealie/core/ui/theme/Type.kt` - default M3 `Typography()` with no overrides
  - [x] Create `core/ui/src/main/java/dev/xexanos/mealie/core/ui/theme/MealieTheme.kt` - with Dynamic Color gate for API 31+

- [x] Task 4: Create empty Koin modules for existing `:core:*` modules (AC: 1)
  - [x] Create `core/network/src/main/java/dev/xexanos/mealie/core/network/di/NetworkModule.kt` - `val networkModule = module { }`
  - [x] Create `core/data/src/main/java/dev/xexanos/mealie/core/data/di/DataModule.kt` - `val dataModule = module { }`
  - [x] Create `core/sync/src/main/java/dev/xexanos/mealie/core/sync/di/SyncModule.kt` - `val syncModule = module { }`

- [x] Task 5: Update `MealieApplication` with Koin + Timber (AC: 1)
  - [x] Change `debugImplementation(libs.timber)` to `implementation(libs.timber)` in `app/build.gradle.kts`
  - [x] Replace stub `MealieApplication.kt` with full implementation (see Dev Notes for exact code)
  - [x] Delete `core/ui/src/main/java/dev/xexanos/mealie/core/ui/UiStub.kt` (replaced by real files)

- [x] Task 6: Set up NavHost in `MainActivity` (AC: 4)
  - [x] Create `app/src/main/java/dev/xexanos/mealie/navigation/AppNavGraph.kt` (see Dev Notes)
  - [x] Replace stub `MainActivity.kt` with implementation that calls `MealieTheme { AppNavGraph(rememberNavController()) }`

- [x] Task 7: Verify build and tests pass
  - [x] `./gradlew assembleDebug` - BUILD SUCCESSFUL
  - [x] `./gradlew :core:ui:test` - NavigationManagerTest passes
  - [x] `./gradlew ktlintCheck detekt lint` - all pass

### Review Findings

- [x] [Review][Patch] Missing XML theme causes white flash on cold start [app/src/main/AndroidManifest.xml:4]
- [x] [Review][Patch] Missing `enableEdgeToEdge()` in MainActivity [app/src/main/java/dev/xexanos/mealie/MainActivity.kt:11]
- [x] [Review][Patch] No `Surface` composable wrapping NavHost content [app/src/main/java/dev/xexanos/mealie/MainActivity.kt:13]
- [x] [Review][Defer] `applicationScope` not registered in Koin DI [app/src/main/java/dev/xexanos/mealie/MealieApplication.kt] - deferred, no consumer in current story
- [x] [Review][Defer] `core:ui` no explicit coroutines main dependency [core/ui/build.gradle.kts] - deferred, works via transitive through koin.android

## Dev Notes

### CRITICAL: No `kotlin.android` Plugin

**Never add `alias(libs.plugins.kotlin.android)` to any module.** AGP 9.0+ has built-in Kotlin. Adding this plugin causes a fatal error:
```
The kotlin-android plugin cannot be applied to a project that uses AGP 9.0+
```
Use only `kotlin.compose` (for Compose), `kotlin.serialization` (for @Serializable routes), `android.library`/`android.application`.

### CRITICAL: Timber Must Be `implementation`, Not `debugImplementation`

`app/build.gradle.kts` from Story 1.1 declares `debugImplementation(libs.timber)`. **Change this to `implementation(libs.timber)`.** `MealieApplication.kt` is in `src/main` (compiles for both variants). With `debugImplementation`, `Timber` is absent from the release classpath and compilation fails. Using `implementation` is safe: R8 strips dead code in release, and tree planting is gated on `BuildConfig.DEBUG`.

### `NavigationManager.kt` - Complete Implementation

```kotlin
// core/ui/src/main/java/dev/xexanos/mealie/core/ui/navigation/NavigationManager.kt
package dev.xexanos.mealie.core.ui.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface NavigationCommand

class NavigationManager {
    private val _commands = MutableSharedFlow<NavigationCommand>(
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val commands: SharedFlow<NavigationCommand> = _commands.asSharedFlow()

    fun navigate(command: NavigationCommand) {
        _commands.tryEmit(command)
    }
}
```

`Int.MAX_VALUE` buffer with `DROP_OLDEST` ensures commands are never silently dropped during recomposition. `navigate()` is non-suspend; callers do not need a coroutine scope.

`NavigationCommand` is an empty sealed interface in Story 1.2. Feature stories add their command objects as nested objects/data classes. Do not add any cases here.

### `UiModule.kt`

```kotlin
// core/ui/src/main/java/dev/xexanos/mealie/core/ui/di/UiModule.kt
package dev.xexanos.mealie.core.ui.di

import dev.xexanos.mealie.core.ui.navigation.NavigationManager
import org.koin.dsl.module

val uiModule = module {
    single { NavigationManager() }
}
```

### `MealieTheme.kt` - Complete Implementation

```kotlin
// core/ui/src/main/java/dev/xexanos/mealie/core/ui/theme/MealieTheme.kt
package dev.xexanos.mealie.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun MealieTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

`Build.VERSION_CODES.S` = API 31.

### `Color.kt` - Static Fallback Scheme

Generate `LightColorScheme` and `DarkColorScheme` using seed `#E58325` at [Material Theme Builder](https://material-foundation.github.io/material-theme-builder/). Select "Export → Jetpack Compose" and copy the `lightColorScheme(...)` and `darkColorScheme(...)` declarations into `Color.kt`.

**Critical rule:** Never use `Color(0xFFE58325)` or any raw hex color in production composables. Always reference `MaterialTheme.colorScheme.primary` (and other scheme roles). The design token rule applies to `:core:ui` component code as well - use `MaterialTheme.colorScheme.*` and `MaterialTheme.typography.*`, never raw `dp`/`sp` literals for semantic values.

### `Type.kt`

```kotlin
// core/ui/src/main/java/dev/xexanos/mealie/core/ui/theme/Type.kt
package dev.xexanos.mealie.core.ui.theme

import androidx.compose.material3.Typography

val Typography = Typography()
```

Default M3 scale, no custom fonts. Roboto is the system default on Android. Override specific tokens only when UX spec explicitly requires (e.g. Shopping mode item text in Story 2.2).

### `AuthNavGraph.kt` - Minimal Scaffold

```kotlin
// feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/navigation/AuthNavGraph.kt
package dev.xexanos.mealie.feature.auth.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable

@Serializable object AuthGraph
@Serializable object AuthPlaceholder

fun NavGraphBuilder.authGraph(navController: NavController) {
    navigation<AuthGraph>(startDestination = AuthPlaceholder) {
        composable<AuthPlaceholder> {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
```

`AuthPlaceholder` is replaced by `ServerUrlScreen` in Story 1.3. `AuthGraph` is the nested nav graph object and remains as the entry point for the auth flow throughout Epic 1.

### `AppNavGraph.kt` - Complete Implementation

```kotlin
// app/src/main/java/dev/xexanos/mealie/navigation/AppNavGraph.kt
package dev.xexanos.mealie.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import dev.xexanos.mealie.core.ui.navigation.NavigationManager
import dev.xexanos.mealie.feature.auth.navigation.AuthGraph
import dev.xexanos.mealie.feature.auth.navigation.authGraph
import org.koin.androidx.compose.koinInject

@Composable
fun AppNavGraph(navController: NavHostController) {
    val navigationManager: NavigationManager = koinInject()

    LaunchedEffect(Unit) {
        navigationManager.commands.collect {
            // Navigation routing implemented in Stories 1.3+
        }
    }

    NavHost(
        navController = navController,
        startDestination = AuthGraph
    ) {
        authGraph(navController)
        // shoppingGraph(navController) added in Story 2.1
    }
}
```

`koinInject()` is from `koin-androidx-compose` (already in `app/build.gradle.kts`). `startDestination = AuthGraph` is the type-safe nav graph object.

### `MealieApplication.kt` - Full Replacement

```kotlin
// app/src/main/java/dev/xexanos/mealie/MealieApplication.kt
package dev.xexanos.mealie

import android.app.Application
import dev.xexanos.mealie.core.data.di.dataModule
import dev.xexanos.mealie.core.network.di.networkModule
import dev.xexanos.mealie.core.sync.di.syncModule
import dev.xexanos.mealie.core.ui.di.uiModule
import dev.xexanos.mealie.feature.auth.di.authFeatureModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class MealieApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        startKoin {
            androidContext(this@MealieApplication)
            modules(
                networkModule,
                dataModule,
                syncModule,
                uiModule,
                authFeatureModule
            )
        }
    }

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }
}
```

`applicationScope` is declared now for Story 2.9 (GAP-2 resolution: ConnectivityMonitor initial probe). Do not use it yet - just declare it.

### `MainActivity.kt` - Full Replacement

```kotlin
// app/src/main/java/dev/xexanos/mealie/MainActivity.kt
package dev.xexanos.mealie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import dev.xexanos.mealie.core.ui.theme.MealieTheme
import dev.xexanos.mealie.navigation.AppNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MealieTheme {
                AppNavGraph(navController = rememberNavController())
            }
        }
    }
}
```

### `feature/auth/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.xexanos.mealie.feature.auth"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> { useJUnitPlatform() }
```

`kotlin.serialization` plugin is required for `@Serializable` route objects (`AuthGraph`, `AuthPlaceholder`). **No `kotlin.android` plugin** - AGP 9.0+ built-in Kotlin only.

### `core/ui/build.gradle.kts` - Add Test Dependencies

Add to existing `testImplementation` block:
```kotlin
testImplementation(libs.turbine)
testImplementation(libs.kotlinx.coroutines.test)
```

### Empty Koin Modules for Existing `:core:*` Modules

```kotlin
// Pattern - same for networkModule, dataModule, syncModule
val networkModule = module { }
val dataModule = module { }
val syncModule = module { }
```

These will grow in later stories. Declare now so `MealieApplication` can import and start them.

### NavigationManager Unit Test

```kotlin
// core/ui/src/test/java/dev/xexanos/mealie/core/ui/testutil/MainDispatcherExtension.kt
package dev.xexanos.mealie.core.ui.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class MainDispatcherExtension : BeforeEachCallback, AfterEachCallback {
    private val testDispatcher = UnconfinedTestDispatcher()
    override fun beforeEach(context: ExtensionContext?) { Dispatchers.setMain(testDispatcher) }
    override fun afterEach(context: ExtensionContext?) { Dispatchers.resetMain() }
}
```

```kotlin
// core/ui/src/test/java/dev/xexanos/mealie/core/ui/navigation/NavigationManagerTest.kt
package dev.xexanos.mealie.core.ui.navigation

import app.cash.turbine.test
import dev.xexanos.mealie.core.ui.testutil.MainDispatcherExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

@ExtendWith(MainDispatcherExtension::class)
class NavigationManagerTest {

    private val manager = NavigationManager()
    private object TestCommand : NavigationCommand

    @Test
    fun `navigate emits command to collector`() = runTest {
        manager.commands.test {
            manager.navigate(TestCommand)
            assertEquals(TestCommand, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple commands emitted in order`() = runTest {
        val second = object : NavigationCommand {}
        manager.commands.test {
            manager.navigate(TestCommand)
            manager.navigate(second)
            assertEquals(TestCommand, awaitItem())
            assertEquals(second, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

`@Test` from `org.junit.jupiter.api.Test` (JUnit 5). `runTest` from `kotlinx.coroutines.test`. `test {}` from Turbine.

### What NOT to Create in This Story

- No `TokenManager`, `MealieAuthenticator`, `ConnectivityMonitor` (Stories 1.5-1.7)
- No Room database, entities, DAOs (Story 2.1+)
- No DataStore instances (Story 1.5)
- No `ServerUrlScreen`, `CredentialScreen`, `ReAuthScreen` composables (Stories 1.3, 1.5, 1.8)
- No `ShoppingListScreen` (Story 2.1+)
- No `:feature:shopping` or `:feature:settings` module (their own stories)
- Do NOT issue ConnectivityMonitor probe in `MealieApplication.onCreate()` (Story 2.9 / GAP-2)
- Do NOT implement actual navigation routing logic - `startDestination = AuthGraph` is the only destination for now
- Do NOT add `windowSizeClass` dependencies or responsive layout - that's needed for Shopping List (Story 2.1+)
- Do NOT delete `core/data/src/main/java/.../Stub.kt`, `core/network/...Stub.kt`, `core/sync/...Stub.kt` - keep them (or replace with the real `Module.kt` file in same location)

### Project Structure Notes

- All source directories use `java/` not `kotlin/` (consistent with Story 1.1 Android Studio output)
- `:feature:auth` directory: `feature/auth/` under project root (consistent with `core/data/`, `core/network/` convention)
- `AppNavGraph.kt` lives in `:app`'s `navigation/` subpackage, not alongside `MainActivity.kt`
- `UiStub.kt` in `core/ui/src/main/java/` must be deleted once `UiModule.kt` and theme files are created (otherwise duplicate symbols may confuse Detekt/lint)
- Existing stub files in `:core:data`, `:core:network`, `:core:sync` can be deleted when their `di/Module.kt` files replace them as the sole Kotlin source, or retained alongside - either works

### References

- [Source: epics.md#Story 1.2] - Acceptance criteria
- [Source: architecture.md#Module Structure] - `:feature:auth` boundary: depends on `:core:data`, `:core:network`, `:core:ui`
- [Source: architecture.md#Dependency Injection: Koin] - One Koin module per Gradle module, all wired in `:app`
- [Source: architecture.md#Frontend Architecture / Navigation] - `NavigationManager` SharedFlow pattern, type-safe `@Serializable` routes, `NavGraphBuilder.authGraph()` extension
- [Source: architecture.md#Build Variants] - Timber gated on `BuildConfig.DEBUG` in single `MealieApplication.kt`
- [Source: architecture.md#Single MealieApplication.kt (GAP-4)] - No DebugApplication; Timber and HttpLoggingInterceptor in main Application class
- [Source: architecture.md#AppOrchestrator / Connectivity-to-Sync Wiring (GAP-1)] - `applicationScope` declared in MealieApplication for Story 2.9
- [Source: architecture.md#Testing Strategy] - JUnit 5 in `:core:ui`, `MainDispatcherExtension`, Turbine for SharedFlow
- [Source: ux-design-specification.md#Color System] - Seed `#E58325`, dynamic color API 31+, never hardcode hex
- [Source: ux-design-specification.md#Design System Foundation] - Single `MealieTheme.kt` at app root
- [Source: story 1-1 Dev Notes + Dev Agent Record] - No `kotlin.android` plugin (AGP 9+); Timber must be `implementation`; source dirs are `java/` not `kotlin/`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

- `NavigationCommand` changed from `sealed interface` to `interface` - sealed blocks implementations from other modules and test sources; feature modules need to implement it
- `koinInject` import updated to `org.koin.compose.koinInject` (Koin 4.x moved it from `org.koin.androidx.compose`)
- `MainDispatcherExtension` override signatures changed from nullable `ExtensionContext?` to non-nullable to match JUnit 5 interface contract
- Added `testRuntimeOnly(libs.junit.platform.launcher)` to fix "Failed to load JUnit Platform" (not in JUnit 6.1.0 BOM)
- Added `testImplementation(kotlin("test"))` for `kotlin.test.assertEquals`
- Added `lint { ignoreTestSources = true }` to `:core:ui` to work around AGP lint Metaspace OOM on test files (lint engine bug, not code issue)
- Added `koin-android` to `:core:network`, `:core:data`, `:core:sync` build.gradle.kts for `module { }` DSL access
- Added `junit-platform-launcher` entry to `gradle/libs.versions.toml`

### Completion Notes List

All 7 tasks complete. `:feature:auth` scaffolded with typed nav graph. `NavigationManager` (SharedFlow-based) and `MealieTheme` (M3 with dynamic color gate) created in `:core:ui`. Empty Koin modules declared in all `:core:*` modules. `MealieApplication` initializes Koin with all modules. `MainActivity` sets `MealieTheme { AppNavGraph(navController) }`. Two NavigationManager unit tests pass via Turbine. `assembleDebug`, `:core:ui:test`, `ktlintCheck`, `detekt`, and `lint` all pass.

### File List

feature/auth/build.gradle.kts
feature/auth/src/main/AndroidManifest.xml
feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/di/AuthFeatureModule.kt
feature/auth/src/main/java/dev/xexanos/mealie/feature/auth/navigation/AuthNavGraph.kt
core/ui/src/main/java/dev/xexanos/mealie/core/ui/navigation/NavigationManager.kt
core/ui/src/main/java/dev/xexanos/mealie/core/ui/di/UiModule.kt
core/ui/src/main/java/dev/xexanos/mealie/core/ui/theme/Color.kt
core/ui/src/main/java/dev/xexanos/mealie/core/ui/theme/Type.kt
core/ui/src/main/java/dev/xexanos/mealie/core/ui/theme/MealieTheme.kt
core/ui/src/test/java/dev/xexanos/mealie/core/ui/testutil/MainDispatcherExtension.kt
core/ui/src/test/java/dev/xexanos/mealie/core/ui/navigation/NavigationManagerTest.kt
core/network/src/main/java/dev/xexanos/mealie/core/network/di/NetworkModule.kt
core/data/src/main/java/dev/xexanos/mealie/core/data/di/DataModule.kt
core/sync/src/main/java/dev/xexanos/mealie/core/sync/di/SyncModule.kt
app/src/main/java/dev/xexanos/mealie/navigation/AppNavGraph.kt
app/src/main/java/dev/xexanos/mealie/MealieApplication.kt (modified)
app/src/main/java/dev/xexanos/mealie/MainActivity.kt (modified)
app/build.gradle.kts (modified)
settings.gradle.kts (modified)
core/ui/build.gradle.kts (modified)
core/network/build.gradle.kts (modified)
core/data/build.gradle.kts (modified)
core/sync/build.gradle.kts (modified)
gradle/libs.versions.toml (modified)
core/ui/src/main/java/dev/xexanos/mealie/core/ui/UiStub.kt (deleted)

### Change Log

- 2026-05-24: Implemented Story 1.2 - scaffolded :feature:auth, NavigationManager, MealieTheme, Koin modules, MealieApplication, and AppNavGraph
