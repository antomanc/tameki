plugins {
    id("tameki.android.library")
    id("tameki.android.compose")
}

android {
    namespace = "dev.antomanc.tameki.widget"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.data)
    implementation(projects.core.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.glance.preview)
    implementation(libs.glance.appwidget.preview)
    implementation(libs.koin.android)
}
