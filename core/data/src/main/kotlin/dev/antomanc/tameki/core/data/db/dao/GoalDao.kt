package dev.antomanc.tameki.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.antomanc.tameki.core.data.db.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE archived = 0 OR :includeArchived = 1 ORDER BY name")
    fun observeAll(includeArchived: Boolean): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun get(id: Long): GoalEntity?

    @Query("SELECT * FROM goals")
    suspend fun getAll(): List<GoalEntity>

    @Upsert
    suspend fun upsert(entity: GoalEntity): Long

    @Query("UPDATE goals SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}
