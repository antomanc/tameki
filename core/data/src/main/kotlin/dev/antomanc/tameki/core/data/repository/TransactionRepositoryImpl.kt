package dev.antomanc.tameki.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import dev.antomanc.tameki.core.data.db.TamekiDatabase
import dev.antomanc.tameki.core.data.db.dao.TransactionDao
import dev.antomanc.tameki.core.data.db.entity.TransactionSplitEntity
import dev.antomanc.tameki.core.data.db.entity.TransactionTagEntity
import dev.antomanc.tameki.core.data.db.entity.toDomain
import dev.antomanc.tameki.core.data.db.entity.toEntity
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.DayTotal
import dev.antomanc.tameki.core.model.repository.TransactionFilter
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val db: TamekiDatabase,
    private val categoryRepository: CategoryRepository,
) : TransactionRepository {
    private val dao: TransactionDao get() = db.transactionDao()

    override fun observe(filter: TransactionFilter): Flow<List<Transaction>> {
        val b = filter.bind()
        return dao.observeFiltered(
            fromIso = b.fromIso,
            toIso = b.toIso,
            accountId = b.accountId,
            categoryId = b.categoryId,
            source = b.source,
            tag = b.tag,
            ftsQuery = b.ftsQuery,
        ).map { list -> list.map { it.toDomain() } }
    }

    fun paging(filter: TransactionFilter = TransactionFilter()): Flow<PagingData<Transaction>> {
        val b = filter.bind()
        return Pager(
            config = PagingConfig(pageSize = 40, enablePlaceholders = false),
            pagingSourceFactory = {
                dao.pagingSource(
                    fromIso = b.fromIso,
                    toIso = b.toIso,
                    accountId = b.accountId,
                    categoryId = b.categoryId,
                    source = b.source,
                    tag = b.tag,
                    ftsQuery = b.ftsQuery,
                )
            },
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    override fun observeDayTotals(filter: TransactionFilter): Flow<List<DayTotal>> {
        val b = filter.bind()
        return dao.observeDayTotals(
            fromIso = b.fromIso,
            toIso = b.toIso,
            accountId = b.accountId,
            categoryId = b.categoryId,
            source = b.source,
            tag = b.tag,
            ftsQuery = b.ftsQuery,
        ).map { rows ->
            rows.groupBy { it.day to it.currency }
                .map { (key, group) ->
                    val (day, currencyCode) = key
                    val currency = CurrencyCode(currencyCode)
                    var expense = 0L
                    var income = 0L
                    for (row in group) {
                        when (row.kind) {
                            TransactionKind.EXPENSE.name -> expense += row.totalMinor
                            TransactionKind.INCOME.name -> income += row.totalMinor
                        }
                    }
                    DayTotal(
                        date = LocalDate.parse(day),
                        currency = currency,
                        expense = Money(expense, currency),
                        income = Money(income, currency),
                    )
                }
                .sortedWith(compareByDescending<DayTotal> { it.date }.thenBy { it.currency.value })
        }
    }

    override suspend fun get(id: Long): Transaction? {
        val entity = dao.get(id) ?: return null
        return entity.toDomain(dao.splitsFor(id), dao.tagsFor(id))
    }

    override suspend fun upsert(transaction: Transaction): Long {
        return db.withTransaction {
            val id = dao.upsert(transaction.toEntity())
            dao.deleteSplits(id)
            dao.deleteTags(id)
            if (transaction.splits.isNotEmpty()) {
                dao.upsertSplits(
                    transaction.splits.mapIndexed { index, split ->
                        TransactionSplitEntity(
                            transactionId = id,
                            ordinal = index,
                            categoryId = split.categoryId,
                            amountMinor = split.amount.minor,
                            currency = split.amount.currency.value,
                            note = split.note,
                        )
                    },
                )
            }
            if (transaction.tags.isNotEmpty()) {
                dao.upsertTags(
                    transaction.tags.map { tag ->
                        TransactionTagEntity(transactionId = id, tag = tag)
                    },
                )
            }
            val merchant = transaction.merchant?.trim()
            val categoryId = transaction.categoryId
            if (!merchant.isNullOrEmpty() && categoryId != null) {
                categoryRepository.rememberMerchantCategory(merchant, categoryId)
            }
            id
        }
    }

    override suspend fun delete(id: Long) {
        db.withTransaction {
            dao.deleteSplits(id)
            dao.deleteTags(id)
            dao.delete(id)
        }
    }
}
