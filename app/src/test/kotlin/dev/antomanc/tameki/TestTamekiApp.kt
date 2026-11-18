package dev.antomanc.tameki

import android.app.Application
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper

/** Robolectric Application that initialises WorkManager for Koin graph smoke tests. */
class TestTamekiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            this,
            Configuration.Builder().build(),
        )
    }
}
