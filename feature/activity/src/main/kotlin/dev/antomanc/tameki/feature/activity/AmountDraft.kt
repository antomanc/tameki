package dev.antomanc.tameki.feature.activity

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.money.Currencies

/**
 * Numpad buffer for the quick-add sheet. Digits accumulate as a decimal string; [toMoney] converts
 * to minor units without ever touching floating point.
 */
data class AmountDraft(
    val integer: String = "0",
    val fraction: String = "",
    val hasDecimal: Boolean = false,
) {
    fun appendDigit(digit: Char): AmountDraft {
        require(digit in '0'..'9')
        return if (!hasDecimal) {
            val next = if (integer == "0") digit.toString() else integer + digit
            if (next.length > 12) this else copy(integer = next)
        } else {
            // Fraction length is capped per currency when converting; keep a generous buffer here.
            if (fraction.length >= 6) this else copy(fraction = fraction + digit)
        }
    }

    fun appendDecimal(): AmountDraft = if (hasDecimal) this else copy(hasDecimal = true)

    fun backspace(): AmountDraft = when {
        hasDecimal && fraction.isNotEmpty() -> copy(fraction = fraction.dropLast(1))
        hasDecimal -> copy(hasDecimal = false)
        integer.length <= 1 -> copy(integer = "0")
        else -> copy(integer = integer.dropLast(1))
    }

    fun toMoney(currency: CurrencyCode): Money {
        val digits = Currencies.fractionDigits(currency)
        val frac = fraction.padEnd(digits, '0').take(digits)
        val combined = if (digits == 0) integer else integer + frac
        val minor = combined.toLongOrNull() ?: 0L
        return Money(minor, currency)
    }

    companion object {
        fun fromMoney(money: Money): AmountDraft {
            val digits = Currencies.fractionDigits(money.currency)
            if (digits == 0) {
                return AmountDraft(integer = money.minor.toString())
            }
            val raw = money.minor.toString().padStart(digits + 1, '0')
            val integer = raw.dropLast(digits).ifEmpty { "0" }
            val fraction = raw.takeLast(digits).trimEnd('0')
            return AmountDraft(
                integer = integer,
                fraction = fraction,
                hasDecimal = fraction.isNotEmpty(),
            )
        }
    }
}
