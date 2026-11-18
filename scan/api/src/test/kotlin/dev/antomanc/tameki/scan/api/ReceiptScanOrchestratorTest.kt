package dev.antomanc.tameki.scan.api

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Receipt
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ReceiptScanOrchestratorTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC)
    private val eur = CurrencyCode("EUR")

    @Test
    fun cancel_mapsToIgnore() {
        assertThat(DocumentScanOutcome.Cancelled.toNonImageEffect())
            .isEqualTo(ReceiptScanNonImageEffect.Ignore)
    }

    @Test
    fun unavailable_mapsToPhotoPickerFallback() {
        assertThat(DocumentScanOutcome.Unavailable.toNonImageEffect())
            .isEqualTo(ReceiptScanNonImageEffect.FallBackToPicker)
    }

    @Test
    fun success_storesAndPrefillsDraft() = runTest {
        val stored = mutableListOf<Receipt>()
        val orchestrator = ReceiptScanOrchestrator(
            copyToReceiptStorage = { src -> "/receipts/copied_${src.substringAfterLast('/')}" },
            upsertReceipt = { receipt ->
                val id = if (receipt.id == 0L) stored.size + 1L else receipt.id
                stored += receipt.copy(id = id)
                id
            },
            reader = object : ReceiptReader {
                override suspend fun read(imagePath: String) = ReceiptReadResult(
                    draft = TransactionDraft(
                        amount = Money(1_250, eur),
                        merchant = "Bar Centrale",
                    ),
                    rawText = "Bar Centrale\n12,50",
                    engine = ReceiptEngine.GEMINI_NANO,
                )
            },
            clock = clock,
        )

        val ready = orchestrator.processCaptured("/cache/scan.jpg")

        assertThat(ready.receiptPath).isEqualTo("/receipts/copied_scan.jpg")
        assertThat(ready.receiptId).isEqualTo(1L)
        assertThat(ready.draft.amount).isEqualTo(Money(1_250, eur))
        assertThat(ready.draft.merchant).isEqualTo("Bar Centrale")
        assertThat(ready.engine).isEqualTo(ReceiptEngine.GEMINI_NANO)
        assertThat(ready.readFailed).isFalse()
        assertThat(stored.last().rawText).isEqualTo("Bar Centrale\n12,50")
    }

    @Test
    fun cancel_successOutcome_hasNoNonImageEffect() {
        assertThat(DocumentScanOutcome.Success("/x.jpg").toNonImageEffect()).isNull()
    }

    @Test
    fun readFailure_stillAttachesPhoto() = runTest {
        val orchestrator = ReceiptScanOrchestrator(
            copyToReceiptStorage = { "/receipts/kept.jpg" },
            upsertReceipt = { 9L },
            reader = object : ReceiptReader {
                override suspend fun read(imagePath: String): ReceiptReadResult {
                    error("boom")
                }
            },
            clock = clock,
        )

        val ready = orchestrator.processCaptured("/cache/scan.jpg")

        assertThat(ready.receiptId).isEqualTo(9L)
        assertThat(ready.receiptPath).isEqualTo("/receipts/kept.jpg")
        assertThat(ready.draft.amount).isNull()
        assertThat(ready.readFailed).isTrue()
        assertThat(ready.engine).isNull()
    }

    @Test
    fun readTimeout_stillAttachesPhoto() = runTest {
        val orchestrator = ReceiptScanOrchestrator(
            copyToReceiptStorage = { "/receipts/slow.jpg" },
            upsertReceipt = { 3L },
            reader = object : ReceiptReader {
                override suspend fun read(imagePath: String): ReceiptReadResult {
                    delay(5_000)
                    return ReceiptReadResult(
                        draft = TransactionDraft(amount = Money(100, eur)),
                        rawText = "late",
                        engine = ReceiptEngine.ML_KIT_OCR,
                    )
                }
            },
            clock = clock,
            readTimeoutMs = 50L,
        )

        val ready = orchestrator.processCaptured("/cache/scan.jpg")

        assertThat(ready.readFailed).isTrue()
        assertThat(ready.receiptPath).isEqualTo("/receipts/slow.jpg")
        assertThat(ready.draft.amount).isNull()
    }
}
