package dev.antomanc.tameki.core.data.repository

import dev.antomanc.tameki.core.data.db.dao.AccountDao
import dev.antomanc.tameki.core.data.db.entity.AccountEntity
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountRepositoryImpl(
    private val dao: AccountDao,
) : AccountRepository {
    override fun observeAll(includeArchived: Boolean): Flow<List<Account>> =
        dao.observeAll(includeArchived).map { list -> list.map { it.toDomain() } }

    override suspend fun get(id: Long): Account? = dao.get(id)?.toDomain()

    override suspend fun upsert(account: Account): Long =
        dao.upsert(AccountEntity.from(account))

    override suspend fun archive(id: Long, archived: Boolean) {
        dao.setArchived(id, archived)
    }
}
