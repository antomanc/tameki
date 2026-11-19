package dev.antomanc.tameki.scan.mlkit

import dev.antomanc.tameki.core.model.coroutines.runCatchingCancellable
import dev.antomanc.tameki.scan.api.ReceiptReadResult
import dev.antomanc.tameki.scan.api.ReceiptReader
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Tries Gemini Nano when available; falls back to OCR, then to the heuristic path when the
 * unbundled ML Kit model is not yet on the device.
 */
class ChainedReceiptReader(
    private val tryGemini: suspend (imagePath: String) -> ReceiptReadResult?,
    private val ocr: ReceiptReader,
    private val heuristic: ReceiptReader,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
) : ReceiptReader {

    constructor(
        gemini: GeminiNanoReceiptReader,
        ocr: MlKitOcrReceiptReader,
        heuristic: HeuristicReceiptReader,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ) : this(
        tryGemini = { path -> gemini.tryRead(path) },
        ocr = ocr,
        heuristic = heuristic,
        timeoutMs = timeoutMs,
    )

    override suspend fun read(imagePath: String): ReceiptReadResult =
        chainRead(
            tryPrimary = { tryGemini(imagePath) },
            trySecondary = {
                runCatchingCancellable { ocr.read(imagePath) }.getOrNull()
            },
            fallback = { heuristic.read(imagePath) },
            timeoutMs = timeoutMs,
        )

    companion object {
        /**
         * Gemini Nano gets a short budget: if it hangs, OCR must still have time to run inside the
         * orchestrator's overall limit.
         */
        const val DEFAULT_TIMEOUT_MS: Long = 6_000L

        suspend fun chainRead(
            tryPrimary: suspend () -> ReceiptReadResult?,
            trySecondary: suspend () -> ReceiptReadResult?,
            fallback: suspend () -> ReceiptReadResult,
            timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        ): ReceiptReadResult {
            val primary = try {
                withTimeout(timeoutMs) { tryPrimary() }
            } catch (_: TimeoutCancellationException) {
                null
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (primary != null) return primary
            val secondary = try {
                trySecondary()
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            return secondary ?: fallback()
        }

        /** Back-compat for older tests that only pass primary + OCR fallback. */
        suspend fun chainRead(
            tryPrimary: suspend () -> ReceiptReadResult?,
            fallback: suspend () -> ReceiptReadResult,
            timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        ): ReceiptReadResult = chainRead(
            tryPrimary = tryPrimary,
            trySecondary = { null },
            fallback = fallback,
            timeoutMs = timeoutMs,
        )
    }
}
