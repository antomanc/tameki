package dev.antomanc.tameki.core.model.insight

import dev.antomanc.tameki.core.model.CurrencyCode
import kotlinx.serialization.Serializable

/**
 * Tunable thresholds for [SavingsInsights].
 * Kept as data so tests and Settings can change them without forking rule code.
 * Money thresholds are minor units of [thresholdCurrency] (default EUR).
 */
@Serializable
data class InsightRules(
    val priceIncreaseWindowDays: Int = 90,
    val trialEndingDays: Int = 7,
    val categorySpikePercent: Int = 25,
    val categorySpikeMinExtraMinor: Long = 2_000L,
    val smallFrequentMinCount: Int = 8,
    val smallFrequentMaxMinor: Long = 1_000L,
    val smallFrequentSavingPercent: Int = 50,
    val thresholdCurrency: CurrencyCode = CurrencyCode("EUR"),
)
