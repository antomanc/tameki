package dev.antomanc.tameki.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.antomanc.tameki.core.data.db.entity.BudgetCategoryEntity
import dev.antomanc.tameki.core.data.db.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY name")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun get(id: Long): BudgetEntity?

    @Query("SELECT * FROM budgets")
    suspend fun getAll(): List<BudgetEntity>

    @Upsert
    suspend fun upsert(entity: BudgetEntity): Long

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()

    @Query("SELECT * FROM budget_categories WHERE budgetId = :budgetId")
    suspend fun categoriesFor(budgetId: Long): List<BudgetCategoryEntity>

    @Query("SELECT * FROM budget_categories")
    suspend fun allCategories(): List<BudgetCategoryEntity>

    @Query("DELETE FROM budget_categories WHERE budgetId = :budgetId")
    suspend fun deleteCategories(budgetId: Long)

    @Upsert
    suspend fun upsertCategories(entities: List<BudgetCategoryEntity>)

    @Query("DELETE FROM budget_categories")
    suspend fun deleteAllCategories()
}
