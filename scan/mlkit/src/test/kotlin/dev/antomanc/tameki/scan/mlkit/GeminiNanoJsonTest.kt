package dev.antomanc.tameki.scan.mlkit

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Test

class GeminiNanoJsonTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun acceptsValidExchangeJson() {
        val draft = GeminiNanoJson.parseTransaction(
            """
            {"tameki":1,"transactions":[{"amount":"12.50","currency":"EUR","merchant":"Esselunga",
             "items":[{"name":"Latte","amount":"1.29","quantity":1}]}]}
            """.trimIndent(),
            CurrencyCode("EUR"),
            clock,
        )
        assertThat(draft).isNotNull()
        assertThat(draft?.merchant).isEqualTo("Esselunga")
        assertThat(draft?.items).hasSize(1)
    }

    @Test
    fun rejectsUnparsableOrEmpty() {
        assertThat(GeminiNanoJson.parseTransaction("not json", CurrencyCode("EUR"), clock)).isNull()
        assertThat(
            GeminiNanoJson.parseTransaction("""{"tameki":1,"transactions":[]}""", CurrencyCode("EUR"), clock),
        ).isNull()
        assertThat(
            GeminiNanoJson.parseTransaction("""{"tameki":99,"transactions":[{"amount":"1"}]}""", CurrencyCode("EUR"), clock),
        ).isNull()
    }
}
