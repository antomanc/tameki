package dev.antomanc.tameki.core.data

import androidx.paging.PagingSource
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.data.repository.bind
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.repository.TransactionFilter
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TransactionPagingTest : DbTestRule() {
    private val eur = CurrencyCode("EUR")

    @Test
    fun pagingSourceReturnsNewestFirst() = runTest {
        val accountId = accounts.upsert(Account(name = "Card", currency = eur, type = AccountType.CARD))
        for (day in 1..5) {
            transactions.upsert(
                Transaction(
                    kind = TransactionKind.EXPENSE,
                    amount = Money(100L * day, eur),
                    accountId = accountId,
                    merchant = "M$day",
                    occurredAt = LocalDateTime.of(2026, 11, day, 12, 0),
                ),
            )
        }
        val b = TransactionFilter().bind()
        val source = db.transactionDao().pagingSource(
            fromIso = b.fromIso,
            toIso = b.toIso,
            accountId = b.accountId,
            categoryId = b.categoryId,
            source = b.source,
            tag = b.tag,
            ftsQuery = b.ftsQuery,
        )
        val loaded = source.load(PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false))
        val page = loaded as PagingSource.LoadResult.Page
        assertThat(page.data).hasSize(5)
        assertThat(page.data.map { it.transaction.merchant })
            .containsExactly("M5", "M4", "M3", "M2", "M1")
            .inOrder()
    }
}
