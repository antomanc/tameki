package dev.antomanc.tameki.feature.insights

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Budget
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.Goal
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.insight.InsightRules
import dev.antomanc.tameki.core.model.repository.BudgetRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.DayTotal
import dev.antomanc.tameki.core.model.repository.GoalRepository
import dev.antomanc.tameki.core.model.repository.InsightSnoozeRepository
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionFilter
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal val eur = CurrencyCode("EUR")
internal fun money(minor: Long, currency: CurrencyCode = eur): Money = Money(minor, currency)

/** Transactions fixture: dated so month buckets and ranges are easy to reason about. */
internal fun expense(
    id: Long,
    minor: Long,
    date: LocalDate,
    categoryId: Long? = 1L,
    currency: CurrencyCode = eur,
): Transaction = Transaction(
    id = id,
    kind = TransactionKind.EXPENSE,
    amount = Money(minor, currency),
    accountId = 1L,
    categoryId = categoryId,
    occurredAt = LocalDateTime.of(date, java.time.LocalTime.of(12, 0)),
    source = TransactionSource.MANUAL,
)

class FakeTransactionRepository(initial: List<Transaction> = emptyList()) : TransactionRepository {
    val items = MutableStateFlow(initial)

    override fun observe(filter: TransactionFilter): Flow<List<Transaction>> = items.map { list ->
        list.filter { transaction ->
            val date = transaction.occurredAt.toLocalDate()
            (filter.from == null || !date.isBefore(filter.from)) &&
                (filter.to == null || !date.isAfter(filter.to)) &&
                (filter.categoryId == null || transaction.categoryId == filter.categoryId) &&
                (filter.accountId == null || transaction.accountId == filter.accountId) &&
                (filter.source == null || transaction.source == filter.source)
        }
    }

    override fun observeDayTotals(filter: TransactionFilter): Flow<List<DayTotal>> = flowOf(emptyList())
    override suspend fun get(id: Long): Transaction? = items.value.firstOrNull { it.id == id }
    override suspend fun upsert(transaction: Transaction): Long {
        items.update { list -> list.filterNot { it.id == transaction.id } + transaction }
        return transaction.id
    }
    override suspend fun delete(id: Long) {
        items.update { list -> list.filterNot { it.id == id } }
    }
}

class FakeRecurringRepository(initial: List<Recurring> = emptyList()) : RecurringRepository {
    val items = MutableStateFlow(initial)
    override fun observeAll(): Flow<List<Recurring>> = items
    override suspend fun get(id: Long): Recurring? = items.value.firstOrNull { it.id == id }
    override suspend fun upsert(recurring: Recurring): Long {
        items.update { list -> list.filterNot { it.id == recurring.id } + recurring }
        return recurring.id
    }
    override suspend fun delete(id: Long) {
        items.update { list -> list.filterNot { it.id == id } }
    }
}

class FakeBudgetRepository(initial: List<Budget> = emptyList()) : BudgetRepository {
    val items = MutableStateFlow(initial)
    override fun observeAll(): Flow<List<Budget>> = items
    override suspend fun get(id: Long): Budget? = items.value.firstOrNull { it.id == id }
    override suspend fun upsert(budget: Budget): Long {
        val id = if (budget.id == 0L) (items.value.maxOfOrNull { it.id } ?: 0L) + 1 else budget.id
        items.update { list -> list.filterNot { it.id == id } + budget.copy(id = id) }
        return id
    }
    override suspend fun delete(id: Long) {
        items.update { list -> list.filterNot { it.id == id } }
    }
}

class FakeGoalRepository(initial: List<Goal> = emptyList()) : GoalRepository {
    val items = MutableStateFlow(initial)
    override fun observeAll(includeArchived: Boolean): Flow<List<Goal>> = items
    override suspend fun get(id: Long): Goal? = items.value.firstOrNull { it.id == id }
    override suspend fun upsert(goal: Goal): Long {
        val id = if (goal.id == 0L) (items.value.maxOfOrNull { it.id } ?: 0L) + 1 else goal.id
        items.update { list -> list.filterNot { it.id == id } + goal.copy(id = id) }
        return id
    }
    override suspend fun archive(id: Long, archived: Boolean) {
        items.update { list -> list.map { if (it.id == id) it.copy(archived = archived) else it } }
    }
}

class FakeCategoryRepository(initial: List<Category> = emptyList()) : CategoryRepository {
    val items = MutableStateFlow(initial)
    override fun observeAll(includeArchived: Boolean): Flow<List<Category>> = items
    override suspend fun get(id: Long): Category? = items.value.firstOrNull { it.id == id }
    override suspend fun upsert(category: Category): Long = category.id
    override suspend fun archive(id: Long, archived: Boolean) = Unit
    override fun observeMerchantCategoryMemory(): Flow<Map<String, Long>> = flowOf(emptyMap())
    override suspend fun rememberMerchantCategory(merchant: String, categoryId: Long) = Unit
}

internal fun category(id: Long, name: String, key: String, kind: CategoryKind = CategoryKind.EXPENSE) =
    Category(id = id, name = name, kind = kind, icon = key, color = 0L)

class FakeSettingsRepository(currency: CurrencyCode = eur, rules: InsightRules = InsightRules()) : SettingsRepository {
    private val base = MutableStateFlow(currency)
    private val insightRules = MutableStateFlow(rules)

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
    override fun observeInsightRules(): Flow<InsightRules> = insightRules
    override suspend fun setInsightRules(rules: InsightRules) {
        insightRules.value = rules
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

class FakeSnoozeRepository : InsightSnoozeRepository {
    val snoozed = MutableStateFlow<Map<String, LocalDate>>(emptyMap())

    override fun observeSnoozed(on: LocalDate): Flow<Set<String>> =
        snoozed.map { map -> map.filterValues { !it.isBefore(on) }.keys }

    override suspend fun snooze(insightId: String, until: LocalDate) {
        snoozed.update { it + (insightId to until) }
    }

    override suspend fun clear(insightId: String) {
        snoozed.update { it - insightId }
    }
}
