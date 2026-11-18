package dev.antomanc.tameki.scan.mlkit

import dev.antomanc.tameki.scan.api.ReceiptReadResult
import dev.antomanc.tameki.scan.api.ReceiptReader
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Tries Gemini Nano when available; falls back to OCR + heuristic on unavailability,
 * timeout, or parse failure. The result always carries the engine that produced it.
 */
class ChainedReceiptReader(
    private val tryGemini: suspend (imagePath: String) -> ReceiptReadResult?,
    private val ocr: ReceiptReader,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
) : ReceiptReader {

    constructor(
        gemini: GeminiNanoReceiptReader,
        ocr: MlKitOcrReceiptReader,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ) : this(
        tryGemini = { path -> gemini.tryRead(path) },
        ocr = ocr,
        timeoutMs = timeoutMs,
    )

    override suspend fun read(imagePath: String): ReceiptReadResult =
        chainRead(
            tryPrimary = { tryGemini(imagePath) },
            fallback = { ocr.read(imagePath) },
            timeoutMs = timeoutMs,
        )

    companion object {
        const val DEFAULT_TIMEOUT_MS: Long = 15_000L

        /**
         * Pure chain logic (testable with fakes on the JVM).
         * [tryPrimary] returns null to signal unavailability / failure / unparsable output.
         */
        suspend fun chainRead(
            tryPrimary: suspend () -> ReceiptReadResult?,
            fallback: suspend () -> ReceiptReadResult,
            timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        ): ReceiptReadResult {
            val primary = try {
                withTimeout(timeoutMs) { tryPrimary() }
            } catch (_: TimeoutCancellationException) {
                null
            } catch (_: Exception) {
                null
            }
            return primary ?: fallback()
        }
    }
}
