package dev.antomanc.tameki.core.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.antomanc.tameki.core.data.db.entity.TransactionAggregate
import dev.antomanc.tameki.core.data.db.entity.TransactionEntity
import dev.antomanc.tameki.core.data.db.entity.TransactionSplitEntity
import dev.antomanc.tameki.core.data.db.entity.TransactionTagEntity
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

data class DayTotalRow(
    val day: String,
    val kind: String,
    val currency: String,
    val totalMinor: Long,
)

@Dao
interface TransactionDao {
    @Transaction
    @Query(
        """
        SELECT t.* FROM transactions t
        WHERE
          (:fromIso IS NULL OR t.occurredAt >= :fromIso)
          AND (:toIso IS NULL OR t.occurredAt < :toIso)
          AND (:accountId IS NULL OR t.accountId = :accountId)
          AND (:categoryId IS NULL OR t.categoryId = :categoryId
               OR EXISTS (
                 SELECT 1 FROM transaction_splits s
                 WHERE s.transactionId = t.id AND s.categoryId = :categoryId
               ))
          AND (:source IS NULL OR t.source = :source)
          AND (:tag IS NULL OR EXISTS (
                 SELECT 1 FROM transaction_tags tags
                 WHERE tags.transactionId = t.id AND tags.tag = :tag
               ))
          AND (
            :ftsQuery IS NULL
            OR t.id IN (SELECT rowid FROM transactions_fts WHERE transactions_fts MATCH :ftsQuery)
          )
        ORDER BY t.occurredAt DESC, t.id DESC
        """,
    )
    fun observeFiltered(
        fromIso: LocalDateTime?,
        toIso: LocalDateTime?,
        accountId: Long?,
        categoryId: Long?,
        source: String?,
        tag: String?,
        ftsQuery: String?,
    ): Flow<List<TransactionAggregate>>

    @Transaction
    @Query(
        """
        SELECT t.* FROM transactions t
        WHERE
          (:fromIso IS NULL OR t.occurredAt >= :fromIso)
          AND (:toIso IS NULL OR t.occurredAt < :toIso)
          AND (:accountId IS NULL OR t.accountId = :accountId)
          AND (:categoryId IS NULL OR t.categoryId = :categoryId
               OR EXISTS (
                 SELECT 1 FROM transaction_splits s
                 WHERE s.transactionId = t.id AND s.categoryId = :categoryId
               ))
          AND (:source IS NULL OR t.source = :source)
          AND (:tag IS NULL OR EXISTS (
                 SELECT 1 FROM transaction_tags tags
                 WHERE tags.transactionId = t.id AND tags.tag = :tag
               ))
          AND (
            :ftsQuery IS NULL
            OR t.id IN (SELECT rowid FROM transactions_fts WHERE transactions_fts MATCH :ftsQuery)
          )
        ORDER BY t.occurredAt DESC, t.id DESC
        """,
    )
    fun pagingSource(
        fromIso: LocalDateTime?,
        toIso: LocalDateTime?,
        accountId: Long?,
        categoryId: Long?,
        source: String?,
        tag: String?,
        ftsQuery: String?,
    ): PagingSource<Int, TransactionAggregate>

    @Query(
        """
        SELECT substr(t.occurredAt, 1, 10) AS day, t.kind AS kind, t.currency AS currency,
               SUM(
                 CASE
                   WHEN :categoryId IS NULL THEN t.amountMinor
                   WHEN EXISTS (
                     SELECT 1 FROM transaction_splits s0 WHERE s0.transactionId = t.id
                   ) THEN COALESCE(
                     (
                       SELECT SUM(s.amountMinor) FROM transaction_splits s
                       WHERE s.transactionId = t.id AND s.categoryId = :categoryId
                     ),
                     0
                   )
                   ELSE t.amountMinor
                 END
               ) AS totalMinor
        FROM transactions t
        WHERE
          (:fromIso IS NULL OR t.occurredAt >= :fromIso)
          AND (:toIso IS NULL OR t.occurredAt < :toIso)
          AND (:accountId IS NULL OR t.accountId = :accountId)
          AND (:categoryId IS NULL OR t.categoryId = :categoryId
               OR EXISTS (
                 SELECT 1 FROM transaction_splits s
                 WHERE s.transactionId = t.id AND s.categoryId = :categoryId
               ))
          AND (:source IS NULL OR t.source = :source)
          AND (:tag IS NULL OR EXISTS (
                 SELECT 1 FROM transaction_tags tags
                 WHERE tags.transactionId = t.id AND tags.tag = :tag
               ))
          AND (
            :ftsQuery IS NULL
            OR t.id IN (SELECT rowid FROM transactions_fts WHERE transactions_fts MATCH :ftsQuery)
          )
        GROUP BY day, t.kind, t.currency
        ORDER BY day DESC
        """,
    )
    fun observeDayTotals(
        fromIso: LocalDateTime?,
        toIso: LocalDateTime?,
        accountId: Long?,
        categoryId: Long?,
        source: String?,
        tag: String?,
        ftsQuery: String?,
    ): Flow<List<DayTotalRow>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun get(id: Long): TransactionEntity?

    @Upsert
    suspend fun upsert(entity: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM transaction_splits WHERE transactionId = :transactionId ORDER BY ordinal")
    suspend fun splitsFor(transactionId: Long): List<TransactionSplitEntity>

    @Query("DELETE FROM transaction_splits WHERE transactionId = :transactionId")
    suspend fun deleteSplits(transactionId: Long)

    @Upsert
    suspend fun upsertSplits(entities: List<TransactionSplitEntity>)

    @Query("SELECT * FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun tagsFor(transactionId: Long): List<TransactionTagEntity>

    @Query("DELETE FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun deleteTags(transactionId: Long)

    @Upsert
    suspend fun upsertTags(entities: List<TransactionTagEntity>)

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC, id DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE occurredAt = :occurredAt
          AND amountMinor = :amountMinor
          AND currency = :currency
          AND IFNULL(merchant, '') = IFNULL(:merchant, '')
          AND accountId = :accountId
        """,
    )
    suspend fun countExactDuplicate(
        occurredAt: LocalDateTime,
        amountMinor: Long,
        currency: String,
        merchant: String?,
        accountId: Long,
    ): Int

    @Query("DELETE FROM transaction_splits")
    suspend fun deleteAllSplits()

    @Query("DELETE FROM transaction_tags")
    suspend fun deleteAllTags()
}
