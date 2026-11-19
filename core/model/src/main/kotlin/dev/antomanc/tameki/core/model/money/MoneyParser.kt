package dev.antomanc.tameki.core.model.money

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale

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

    /**
     * The decimal separator of [locale], which is what the user's numeric keyboard offers. Passed
     * explicitly in tests so an "1.000" fixture means the same thing on every machine.
     */
    fun decimalSeparator(locale: Locale = Locale.getDefault()): Char =
        DecimalFormatSymbols.getInstance(locale).decimalSeparator

    /**
     * @param decimalSeparator the locale's decimal mark. It decides whether a lone three-digit tail
     *   is a decimal ("12,500" → 12.50 in Italian) or a thousands group ("12,500" → 12500 in
     *   English). Defaults to the device locale.
     */
    fun parse(
        text: String,
        currency: CurrencyCode,
        decimalSeparator: Char = decimalSeparator(),
    ): Money? {
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
        val decimal = normalizeDecimal(working, digits, decimalSeparator) ?: return null
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
     * Parses an exchange rate or other plain decimal: locale-aware, positive or negative, and
     * independent of any currency's fraction digits. "1,08" on an Italian phone and "1.08" on an
     * English one both become [BigDecimal] 1.08.
     */
    fun parseDecimal(
        text: String,
        decimalSeparator: Char = decimalSeparator(),
    ): BigDecimal? {
        var working = text.trim()
        if (working.isEmpty()) return null
        val negative = working.startsWith('-')
        if (negative || working.startsWith('+')) {
            working = working.substring(1).trim()
        }
        val filtered = working.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
        if (filtered.isEmpty() || filtered.none { it.isDigit() }) return null
        val cleaned = normalizeDecimal(filtered, fractionDigits = 4, decimalSeparator = decimalSeparator)
            ?: return null
        return if (negative) cleaned.negate() else cleaned
    }

    /**
     * Detects whether ',' or '.' is the decimal separator.
     *
     * When both appear, the last one is the decimal separator and the other is grouping
     * ("1.234,56" in Italian, "1,234.56" in English). With a single separator the honest test is
     * the locale's decimal mark plus the number of digits after it: a currency's own fraction
     * digits (2 for EUR, 0 for JPY) mean decimal, and a three-digit tail is a thousands group only
     * when the mark is the grouping character ("1.000" in Italian) — except for zero-decimal
     * currencies, where "1.234" is always a thousand. "12,500" on an Italian phone is 12.50, not
     * twelve thousand five hundred; the grouping mark there is the dot.
     */
    private fun normalizeDecimal(
        raw: String,
        fractionDigits: Int,
        decimalSeparator: Char,
    ): BigDecimal? {
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
            lastComma >= 0 -> normalizeSingleSeparator(raw, ',', fractionDigits, decimalSeparator)
            lastDot >= 0 -> normalizeSingleSeparator(raw, '.', fractionDigits, decimalSeparator)
            else -> raw
        } ?: return null
        return try {
            BigDecimal(cleaned)
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun normalizeSingleSeparator(
        raw: String,
        separator: Char,
        fractionDigits: Int,
        decimalSeparator: Char,
    ): String? {
        val occurrences = raw.count { it == separator }
        val after = raw.length - raw.lastIndexOf(separator) - 1
        val separatorIsDecimal = separator == decimalSeparator
        if (occurrences > 1) {
            // Repeated separators are grouping ("1,234,567", "1.234.567"). A repeated locale
            // decimal mark is not formatting; only a three-digit tail is a recoverable group.
            return if (!separatorIsDecimal || after == 3) {
                raw.replace(separator.toString(), "")
            } else {
                null
            }
        }
        return when {
            // 1,50 / 1.50: a currency's own fraction digits win even when the mark is unusual.
            after in 1..fractionDigits -> raw.replace(separator, '.')
            // 1.234 yen is a thousand yen: a zero-decimal currency never has a decimal tail.
            fractionDigits == 0 && after == 3 -> raw.replace(separator.toString(), "")
            // On an Italian phone the dot groups and the comma is the decimal mark: "12,500" is
            // 12.50, while "12.500" is twelve thousand five hundred.
            separatorIsDecimal && after > 0 -> raw.replace(separator, '.')
            // "1,000" / "1.000" with the grouping mark: a thousands group, not 1.00.
            after == 3 -> raw.replace(separator.toString(), "")
            // Four or more digits cannot be a group; round as decimals (e.g. 1.2345).
            else -> raw.replace(separator, '.')
        }
    }
}
