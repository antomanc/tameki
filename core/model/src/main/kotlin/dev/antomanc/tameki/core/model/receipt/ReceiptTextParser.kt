package dev.antomanc.tameki.core.model.receipt

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.ReceiptItem
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.money.MoneyParser
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Heuristic OCR → draft when no on-device LLM is available.
 * Looks for TOTAL/TOTALE lines, common IT/EN dates, and "name  amount" item lines.
 */
object ReceiptTextParser {
    private val totalLine = Regex(
        """(?i)^(?:totale|total|importo\s+pagato|amount\s+due|grand\s+total)\s*[:\s]+.*?([\d.,]+)\s*([A-Z]{3}|€|\$)?\s*$""",
    )
    private val totalInline = Regex(
        """(?i)(?:totale|total|importo\s+pagato)\s*[:\s]+([\d.,]+)""",
    )
    private val itemLine = Regex(
        """^(.+?)\s+([€$]?\s*[\d]+[.,][\d]{2})\s*$""",
    )
    private val noise = Regex(
        """(?i)^(tel|p\.?\s*iva|iva|cf|www\.|http|scontrino|documento|cassa|operatore)\b""",
    )

    private val dateFormats = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.ITALIAN),
    )

    fun parse(
        rawText: String,
        defaultCurrency: CurrencyCode = CurrencyCode("EUR"),
        today: LocalDate,
    ): TransactionDraft {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val merchant = lines.firstOrNull { line ->
            line.length in 2..48 && !noise.containsMatchIn(line) && !line.all { it.isDigit() || it == ' ' }
        }
        var amount: Money? = null
        for (line in lines) {
            val m = totalLine.find(line) ?: totalInline.find(line) ?: continue
            val rawAmount = m.groupValues[1]
            val currency = m.groupValues.getOrNull(2)
                ?.let { sym ->
                    when (sym) {
                        "€" -> CurrencyCode("EUR")
                        "$" -> CurrencyCode("USD")
                        else -> CurrencyCode.parse(sym)
                    }
                }
                ?: defaultCurrency
            amount = MoneyParser.parse(rawAmount, currency)
            if (amount != null) break
        }
        val occurred = (findDate(lines) ?: today).atTime(LocalTime.NOON)
        val items = lines.mapNotNull { line ->
            if (totalLine.containsMatchIn(line) || totalInline.containsMatchIn(line)) return@mapNotNull null
            val m = itemLine.find(line) ?: return@mapNotNull null
            val name = m.groupValues[1].trim()
            if (noise.containsMatchIn(name) || name.equals(merchant, ignoreCase = true)) return@mapNotNull null
            val itemAmount = MoneyParser.parse(m.groupValues[2], amount?.currency ?: defaultCurrency)
                ?: return@mapNotNull null
            ReceiptItem(name = name, amount = itemAmount)
        }
        return TransactionDraft(
            kind = TransactionKind.EXPENSE,
            amount = amount,
            merchant = merchant,
            occurredAt = occurred,
            items = items,
            note = null,
        )
    }

    private fun findDate(lines: List<String>): LocalDate? {
        val dateToken = Regex(
            """(\d{1,2}[/.-]\d{1,2}[/.-]\d{2,4}|\d{4}-\d{2}-\d{2}|[A-Za-zàéù]{3,9}\s+\d{1,2},?\s+\d{4}|\d{1,2}\s+[A-Za-zàéù]{3,9}\s+\d{4})""",
        )
        for (line in lines) {
            val token = dateToken.find(line)?.value ?: continue
            for (fmt in dateFormats) {
                try {
                    return LocalDate.parse(token.replace(',', ' ').replace(Regex("""\s+"""), " ").trim(), fmt)
                } catch (_: DateTimeParseException) {
                    // try next
                }
            }
        }
        return null
    }
}
