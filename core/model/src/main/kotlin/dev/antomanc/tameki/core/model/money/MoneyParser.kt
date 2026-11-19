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

        val digits = Currencies.fractionDigits(currency)
        val decimal = normalizeDecimal(working, digits) ?: return null
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
     * Detects whether ',' or '.' is the decimal separator.
     *
     * When both appear, the last one is the decimal separator and the other is grouping
     * ("1.234,56" in Italian, "1,234.56" in English). With a single separator the honest test is
     * the number of digits after it: a currency's own fraction digits (2 for EUR, 0 for JPY)
     * mean decimal, while a three-digit tail is a thousands group ("1,000", "1.000"). Three-digit
     * tails are only decimal for currencies that actually use three fraction digits (BHD, KWD).
     */
    private fun normalizeDecimal(raw: String, fractionDigits: Int): BigDecimal? {
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
            lastComma >= 0 -> normalizeSingleSeparator(raw, ',', fractionDigits)
            lastDot >= 0 -> normalizeSingleSeparator(raw, '.', fractionDigits)
            else -> raw
        } ?: return null
        return try {
            BigDecimal(cleaned)
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun normalizeSingleSeparator(raw: String, separator: Char, fractionDigits: Int): String? {
        val occurrences = raw.count { it == separator }
        val after = raw.length - raw.lastIndexOf(separator) - 1
        if (occurrences > 1) {
            // Repeated separators can only be grouping ("1,234,567", "1.234.567").
            // A last group that is not three digits is not a number this parser can recover.
            return if (after == 3) raw.replace(separator.toString(), "") else null
        }
        return when {
            after in 1..fractionDigits -> raw.replace(separator, '.')
            // 1,000 / 1.000: a three-digit tail is a group, not 1.00.
            after == 3 -> raw.replace(separator.toString(), "")
            // Four or more digits cannot be a group; round as decimals (e.g. 1.2345).
            else -> raw.replace(separator, '.')
        }
    }
}
