plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.xexanos.mealie.core.data"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    implementation(project(":core:network"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> { useJUnitPlatform() }
