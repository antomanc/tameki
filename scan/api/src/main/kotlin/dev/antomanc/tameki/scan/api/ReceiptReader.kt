package dev.antomanc.tameki.scan.api

/**
 * On-device text recognition. Receipts first, then the pure heuristic parser in core:model.
 * Implementations are swapped in Koin, so nothing here touches ML Kit or Gemini Nano.
 */
interface ReceiptReader {
    /** Returns the raw text found in [image] (JPEG or PNG bytes), or null when nothing readable was found. */
    suspend fun readText(image: ByteArray): String?
}
