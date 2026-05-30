import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.TimeUnit

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val wiremockRunner: Configuration by configurations.creating

android {
    namespace = "dev.xexanos.mealie"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.xexanos.mealie"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val keystorePath = System.getenv("KEYSTORE_PATH")
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAliasValue = System.getenv("KEY_ALIAS")
            val keyPasswordValue = System.getenv("KEY_PASSWORD")
            if (keystorePath != null && keystorePassword != null && keyAliasValue != null && keyPasswordValue != null) {
                signingConfig = signingConfigs.create("release") {
                    storeFile = file(keystorePath)
                    storePassword = keystorePassword
                    keyAlias = keyAliasValue
                    keyPassword = keyPasswordValue
                }
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"

        managedDevices {
            localDevices {
                create("mediumPhoneApi30") {
                    device = "Medium Phone"
                    apiLevel = 30
                    systemImageSource = "aosp"
                }
                create("mediumPhoneApi34") {
                    device = "Medium Phone"
                    apiLevel = 34
                    systemImageSource = "aosp"
                }
                create("mediumPhoneApi36") {
                    device = "Medium Phone"
                    apiLevel = 36
                    systemImageSource = "google"
                }
            }
        }
    }

    splits {
        abi {
            isEnable = true
            include("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:sync"))
    implementation(project(":core:ui"))
    implementation(project(":feature:auth"))

    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.timber)
    debugImplementation(libs.compose.ui.tooling)

    wiremockRunner(libs.wiremock.standalone)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit5)
    testImplementation(libs.okhttp)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
    testRuntimeOnly(libs.junit.platform.launcher)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestUtil(libs.androidx.test.orchestrator)
    debugImplementation(libs.compose.ui.test.manifest)
}

var wiremockProcess: Process? = null

val wiremockStart by tasks.registering {
    doLast {
        val jar = wiremockRunner.singleFile
        val rootDir = file("src/androidTest/resources/wiremock")
        wiremockProcess = ProcessBuilder(
            "java", "-jar", jar.absolutePath,
            "--port", "8080",
            "--root-dir", rootDir.absolutePath,
        ).inheritIO().start()

        val deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        var ready = false
        while (System.nanoTime() < deadlineNanos && !ready) {
            check(wiremockProcess?.isAlive != false) { "WireMock process terminated unexpectedly" }
            ready = runCatching {
                val conn = URI("http://localhost:8080/__admin/mappings").toURL().openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 500
                conn.readTimeout = 500
                try {
                    conn.responseCode == 200
                } finally {
                    conn.disconnect()
                }
            }.getOrDefault(false)
            if (!ready) Thread.sleep(250)
        }
        check(ready) { "WireMock did not start on port 8080 within 15 seconds" }
        logger.lifecycle("WireMock started on port 8080")
    }
}

val wiremockStop by tasks.registering {
    doLast {
        runCatching {
            val conn = URI("http://localhost:8080/__admin/shutdown").toURL()
                .openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            conn.doOutput = false
            try {
                conn.responseCode
            } finally {
                conn.disconnect()
            }
        }
        wiremockProcess?.destroyForcibly()
        wiremockProcess = null
        logger.lifecycle("WireMock stopped")
    }
}

afterEvaluate {
    tasks.matching { it.name.endsWith("DebugAndroidTest") }.configureEach {
        dependsOn(wiremockStart)
        finalizedBy(wiremockStop)
    }
}

tasks.withType<Test> { useJUnitPlatform() }
