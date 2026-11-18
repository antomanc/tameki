package dev.antomanc.tameki.scan.mlkit

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.receipt.ReceiptTextParser
import dev.antomanc.tameki.scan.api.ReceiptEngine
import dev.antomanc.tameki.scan.api.ReceiptReadResult
import dev.antomanc.tameki.scan.api.ReceiptReader
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Last-resort reader when on-device OCR models are unavailable: runs the pure heuristic parser
 * with empty text so the quick-add sheet still opens with a usable (empty) draft.
 */
class HeuristicReceiptReader(
    private val baseCurrency: suspend () -> CurrencyCode = { CurrencyCode("EUR") },
    private val clock: Clock = Clock.systemDefaultZone(),
) : ReceiptReader {
    override suspend fun read(imagePath: String): ReceiptReadResult = withContext(Dispatchers.IO) {
        val draft = ReceiptTextParser.parse(
            rawText = "",
            defaultCurrency = baseCurrency(),
            today = LocalDate.now(clock),
        )
        ReceiptReadResult(draft = draft, rawText = null, engine = ReceiptEngine.HEURISTIC)
    }
}
