plugins {
    id("tameki.android.library")
}

android {
    namespace = "dev.antomanc.tameki.scan.mlkit"
}

dependencies {
    api(projects.scan.api)
    implementation(projects.core.model)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.document.scanner)
    implementation(libs.mlkit.genai.prompt)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
}
