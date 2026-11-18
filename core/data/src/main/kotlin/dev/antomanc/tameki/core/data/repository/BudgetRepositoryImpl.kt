package dev.antomanc.tameki.core.data.repository

import androidx.room.withTransaction
import dev.antomanc.tameki.core.data.db.TamekiDatabase
import dev.antomanc.tameki.core.data.db.entity.BudgetCategoryEntity
import dev.antomanc.tameki.core.data.db.entity.toDomain
import dev.antomanc.tameki.core.data.db.entity.toEntity
import dev.antomanc.tameki.core.model.entity.Budget
import dev.antomanc.tameki.core.model.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(
    private val db: TamekiDatabase,
) : BudgetRepository {
    private val dao get() = db.budgetDao()

    override fun observeAll(): Flow<List<Budget>> =
        dao.observeAll().map { entities ->
            val cats = dao.allCategories().groupBy { it.budgetId }
            entities.map { entity ->
                entity.toDomain(cats[entity.id].orEmpty().map { it.categoryId }.toSet())
            }
        }

    override suspend fun get(id: Long): Budget? {
        val entity = dao.get(id) ?: return null
        return entity.toDomain(dao.categoriesFor(id).map { it.categoryId }.toSet())
    }

    override suspend fun upsert(budget: Budget): Long =
        db.withTransaction {
            val id = dao.upsert(budget.toEntity())
            dao.deleteCategories(id)
            if (budget.categoryIds.isNotEmpty()) {
                dao.upsertCategories(
                    budget.categoryIds.map { categoryId ->
                        BudgetCategoryEntity(budgetId = id, categoryId = categoryId)
                    },
                )
            }
            id
        }

    override suspend fun delete(id: Long) {
        db.withTransaction {
            dao.deleteCategories(id)
            dao.delete(id)
        }
    }
}
