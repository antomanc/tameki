package dev.antomanc.tameki.core.model.insight

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.budget.Pace
import dev.antomanc.tameki.core.model.budget.PaceStatus
import dev.antomanc.tameki.core.model.entity.Budget
import dev.antomanc.tameki.core.model.entity.BudgetPeriod
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.PricePoint
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.Worth
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Test

class SavingsInsightsTest {
    private val eur = CurrencyCode("EUR")
    private val today = LocalDate.of(2026, 11, 18)

    private fun sub(
        id: Long,
        minor: Long,
        categoryId: Long? = 1L,
        worth: Worth? = null,
        status: RecurringStatus = RecurringStatus.ACTIVE,
        annual: Money? = null,
        history: List<PricePoint> = emptyList(),
        trialEnds: LocalDate? = null,
    ) = Recurring(
        id = id,
        type = RecurringType.SUBSCRIPTION,
        name = "Sub$id",
        amount = Money(minor, eur),
        cycle = Cycle(1, CycleUnit.MONTH),
        startDate = LocalDate.of(2026, 1, 1),
        nextDate = LocalDate.of(2026, 12, 1),
        categoryId = categoryId,
        worth = worth,
        status = status,
        annualAlternative = annual,
        priceHistory = history,
        trialEndsOn = trialEnds,
    )

    @Test
    fun unusedSubscriptionUsesYearlyCost() {
        val insights = SavingsInsights.compute(
            InsightInput(recurrings = listOf(sub(1, 1_000, worth = Worth.UNUSED))),
            today,
        )
        val unused = insights.filterIsInstance<Insight.UnusedSubscription>().single()
        assertThat(unused.yearlySaving).isEqualTo(Money(12_000, eur))
    }

    @Test
    fun overlappingTakesCheapestYearly() {
        val insights = SavingsInsights.compute(
            InsightInput(
                recurrings = listOf(
                    sub(1, 1_000, categoryId = 5),
                    sub(2, 2_000, categoryId = 5),
                ),
            ),
            today,
        )
        val overlap = insights.filterIsInstance<Insight.OverlappingSubscriptions>().single()
        assertThat(overlap.yearlySaving).isEqualTo(Money(12_000, eur))
    }

    @Test
    fun annualPlanOnlyWhenAlternativeLower() {
        val insights = SavingsInsights.compute(
            InsightInput(
                recurrings = listOf(
                    sub(1, 1_200, annual = Money(10_000, eur)),
                ),
            ),
            today,
        )
        val annual = insights.filterIsInstance<Insight.AnnualPlanSaving>().single()
        // yearly of 12.00/mo = 144.00; alt 100.00 → save 44.00
        assertThat(annual.yearlySaving).isEqualTo(Money(4_400, eur))
    }

    @Test
    fun priceIncreaseWithinWindow() {
        val insights = SavingsInsights.compute(
            InsightInput(
                recurrings = listOf(
                    sub(
                        1, 1_500,
                        history = listOf(
                            PricePoint(Money(1_000, eur), LocalDate.of(2026, 1, 1)),
                            PricePoint(Money(1_500, eur), LocalDate.of(2026, 10, 1)),
                        ),
                    ),
                ),
            ),
            today,
        )
        assertThat(insights.filterIsInstance<Insight.PriceIncrease>()).hasSize(1)
    }

    @Test
    fun trialEndingWithinSevenDays() {
        val insights = SavingsInsights.compute(
            InsightInput(
                recurrings = listOf(
                    sub(1, 999, status = RecurringStatus.TRIAL, trialEnds = today.plusDays(3)),
                ),
            ),
            today,
        )
        assertThat(insights.filterIsInstance<Insight.TrialEnding>()).hasSize(1)
    }

    @Test
    fun smallFrequentIsEstimateHalfYearly() {
        val txs = (1..8).map { i ->
            Transaction(
                id = i.toLong(),
                kind = TransactionKind.EXPENSE,
                amount = Money(250, eur),
                accountId = 1,
                merchant = "CoffeeBar",
                occurredAt = LocalDateTime.of(2026, 11, i, 9, 0),
            )
        }
        val insights = SavingsInsights.compute(
            InsightInput(transactionsThisMonth = txs),
            today,
        )
        val frequent = insights.filterIsInstance<Insight.SmallFrequentSpend>().single()
        assertThat(frequent.estimate).isTrue()
        // monthly 20.00 → yearly 240.00 → 50% = 120.00
        assertThat(frequent.yearlySaving).isEqualTo(Money(12_000, eur))
    }

    @Test
    fun budgetAtRiskAndNoSpendStreak() {
        val budget = Budget(id = 9, name = "All", limit = Money(1000, eur), period = BudgetPeriod.MONTH)
        val pace = Pace(
            spent = Money(900, eur),
            limit = Money(1000, eur),
            projected = Money(2000, eur),
            status = PaceStatus.AT_RISK,
        )
        val insights = SavingsInsights.compute(
            InsightInput(
                budgets = listOf(budget),
                budgetPaces = mapOf(9L to pace),
                noSpendStreakDays = 4,
            ),
            today,
        )
        assertThat(insights.filterIsInstance<Insight.BudgetAtRisk>()).hasSize(1)
        assertThat(insights.filterIsInstance<Insight.NoSpendStreak>().single().days).isEqualTo(4)
    }

    @Test
    fun sortsByYearlySavingDescending() {
        val insights = SavingsInsights.compute(
            InsightInput(
                recurrings = listOf(
                    sub(1, 500, worth = Worth.UNUSED),
                    sub(2, 2_000, worth = Worth.RARELY, categoryId = 2),
                ),
            ),
            today,
        )
        val unused = insights.filterIsInstance<Insight.UnusedSubscription>()
        assertThat(unused.first().recurringId).isEqualTo(2L)
    }
}
