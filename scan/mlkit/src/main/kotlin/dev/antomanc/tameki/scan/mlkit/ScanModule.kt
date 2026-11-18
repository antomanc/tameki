package dev.antomanc.tameki.scan.mlkit

import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.scan.api.ReceiptReader
import java.time.Clock
import kotlinx.coroutines.flow.first
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
            baseCurrency = { settings.observeBaseCurrency().first() },
            clock = clock,
        )
    }
    single {
        ChainedReceiptReader(
            gemini = get(),
            ocr = get(),
        )
    } bind ReceiptReader::class
}
