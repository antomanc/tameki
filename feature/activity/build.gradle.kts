plugins {
    id("tameki.android.library")
    id("tameki.android.compose")
}

android {
    namespace = "dev.antomanc.tameki.feature.activity"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.data)
    implementation(projects.core.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.paging.compose)
    implementation(libs.koin.androidx.compose)
    implementation(libs.compose.material.icons.core)
}
