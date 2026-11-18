package dev.antomanc.tameki.feature.subscriptions

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
import dev.antomanc.tameki.core.model.insight.InsightRules
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.ReminderScheduler
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory recurring store: same observable contract as Room, no database. */
class FakeRecurringRepository(initial: List<Recurring> = emptyList()) : RecurringRepository {
    val items = MutableStateFlow(initial)
    val upserts = mutableListOf<Recurring>()

    override fun observeAll(): Flow<List<Recurring>> = items

    override suspend fun get(id: Long): Recurring? = items.value.firstOrNull { it.id == id }

    override suspend fun upsert(recurring: Recurring): Long {
        val id = if (recurring.id == 0L) (items.value.maxOfOrNull { it.id } ?: 0L) + 1 else recurring.id
        val stored = recurring.copy(id = id)
        items.update { list -> list.filterNot { it.id == id } + stored }
        upserts += stored
        return id
    }

    override suspend fun delete(id: Long) {
        items.update { list -> list.filterNot { it.id == id } }
    }
}

class FakeAccountRepository(private val accounts: List<Account> = emptyList()) : AccountRepository {
    override fun observeAll(includeArchived: Boolean): Flow<List<Account>> = flowOf(accounts)
    override suspend fun get(id: Long): Account? = accounts.firstOrNull { it.id == id }
    override suspend fun upsert(account: Account): Long = account.id
    override suspend fun archive(id: Long, archived: Boolean) = Unit
}

class FakeCategoryRepository(private val categories: List<Category> = emptyList()) : CategoryRepository {
    override fun observeAll(includeArchived: Boolean): Flow<List<Category>> = flowOf(categories)
    override suspend fun get(id: Long): Category? = categories.firstOrNull { it.id == id }
    override suspend fun upsert(category: Category): Long = category.id
    override suspend fun archive(id: Long, archived: Boolean) = Unit
    override fun observeMerchantCategoryMemory(): Flow<Map<String, Long>> = flowOf(emptyMap())
    override suspend fun rememberMerchantCategory(merchant: String, categoryId: Long) = Unit
}

/** The subset of settings these screens read, plus records of what was written. */
class FakeSettingsRepository(currency: CurrencyCode = CurrencyCode("EUR")) : SettingsRepository {
    private val base = MutableStateFlow(currency)
    private val rules = MutableStateFlow(InsightRules())

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
    override fun observeInsightRules(): Flow<InsightRules> = rules
    override suspend fun setInsightRules(rules: InsightRules) {
        this.rules.value = rules
    }

    override fun observeOnboardingDone(): Flow<Boolean> = flowOf(true)
    override suspend fun setOnboardingDone(done: Boolean) = Unit
    override fun observeAutoBackupLastSuccessAt(): Flow<Instant?> = flowOf(null)
    override suspend fun setAutoBackupLastSuccessAt(at: Instant?) = Unit
    override fun observeAutoBackupLastFailureAt(): Flow<Instant?> = flowOf(null)
    override suspend fun setAutoBackupLastFailureAt(at: Instant?) = Unit
    override fun observeAutoBackupLastFailureReason(): Flow<String?> = flowOf(null)
    override suspend fun setAutoBackupLastFailureReason(reason: String?) = Unit
}

/** Records renewals scheduled and cancelled; no WorkManager involved. */
class FakeReminderScheduler : ReminderScheduler {
    val scheduled = mutableListOf<Pair<Long, LocalDate>>()
    val cancelled = mutableListOf<Long>()
    val trialEndings = mutableListOf<Pair<Long, LocalDate>>()
    val cancelledTrials = mutableListOf<Long>()

    override suspend fun ensureDailyWorker() = Unit

    override suspend fun scheduleRecurringReminder(recurringId: Long, on: LocalDate, daysBefore: Int) {
        scheduled += recurringId to on
    }

    override suspend fun cancelRecurringReminder(recurringId: Long) {
        cancelled += recurringId
    }

    override suspend fun scheduleTrialEnding(recurringId: Long, endsOn: LocalDate) {
        trialEndings += recurringId to endsOn
    }

    override suspend fun cancelTrialEnding(recurringId: Long) {
        cancelledTrials += recurringId
    }
}

/** Recurring fixture with every field spelled out, so tests read as examples. */
internal fun recurring(
    id: Long,
    name: String,
    minor: Long,
    nextDate: LocalDate,
    type: dev.antomanc.tameki.core.model.entity.RecurringType =
        dev.antomanc.tameki.core.model.entity.RecurringType.SUBSCRIPTION,
    status: dev.antomanc.tameki.core.model.entity.RecurringStatus =
        dev.antomanc.tameki.core.model.entity.RecurringStatus.ACTIVE,
    cycle: dev.antomanc.tameki.core.model.entity.Cycle =
        dev.antomanc.tameki.core.model.entity.Cycle(1, dev.antomanc.tameki.core.model.entity.CycleUnit.MONTH),
    sharedWith: Int = 1,
    worth: dev.antomanc.tameki.core.model.entity.Worth? = null,
    worthRatedOn: LocalDate? = null,
    currency: CurrencyCode = CurrencyCode("EUR"),
    startDate: LocalDate = LocalDate.of(2025, 1, 1),
    categoryId: Long? = null,
    brandKey: String? = null,
    priceHistory: List<dev.antomanc.tameki.core.model.entity.PricePoint> = emptyList(),
): Recurring = Recurring(
    id = id,
    type = type,
    name = name,
    amount = Money(minor, currency),
    cycle = cycle,
    startDate = startDate,
    nextDate = nextDate,
    status = status,
    sharedWith = sharedWith,
    worth = worth,
    worthRatedOn = worthRatedOn,
    categoryId = categoryId,
    brandKey = brandKey,
    priceHistory = priceHistory,
)
