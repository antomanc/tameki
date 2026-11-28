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
        versionCode = 4
        versionName = "0.1.2"
    }

    androidResources {
        localeFilters += listOf("en", "it", "sr", "hr")
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
    implementation(libs.compose.material.icons.extended)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.work.testing)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.compose.ui.test.junit4)
    // ActivityScreen's signature references LazyPagingItems; needed to compile store captures.
    testImplementation(libs.androidx.paging.compose)
    debugImplementation(libs.compose.ui.test.manifest)
}

val recordStoreAssetsRequested = gradle.startParameter.taskNames.any {
    it.substringAfterLast(':').equals("recordStoreAssets", ignoreCase = true)
}
val storeAssetsEnabled = project.hasProperty("storeAssets") || recordStoreAssetsRequested

tasks.withType<Test>().configureEach {
    systemProperty("roborazzi.test.record", "true")
    if (storeAssetsEnabled) {
        systemProperty("store.assets.enabled", "true")
        systemProperty(
            "store.assets.out",
            rootProject.layout.projectDirectory.dir("fastlane/metadata/android").asFile.absolutePath,
        )
        // When regenerating store assets, skip the rest of the app unit tests.
        if (recordStoreAssetsRequested) {
            filter { includeTestsMatching("dev.antomanc.tameki.store.*") }
        }
    } else {
        filter { excludeTestsMatching("dev.antomanc.tameki.store.*") }
    }
}

tasks.register("recordStoreAssets") {
    group = "publishing"
    description = "Generate Play Store listing graphics into fastlane/metadata/android/"
    dependsOn("testDebugUnitTest")
}
