package dev.antomanc.tameki.core.data.repository

import androidx.room.withTransaction
import dev.antomanc.tameki.core.data.db.TamekiDatabase
import dev.antomanc.tameki.core.data.db.entity.ReceiptEntity
import dev.antomanc.tameki.core.data.db.entity.ReceiptItemEntity
import dev.antomanc.tameki.core.data.db.entity.toDomain
import dev.antomanc.tameki.core.model.entity.Receipt
import dev.antomanc.tameki.core.model.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReceiptRepositoryImpl(
    private val db: TamekiDatabase,
) : ReceiptRepository {
    private val dao get() = db.receiptDao()

    override fun observeAll(): Flow<List<Receipt>> =
        dao.observeAll().map { entities ->
            if (entities.isEmpty()) return@map emptyList()
            val items = dao.itemsForIds(entities.map { it.id }).groupBy { it.receiptId }
            entities.map { it.toDomain(items[it.id].orEmpty()) }
        }

    override suspend fun get(id: Long): Receipt? {
        val entity = dao.get(id) ?: return null
        return entity.toDomain(dao.itemsFor(id))
    }

    override suspend fun upsert(receipt: Receipt): Long =
        db.withTransaction {
            val id = dao.upsert(
                ReceiptEntity(
                    id = receipt.id,
                    imagePath = receipt.imagePath,
                    rawText = receipt.rawText,
                    createdAt = receipt.createdAt,
                ),
            )
            dao.deleteItems(id)
            if (receipt.items.isNotEmpty()) {
                dao.upsertItems(
                    receipt.items.mapIndexed { index, item ->
                        ReceiptItemEntity(
                            receiptId = id,
                            ordinal = index,
                            name = item.name,
                            amountMinor = item.amount.minor,
                            currency = item.amount.currency.value,
                            quantity = item.quantity,
                        )
                    },
                )
            }
            id
        }

    override suspend fun delete(id: Long) {
        db.withTransaction {
            dao.deleteItems(id)
            dao.delete(id)
        }
    }
}
