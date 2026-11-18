package dev.antomanc.tameki

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.data.db.TamekiDatabase
import dev.antomanc.tameki.core.model.money.ExchangeRates
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.BackupService
import dev.antomanc.tameki.core.model.repository.BudgetRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.CsvService
import dev.antomanc.tameki.core.model.repository.GoalRepository
import dev.antomanc.tameki.core.model.repository.InboxRepository
import dev.antomanc.tameki.core.model.repository.InsightSnoozeRepository
import dev.antomanc.tameki.core.model.repository.ReceiptRepository
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.ReminderScheduler
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import dev.antomanc.tameki.feature.activity.ActivityViewModel
import dev.antomanc.tameki.feature.activity.QuickAddViewModel
import dev.antomanc.tameki.feature.aibridge.AiBridgeViewModel
import dev.antomanc.tameki.feature.home.HomeViewModel
import dev.antomanc.tameki.feature.insights.InsightsViewModel
import dev.antomanc.tameki.feature.settings.GeminiNanoBridge
import dev.antomanc.tameki.feature.settings.NanoStatusUi
import dev.antomanc.tameki.feature.settings.SettingsViewModel
import dev.antomanc.tameki.feature.subscriptions.RecurringDetailViewModel
import dev.antomanc.tameki.feature.subscriptions.RecurringEditViewModel
import dev.antomanc.tameki.feature.subscriptions.SubscriptionsReviewViewModel
import dev.antomanc.tameki.feature.subscriptions.SubscriptionsViewModel
import dev.antomanc.tameki.scan.api.ReceiptEngine
import dev.antomanc.tameki.scan.api.ReceiptReadResult
import dev.antomanc.tameki.scan.api.ReceiptReader
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import java.time.Clock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Starts the real app Koin graph (in-memory Room) and resolves every singleton and ViewModel
 * the app wires in [TamekiApp.appModules]. Catches missing bindings before a device launch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestTamekiApp::class, sdk = [34])
class KoinGraphSmokeTest {
    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        runCatching { stopKoin() }
        startKoin {
            allowOverride(true)
            androidContext(app)
            modules(TamekiApp.appModules)
            modules(
                module {
                    single { TamekiDatabase.inMemory(app) }
                    // ML Kit / GenAI need Play Services; substitute no-ops for the graph check.
                    single<ReceiptReader> {
                        object : ReceiptReader {
                            override suspend fun read(imagePath: String) = ReceiptReadResult(
                                draft = TransactionDraft(),
                                rawText = null,
                                engine = ReceiptEngine.HEURISTIC,
                            )
                        }
                    }
                    single<GeminiNanoBridge> {
                        object : GeminiNanoBridge {
                            override suspend fun status() = NanoStatusUi.UNSUPPORTED
                            override suspend fun download() = Unit
                        }
                    }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun everyWiredDefinitionResolves() {
        val koin = GlobalContext.get()

        assertThat(koin.get<TamekiDatabase>()).isNotNull()
        assertThat(koin.get<Clock>()).isNotNull()
        assertThat(koin.get<SettingsRepository>()).isNotNull()
        assertThat(koin.get<AccountRepository>()).isNotNull()
        assertThat(koin.get<CategoryRepository>()).isNotNull()
        assertThat(koin.get<TransactionRepository>()).isNotNull()
        assertThat(koin.get<RecurringRepository>()).isNotNull()
        assertThat(koin.get<BudgetRepository>()).isNotNull()
        assertThat(koin.get<GoalRepository>()).isNotNull()
        assertThat(koin.get<ReceiptRepository>()).isNotNull()
        assertThat(koin.get<InboxRepository>()).isNotNull()
        assertThat(koin.get<InsightSnoozeRepository>()).isNotNull()
        assertThat(koin.get<ExchangeRates>()).isNotNull()
        assertThat(koin.get<BackupService>()).isNotNull()
        assertThat(koin.get<CsvService>()).isNotNull()
        assertThat(koin.get<ReminderScheduler>()).isNotNull()
        assertThat(koin.get<ReceiptReader>()).isNotNull()

        assertThat(koin.get<HomeViewModel>()).isNotNull()
        assertThat(koin.get<ActivityViewModel>()).isNotNull()
        assertThat(koin.get<QuickAddViewModel>()).isNotNull()
        assertThat(koin.get<SubscriptionsViewModel>()).isNotNull()
        assertThat(koin.get<SubscriptionsReviewViewModel>()).isNotNull()
        assertThat(koin.get<RecurringDetailViewModel>()).isNotNull()
        assertThat(koin.get<RecurringEditViewModel>()).isNotNull()
        assertThat(koin.get<InsightsViewModel>()).isNotNull()
        assertThat(koin.get<SettingsViewModel>()).isNotNull()
        assertThat(koin.get<AiBridgeViewModel>()).isNotNull()
    }
}
