package dev.antomanc.tameki.core.model.budget

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Budget
import dev.antomanc.tameki.core.model.entity.BudgetPeriod
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.TransactionSource
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Test

class AvailableToSpendTest {
    private val eur = CurrencyCode("EUR")
    private val today = LocalDate.of(2026, 11, 18)

    @Test
    fun budgetMinusSpentMinusUpcoming() {
        val result = AvailableToSpendCalculator.compute(
            budgets = listOf(
                Budget(1, "Month", limit = Money(100_000, eur), period = BudgetPeriod.MONTH),
            ),
            transactionsThisMonth = listOf(
                expense(40_000),
            ),
            recurrings = listOf(
                recurring(amount = 5_000, next = today.plusDays(2)),
            ),
            today = today,
            baseCurrency = eur,
        )
        assertThat(result.mode).isEqualTo(HeroMode.AVAILABLE)
        assertThat(result.amount.minor).isEqualTo(55_000)
        assertThat(result.upcomingRecurring.minor).isEqualTo(5_000)
    }

    @Test
    fun incomeUsedWhenNoBudget() {
        val result = AvailableToSpendCalculator.compute(
            budgets = emptyList(),
            transactionsThisMonth = listOf(
                income(200_000),
                expense(30_000),
            ),
            recurrings = emptyList(),
            today = today,
            baseCurrency = eur,
        )
        assertThat(result.mode).isEqualTo(HeroMode.AVAILABLE)
        assertThat(result.amount.minor).isEqualTo(170_000)
    }

    @Test
    fun spentOnlyWhenNoBudgetAndNoIncome() {
        val result = AvailableToSpendCalculator.compute(
            budgets = emptyList(),
            transactionsThisMonth = listOf(expense(12_500)),
            recurrings = emptyList(),
            today = today,
            baseCurrency = eur,
        )
        assertThat(result.mode).isEqualTo(HeroMode.SPENT_ONLY)
        assertThat(result.amount.minor).isEqualTo(12_500)
    }

    @Test
    fun openingBalanceNeverBecomesASpendingPool() {
        // Onboarding's "current balance" is a snapshot of the bank account, not this month's
        // earnings: €3,000 in the bank must not print "€3,000 available to spend" and then vanish.
        val result = AvailableToSpendCalculator.compute(
            budgets = emptyList(),
            transactionsThisMonth = listOf(
                income(300_000).copy(source = TransactionSource.OPENING_BALANCE),
                expense(12_500),
            ),
            recurrings = emptyList(),
            today = today,
            baseCurrency = eur,
        )
        assertThat(result.mode).isEqualTo(HeroMode.SPENT_ONLY)
        assertThat(result.incomeThisMonth.isZero).isTrue()
        assertThat(result.amount.minor).isEqualTo(12_500)
    }

    @Test
    fun categoryBudgetIsNotAGlobalPool() {
        // A €350 groceries budget must never subtract the whole month's spending (and rent) from
        // €350: with no income and no global budget the hero shows spent alone, not −€161.
        val result = AvailableToSpendCalculator.compute(
            budgets = listOf(
                Budget(
                    id = 1,
                    name = "Groceries",
                    categoryIds = setOf(7L),
                    limit = Money(35_000, eur),
                    period = BudgetPeriod.MONTH,
                ),
            ),
            transactionsThisMonth = listOf(
                expense(35_000),
                expense(51_200),
            ),
            recurrings = emptyList(),
            today = today,
            baseCurrency = eur,
        )
        assertThat(result.mode).isEqualTo(HeroMode.SPENT_ONLY)
        assertThat(result.amount.minor).isEqualTo(86_200)
        assertThat(result.budgetLimit).isNull()
    }

    @Test
    fun onlyUnscopedBudgetsFormThePool() {
        val result = AvailableToSpendCalculator.compute(
            budgets = listOf(
                Budget(
                    id = 1,
                    name = "Groceries",
                    categoryIds = setOf(7L),
                    limit = Money(35_000, eur),
                    period = BudgetPeriod.MONTH,
                ),
                Budget(
                    id = 2,
                    name = "Month",
                    limit = Money(100_000, eur),
                    period = BudgetPeriod.MONTH,
                ),
            ),
            transactionsThisMonth = listOf(expense(40_000)),
            recurrings = emptyList(),
            today = today,
            baseCurrency = eur,
        )
        assertThat(result.mode).isEqualTo(HeroMode.AVAILABLE)
        assertThat(result.budgetLimit?.minor).isEqualTo(100_000)
        assertThat(result.amount.minor).isEqualTo(60_000)
    }

    @Test
    fun weeklyBudgetIsIncludedAsMonthEquivalent() {
        // €100/week in a 30-day month is 30/7 weeks: €428,57. Ignoring it used to send the hero
        // to SPENT_ONLY (or the income pool) as if the budget did not exist.
        val result = AvailableToSpendCalculator.compute(
            budgets = listOf(
                Budget(
                    id = 1,
                    name = "Week",
                    limit = Money(10_000, eur),
                    period = BudgetPeriod.WEEK,
                ),
            ),
            transactionsThisMonth = listOf(expense(20_000)),
            recurrings = emptyList(),
            today = today,
            baseCurrency = eur,
        )
        assertThat(result.mode).isEqualTo(HeroMode.AVAILABLE)
        assertThat(result.budgetLimit?.minor).isEqualTo(42_857)
        assertThat(result.amount.minor).isEqualTo(22_857)
    }

    @Test
    fun weeklyAndMonthlyGlobalBudgetsBothCount() {
        val result = AvailableToSpendCalculator.compute(
            budgets = listOf(
                Budget(1, "Week", limit = Money(7_000, eur), period = BudgetPeriod.WEEK),
                Budget(2, "Month", limit = Money(100_000, eur), period = BudgetPeriod.MONTH),
            ),
            transactionsThisMonth = emptyList(),
            recurrings = emptyList(),
            today = today,
            baseCurrency = eur,
        )
        // 7000 × 30 / 7 = 30 000, plus the monthly 100 000.
        assertThat(result.budgetLimit?.minor).isEqualTo(130_000)
    }

    @Test
    fun expectedByTodayIsLinear() {
        // day 18 of 30 → 18/30 of 90_000 = 54_000
        val shortMonth = LocalDate.of(2026, 11, 18)
        val expected = AvailableToSpendCalculator.expectedByToday(Money(90_000, eur), shortMonth)
        assertThat(expected.minor).isEqualTo(54_000)
    }

    private fun expense(minor: Long) = Transaction(
        id = minor,
        kind = TransactionKind.EXPENSE,
        amount = Money(minor, eur),
        accountId = 1,
        occurredAt = LocalDateTime.of(2026, 11, 10, 12, 0),
    )

    private fun income(minor: Long) = Transaction(
        id = minor,
        kind = TransactionKind.INCOME,
        amount = Money(minor, eur),
        accountId = 1,
        occurredAt = LocalDateTime.of(2026, 11, 1, 9, 0),
    )

    private fun recurring(amount: Long, next: LocalDate) = Recurring(
        id = amount,
        type = RecurringType.SUBSCRIPTION,
        name = "Sub",
        amount = Money(amount, eur),
        cycle = Cycle(1, CycleUnit.MONTH),
        startDate = next.minusMonths(2),
        nextDate = next,
    )
}
