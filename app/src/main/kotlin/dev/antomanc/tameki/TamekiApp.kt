package dev.antomanc.tameki

import android.app.Application
import dev.antomanc.tameki.capture.captureModule
import dev.antomanc.tameki.core.data.di.dataModule
import dev.antomanc.tameki.feature.activity.activityModule
import dev.antomanc.tameki.feature.aibridge.aibridgeModule
import dev.antomanc.tameki.feature.home.homeModule
import dev.antomanc.tameki.feature.insights.insightsModule
import dev.antomanc.tameki.feature.settings.settingsModule
import dev.antomanc.tameki.feature.subscriptions.subscriptionsModule
import dev.antomanc.tameki.scan.mlkit.scanModule
import dev.antomanc.tameki.widget.widgetModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TamekiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TamekiApp)
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
