package dev.antomanc.tameki.core.model.money

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import java.time.LocalDate

/**
 * Converts Money across currencies for aggregations and EUR-equivalent insight thresholds.
 * Implementations live in core:data (manual table + optional ECB/Frankfurter cache).
 */
interface ExchangeRates {
    suspend fun convert(money: Money, to: CurrencyCode, on: LocalDate): Money?
}
