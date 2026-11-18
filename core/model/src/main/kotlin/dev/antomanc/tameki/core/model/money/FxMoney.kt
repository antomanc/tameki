package dev.antomanc.tameki.core.model.money

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import java.time.LocalDate

/**
 * Converts [money] to [base] when needed. Returns the converted amount, or null when the rate is
 * missing (caller must surface it as unconverted — never treat as zero).
 */
suspend fun ExchangeRates.toBaseOrNull(
    money: Money,
    base: CurrencyCode,
    on: LocalDate,
): Money? {
    if (money.currency == base) return money
    return convert(money, base, on)
}

/** Copy helper: replace amount with a converted one when a rate exists. */
suspend fun ExchangeRates.convertOrKeep(
    money: Money,
    base: CurrencyCode,
    on: LocalDate,
): Pair<Money, Money?> {
    if (money.currency == base) return money to null
    val converted = convert(money, base, on)
    return if (converted != null) converted to null else money to money
}
