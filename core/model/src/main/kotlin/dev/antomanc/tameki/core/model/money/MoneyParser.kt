package dev.antomanc.tameki.core.model.money

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Locale-tolerant amount parsing for OCR, notifications and AI paste.
 * Users type "12,50" or "1.234,56"; inventing floating point here would lose cents.
 */
object MoneyParser {
    private val currencySymbols = mapOf(
        '€' to "EUR",
        '$' to "USD",
        '£' to "GBP",
        '¥' to "JPY",
    )

    fun parse(text: String, currency: CurrencyCode): Money? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        var working = trimmed
        var negative = false
        if (working.startsWith('-') || working.startsWith('+')) {
            negative = working.startsWith('-')
            working = working.substring(1).trim()
        }
        if (working.startsWith('(') && working.endsWith(')')) {
            negative = true
            working = working.substring(1, working.length - 1).trim()
        }

        working = working.filter { ch ->
            ch.isDigit() || ch == '.' || ch == ',' || ch in currencySymbols
        }
        if (working.isEmpty()) return null

        for ((symbol, _) in currencySymbols) {
            working = working.replace(symbol.toString(), "")
        }
        working = working.trim()
        if (working.isEmpty()) return null

        val decimal = normalizeDecimal(working) ?: return null
        val digits = Currencies.fractionDigits(currency)
        val scaled = try {
            decimal.setScale(digits, RoundingMode.HALF_EVEN)
        } catch (_: ArithmeticException) {
            return null
        }
        val minor = try {
            scaled.movePointRight(digits).longValueExact()
        } catch (_: ArithmeticException) {
            return null
        }
        val signed = if (negative) -minor else minor
        return Money(signed, currency)
    }

    /**
     * Detects whether ',' or '.' is the decimal separator by looking at the last separator
     * and typical thousand-grouping patterns from IT and EN locales.
     */
    private fun normalizeDecimal(raw: String): BigDecimal? {
        val lastComma = raw.lastIndexOf(',')
        val lastDot = raw.lastIndexOf('.')
        val cleaned = when {
            lastComma >= 0 && lastDot >= 0 -> {
                if (lastComma > lastDot) {
                    // 1.234,56
                    raw.replace(".", "").replace(',', '.')
                } else {
                    // 1,234.56
                    raw.replace(",", "")
                }
            }
            lastComma >= 0 -> {
                val after = raw.length - lastComma - 1
                if (after == 3 && raw.count { it == ',' } > 1) {
                    // 1,234,567 thousand separators only
                    raw.replace(",", "")
                } else if (after in 1..2 || raw.count { it == ',' } == 1) {
                    raw.replace(',', '.')
                } else {
                    raw.replace(",", "")
                }
            }
            lastDot >= 0 -> {
                val after = raw.length - lastDot - 1
                if (after == 3 && raw.count { it == '.' } > 1) {
                    raw.replace(".", "")
                } else {
                    raw
                }
            }
            else -> raw
        }
        return try {
            BigDecimal(cleaned)
        } catch (_: NumberFormatException) {
            null
        }
    }
}
