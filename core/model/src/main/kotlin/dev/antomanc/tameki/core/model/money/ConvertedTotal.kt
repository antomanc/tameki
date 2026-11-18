package dev.antomanc.tameki.core.model.money

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import java.time.LocalDate

/**
 * A sum in [base] plus any amounts that could not be converted (honest, never dropped).
 */
data class ConvertedTotal(
    val converted: Money,
    val unconverted: List<Money> = emptyList(),
) {
    companion object {
        fun empty(base: CurrencyCode) = ConvertedTotal(Money.zero(base))
    }
}

/**
 * Converts each [amounts] entry into [base] via [rates] on [on]. Same-currency amounts add
 * directly; missing rates accumulate in [ConvertedTotal.unconverted] (merged per currency).
 */
suspend fun sumConverting(
    amounts: Iterable<Money>,
    base: CurrencyCode,
    on: LocalDate,
    rates: ExchangeRates,
): ConvertedTotal {
    var converted = Money.zero(base)
    val unconverted = linkedMapOf<CurrencyCode, Long>()
    for (amount in amounts) {
        if (amount.currency == base) {
            converted += amount
            continue
        }
        val asBase = rates.convert(amount, base, on)
        if (asBase != null) {
            converted += asBase
        } else {
            unconverted[amount.currency] =
                (unconverted[amount.currency] ?: 0L) + amount.minor
        }
    }
    return ConvertedTotal(
        converted = converted,
        unconverted = unconverted.map { (currency, minor) -> Money(minor, currency) },
    )
}
