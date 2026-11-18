package dev.antomanc.tameki.core.model.ai

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Test

class AiExchangeFormatTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC)

    private val docExample = """
        {
          "tameki": 1,
          "transactions": [
            {
              "date": "2026-11-18",
              "time": "13:05",
              "kind": "expense",
              "amount": "12.50",
              "currency": "EUR",
              "merchant": "Esselunga",
              "category": "Groceries",
              "account": "Card",
              "note": "",
              "tags": [],
              "items": [
                { "name": "Latte", "amount": "1.29", "quantity": 1 }
              ]
            }
          ],
          "subscriptions": [
            {
              "name": "Netflix",
              "amount": "13.99",
              "currency": "EUR",
              "cycle": "1 month",
              "next_date": "2026-12-01",
              "trial_ends_on": null,
              "category": "Entertainment",
              "cancel_url": null
            }
          ]
        }
    """.trimIndent()

    @Test
    fun parsesCanonicalDocExample() {
        val result = AiExchangeFormat.parse(docExample, clock = clock)
        assertThat(result.problems).isEmpty()
        assertThat(result.transactions).hasSize(1)
        val tx = result.transactions.single()
        assertThat(tx.amount?.minor).isEqualTo(1250L)
        assertThat(tx.merchant).isEqualTo("Esselunga")
        assertThat(tx.items.single().name).isEqualTo("Latte")
        // money-ok: quantity assertion
        assertThat(tx.items.single().quantity).isEqualTo(1.0)
        assertThat(result.subscriptions.single().name).isEqualTo("Netflix")
        assertThat(result.subscriptions.single().nextDate).isEqualTo(LocalDate.of(2026, 12, 1))
    }

    @Test
    fun docExampleMatchesMarkdownAndSchemaFiles() {
        val root = findRepoRoot()
        val md = Files.readString(root.resolve("docs/ai-format.md"))
        val schema = Files.readString(root.resolve("docs/ai-format.schema.json"))
        assertThat(md).contains("\"tameki\": 1")
        assertThat(md).contains("Esselunga")
        assertThat(schema).contains("\"const\": 1")
        assertThat(schema).contains("Esselunga")
        // Parsing the doc example keeps markdown/schema/tests in sync.
        val result = AiExchangeFormat.parse(docExample, clock = clock)
        assertThat(result.transactions).isNotEmpty()
        assertThat(result.subscriptions).isNotEmpty()
    }

    @Test
    fun stripsProseAndCodeFences() {
        val wrapped = """
            Sure! Here is the JSON:
            ```json
            { "tameki": 1, "transactions": [{ "amount": "9,99", "merchant": "Bar" }] }
            ```
            Hope that helps!
        """.trimIndent()
        val result = AiExchangeFormat.parse(wrapped, clock = clock)
        assertThat(result.transactions.single().amount?.minor).isEqualTo(999L)
        assertThat(result.transactions.single().occurredAt?.toLocalDate())
            .isEqualTo(LocalDate.of(2026, 11, 18))
    }

    @Test
    fun acceptsNumericAmountAndDefaultsCurrency() {
        val result = AiExchangeFormat.parse(
            """{"tameki":1,"transactions":[{"amount":12.5,"kind":"income"}]}""",
            baseCurrency = CurrencyCode("USD"),
            clock = clock,
        )
        assertThat(result.transactions.single().amount?.currency).isEqualTo(CurrencyCode("USD"))
        assertThat(result.transactions.single().amount?.minor).isEqualTo(1250L)
    }

    @Test
    fun malformedReturnsProblemsNeverThrows() {
        val result = AiExchangeFormat.parse("not json at all", clock = clock)
        assertThat(result.transactions).isEmpty()
        assertThat(result.problems).isNotEmpty()
    }

    @Test
    fun promptsRespectLocale() {
        val it = AiPrompts.naturalLanguageEntry(java.util.Locale.ITALIAN, listOf("Spesa"), listOf("Carta"))
        val en = AiPrompts.naturalLanguageEntry(java.util.Locale.ENGLISH, listOf("Groceries"), listOf("Card"))
        assertThat(it).contains("JSON")
        assertThat(it.lowercase()).contains("categorie")
        assertThat(en.lowercase()).contains("categories")
    }

    private fun findRepoRoot(): Path {
        var dir = Path.of("").toAbsolutePath()
        repeat(8) {
            if (Files.exists(dir.resolve("docs/ai-format.md"))) return dir
            dir = dir.parent ?: return Path.of(".")
        }
        // Gradle runs tests with module dir as cwd.
        val fromModule = Path.of("").toAbsolutePath().resolve("../..").normalize()
        if (Files.exists(fromModule.resolve("docs/ai-format.md"))) return fromModule
        return Path.of("../..").toAbsolutePath().normalize()
    }
}
