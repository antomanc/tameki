package dev.antomanc.tameki.feature.home

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
