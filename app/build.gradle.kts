plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.xexanos.mealie"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.xexanos.mealie"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
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

    debugImplementation(libs.timber)
    debugImplementation(libs.compose.ui.tooling)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
