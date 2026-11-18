package dev.antomanc.tameki.feature.insights

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.entity.BudgetPeriod
import dev.antomanc.tameki.core.model.entity.Split
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Test

class InsightsMathTest {
    private val today = LocalDate.of(2026, 11, 18)

    @Test
    fun `budget periods start on Monday or the first of the month`() {
        assertThat(periodBounds(BudgetPeriod.MONTH, today))
            .isEqualTo(LocalDate.of(2026, 11, 1) to LocalDate.of(2026, 11, 30))
        assertThat(periodBounds(BudgetPeriod.WEEK, today))
            .isEqualTo(LocalDate.of(2026, 11, 16) to LocalDate.of(2026, 11, 22))
    }

    @Test
    fun `expected by today is the elapsed share of the limit`() {
        val limit = money(35000)
        val expected = expectedByToday(limit, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), today)
        assertThat(expected.minor).isEqualTo(21000)
    }

    @Test
    fun `expected by today clamps past the end of the period`() {
        val expected = expectedByToday(
            money(35000),
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30),
            LocalDate.of(2026, 12, 5),
        )
        assertThat(expected.minor).isEqualTo(35000)
    }

    @Test
    fun `sum expenses honours category filters, dates and currency`() {
        val list = listOf(
            expense(1, 1200, LocalDate.of(2026, 11, 2), categoryId = 1),
            expense(2, 800, LocalDate.of(2026, 11, 3), categoryId = 2),
            expense(3, 500, LocalDate.of(2026, 10, 30), categoryId = 1),
            expense(4, 900, LocalDate.of(2026, 11, 4), categoryId = 1, currency = dev.antomanc.tameki.core.model.CurrencyCode("USD")),
        )
        val total = sumExpenses(
            transactions = list,
            categoryIds = setOf(1),
            from = LocalDate.of(2026, 11, 1),
            to = today,
            currency = eur,
        )
        assertThat(total.minor).isEqualTo(1200)
    }

    @Test
    fun `a split transaction counts each split against its category`() {
        val split = Transaction(
            id = 9,
            kind = TransactionKind.EXPENSE,
            amount = money(5000),
            accountId = 1,
            categoryId = 1,
            splits = listOf(Split(1, money(2000), null), Split(2, money(3000), null)),
            occurredAt = java.time.LocalDateTime.of(today, java.time.LocalTime.NOON),
        )
        val first = sumExpenses(listOf(split), setOf(1), today.minusDays(1), today, eur)
        val second = sumExpenses(listOf(split), setOf(2), today.minusDays(1), today, eur)
        assertThat(first.minor).isEqualTo(2000)
        assertThat(second.minor).isEqualTo(3000)
    }

    @Test
    fun `month totals cover the six months ending with the current one`() {
        val list = listOf(
            expense(1, 1000, LocalDate.of(2026, 6, 10)),
            expense(2, 2000, LocalDate.of(2026, 11, 2)),
            expense(3, 3000, LocalDate.of(2026, 5, 2)),
        )
        val totals = monthTotals(list, today, months = 6, currency = eur)
        assertThat(totals.map { it.month }).containsExactly(
            YearMonth.of(2026, 6),
            YearMonth.of(2026, 7),
            YearMonth.of(2026, 8),
            YearMonth.of(2026, 9),
            YearMonth.of(2026, 10),
            YearMonth.of(2026, 11),
        ).inOrder()
        assertThat(totals.first().amount.minor).isEqualTo(1000)
        assertThat(totals.last().amount.minor).isEqualTo(2000)
    }

    @Test
    fun `category totals are largest first and keep the category key`() {
        val categories = mapOf(
            1L to category(1, "Alimentari", "groceries"),
            2L to category(2, "Caffè", "coffee"),
        )
        val list = listOf(
            expense(1, 1200, today, categoryId = 1),
            expense(2, 4000, today, categoryId = 2),
            expense(3, 300, today, categoryId = null),
        )
        val rows = categoryTotals(list, categories, limit = 3, currency = eur)
        assertThat(rows.map { it.label }).isEqualTo(listOf("Caffè", "Alimentari", null))
        assertThat(rows.first().categoryKey).isEqualTo("coffee")
        assertThat(rows.last().categoryId).isNull()
    }

    @Test
    fun `the no-spend streak stops at the first day with an expense`() {
        assertThat(noSpendStreak(listOf(expense(1, 100, today)), today)).isEqualTo(0)
        assertThat(
            noSpendStreak(listOf(expense(1, 100, today.minusDays(3))), today),
        ).isEqualTo(3)
        assertThat(noSpendStreak(emptyList(), today, max = 5)).isEqualTo(5)
    }

    @Test
    fun `percent rounds half even without floating point`() {
        assertThat(percentOf(2000, 11988)).isEqualTo(17)
        assertThat(percentOf(5000, 20000)).isEqualTo(25)
        assertThat(percentOf(100, 0)).isEqualTo(0)
    }
}
