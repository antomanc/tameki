plugins {
    id("spare.android.library")
}

android {
    namespace = "dev.antomanc.spare.scan.mlkit"
}

dependencies {
    api(projects.scan.api)
    implementation(projects.core.model)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.genai.prompt)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
}
