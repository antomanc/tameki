package dev.antomanc.tameki.core.data.repository

import dev.antomanc.tameki.core.data.db.dao.GoalDao
import dev.antomanc.tameki.core.data.db.entity.GoalEntity
import dev.antomanc.tameki.core.model.entity.Goal
import dev.antomanc.tameki.core.model.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoalRepositoryImpl(
    private val dao: GoalDao,
) : GoalRepository {
    override fun observeAll(includeArchived: Boolean): Flow<List<Goal>> =
        dao.observeAll(includeArchived).map { list -> list.map { it.toDomain() } }

    override suspend fun get(id: Long): Goal? = dao.get(id)?.toDomain()

    override suspend fun upsert(goal: Goal): Long = dao.upsert(GoalEntity.from(goal))

    override suspend fun archive(id: Long, archived: Boolean) {
        dao.setArchived(id, archived)
    }
}
