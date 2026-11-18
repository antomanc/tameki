package dev.antomanc.tameki.core.data

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.repository.TransactionFilter
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TransactionRepositoryTest : DbTestRule() {
    private val eur = CurrencyCode("EUR")

    @Test
    fun seedCreatesCategoriesAndAccounts() = runTest {
        seed()
        categories.observeAll().test {
            val cats = awaitItem()
            assertThat(cats).isNotEmpty()
            cancelAndIgnoreRemainingEvents()
        }
        accounts.observeAll().test {
            val accs = awaitItem()
            assertThat(accs).hasSize(2)
            assertThat(accs.map { it.type }).containsExactly(AccountType.CASH, AccountType.CARD)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun filtersByPeriodAccountCategoryTagSourceAndText() = runTest {
        val accountId = accounts.upsert(Account(name = "Card", currency = eur, type = AccountType.CARD))
        val otherAccount = accounts.upsert(Account(name = "Cash", currency = eur, type = AccountType.CASH))
        val catId = categories.upsert(
            Category(name = "Groceries", kind = CategoryKind.EXPENSE, icon = "groceries", color = 1L),
        )
        val otherCat = categories.upsert(
            Category(name = "Fuel", kind = CategoryKind.EXPENSE, icon = "fuel", color = 2L),
        )

        val kept = transactions.upsert(
            Transaction(
                kind = TransactionKind.EXPENSE,
                amount = Money(1_250L, eur),
                accountId = accountId,
                categoryId = catId,
                merchant = "Esselunga",
                note = "weekly shop",
                tags = setOf("food"),
                occurredAt = LocalDateTime.of(2026, 11, 10, 12, 0),
                source = TransactionSource.MANUAL,
            ),
        )
        transactions.upsert(
            Transaction(
                kind = TransactionKind.EXPENSE,
                amount = Money(4_000L, eur),
                accountId = otherAccount,
                categoryId = otherCat,
                merchant = "Shell",
                occurredAt = LocalDateTime.of(2026, 10, 1, 12, 0),
                source = TransactionSource.CSV_IMPORT,
            ),
        )

        transactions.observe(
            TransactionFilter(
                from = LocalDate.of(2026, 11, 1),
                to = LocalDate.of(2026, 11, 30),
                accountId = accountId,
                categoryId = catId,
                text = "Esselunga",
                tag = "food",
                source = TransactionSource.MANUAL,
            ),
        ).test {
            val rows = awaitItem()
            assertThat(rows).hasSize(1)
            assertThat(rows.single().id).isEqualTo(kept)
            assertThat(rows.single().merchant).isEqualTo("Esselunga")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dayTotalsGroupExpenseAndIncome() = runTest {
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
                amount = Money(300L, eur),
                accountId = accountId,
                occurredAt = LocalDateTime.of(2026, 11, 18, 18, 0),
            ),
        )
        transactions.upsert(
            Transaction(
                kind = TransactionKind.INCOME,
                amount = Money(2_000L, eur),
                accountId = accountId,
                occurredAt = LocalDateTime.of(2026, 11, 18, 12, 0),
            ),
        )

        transactions.observeDayTotals().test {
            val totals = awaitItem()
            assertThat(totals).hasSize(1)
            assertThat(totals.single().date).isEqualTo(LocalDate.of(2026, 11, 18))
            assertThat(totals.single().expense.minor).isEqualTo(800L)
            assertThat(totals.single().income.minor).isEqualTo(2_000L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun ftsFindsMerchantAndNote() = runTest {
        val accountId = accounts.upsert(Account(name = "Card", currency = eur, type = AccountType.CARD))
        transactions.upsert(
            Transaction(
                kind = TransactionKind.EXPENSE,
                amount = Money(100L, eur),
                accountId = accountId,
                merchant = "Netflix",
                note = "streaming",
                occurredAt = LocalDateTime.of(2026, 11, 1, 10, 0),
            ),
        )
        transactions.upsert(
            Transaction(
                kind = TransactionKind.EXPENSE,
                amount = Money(200L, eur),
                accountId = accountId,
                merchant = "Enel",
                note = "utilities bill",
                occurredAt = LocalDateTime.of(2026, 11, 2, 10, 0),
            ),
        )

        transactions.observe(TransactionFilter(text = "netflix")).test {
            val rows = awaitItem()
            assertThat(rows.map { it.merchant }).containsExactly("Netflix")
            cancelAndIgnoreRemainingEvents()
        }
        transactions.observe(TransactionFilter(text = "utilities")).test {
            val rows = awaitItem()
            assertThat(rows.map { it.merchant }).containsExactly("Enel")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun upsertRemembersMerchantCategory() = runTest {
        val accountId = accounts.upsert(Account(name = "Card", currency = eur, type = AccountType.CARD))
        val catId = categories.upsert(
            Category(name = "Groceries", kind = CategoryKind.EXPENSE, icon = "groceries", color = 1L),
        )
        transactions.upsert(
            Transaction(
                kind = TransactionKind.EXPENSE,
                amount = Money(100L, eur),
                accountId = accountId,
                categoryId = catId,
                merchant = "Esselunga",
                occurredAt = LocalDateTime.of(2026, 11, 1, 10, 0),
            ),
        )
        categories.observeMerchantCategoryMemory().test {
            val memory = awaitItem()
            assertThat(memory["Esselunga"]).isEqualTo(catId)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
