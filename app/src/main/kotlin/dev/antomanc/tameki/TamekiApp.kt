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
import dev.antomanc.tameki.widget.WidgetRefresher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class TamekiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TamekiApp)
            modules(appModules)
        }
        val koin = org.koin.core.context.GlobalContext.get()
        WidgetRefresher.start(
            context = this,
            scope = appScope,
            transactions = koin.get(),
            budgets = koin.get(),
            recurrings = koin.get(),
        )
    }

    /** Lives as long as the process: app-wide observers (the widget refresher) run here. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        val appModules = listOf(
            dataModule,
            scanModule,
            captureModule,
            appModule,
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
