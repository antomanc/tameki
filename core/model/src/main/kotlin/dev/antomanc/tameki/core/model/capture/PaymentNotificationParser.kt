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
        """(?i)\b(otp|one[-\s]?time|codice\s*(di\s*)?(sicurezza|verifica|autorizzativo)|security\s*code|verification\s*code|password\s*usa\s*e\s*getta|autorizza\s+l['’]operazione)\b""",
    )

    private val incomeHints = Regex(
        """(?i)\b(accredito|bonifico\s+ricevuto|hai\s+ricevuto|rimborso|incoming|received\s+(?:a\s+)?(?:payment|transfer|credit)|credit\s+of|refund)\b""",
    )

    private data class Rule(
        val pattern: Regex,
        val amountGroup: Int,
        val currencyGroup: Int?,
        val trailingCurrencyGroup: Int? = null,
        val merchantGroup: Int?,
        val defaultCurrency: String = "EUR",
    )

    private val rules: List<Rule> = listOf(
        // Intesa Sanpaolo / Isybank / Italian banks with card & date metadata before merchant:
        // "💸 Hai pagato 105,99 € con la carta *1234 il 19.11 alle ore 09:25 da WWW.AMAZON.IT."
        // "Hai effettuato un pagamento di 14,90 € con carta *1234 presso ESSELUNGA."
        Rule(
            pattern = Regex(
                """(?i)(?:hai\s+pagato|hai\s+effettuato\s+un\s+pagamento\s+di|pagamento\s+di|spesa\s+di|acquisto\s+di|addebito\s+di)\s*(€|\$|£)?\s*([\d.,]+)\s*(€|\$|£|[A-Z]{3})?\s+.*?\b(?:da|presso)\s+(.+?)\s*$""",
            ),
            amountGroup = 2,
            currencyGroup = 1,
            trailingCurrencyGroup = 3,
            merchantGroup = 4,
        ),
        // Pagamento / Spesa / Acquisto / Addebito di 12,50 EUR presso/da/a X
        Rule(
            pattern = Regex(
                """(?i)(?:hai\s+effettuato\s+un\s+)?(?:pagamento|spesa|acquisto|addebito)\s+di\s*(€|\$|£)?\s*([\d.,]+)\s*([A-Z]{3}|€|\$|£)?(?:\s+(?:presso|da|a)\s+(.+))?""",
            ),
            amountGroup = 2,
            currencyGroup = 1,
            trailingCurrencyGroup = 3,
            merchantGroup = 4,
        ),
        // Hai pagato 12,50 € a/presso/da X (short form, e.g. Revolut / Satispay / Intesa short)
        Rule(
            pattern = Regex(
                """(?i)hai\s+pagato\s*(€|\$|£)?\s*([\d.,]+)\s*(€|EUR|\$|USD|£|GBP|[A-Z]{3})?(?:\s+(?:a|presso|da)\s+(.+))?""",
            ),
            amountGroup = 2,
            currencyGroup = 1,
            trailingCurrencyGroup = 3,
            merchantGroup = 4,
        ),
        // Addebito ... importo 12,50 EUR ... presso/da X
        Rule(
            pattern = Regex(
                """(?i)addebito.*?importo\s*(?:di\s*)?(€|\$|£)?\s*([\d.,]+)\s*([A-Z]{3}|€|\$|£)?(?:.*?\b(?:presso|da)\s+(.+))?""",
            ),
            amountGroup = 2,
            currencyGroup = 1,
            trailingCurrencyGroup = 3,
            merchantGroup = 4,
        ),
        // Bonifico ricevuto / Accredito / Hai ricevuto 100,00 EUR da X
        Rule(
            pattern = Regex(
                """(?i)(?:bonifico\s+ricevuto|accredito|hai\s+ricevuto|rimborso)(?:\s+di)?\s*(€|\$|£)?\s*([\d.,]+)\s*([A-Z]{3}|€|\$|£)?(?:.*?\b(?:da|from)\s+(.+))?""",
            ),
            amountGroup = 2,
            currencyGroup = 1,
            trailingCurrencyGroup = 3,
            merchantGroup = 4,
        ),
        // You paid €12.50 to X / You spent €12.50 at X
        Rule(
            pattern = Regex(
                """(?i)you\s+(?:paid|spent)\s*(€|\$|£)?\s*([\d.,]+)\s*([A-Z]{3}|€|\$|£)?\s*(?:to|at)\s+(.+)""",
            ),
            amountGroup = 2,
            currencyGroup = 1,
            trailingCurrencyGroup = 3,
            merchantGroup = 4,
        ),
        // Payment of $3.20 at X / Payment of 3.20 EUR at X
        Rule(
            pattern = Regex(
                """(?i)payment\s+of\s*(€|\$|£)?\s*([\d.,]+)\s*([A-Z]{3}|€|\$|£)?\s*(?:at|to|for)\s+(.+)""",
            ),
            amountGroup = 2,
            currencyGroup = 1,
            trailingCurrencyGroup = 3,
            merchantGroup = 4,
        ),
        // Google Wallet style: €4.50 at X / 4,50 € presso X
        Rule(
            pattern = Regex(
                """(?i)^(€|\$|£)?\s*([\d.,]+)\s*(€|\$|£|[A-Z]{3})?\s+(?:at|presso|da)\s+(.+)$""",
            ),
            amountGroup = 2,
            currencyGroup = 1,
            trailingCurrencyGroup = 3,
            merchantGroup = 4,
        ),
        // Paid 12.50 EUR to X
        Rule(
            pattern = Regex(
                """(?i)(?:paid|pagamento)\s*(€|\$|£)?\s*([\d.,]+)\s*([A-Z]{3}|€|\$|£)\s*(?:to|at|a|presso|da)\s+(.+)""",
            ),
            amountGroup = 2,
            currencyGroup = 1,
            trailingCurrencyGroup = 3,
            merchantGroup = 4,
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
                val leadingCurrency = rule.currencyGroup?.let { match.groupValues.getOrNull(it) }?.trim()
                val trailingCurrency = rule.trailingCurrencyGroup?.let { match.groupValues.getOrNull(it) }?.trim()
                val currencyRaw = leadingCurrency?.takeIf { it.isNotEmpty() }
                    ?: trailingCurrency?.takeIf { it.isNotEmpty() }
                val currency = resolveCurrency(currencyRaw, rule.defaultCurrency) ?: continue
                val amount = MoneyParser.parse(amountRaw, currency) ?: continue
                val absolute = if (amount.minor < 0) amount.negate() else amount
                val merchant = rule.merchantGroup
                    ?.let { match.groupValues.getOrNull(it) }
                    ?.let(::cleanMerchant)
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
            val amountMatch = Regex("""(€|\$|£)?\s*([\d.,]+)\s*(€|EUR|\$|USD|£|GBP|[A-Z]{3})?""").find(blob)
            if (amountMatch != null) {
                val rawCur = amountMatch.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
                    ?: amountMatch.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }
                val currency = resolveCurrency(rawCur, "EUR") ?: CurrencyCode("EUR")
                val amount = MoneyParser.parse(amountMatch.groupValues[2], currency)
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

    private fun cleanMerchant(raw: String): String? {
        val cleaned = raw
            .lineSequence()
            .firstOrNull()
            .orEmpty()
            .trim()
            .trimEnd('.', ',', ';', '!', ' ')
            .trim()
        // Never treat card/date filler clauses as a merchant name.
        if (cleaned.isEmpty()) return null
        if (cleaned.startsWith("con la carta", ignoreCase = true) ||
            cleaned.startsWith("con carta", ignoreCase = true)
        ) {
            return null
        }
        return cleaned
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
