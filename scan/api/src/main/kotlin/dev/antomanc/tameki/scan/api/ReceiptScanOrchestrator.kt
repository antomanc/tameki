package dev.antomanc.tameki.scan.api

import dev.antomanc.tameki.core.model.entity.Receipt
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import java.time.Clock
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Stored receipt ready for the quick-add sheet, with optional read result.
 */
data class ReceiptScanReady(
    val receiptId: Long,
    val receiptPath: String,
    val draft: TransactionDraft,
    val engine: ReceiptEngine?,
    val readFailed: Boolean,
)

/**
 * Pure scan → store → read pipeline. UI hosts map [DocumentScanOutcome] cancel/unavailable
 * themselves; this class handles a captured image path.
 */
class ReceiptScanOrchestrator(
    private val copyToReceiptStorage: suspend (sourcePath: String) -> String,
    private val upsertReceipt: suspend (Receipt) -> Long,
    private val reader: ReceiptReader,
    private val clock: Clock,
    private val readTimeoutMs: Long = DEFAULT_READ_TIMEOUT_MS,
) {
    /**
     * Copy [sourcePath] into private storage, persist a [Receipt], then read with timeout.
     * On read failure/timeout the receipt stays attached and [ReceiptScanReady.readFailed] is true.
     */
    suspend fun processCaptured(sourcePath: String): ReceiptScanReady {
        val storedPath = copyToReceiptStorage(sourcePath)
        val createdAt = Instant.now(clock)
        val receiptId = upsertReceipt(
            Receipt(imagePath = storedPath, createdAt = createdAt),
        )
        val read = try {
            withTimeout(readTimeoutMs) { reader.read(storedPath) }
        } catch (_: TimeoutCancellationException) {
            null
        } catch (e: CancellationException) {
            // The caller went away: stop, do not report a failed read.
            throw e
        } catch (_: Exception) {
            null
        }
        if (read == null) {
            return ReceiptScanReady(
                receiptId = receiptId,
                receiptPath = storedPath,
                draft = TransactionDraft(),
                engine = null,
                readFailed = true,
            )
        }
        upsertReceipt(
            Receipt(
                id = receiptId,
                imagePath = storedPath,
                rawText = read.rawText,
                items = read.draft.items,
                createdAt = createdAt,
            ),
        )
        return ReceiptScanReady(
            receiptId = receiptId,
            receiptPath = storedPath,
            draft = read.draft,
            engine = read.engine,
            readFailed = false,
        )
    }

    companion object {
        const val DEFAULT_READ_TIMEOUT_MS: Long = 15_000L
    }
}

/** Map a launcher outcome that needs no I/O. */
fun DocumentScanOutcome.toNonImageEffect(): ReceiptScanNonImageEffect? = when (this) {
    DocumentScanOutcome.Cancelled -> ReceiptScanNonImageEffect.Ignore
    DocumentScanOutcome.Unavailable -> ReceiptScanNonImageEffect.FallBackToPicker
    is DocumentScanOutcome.Success -> null
}

enum class ReceiptScanNonImageEffect {
    Ignore,
    FallBackToPicker,
}
