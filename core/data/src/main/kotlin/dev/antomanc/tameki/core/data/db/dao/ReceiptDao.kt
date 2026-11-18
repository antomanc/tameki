package dev.antomanc.tameki.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.antomanc.tameki.core.data.db.entity.ReceiptEntity
import dev.antomanc.tameki.core.data.db.entity.ReceiptItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun get(id: Long): ReceiptEntity?

    @Upsert
    suspend fun upsert(entity: ReceiptEntity): Long

    @Query("DELETE FROM receipts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM receipts")
    suspend fun deleteAll()

    @Query("DELETE FROM receipt_items")
    suspend fun deleteAllItems()

    @Query("SELECT * FROM receipt_items WHERE receiptId = :receiptId ORDER BY ordinal")
    suspend fun itemsFor(receiptId: Long): List<ReceiptItemEntity>

    @Query("SELECT * FROM receipt_items WHERE receiptId IN (:ids)")
    suspend fun itemsForIds(ids: List<Long>): List<ReceiptItemEntity>

    @Query("DELETE FROM receipt_items WHERE receiptId = :receiptId")
    suspend fun deleteItems(receiptId: Long)

    @Upsert
    suspend fun upsertItems(entities: List<ReceiptItemEntity>)

    @Query("SELECT * FROM receipts")
    suspend fun getAll(): List<ReceiptEntity>
}
