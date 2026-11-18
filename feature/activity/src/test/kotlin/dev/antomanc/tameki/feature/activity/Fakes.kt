package dev.antomanc.tameki.feature.activity

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.InboxItem
import dev.antomanc.tameki.core.model.entity.InboxStatus
import dev.antomanc.tameki.core.model.entity.Receipt
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.core.model.insight.InsightRules
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.DayTotal
import dev.antomanc.tameki.core.model.repository.InboxRepository
import dev.antomanc.tameki.core.model.repository.ReceiptRepository
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionFilter
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import dev.antomanc.tameki.scan.api.ReceiptEngine
import dev.antomanc.tameki.scan.api.ReceiptReadResult
import dev.antomanc.tameki.scan.api.ReceiptReader
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class FakeAccountRepository(
    private val accounts: List<Account>,
) : AccountRepository {
    override fun observeAll(includeArchived: Boolean): Flow<List<Account>> = flowOf(accounts)
    override suspend fun get(id: Long): Account? = accounts.firstOrNull { it.id == id }
    override suspend fun upsert(account: Account): Long = account.id
    override suspend fun archive(id: Long, archived: Boolean) = Unit
}

internal class FakeCategoryRepository(
    private val categories: List<Category>,
    private val memory: Map<String, Long> = emptyMap(),
) : CategoryRepository {
    override fun observeAll(includeArchived: Boolean): Flow<List<Category>> = flowOf(categories)
    override suspend fun get(id: Long): Category? = categories.firstOrNull { it.id == id }
    override suspend fun upsert(category: Category): Long = category.id
    override suspend fun archive(id: Long, archived: Boolean) = Unit
    override fun observeMerchantCategoryMemory(): Flow<Map<String, Long>> = flowOf(memory)
    override suspend fun rememberMerchantCategory(merchant: String, categoryId: Long) = Unit
}

internal class FakeTransactionRepository(
    private val stored: MutableMap<Long, Transaction> = mutableMapOf(),
) : TransactionRepository {
    // Every write bumps the revision, like Room's invalidation tracker: an observer that is not
    // told about it would cache a stale row forever. The map stays the store so tests can inspect
    // exactly what was persisted.
    private val revision = MutableStateFlow(0L)

    override fun observe(filter: TransactionFilter): Flow<List<Transaction>> =
        revision.map { stored.values.toList() }
    override fun observeDayTotals(filter: TransactionFilter): Flow<List<DayTotal>> = flowOf(emptyList())
    override suspend fun get(id: Long): Transaction? = stored[id]
    override suspend fun upsert(transaction: Transaction): Long {
        val id = if (transaction.id == 0L) (stored.keys.maxOrNull() ?: 0L) + 1 else transaction.id
        stored[id] = transaction.copy(id = id)
        revision.update { it + 1 }
        return id
    }
    override suspend fun delete(id: Long) {
        stored.remove(id)
        revision.update { it + 1 }
    }
    fun contains(id: Long): Boolean = stored.containsKey(id)
}

internal class FakeReceiptRepository : ReceiptRepository {
    private var nextId = 1L
    private val items = mutableMapOf<Long, Receipt>()
    override fun observeAll(): Flow<List<Receipt>> = flowOf(items.values.toList())
    override suspend fun get(id: Long): Receipt? = items[id]
    override suspend fun upsert(receipt: Receipt): Long {
        val id = if (receipt.id == 0L) nextId++ else receipt.id
        items[id] = receipt.copy(id = id)
        return id
    }
    override suspend fun delete(id: Long) {
        items.remove(id)
    }
}

internal class FakeInboxRepository : InboxRepository {
    override fun observe(status: InboxStatus?): Flow<List<InboxItem>> = flowOf(emptyList())
    override suspend fun get(id: Long): InboxItem? = null
    override suspend fun upsert(item: InboxItem): Long = item.id
    override suspend fun setStatus(id: Long, status: InboxStatus) = Unit
}

internal class FakeSettingsRepository(
    currency: CurrencyCode,
) : SettingsRepository {
    private val base = MutableStateFlow(currency)
    override fun observeBaseCurrency(): Flow<CurrencyCode> = base
    override suspend fun setBaseCurrency(currency: CurrencyCode) {
        base.value = currency
    }
    override fun observeTheme(): Flow<String> = flowOf("system")
    override suspend fun setTheme(theme: String) = Unit
    override fun observeDynamicColor(): Flow<Boolean> = flowOf(true)
    override suspend fun setDynamicColor(enabled: Boolean) = Unit
    override fun observeLockEnabled(): Flow<Boolean> = flowOf(false)
    override suspend fun setLockEnabled(enabled: Boolean) = Unit
    override fun observeNotificationCaptureEnabled(): Flow<Boolean> = flowOf(false)
    override suspend fun setNotificationCaptureEnabled(enabled: Boolean) = Unit
    override fun observeNotificationAllowlist(): Flow<Set<String>> = flowOf(emptySet())
    override suspend fun setNotificationAllowlist(packages: Set<String>) = Unit
    override fun observeSilentAutomationSaves(): Flow<Boolean> = flowOf(false)
    override suspend fun setSilentAutomationSaves(enabled: Boolean) = Unit
    override fun observeInsightRules(): Flow<InsightRules> = flowOf(InsightRules())
    override suspend fun setInsightRules(rules: InsightRules) = Unit
    override fun observeOnboardingDone(): Flow<Boolean> = flowOf(true)
    override suspend fun setOnboardingDone(done: Boolean) = Unit
    override fun observeAutoBackupLastSuccessAt(): Flow<Instant?> = flowOf(null)
    override suspend fun setAutoBackupLastSuccessAt(at: Instant?) = Unit
    override fun observeAutoBackupLastFailureAt(): Flow<Instant?> = flowOf(null)
    override suspend fun setAutoBackupLastFailureAt(at: Instant?) = Unit
    override fun observeAutoBackupLastFailureReason(): Flow<String?> = flowOf(null)
    override suspend fun setAutoBackupLastFailureReason(reason: String?) = Unit
}

internal object FakeReceiptReader : ReceiptReader {
    override suspend fun read(imagePath: String) = ReceiptReadResult(
        draft = TransactionDraft(),
        rawText = null,
        engine = ReceiptEngine.HEURISTIC,
    )
}
