package dev.antomanc.spare

import android.app.Application
import dev.antomanc.spare.capture.captureModule
import dev.antomanc.spare.core.data.di.dataModule
import dev.antomanc.spare.feature.activity.activityModule
import dev.antomanc.spare.feature.aibridge.aibridgeModule
import dev.antomanc.spare.feature.home.homeModule
import dev.antomanc.spare.feature.insights.insightsModule
import dev.antomanc.spare.feature.settings.settingsModule
import dev.antomanc.spare.feature.subscriptions.subscriptionsModule
import dev.antomanc.spare.scan.mlkit.scanModule
import dev.antomanc.spare.widget.widgetModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SpareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SpareApp)
            modules(appModules)
        }
    }

    companion object {
        /** One empty module per layer; each feature fills its own as it lands. */
        val appModules = listOf(
            dataModule,
            scanModule,
            captureModule,
            homeModule,
            activityModule,
            subscriptionsModule,
            insightsModule,
            settingsModule,
            aibridgeModule,
            widgetModule,
        )
    }
}
