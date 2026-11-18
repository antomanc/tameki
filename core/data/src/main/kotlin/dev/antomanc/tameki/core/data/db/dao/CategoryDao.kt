package dev.antomanc.tameki.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.antomanc.tameki.core.data.db.entity.CategoryEntity
import dev.antomanc.tameki.core.data.db.entity.MerchantCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE archived = 0 OR :includeArchived = 1 ORDER BY kind, name")
    fun observeAll(includeArchived: Boolean): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun get(id: Long): CategoryEntity?

    @Upsert
    suspend fun upsert(entity: CategoryEntity): Long

    @Query("UPDATE categories SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    @Query("SELECT * FROM merchant_category_memory")
    fun observeMerchantMemory(): Flow<List<MerchantCategoryEntity>>

    @Upsert
    suspend fun upsertMerchantMemory(entity: MerchantCategoryEntity)

    @Query("DELETE FROM merchant_category_memory")
    suspend fun deleteAllMerchantMemory()
}
