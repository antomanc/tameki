package dev.antomanc.tameki.core.data

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.Split
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.repository.TransactionFilter
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DayTotalsRegressionTest : DbTestRule() {
    private val eur = CurrencyCode("EUR")
    private val usd = CurrencyCode("USD")

    @Test
    fun dayTotalsArePerCurrencyNotSummedAcrossCurrencies() = runTest {
        val accountId = accounts.upsert(Account(name = "Card", currency = eur, type = AccountType.CARD))
        transactions.upsert(
            Transaction(
                kind = TransactionKind.EXPENSE,
                amount = Money(500L, eur),
                accountId = accountId,
                occurredAt = LocalDateTime.of(2026, 11, 18, 9, 0),
            ),
        )
        transactions.upsert(
            Transaction(
                kind = TransactionKind.EXPENSE,
                amount = Money(300L, usd),
                accountId = accountId,
                occurredAt = LocalDateTime.of(2026, 11, 18, 10, 0),
            ),
        )

        transactions.observeDayTotals().test {
            val totals = awaitItem()
            assertThat(totals).hasSize(2)
            val byCurrency = totals.associateBy { it.currency }
            assertThat(byCurrency[eur]!!.expense.minor).isEqualTo(500L)
            assertThat(byCurrency[usd]!!.expense.minor).isEqualTo(300L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dayTotalsWithTwoTagsDoNotDoubleCount() = runTest {
        val accountId = accounts.upsert(Account(name = "Card", currency = eur, type = AccountType.CARD))
        transactions.upsert(
            Transaction(
                kind = TransactionKind.EXPENSE,
                amount = Money(1_000L, eur),
                accountId = accountId,
                tags = setOf("food", "weekly"),
                occurredAt = LocalDateTime.of(2026, 11, 18, 9, 0),
            ),
        )

        transactions.observeDayTotals(TransactionFilter(tag = "food")).test {
            val totals = awaitItem()
            assertThat(totals).hasSize(1)
            assertThat(totals.single().expense.minor).isEqualTo(1_000L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dayTotalsCategoryFilterUsesSplitAmountOnly() = runTest {
        val accountId = accounts.upsert(Account(name = "Card", currency = eur, type = AccountType.CARD))
        val groceries = categories.upsert(
            Category(name = "Groceries", kind = CategoryKind.EXPENSE, icon = "g", color = 1L),
        )
        val fuel = categories.upsert(
            Category(name = "Fuel", kind = CategoryKind.EXPENSE, icon = "f", color = 2L),
        )
        transactions.upsert(
            Transaction(
                kind = TransactionKind.EXPENSE,
                amount = Money(1_000L, eur),
                accountId = accountId,
                splits = listOf(
                    Split(groceries, Money(400L, eur)),
                    Split(fuel, Money(600L, eur)),
                ),
                occurredAt = LocalDateTime.of(2026, 11, 18, 9, 0),
            ),
        )
        transactions.upsert(
            Transaction(
                kind = TransactionKind.EXPENSE,
                amount = Money(250L, eur),
                accountId = accountId,
                categoryId = groceries,
                occurredAt = LocalDateTime.of(2026, 11, 18, 10, 0),
            ),
        )

        transactions.observeDayTotals(TransactionFilter(categoryId = groceries)).test {
            val totals = awaitItem()
            assertThat(totals.single().date).isEqualTo(LocalDate.of(2026, 11, 18))
            assertThat(totals.single().expense.minor).isEqualTo(650L)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
