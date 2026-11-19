package dev.antomanc.tameki.core.data.repository

import dev.antomanc.tameki.core.data.db.dao.InboxDao
import dev.antomanc.tameki.core.data.db.entity.InboxEntity
import dev.antomanc.tameki.core.data.db.entity.toDomain
import dev.antomanc.tameki.core.data.serialization.DraftJson
import dev.antomanc.tameki.core.model.entity.InboxItem
import dev.antomanc.tameki.core.model.entity.InboxStatus
import dev.antomanc.tameki.core.model.repository.InboxRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InboxRepositoryImpl(
    private val dao: InboxDao,
) : InboxRepository {
    override fun observe(status: InboxStatus?): Flow<List<InboxItem>> =
        dao.observe(status?.name).map { list ->
            list.map { it.toDomain(DraftJson.decode(it.draftJson)) }
        }

    override suspend fun get(id: Long): InboxItem? {
        val entity = dao.get(id) ?: return null
        return entity.toDomain(DraftJson.decode(entity.draftJson))
    }

    override suspend fun upsert(item: InboxItem): Long =
        dao.upsert(
            InboxEntity(
                id = item.id,
                draftJson = DraftJson.encode(item.draft),
                origin = item.origin.name,
                rawText = item.rawText,
                receivedAt = item.receivedAt,
                status = item.status.name,
            ),
        )

    override suspend fun setStatus(id: Long, status: InboxStatus) {
        dao.setStatus(id, status.name)
    }

    override suspend fun claim(id: Long, from: InboxStatus, to: InboxStatus): Boolean =
        dao.compareAndSetStatus(id, from.name, to.name) == 1
}
