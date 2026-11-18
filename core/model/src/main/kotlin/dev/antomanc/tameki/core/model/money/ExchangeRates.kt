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

    /** User-supplied rate: how many units of [to] equal 1 unit of [from] (decimal string, e.g. "1.08"). */
    suspend fun setManualRate(from: CurrencyCode, to: CurrencyCode, rate: String)

    /** Best-effort refresh of ECB/Frankfurter daily rates; never throws to callers that ignore the result. */
    suspend fun refreshRemoteRates(base: CurrencyCode = CurrencyCode("EUR")): Boolean
}
