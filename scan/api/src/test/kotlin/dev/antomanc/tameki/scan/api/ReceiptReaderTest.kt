package dev.antomanc.tameki.scan.api

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ReceiptReaderTest {
    @Test
    fun resultCarriesDraftRawTextAndEngine() = runBlocking {
        val reader = object : ReceiptReader {
            override suspend fun read(imagePath: String) = ReceiptReadResult(
                draft = TransactionDraft(amount = Money(100, CurrencyCode("EUR"))),
                rawText = "TOTAL 1.00",
                engine = ReceiptEngine.HEURISTIC,
            )
        }
        val result = reader.read("/tmp/receipt.jpg")
        assertThat(result.engine).isEqualTo(ReceiptEngine.HEURISTIC)
        assertThat(result.rawText).contains("TOTAL")
        assertThat(result.draft.amount?.minor).isEqualTo(100L)
    }
}
