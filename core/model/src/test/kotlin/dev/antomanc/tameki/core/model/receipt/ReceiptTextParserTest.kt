package dev.antomanc.tameki.core.model.receipt

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import java.time.LocalDate
import org.junit.Test

class ReceiptTextParserTest {
    @Test
    fun extractsTotalMerchantDateAndItems() {
        val text = """
            ESSELUNGA
            Via Roma 1
            Latte intero        1,29
            Pane                2,50
            TOTALE EUR         12,50
            Data 18/11/2026
        """.trimIndent()
        val draft = ReceiptTextParser.parse(text, today = LocalDate.of(2026, 11, 18))
        assertThat(draft.merchant).isEqualTo("ESSELUNGA")
        assertThat(draft.amount?.minor).isEqualTo(1250L)
        assertThat(draft.amount?.currency).isEqualTo(CurrencyCode("EUR"))
        assertThat(draft.occurredAt?.toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 18))
        assertThat(draft.items).hasSize(2)
    }

    @Test
    fun fallsBackToTodayWhenNoDate() {
        val draft = ReceiptTextParser.parse(
            "CAFE\nTOTAL 3.00",
            today = LocalDate.of(2026, 5, 1),
        )
        assertThat(draft.occurredAt?.toLocalDate()).isEqualTo(LocalDate.of(2026, 5, 1))
    }
}
