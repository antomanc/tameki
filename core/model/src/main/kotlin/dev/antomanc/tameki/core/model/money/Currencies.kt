package dev.antomanc.tameki.core.model.money

import dev.antomanc.tameki.core.model.CurrencyCode
import java.util.Currency

/**
 * Fraction-digit lookup for ISO currencies.
 * Keeps Money in minor units without hard-coding every code; unknown codes fall back to 2
 * so a typo never crashes import paths.
 */
object Currencies {
    fun fractionDigits(currency: CurrencyCode): Int =
        try {
            Currency.getInstance(currency.value).defaultFractionDigits.coerceAtLeast(0)
        } catch (_: IllegalArgumentException) {
            2
        }
}
