plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.xexanos.mealie.core.sync"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:network"))

    implementation(libs.workmanager.ktx)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.workmanager.testing)
}

tasks.withType<Test> { useJUnitPlatform() }
