plugins {
    id("spare.android.library")
    id("spare.android.compose")
}

android {
    namespace = "dev.antomanc.spare.feature.home"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.data)
    implementation(projects.core.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.koin.androidx.compose)
    implementation(libs.compose.material.icons.core)
}
