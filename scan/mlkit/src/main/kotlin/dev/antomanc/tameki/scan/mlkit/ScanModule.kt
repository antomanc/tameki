package dev.antomanc.tameki.scan.mlkit

import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.scan.api.ReceiptReader
import java.time.Clock
import kotlinx.coroutines.flow.first
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val scanModule = module {
    single {
        val settings = get<SettingsRepository>()
        val clock = get<Clock>()
        GeminiNanoReceiptReader(
            baseCurrency = { settings.observeBaseCurrency().first() },
            clock = clock,
        )
    }
    single {
        val settings = get<SettingsRepository>()
        val clock = get<Clock>()
        MlKitOcrReceiptReader(
            appContext = androidContext(),
            baseCurrency = { settings.observeBaseCurrency().first() },
            clock = clock,
        )
    }
    single {
        val settings = get<SettingsRepository>()
        val clock = get<Clock>()
        HeuristicReceiptReader(
            baseCurrency = { settings.observeBaseCurrency().first() },
            clock = clock,
        )
    }
    single {
        ChainedReceiptReader(
            gemini = get(),
            ocr = get(),
            heuristic = get(),
        )
    } bind ReceiptReader::class
}
