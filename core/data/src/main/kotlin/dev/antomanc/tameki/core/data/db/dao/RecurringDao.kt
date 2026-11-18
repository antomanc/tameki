package dev.antomanc.tameki.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.antomanc.tameki.core.data.db.entity.PricePointEntity
import dev.antomanc.tameki.core.data.db.entity.RecurringEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring ORDER BY nextDate, name")
    fun observeAll(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring WHERE id = :id")
    suspend fun get(id: Long): RecurringEntity?

    @Query("SELECT * FROM recurring")
    suspend fun getAll(): List<RecurringEntity>

    @Upsert
    suspend fun upsert(entity: RecurringEntity): Long

    @Query("DELETE FROM recurring WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM recurring")
    suspend fun deleteAll()

    /** Compare-and-set advance of nextDate; returns rows updated (0 if another writer won). */
    @Query(
        """
        UPDATE recurring SET nextDate = :newDate
        WHERE id = :id AND nextDate = :expectedDate
        """,
    )
    suspend fun updateNextDateCas(id: Long, expectedDate: java.time.LocalDate, newDate: java.time.LocalDate): Int

    @Query("SELECT * FROM recurring_price_points WHERE recurringId = :recurringId ORDER BY since")
    suspend fun pricePointsFor(recurringId: Long): List<PricePointEntity>

    @Query("SELECT * FROM recurring_price_points WHERE recurringId IN (:ids)")
    suspend fun pricePointsForIds(ids: List<Long>): List<PricePointEntity>

    @Query("DELETE FROM recurring_price_points WHERE recurringId = :recurringId")
    suspend fun deletePricePoints(recurringId: Long)

    @Upsert
    suspend fun upsertPricePoints(entities: List<PricePointEntity>)

    @Query("DELETE FROM recurring_price_points")
    suspend fun deleteAllPricePoints()
}
