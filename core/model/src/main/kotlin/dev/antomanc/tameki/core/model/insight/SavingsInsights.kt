package dev.antomanc.tameki.core.model.insight

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.budget.PaceStatus
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.Worth
import dev.antomanc.tameki.core.model.recurring.CostNormalizer
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Builds the ranked "Risparmia" list.
 * Every yearly figure comes from user-entered prices or observed spend — never invented annual plans.
 */
object SavingsInsights {
    fun compute(input: InsightInput, today: LocalDate): List<Insight> {
        val rules = input.rules
        val currency = rules.thresholdCurrency
        val insights = mutableListOf<Insight>()

        val activeSubs = input.recurrings.filter {
            it.type == RecurringType.SUBSCRIPTION &&
                (it.status == RecurringStatus.ACTIVE || it.status == RecurringStatus.TRIAL)
        }

        for (r in activeSubs) {
            val worth = r.worth
            if (worth == Worth.UNUSED || worth == Worth.RARELY) {
                insights += Insight.UnusedSubscription(
                    recurringId = r.id,
                    worth = worth,
                    yearlySaving = CostNormalizer.yearly(r),
                )
            }
        }

        activeSubs
            .filter { it.categoryId != null && it.status == RecurringStatus.ACTIVE }
            .groupBy { it.categoryId }
            .forEach { (categoryId, group) ->
                if (categoryId != null && group.size >= 2) {
                    val yearlies = group.map { it to CostNormalizer.yearly(it) }
                    val cheapest = yearlies.minBy { it.second.minor }.second
                    insights += Insight.OverlappingSubscriptions(
                        recurringIds = group.map { it.id },
                        categoryId = categoryId,
                        yearlySaving = cheapest,
                    )
                }
            }

        for (r in activeSubs) {
            val alt = r.annualAlternative ?: continue
            if (alt.currency != r.amount.currency) continue
            val currentYearly = CostNormalizer.yearly(r)
            if (alt.minor < currentYearly.minor) {
                insights += Insight.AnnualPlanSaving(
                    recurringId = r.id,
                    yearlySaving = currentYearly - alt,
                )
            }
        }

        for (r in activeSubs) {
            val increase = priceIncrease(r, today, rules) ?: continue
            insights += increase
        }

        for (r in input.recurrings) {
            if (r.status != RecurringStatus.TRIAL) continue
            val ends = r.trialEndsOn ?: continue
            val days = java.time.temporal.ChronoUnit.DAYS.between(today, ends)
            if (days in 0..rules.trialEndingDays.toLong()) {
                insights += Insight.TrialEnding(
                    recurringId = r.id,
                    endsOn = ends,
                    yearlySaving = CostNormalizer.yearly(r),
                )
            }
        }

        insights += categorySpikes(input, currency, rules)
        insights += smallFrequent(input, currency, rules)

        for (budget in input.budgets) {
            val pace = input.budgetPaces[budget.id] ?: continue
            if (pace.status == PaceStatus.AT_RISK || pace.status == PaceStatus.OVER) {
                insights += Insight.BudgetAtRisk(budgetId = budget.id, pace = pace)
            }
        }

        if (input.noSpendStreakDays > 0) {
            insights += Insight.NoSpendStreak(days = input.noSpendStreakDays)
        }

        return insights.sortedWith(
            compareByDescending<Insight> { it.yearlySaving?.minor ?: Long.MIN_VALUE }
                .thenByDescending { it.severity.ordinal },
        )
    }

    private fun priceIncrease(r: Recurring, today: LocalDate, rules: InsightRules): Insight.PriceIncrease? {
        val history = r.priceHistory.sortedBy { it.since }
        if (history.size < 2) return null
        val latest = history.last()
        val previous = history[history.lastIndex - 1]
        if (latest.amount.currency != previous.amount.currency) return null
        if (latest.amount.minor <= previous.amount.minor) return null
        val age = java.time.temporal.ChronoUnit.DAYS.between(latest.since, today)
        if (age > rules.priceIncreaseWindowDays) return null
        val deltaPerPeriod = latest.amount - previous.amount
        val bumped = r.copy(amount = deltaPerPeriod)
        return Insight.PriceIncrease(
            recurringId = r.id,
            old = previous.amount,
            new = latest.amount,
            yearlySaving = CostNormalizer.yearly(bumped),
        )
    }

    private fun categorySpikes(
        input: InsightInput,
        currency: CurrencyCode,
        rules: InsightRules,
    ): List<Insight> {
        val thisMonth = sumByCategory(input.transactionsThisMonth)
        val trailing = sumByCategory(input.transactionsTrailingThreeMonths)
        val out = mutableListOf<Insight>()
        for ((categoryId, monthTotal) in thisMonth) {
            if (monthTotal.currency != currency) continue
            val trail = trailing[categoryId] ?: continue
            if (trail.currency != currency || trail.minor <= 0L) continue
            val averageMinor = BigDecimal.valueOf(trail.minor)
                .divide(BigDecimal.valueOf(3L), 0, RoundingMode.HALF_EVEN)
                .longValueExact()
            if (averageMinor <= 0L) continue
            val extra = monthTotal.minor - averageMinor
            val percent = BigDecimal.valueOf(extra)
                .multiply(BigDecimal.valueOf(100L))
                .divide(BigDecimal.valueOf(averageMinor), 0, RoundingMode.HALF_EVEN)
                .longValueExact()
            if (percent > rules.categorySpikePercent && extra > rules.categorySpikeMinExtraMinor) {
                out += Insight.CategorySpike(
                    categoryId = categoryId,
                    thisMonth = monthTotal,
                    average = Money(averageMinor, currency),
                )
            }
        }
        return out
    }

    private fun smallFrequent(
        input: InsightInput,
        currency: CurrencyCode,
        rules: InsightRules,
    ): List<Insight> {
        val expenses = input.transactionsThisMonth.filter {
            it.kind == TransactionKind.EXPENSE && it.amount.currency == currency
        }
        val byKey = expenses.groupBy { tx ->
            tx.merchant?.takeIf { it.isNotBlank() } ?: "category:${tx.categoryId}"
        }
        val out = mutableListOf<Insight>()
        for ((key, group) in byKey) {
            if (group.size < rules.smallFrequentMinCount) continue
            if (group.any { it.amount.minor >= rules.smallFrequentMaxMinor }) continue
            if (!group.all { it.amount.minor < rules.smallFrequentMaxMinor }) continue
            val monthlyMinor = group.sumOf { it.amount.minor }
            val monthly = Money(monthlyMinor, currency)
            val yearly = Money(monthlyMinor * 12, currency)
            val saving = Money(
                BigDecimal.valueOf(yearly.minor)
                    .multiply(BigDecimal.valueOf(rules.smallFrequentSavingPercent.toLong()))
                    .divide(BigDecimal.valueOf(100L), 0, RoundingMode.HALF_EVEN)
                    .longValueExact(),
                currency,
            )
            out += Insight.SmallFrequentSpend(
                merchantOrCategory = key,
                count = group.size,
                monthly = monthly,
                yearlySaving = saving,
            )
        }
        return out
    }

    private fun sumByCategory(transactions: List<dev.antomanc.tameki.core.model.entity.Transaction>): Map<Long, Money> {
        val map = mutableMapOf<Long, Money>()
        for (tx in transactions) {
            if (tx.kind != TransactionKind.EXPENSE) continue
            // A split transaction counts towards each of its categories, not its headline one.
            val parts = if (tx.splits.isNotEmpty()) {
                tx.splits.map { it.categoryId to it.amount }
            } else {
                listOfNotNull(tx.categoryId?.let { it to tx.amount })
            }
            for ((categoryId, amount) in parts) {
                val current = map[categoryId]
                map[categoryId] = if (current == null) amount else current + amount
            }
        }
        return map
    }

    /**
     * The headline "you could save" figure. Several insights can point at the same subscription
     * (unused, overlapping, trial ending), so each recurring payment counts once, at its largest
     * saving; overlaps count only for subscriptions not already counted.
     */
    fun totalYearlySaving(insights: List<Insight>, currency: CurrencyCode): Money {
        val perRecurring = mutableMapOf<Long, Long>()
        var other = 0L
        // Single-subscription insights first, so an overlap never double-counts one of them.
        for (insight in insights.sortedBy { it is Insight.OverlappingSubscriptions }) {
            val saving = insight.yearlySaving ?: continue
            if (saving.currency != currency) continue
            val ids = when (insight) {
                is Insight.UnusedSubscription -> listOf(insight.recurringId)
                is Insight.AnnualPlanSaving -> listOf(insight.recurringId)
                is Insight.PriceIncrease -> listOf(insight.recurringId)
                is Insight.TrialEnding -> listOf(insight.recurringId)
                is Insight.OverlappingSubscriptions -> insight.recurringIds
                else -> emptyList()
            }
            if (ids.isEmpty()) {
                other += saving.minor
            } else if (insight is Insight.OverlappingSubscriptions) {
                if (ids.none { it in perRecurring }) perRecurring[ids.min()] = saving.minor
            } else {
                val id = ids.single()
                perRecurring[id] = maxOf(perRecurring[id] ?: 0L, saving.minor)
            }
        }
        return Money(other + perRecurring.values.sum(), currency)
    }
}
