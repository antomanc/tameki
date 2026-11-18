plugins {
    id("tameki.android.application")
    id("tameki.android.compose")
}

android {
    namespace = "dev.antomanc.tameki"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "dev.antomanc.tameki"
        versionCode = 1
        versionName = "0.1.0"
    }

    androidResources {
        localeFilters += listOf("en", "it")
    }

    signingConfigs {
        create("release") {
            // CI injects these; local release builds fall back to the debug key.
            val storePath = System.getenv("TAMEKI_KEYSTORE_PATH")
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = System.getenv("TAMEKI_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("TAMEKI_KEY_ALIAS")
                keyPassword = System.getenv("TAMEKI_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (System.getenv("TAMEKI_KEYSTORE_PATH") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.ui)
    implementation(projects.scan.api)
    implementation(projects.scan.mlkit)
    implementation(projects.capture)
    implementation(projects.feature.home)
    implementation(projects.feature.activity)
    implementation(projects.feature.subscriptions)
    implementation(projects.feature.insights)
    implementation(projects.feature.settings)
    implementation(projects.feature.aibridge)
    implementation(projects.widget)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.compose.material.icons.core)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

tasks.withType<Test>().configureEach {
    systemProperty("roborazzi.test.record", "true")
}
