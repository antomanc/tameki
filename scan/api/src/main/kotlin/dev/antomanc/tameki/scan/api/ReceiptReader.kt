package dev.antomanc.tameki.scan.api

import dev.antomanc.tameki.core.model.entity.TransactionDraft

/**
 * Which on-device engine produced a [ReceiptReadResult].
 * Callers chain Gemini Nano → ML Kit OCR + heuristic so the UI can show confidence/source.
 */
enum class ReceiptEngine {
    GEMINI_NANO,
    ML_KIT_OCR,
    HEURISTIC,
}

/**
 * Draft plus the raw OCR/LLM text and the engine that produced it.
 * Keeping raw text lets the user re-parse after upgrades without retaking the photo.
 */
data class ReceiptReadResult(
    val draft: TransactionDraft,
    val rawText: String?,
    val engine: ReceiptEngine,
)

/**
 * On-device receipt reading. Implementations (ML Kit, Gemini Nano) live behind Koin;
 * this module stays pure JVM so feature code never imports vendor SDKs.
 */
interface ReceiptReader {
    suspend fun read(imagePath: String): ReceiptReadResult
}
