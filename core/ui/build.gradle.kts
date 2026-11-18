plugins {
    id("spare.android.library")
    id("spare.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.antomanc.spare.core.ui"
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
