plugins {
    id("tameki.android.library")
    id("tameki.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.antomanc.tameki.core.ui"
}

// Screenshot tests record PNGs under build/outputs/roborazzi on every unit-test run, so the
// gallery can be reviewed locally and in CI without a separate Roborazzi task.
tasks.withType<Test>().configureEach {
    systemProperty("roborazzi.test.record", "true")
}

dependencies {
    api(projects.core.model)
    api(libs.compose.material3)
    api(libs.compose.animation)
    api(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.material.icons.core)
    implementation(libs.coil.compose)

    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
