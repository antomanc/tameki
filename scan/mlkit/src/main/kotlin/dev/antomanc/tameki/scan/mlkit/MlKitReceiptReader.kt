package dev.antomanc.tameki.scan.mlkit

import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.scan.api.ReceiptEngine
import dev.antomanc.tameki.scan.api.ReceiptReadResult
import dev.antomanc.tameki.scan.api.ReceiptReader
import org.koin.dsl.module

/**
 * ML Kit text recognition with the bundled Latin model, plus Gemini Nano through the ML Kit
 * GenAI Prompt API where the device supports it. Placeholder until receipt capture lands.
 */
class MlKitReceiptReader : ReceiptReader {
    override suspend fun read(imagePath: String): ReceiptReadResult =
        ReceiptReadResult(draft = TransactionDraft(), rawText = null, engine = ReceiptEngine.HEURISTIC)
}

val scanModule = module {
}
