package dev.antomanc.tameki.core.model.insight

import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.budget.Pace
import dev.antomanc.tameki.core.model.entity.Worth
import java.time.LocalDate

enum class Severity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
}

/**
 * A ranked savings opportunity or positive streak.
 * [yearlySaving] is null only for reinforcement insights that must not invent a euro figure.
 */
sealed interface Insight {
    val id: String
    val yearlySaving: Money?
    val severity: Severity

    data class UnusedSubscription(
        val recurringId: Long,
        val worth: Worth,
        override val yearlySaving: Money,
        override val severity: Severity = Severity.HIGH,
    ) : Insight {
        override val id: String get() = "unused-$recurringId"
    }

    data class OverlappingSubscriptions(
        val recurringIds: List<Long>,
        val categoryId: Long,
        override val yearlySaving: Money,
        override val severity: Severity = Severity.MEDIUM,
    ) : Insight {
        override val id: String get() = "overlap-$categoryId-${recurringIds.sorted().joinToString("-")}"
    }

    data class AnnualPlanSaving(
        val recurringId: Long,
        override val yearlySaving: Money,
        override val severity: Severity = Severity.MEDIUM,
    ) : Insight {
        override val id: String get() = "annual-$recurringId"
    }

    data class PriceIncrease(
        val recurringId: Long,
        val old: Money,
        val new: Money,
        override val yearlySaving: Money,
        override val severity: Severity = Severity.MEDIUM,
    ) : Insight {
        override val id: String get() = "price-$recurringId"
    }

    data class TrialEnding(
        val recurringId: Long,
        val endsOn: LocalDate,
        override val yearlySaving: Money,
        override val severity: Severity = Severity.HIGH,
    ) : Insight {
        override val id: String get() = "trial-$recurringId"
    }

    data class CategorySpike(
        val categoryId: Long,
        val thisMonth: Money,
        val average: Money,
        /** Null on purpose: one unusual month is not a yearly habit. The UI shows [thisMonth] vs [average]. */
        override val yearlySaving: Money? = null,
        override val severity: Severity = Severity.MEDIUM,
    ) : Insight {
        override val id: String get() = "spike-$categoryId"
    }

    data class SmallFrequentSpend(
        val merchantOrCategory: String,
        val count: Int,
        val monthly: Money,
        override val yearlySaving: Money,
        override val severity: Severity = Severity.LOW,
        val estimate: Boolean = true,
    ) : Insight {
        override val id: String get() = "frequent-$merchantOrCategory"
    }

    data class BudgetAtRisk(
        val budgetId: Long,
        val pace: Pace,
        override val yearlySaving: Money? = null,
        override val severity: Severity = Severity.HIGH,
    ) : Insight {
        override val id: String get() = "budget-$budgetId"
    }

    data class NoSpendStreak(
        val days: Int,
        override val yearlySaving: Money? = null,
        override val severity: Severity = Severity.INFO,
    ) : Insight {
        override val id: String get() = "nostreak-$days"
    }
}
