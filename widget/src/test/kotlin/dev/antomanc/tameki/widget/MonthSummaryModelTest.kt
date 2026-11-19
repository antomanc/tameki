package dev.antomanc.tameki.widget

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.budget.HeroMode
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

class MonthSummaryModelTest {
    @Test
    fun availableIsPoolMinusSpent() {
        val eur = CurrencyCode("EUR")
        val data = MonthSummaryModel.compute(
            budgets = listOf(Budget(1, "Mese", limit = Money(100_00, eur), period = BudgetPeriod.MONTH)),
            transactionsThisMonth = listOf(
                Transaction(
                    kind = TransactionKind.EXPENSE,
                    amount = Money(20_00, eur),
                    accountId = 1,
                    occurredAt = LocalDateTime.of(2026, 11, 5, 12, 0),
                ),
            ),
            recurrings = emptyList(),
            today = LocalDate.of(2026, 11, 18),
            base = eur,
        )
        assertThat(data.available.minor).isEqualTo(80_00)
        assertThat(data.mode).isEqualTo(HeroMode.AVAILABLE)
    }

    @Test
    fun upcomingRecurringIsDeductedLikeToday() {
        val eur = CurrencyCode("EUR")
        val today = LocalDate.of(2026, 11, 18)
        val data = MonthSummaryModel.compute(
            budgets = listOf(Budget(1, "Mese", limit = Money(100_00, eur), period = BudgetPeriod.MONTH)),
            transactionsThisMonth = listOf(expense(20_00, eur, today)),
            recurrings = listOf(recurring(5_00, eur, today.plusDays(2))),
            today = today,
            base = eur,
        )
        // 100 − 20 spent − 5 still due this month, exactly like the Today hero.
        assertThat(data.available.minor).isEqualTo(75_00)
    }

    @Test
    fun noBudgetAndNoIncomeShowsSpentInsteadOfFakeNegative() {
        val eur = CurrencyCode("EUR")
        val today = LocalDate.of(2026, 11, 18)
        val data = MonthSummaryModel.compute(
            budgets = emptyList(),
            transactionsThisMonth = listOf(expense(20_00, eur, today)),
            recurrings = emptyList(),
            today = today,
            base = eur,
        )
        assertThat(data.mode).isEqualTo(HeroMode.SPENT_ONLY)
        assertThat(data.available.minor).isEqualTo(20_00)
    }

    private fun expense(minor: Long, eur: CurrencyCode, day: LocalDate) = Transaction(
        kind = TransactionKind.EXPENSE,
        amount = Money(minor, eur),
        accountId = 1,
        occurredAt = day.atTime(12, 0),
    )

    private fun recurring(minor: Long, eur: CurrencyCode, next: LocalDate) = Recurring(
        type = RecurringType.SUBSCRIPTION,
        name = "Sub",
        amount = Money(minor, eur),
        cycle = Cycle(1, CycleUnit.MONTH),
        startDate = next.minusMonths(2),
        nextDate = next,
    )
}
