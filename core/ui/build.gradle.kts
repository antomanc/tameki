plugins {
    id("tameki.android.library")
    id("tameki.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.antomanc.tameki.core.ui"
}

dependencies {
    api(projects.core.model)
    api(libs.compose.material3)
    api(libs.compose.animation)
    api(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.material.icons.core)
    implementation(libs.coil.compose)
}
