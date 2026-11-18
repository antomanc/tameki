package dev.antomanc.tameki

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.data.db.TamekiDatabase
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.feature.settings.GeminiNanoBridge
import dev.antomanc.tameki.feature.settings.NanoStatusUi
import dev.antomanc.tameki.scan.api.ReceiptEngine
import dev.antomanc.tameki.scan.api.ReceiptReadResult
import dev.antomanc.tameki.scan.api.ReceiptReader
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Boots [MainActivity] with onboarding done and delivers a deep-link while running.
 * Asserts the activity stays alive — catches scaffold/DI crashes before a device launch.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = TestTamekiApp::class, sdk = [34])
class NavigationSmokeTest {
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
        runBlocking {
            val settings = GlobalContext.get().get<SettingsRepository>()
            settings.setOnboardingDone(true)
            settings.setDynamicColor(false)
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun activityStartsAndHandlesNewIntent() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertThat(activity.isFinishing).isFalse()
        assertThat(activity.isDestroyed).isFalse()

        controller.newIntent(
            Intent(Intent.ACTION_VIEW).apply {
                putExtra("tameki.open", "quick_add")
            },
        )
        assertThat(activity.isFinishing).isFalse()

        controller.newIntent(
            Intent(Intent.ACTION_VIEW).apply {
                putExtra("tameki.open", "inbox")
            },
        )
        assertThat(activity.isFinishing).isFalse()
        controller.pause().stop().destroy()
    }
}
