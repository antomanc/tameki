package dev.antomanc.tameki.core.data.repository

import androidx.room.withTransaction
import dev.antomanc.tameki.core.data.db.TamekiDatabase
import dev.antomanc.tameki.core.data.db.entity.PricePointEntity
import dev.antomanc.tameki.core.data.db.entity.toDomain
import dev.antomanc.tameki.core.data.db.entity.toEntity
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecurringRepositoryImpl(
    private val db: TamekiDatabase,
) : RecurringRepository {
    private val dao get() = db.recurringDao()

    override fun observeAll(): Flow<List<Recurring>> =
        dao.observeAll().map { entities ->
            if (entities.isEmpty()) return@map emptyList()
            val prices = dao.pricePointsForIds(entities.map { it.id }).groupBy { it.recurringId }
            entities.map { it.toDomain(prices[it.id].orEmpty()) }
        }

    override suspend fun get(id: Long): Recurring? {
        val entity = dao.get(id) ?: return null
        return entity.toDomain(dao.pricePointsFor(id))
    }

    override suspend fun upsert(recurring: Recurring): Long =
        db.withTransaction {
            val id = dao.upsert(recurring.toEntity())
            dao.deletePricePoints(id)
            if (recurring.priceHistory.isNotEmpty()) {
                dao.upsertPricePoints(
                    recurring.priceHistory.map { point ->
                        PricePointEntity(
                            recurringId = id,
                            since = point.since,
                            amountMinor = point.amount.minor,
                            currency = point.amount.currency.value,
                        )
                    },
                )
            }
            id
        }

    override suspend fun delete(id: Long) {
        db.withTransaction {
            dao.deletePricePoints(id)
            dao.delete(id)
        }
    }
}
