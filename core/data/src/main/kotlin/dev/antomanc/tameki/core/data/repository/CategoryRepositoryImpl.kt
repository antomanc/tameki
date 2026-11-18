package dev.antomanc.tameki.core.data.repository

import dev.antomanc.tameki.core.data.db.dao.CategoryDao
import dev.antomanc.tameki.core.data.db.entity.CategoryEntity
import dev.antomanc.tameki.core.data.db.entity.MerchantCategoryEntity
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val dao: CategoryDao,
) : CategoryRepository {
    override fun observeAll(includeArchived: Boolean): Flow<List<Category>> =
        dao.observeAll(includeArchived).map { list -> list.map { it.toDomain() } }

    override suspend fun get(id: Long): Category? = dao.get(id)?.toDomain()

    override suspend fun upsert(category: Category): Long =
        dao.upsert(CategoryEntity.from(category))

    override suspend fun archive(id: Long, archived: Boolean) {
        dao.setArchived(id, archived)
    }

    override fun observeMerchantCategoryMemory(): Flow<Map<String, Long>> =
        dao.observeMerchantMemory().map { rows ->
            rows.associate { it.merchant to it.categoryId }
        }

    override suspend fun rememberMerchantCategory(merchant: String, categoryId: Long) {
        val key = merchant.trim()
        if (key.isEmpty()) return
        dao.upsertMerchantMemory(MerchantCategoryEntity(merchant = key, categoryId = categoryId))
    }
}
