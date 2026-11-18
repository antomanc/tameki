package dev.antomanc.tameki.core.model.repository

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Budget
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.Goal
import dev.antomanc.tameki.core.model.entity.InboxItem
import dev.antomanc.tameki.core.model.entity.InboxStatus
import dev.antomanc.tameki.core.model.entity.Receipt
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.insight.InsightRules
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Persistence boundary for accounts; Room implements this in core:data. */
interface AccountRepository {
    fun observeAll(includeArchived: Boolean = false): Flow<List<Account>>
    suspend fun get(id: Long): Account?
    suspend fun upsert(account: Account): Long
    suspend fun archive(id: Long, archived: Boolean)
}

interface CategoryRepository {
    fun observeAll(includeArchived: Boolean = false): Flow<List<Category>>
    suspend fun get(id: Long): Category?
    suspend fun upsert(category: Category): Long
    suspend fun archive(id: Long, archived: Boolean)
}

data class TransactionFilter(
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val text: String? = null,
    val tag: String? = null,
    val source: TransactionSource? = null,
)

interface TransactionRepository {
    fun observe(filter: TransactionFilter = TransactionFilter()): Flow<List<Transaction>>
    suspend fun get(id: Long): Transaction?
    suspend fun upsert(transaction: Transaction): Long
    suspend fun delete(id: Long)
}

interface RecurringRepository {
    fun observeAll(): Flow<List<Recurring>>
    suspend fun get(id: Long): Recurring?
    suspend fun upsert(recurring: Recurring): Long
    suspend fun delete(id: Long)
}

interface BudgetRepository {
    fun observeAll(): Flow<List<Budget>>
    suspend fun get(id: Long): Budget?
    suspend fun upsert(budget: Budget): Long
    suspend fun delete(id: Long)
}

interface GoalRepository {
    fun observeAll(includeArchived: Boolean = false): Flow<List<Goal>>
    suspend fun get(id: Long): Goal?
    suspend fun upsert(goal: Goal): Long
    suspend fun archive(id: Long, archived: Boolean)
}

interface ReceiptRepository {
    fun observeAll(): Flow<List<Receipt>>
    suspend fun get(id: Long): Receipt?
    suspend fun upsert(receipt: Receipt): Long
    suspend fun delete(id: Long)
}

interface InboxRepository {
    fun observe(status: InboxStatus? = InboxStatus.PENDING): Flow<List<InboxItem>>
    suspend fun get(id: Long): InboxItem?
    suspend fun upsert(item: InboxItem): Long
    suspend fun setStatus(id: Long, status: InboxStatus)
}

/**
 * App-wide preferences. Insight rule knobs live here so the Risparmia screen stays pure.
 */
interface SettingsRepository {
    fun observeBaseCurrency(): Flow<CurrencyCode>
    suspend fun setBaseCurrency(currency: CurrencyCode)

    fun observeLockEnabled(): Flow<Boolean>
    suspend fun setLockEnabled(enabled: Boolean)

    fun observeTheme(): Flow<String>
    suspend fun setTheme(theme: String)

    fun observeNotificationAllowlist(): Flow<Set<String>>
    suspend fun setNotificationAllowlist(packages: Set<String>)

    fun observeSilentAutomationSaves(): Flow<Boolean>
    suspend fun setSilentAutomationSaves(enabled: Boolean)

    fun observeInsightRules(): Flow<InsightRules>
    suspend fun setInsightRules(rules: InsightRules)
}
