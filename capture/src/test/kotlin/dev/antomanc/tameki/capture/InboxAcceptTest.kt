package dev.antomanc.tameki.capture

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.InboxItem
import dev.antomanc.tameki.core.model.entity.InboxStatus
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.DayTotal
import dev.antomanc.tameki.core.model.repository.InboxRepository
import dev.antomanc.tameki.core.model.repository.TransactionFilter
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test

class InboxAcceptTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC)
    private val eur = CurrencyCode("EUR")

    @Test
    fun acceptCreatesTransactionSuggestsCategoryAndRemembersMerchant() = runTest {
        val inbox = FakeInbox()
        val txs = FakeTransactions()
        val cats = FakeCategories(
            listOf(
                Category(1L, "Groceries", CategoryKind.EXPENSE, "groceries", 0L, null, false),
            ),
        )
        val accounts = FakeAccounts(
            listOf(Account(10L, "Card", eur, AccountType.CARD, false, 0)),
        )
        val itemId = inbox.upsert(
            InboxItem(
                draft = TransactionDraft(
                    kind = TransactionKind.EXPENSE,
                    amount = Money(450, eur),
                    merchant = "Esselunga",
                    categoryHint = "spesa",
                    occurredAt = LocalDateTime.of(2026, 11, 18, 11, 0),
                ),
                origin = TransactionSource.NOTIFICATION,
                rawText = "€4.50 at Esselunga",
                receivedAt = Instant.parse("2026-11-18T11:00:00Z"),
            ),
        )
        val accept = InboxAccept(inbox, txs, cats, accounts, clock)
        assertThat(accept.accept(itemId)).isTrue()
        assertThat(txs.stored).hasSize(1)
        assertThat(txs.stored.single().categoryId).isEqualTo(1L)
        assertThat(txs.stored.single().accountId).isEqualTo(10L)
        assertThat(cats.memory["Esselunga"]).isEqualTo(1L)
        assertThat(inbox.get(itemId)?.status).isEqualTo(InboxStatus.ACCEPTED)
    }

    @Test
    fun preferredAccountPrefersCard() {
        val cash = Account(1L, "Cash", eur, AccountType.CASH, false, 0)
        val card = Account(2L, "Card", eur, AccountType.CARD, false, 1)
        assertThat(InboxAccept.preferredAccount(listOf(cash, card))?.id).isEqualTo(2L)
    }

    private class FakeInbox : InboxRepository {
        private val items = mutableListOf<InboxItem>()
        private var next = 1L
        override fun observe(status: InboxStatus?): Flow<List<InboxItem>> =
            MutableStateFlow(items.toList()).map { list ->
                status?.let { s -> list.filter { it.status == s } } ?: list
            }
        override suspend fun get(id: Long) = items.firstOrNull { it.id == id }
        override suspend fun upsert(item: InboxItem): Long {
            val id = if (item.id == 0L) next++ else item.id
            items.removeAll { it.id == id }
            items += item.copy(id = id)
            return id
        }
        override suspend fun setStatus(id: Long, status: InboxStatus) {
            val i = items.indexOfFirst { it.id == id }
            if (i >= 0) items[i] = items[i].copy(status = status)
        }
    }

    private class FakeTransactions : TransactionRepository {
        val stored = mutableListOf<Transaction>()
        override fun observe(filter: TransactionFilter) = flowOf(stored.toList())
        override fun observeDayTotals(filter: TransactionFilter): Flow<List<DayTotal>> = flowOf(emptyList())
        override suspend fun get(id: Long) = stored.firstOrNull { it.id == id }
        override suspend fun upsert(transaction: Transaction): Long {
            stored += transaction.copy(id = stored.size + 1L)
            return stored.last().id
        }
        override suspend fun delete(id: Long) { stored.removeAll { it.id == id } }
    }

    private class FakeCategories(private val all: List<Category>) : CategoryRepository {
        val memory = mutableMapOf<String, Long>()
        override fun observeAll(includeArchived: Boolean) = flowOf(all)
        override suspend fun get(id: Long) = all.firstOrNull { it.id == id }
        override suspend fun upsert(category: Category) = category.id
        override suspend fun archive(id: Long, archived: Boolean) = Unit
        override fun observeMerchantCategoryMemory() = flowOf(memory.toMap())
        override suspend fun rememberMerchantCategory(merchant: String, categoryId: Long) {
            memory[merchant] = categoryId
        }
    }

    private class FakeAccounts(private val all: List<Account>) : AccountRepository {
        override fun observeAll(includeArchived: Boolean) = flowOf(all)
        override suspend fun get(id: Long) = all.firstOrNull { it.id == id }
        override suspend fun upsert(account: Account) = account.id
        override suspend fun archive(id: Long, archived: Boolean) = Unit
    }
}
