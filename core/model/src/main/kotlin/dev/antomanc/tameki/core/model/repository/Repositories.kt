package dev.antomanc.tameki.core.model.repository

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
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
import java.time.Instant
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

    /** Merchant → categoryId memory for [dev.antomanc.tameki.core.model.entity.CategoryMatcher]. */
    fun observeMerchantCategoryMemory(): Flow<Map<String, Long>>
    suspend fun rememberMerchantCategory(merchant: String, categoryId: Long)
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

/** Expense/income totals for one calendar day in a single currency (caller converts if needed). */
data class DayTotal(
    val date: LocalDate,
    val currency: CurrencyCode,
    val expense: Money,
    val income: Money,
)

interface TransactionRepository {
    fun observe(filter: TransactionFilter = TransactionFilter()): Flow<List<Transaction>>
    fun observeDayTotals(filter: TransactionFilter = TransactionFilter()): Flow<List<DayTotal>>
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

    /**
     * Moves [id] from [from] to [to] only if it is still in [from], atomically. True when this
     * call made the change: two taps (or the notification's Add and the in-app Accept) racing on
     * one capture book it once. The default is for in-memory fakes; storage overrides it.
     */
    suspend fun claim(id: Long, from: InboxStatus, to: InboxStatus): Boolean {
        val item = get(id) ?: return false
        if (item.status != from) return false
        setStatus(id, to)
        return true
    }
}

/**
 * App-wide preferences. Insight rule knobs live here so the Risparmia screen stays pure.
 */
interface SettingsRepository {
    fun observeBaseCurrency(): Flow<CurrencyCode>
    suspend fun setBaseCurrency(currency: CurrencyCode)

    fun observeTheme(): Flow<String>
    suspend fun setTheme(theme: String)

    fun observeDynamicColor(): Flow<Boolean>
    suspend fun setDynamicColor(enabled: Boolean)

    fun observeLockEnabled(): Flow<Boolean>
    suspend fun setLockEnabled(enabled: Boolean)

    fun observeRemindersEnabled(): Flow<Boolean> = kotlinx.coroutines.flow.flowOf(true)
    suspend fun setRemindersEnabled(enabled: Boolean) = Unit

    fun observeDefaultRemindDays(): Flow<Int> = kotlinx.coroutines.flow.flowOf(2)
    suspend fun setDefaultRemindDays(days: Int) = Unit

    fun observeNotificationCaptureEnabled(): Flow<Boolean>
    suspend fun setNotificationCaptureEnabled(enabled: Boolean)

    fun observeNotificationAllowlist(): Flow<Set<String>>
    suspend fun setNotificationAllowlist(packages: Set<String>)

    fun observeSilentAutomationSaves(): Flow<Boolean>
    suspend fun setSilentAutomationSaves(enabled: Boolean)

    fun observeInsightRules(): Flow<InsightRules>
    suspend fun setInsightRules(rules: InsightRules)

    fun observeOnboardingDone(): Flow<Boolean>
    suspend fun setOnboardingDone(done: Boolean)

    fun observeAutoBackupLastSuccessAt(): Flow<Instant?>
    suspend fun setAutoBackupLastSuccessAt(at: Instant?)

    fun observeAutoBackupLastFailureAt(): Flow<Instant?>
    suspend fun setAutoBackupLastFailureAt(at: Instant?)

    fun observeAutoBackupLastFailureReason(): Flow<String?>
    suspend fun setAutoBackupLastFailureReason(reason: String?)
}
