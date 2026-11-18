package dev.antomanc.tameki.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.antomanc.tameki.core.data.db.entity.InboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InboxDao {
    @Query(
        """
        SELECT * FROM inbox_items
        WHERE :status IS NULL OR status = :status
        ORDER BY receivedAt DESC
        """,
    )
    fun observe(status: String?): Flow<List<InboxEntity>>

    @Query("SELECT * FROM inbox_items WHERE id = :id")
    suspend fun get(id: Long): InboxEntity?

    @Query("SELECT * FROM inbox_items")
    suspend fun getAll(): List<InboxEntity>

    @Upsert
    suspend fun upsert(entity: InboxEntity): Long

    @Query("UPDATE inbox_items SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)

    @Query("DELETE FROM inbox_items")
    suspend fun deleteAll()
}
