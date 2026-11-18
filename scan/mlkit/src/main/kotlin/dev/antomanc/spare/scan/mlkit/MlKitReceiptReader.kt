package dev.antomanc.spare.scan.mlkit

import dev.antomanc.spare.scan.api.ReceiptReader
import org.koin.dsl.module

/**
 * ML Kit text recognition with the bundled Latin model, plus Gemini Nano through the ML Kit
 * GenAI Prompt API where the device supports it. Wired in Koin once receipt capture lands.
 */
class MlKitReceiptReader : ReceiptReader {
    override suspend fun readText(image: ByteArray): String? = null
}

val scanModule = module {
}
