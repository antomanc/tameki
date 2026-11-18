package dev.antomanc.tameki.core.model.capture

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.money.MoneyParser
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Rule-table parser for banking/wallet notifications (IT + EN).
 * Returns null for OTP/security noise so the inbox stays actionable.
 */
object PaymentNotificationParser {
    private val otp = Regex(
        """(?i)\b(otp|one[-\s]?time|codice\s*(di\s*)?(sicurezza|verifica)|security\s*code|verification\s*code|password\s*usa\s*e\s*getta)\b""",
    )

    private val incomeHints = Regex(
        """(?i)\b(accredito|bonifico\s+ricevuto|hai\s+ricevuto|incoming|received\s+(?:a\s+)?(?:payment|transfer|credit)|credit\s+of)\b""",
    )

    private data class Rule(
        val pattern: Regex,
        val amountGroup: Int,
        val currencyGroup: Int?,
        val merchantGroup: Int?,
        val defaultCurrency: String = "EUR",
    )

    private val rules: List<Rule> = listOf(
        // Pagamento di 12,50 EUR presso X
        Rule(
            Regex("""(?i)pagamento\s+di\s+([\d.,]+)\s*([A-Z]{3}|€|\$)?\s*(?:presso|a|da)?\s*(.+)?"""),
            1, 2, 3,
        ),
        // Hai pagato 12,50 € a X
        Rule(
            Regex("""(?i)hai\s+pagato\s+([\d.,]+)\s*(€|EUR|\$|USD)?\s*(?:a|presso|da)?\s*(.+)?"""),
            1, 2, 3,
        ),
        // Addebito ... importo 12,50 EUR ...
        Rule(
            Regex("""(?i)addebito.*?importo\s*(?:di\s*)?([\d.,]+)\s*([A-Z]{3}|€|\$)?(?:.*?presso\s+(.+))?"""),
            1, 2, 3,
        ),
        // You paid €12.50 to X
        Rule(
            Regex("""(?i)you\s+paid\s*(€|\$|£)?\s*([\d.,]+)\s*(?:([A-Z]{3}))?\s*(?:to|at)\s+(.+)"""),
            2, 1, 4,
        ),
        // Payment of $3.20 at X / Payment of 3.20 EUR at X
        Rule(
            Regex("""(?i)payment\s+of\s*(€|\$|£)?\s*([\d.,]+)\s*(?:([A-Z]{3}))?\s*(?:at|to|for)\s+(.+)"""),
            2, 1, 4,
        ),
        // Google Wallet style: €4.50 at X
        Rule(
            Regex("""(?i)^(€|\$|£)\s*([\d.,]+)\s+at\s+(.+)$"""),
            2, 1, 3,
        ),
        // Paid 12.50 EUR to X
        Rule(
            Regex("""(?i)(?:paid|pagamento)\s+([\d.,]+)\s*([A-Z]{3}|€|\$)\s*(?:to|at|a|presso)\s+(.+)"""),
            1, 2, 3,
        ),
    )

    fun parse(
        packageName: String,
        title: String,
        text: String,
        postedAt: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): TransactionDraft? {
        val blob = listOf(title, text).filter { it.isNotBlank() }.joinToString("\n")
        if (blob.isBlank()) return null
        if (otp.containsMatchIn(blob)) return null

        val kind = if (incomeHints.containsMatchIn(blob)) {
            TransactionKind.INCOME
        } else {
            TransactionKind.EXPENSE
        }

        val candidates = listOf(blob) + blob.lines().map { it.trim() }.filter { it.isNotEmpty() }
        for (candidate in candidates) {
            for (rule in rules) {
                val match = rule.pattern.find(candidate) ?: continue
                val amountRaw = match.groupValues.getOrNull(rule.amountGroup)?.trim().orEmpty()
                if (amountRaw.isEmpty()) continue
                val currencyRaw = rule.currencyGroup?.let { match.groupValues.getOrNull(it) }?.trim()
                val currency = resolveCurrency(currencyRaw, rule.defaultCurrency) ?: continue
                val amount = MoneyParser.parse(amountRaw, currency) ?: continue
                val absolute = if (amount.minor < 0) amount.negate() else amount
                val merchant = rule.merchantGroup
                    ?.let { match.groupValues.getOrNull(it) }
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.trimEnd('.', ',', ';')
                return TransactionDraft(
                    kind = kind,
                    amount = absolute,
                    merchant = merchant,
                    occurredAt = LocalDateTime.ofInstant(postedAt, zone),
                    note = "from:$packageName",
                )
            }
        }

        // Income without a matching expense rule: try a generic amount scrape.
        if (kind == TransactionKind.INCOME) {
            val amountMatch = Regex("""([\d.,]+)\s*(€|EUR|\$|USD|[A-Z]{3})?""").find(blob)
            if (amountMatch != null) {
                val currency = resolveCurrency(amountMatch.groupValues.getOrNull(2), "EUR") ?: CurrencyCode("EUR")
                val amount = MoneyParser.parse(amountMatch.groupValues[1], currency)
                if (amount != null) {
                    return TransactionDraft(
                        kind = TransactionKind.INCOME,
                        amount = if (amount.minor < 0) amount.negate() else amount,
                        occurredAt = LocalDateTime.ofInstant(postedAt, zone),
                        note = "from:$packageName",
                    )
                }
            }
        }
        return null
    }

    private fun resolveCurrency(raw: String?, defaultCode: String): CurrencyCode? {
        if (raw.isNullOrBlank()) return CurrencyCode.parse(defaultCode)
        return when (raw.trim()) {
            "€" -> CurrencyCode("EUR")
            "$" -> CurrencyCode("USD")
            "£" -> CurrencyCode("GBP")
            "¥" -> CurrencyCode("JPY")
            else -> CurrencyCode.parse(raw)
        }
    }
}
