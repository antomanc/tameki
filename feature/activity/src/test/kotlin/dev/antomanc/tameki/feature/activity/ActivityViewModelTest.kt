package dev.antomanc.tameki.feature.activity

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.repository.DayTotal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import org.junit.Test

class ActivityViewModelTest {
    private val eur = CurrencyCode("EUR")
    private val today = LocalDate.of(2026, 11, 18)

    @Test
    fun buildItemsGroupsByDayWithHeaders() {
        val cats = listOf(Category(1, "Alimentari", CategoryKind.EXPENSE, "groceries", 1))
        val accounts = listOf(Account(1, "Carta", eur, AccountType.CARD))
        val txs = listOf(
            Transaction(
                id = 1,
                kind = TransactionKind.EXPENSE,
                amount = Money(1000, eur),
                accountId = 1,
                categoryId = 1,
                merchant = "Esselunga",
                occurredAt = LocalDateTime.of(2026, 11, 18, 10, 0),
            ),
            Transaction(
                id = 2,
                kind = TransactionKind.EXPENSE,
                amount = Money(250, eur),
                accountId = 1,
                categoryId = 1,
                merchant = "Bar",
                occurredAt = LocalDateTime.of(2026, 11, 17, 9, 0),
            ),
        )
        val items = ActivityViewModel.buildItems(
            transactions = txs,
            categories = cats,
            accounts = accounts,
            totals = listOf(
                DayTotal(today, eur, Money(1000, eur), Money.zero(eur)),
                DayTotal(today.minusDays(1), eur, Money(250, eur), Money.zero(eur)),
            ),
            today = today,
            locale = Locale.ITALIAN,
        )
        assertThat(items.filterIsInstance<ActivityListItem.Header>()).hasSize(2)
        assertThat(items.filterIsInstance<ActivityListItem.Row>()).hasSize(2)
        assertThat((items.first() as ActivityListItem.Header).label).isEqualTo("Today")
    }
}
