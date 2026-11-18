package dev.antomanc.tameki.feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Budget
import dev.antomanc.tameki.core.model.entity.BudgetPeriod
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.InboxItem
import dev.antomanc.tameki.core.model.entity.InboxStatus
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.insight.InsightRules
import dev.antomanc.tameki.core.model.repository.BudgetRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.DayTotal
import dev.antomanc.tameki.core.model.repository.InboxRepository
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionFilter
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val eur = CurrencyCode("EUR")
    private val clock = Clock.fixed(Instant.parse("2026-11-18T10:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun exposesAvailableHeroFromBudget() = runTest {
        val budgets = MutableStateFlow(
            listOf(Budget(1, "Mese", limit = Money(100_000, eur), period = BudgetPeriod.MONTH)),
        )
        val txs = MutableStateFlow(
            listOf(
                Transaction(
                    id = 1,
                    kind = TransactionKind.EXPENSE,
                    amount = Money(20_000, eur),
                    accountId = 1,
                    merchant = "Esselunga",
                    occurredAt = LocalDateTime.of(2026, 11, 10, 12, 0),
                ),
            ),
        )
        val vm = HomeViewModel(
            transactions = FakeTransactions(txs),
            recurrings = FakeRecurrings(),
            budgets = FakeBudgets(budgets),
            categories = FakeCategories(),
            inbox = FakeInbox(),
            settings = FakeSettings(eur),
            clock = clock,
        )
        vm.uiState.test {
            dispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state.loading).isFalse()
            assertThat(state.hero?.mode).isEqualTo(HeroMode.AVAILABLE)
            assertThat(state.hero?.amount?.minor).isEqualTo(80_000)
            assertThat(state.recent).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private class FakeTransactions(
    private val all: MutableStateFlow<List<Transaction>>,
) : TransactionRepository {
    override fun observe(filter: TransactionFilter): Flow<List<Transaction>> = all.map { list ->
        list.filter { tx ->
            val day = tx.occurredAt.toLocalDate()
            (filter.from == null || !day.isBefore(filter.from)) &&
                (filter.to == null || !day.isAfter(filter.to))
        }
    }
    override fun observeDayTotals(filter: TransactionFilter) = flowOf(emptyList<DayTotal>())
    override suspend fun get(id: Long) = all.value.firstOrNull { it.id == id }
    override suspend fun upsert(transaction: Transaction) = transaction.id
    override suspend fun delete(id: Long) = Unit
}

private class FakeRecurrings : RecurringRepository {
    override fun observeAll() = flowOf(emptyList<Recurring>())
    override suspend fun get(id: Long) = null
    override suspend fun upsert(recurring: Recurring) = 0L
    override suspend fun delete(id: Long) = Unit
}

private class FakeBudgets(private val all: MutableStateFlow<List<Budget>>) : BudgetRepository {
    override fun observeAll() = all
    override suspend fun get(id: Long) = all.value.firstOrNull { it.id == id }
    override suspend fun upsert(budget: Budget) = budget.id
    override suspend fun delete(id: Long) = Unit
}

private class FakeCategories : CategoryRepository {
    override fun observeAll(includeArchived: Boolean) = flowOf(
        listOf(Category(1, "Spesa", CategoryKind.EXPENSE, "groceries", 1)),
    )
    override suspend fun get(id: Long) = null
    override suspend fun upsert(category: Category) = 0L
    override suspend fun archive(id: Long, archived: Boolean) = Unit
    override fun observeMerchantCategoryMemory() = flowOf(emptyMap<String, Long>())
    override suspend fun rememberMerchantCategory(merchant: String, categoryId: Long) = Unit
}

private class FakeInbox : InboxRepository {
    override fun observe(status: InboxStatus?) = flowOf(emptyList<InboxItem>())
    override suspend fun get(id: Long) = null
    override suspend fun upsert(item: InboxItem) = 0L
    override suspend fun setStatus(id: Long, status: InboxStatus) = Unit
}

private class FakeSettings(private val currency: CurrencyCode) : SettingsRepository {
    override fun observeBaseCurrency() = flowOf(currency)
    override suspend fun setBaseCurrency(currency: CurrencyCode) = Unit
    override fun observeTheme() = flowOf("system")
    override suspend fun setTheme(theme: String) = Unit
    override fun observeDynamicColor() = flowOf(true)
    override suspend fun setDynamicColor(enabled: Boolean) = Unit
    override fun observeLockEnabled() = flowOf(false)
    override suspend fun setLockEnabled(enabled: Boolean) = Unit
    override fun observeNotificationCaptureEnabled() = flowOf(false)
    override suspend fun setNotificationCaptureEnabled(enabled: Boolean) = Unit
    override fun observeNotificationAllowlist() = flowOf(emptySet<String>())
    override suspend fun setNotificationAllowlist(packages: Set<String>) = Unit
    override fun observeSilentAutomationSaves() = flowOf(false)
    override suspend fun setSilentAutomationSaves(enabled: Boolean) = Unit
    override fun observeInsightRules() = flowOf(InsightRules())
    override suspend fun setInsightRules(rules: InsightRules) = Unit
    override fun observeOnboardingDone() = flowOf(true)
    override suspend fun setOnboardingDone(done: Boolean) = Unit
    override fun observeAutoBackupLastSuccessAt() = flowOf(null)
    override suspend fun setAutoBackupLastSuccessAt(at: Instant?) = Unit
    override fun observeAutoBackupLastFailureAt() = flowOf(null)
    override suspend fun setAutoBackupLastFailureAt(at: Instant?) = Unit
    override fun observeAutoBackupLastFailureReason() = flowOf(null)
    override suspend fun setAutoBackupLastFailureReason(reason: String?) = Unit
}
