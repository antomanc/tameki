package dev.antomanc.tameki.feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Budget
import dev.antomanc.tameki.core.model.entity.BudgetPeriod
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.InboxItem
import dev.antomanc.tameki.core.model.entity.InboxStatus
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.data.exchange.FakeExchangeRates
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.insight.InsightRules
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.BudgetRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.DayTotal
import dev.antomanc.tameki.core.model.repository.InboxRepository
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionFilter
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import dev.antomanc.tameki.core.ui.format.TimeOfDay
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Runs under Robolectric: upcoming-day labels come from the platform's ICU data
 * ([dev.antomanc.tameki.core.ui.format.RelativeDates]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
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
            accounts = FakeAccounts(listOf(Account(1, "Carta", eur, AccountType.CARD))),
            inbox = FakeInbox(),
            settings = FakeSettings(eur),
            exchangeRates = FakeExchangeRates(),
            clock = clock,
        )
        vm.uiState.test {
            dispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state.loading).isFalse()
            assertThat(state.hero?.mode).isEqualTo(HeroMode.AVAILABLE)
            assertThat(state.hero?.amount?.minor).isEqualTo(80_000)
            assertThat(state.recent).hasSize(1)
            // The recent row keeps its account and time: same quiet meta as Activity.
            assertThat(state.recent.single().meta).isEqualTo(
                "Carta · ${TimeOfDay.short(LocalDateTime.of(2026, 11, 10, 12, 0))}",
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun renewalRowUsesTheLocaleDayWords() = runTest {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.ITALIAN)
        try {
            val due = Recurring(
                id = 1,
                type = RecurringType.SUBSCRIPTION,
                name = "Netflix",
                amount = Money(1349, eur),
                cycle = Cycle(1, CycleUnit.DAY),
                startDate = LocalDate.of(2026, 11, 21),
                nextDate = LocalDate.of(2026, 11, 21),
            )
            val vm = HomeViewModel(
                transactions = FakeTransactions(MutableStateFlow(emptyList())),
                recurrings = FakeRecurrings(flowOf(listOf(due))),
                budgets = FakeBudgets(MutableStateFlow(emptyList())),
                categories = FakeCategories(),
                accounts = FakeAccounts(),
                inbox = FakeInbox(),
                settings = FakeSettings(eur),
                exchangeRates = FakeExchangeRates(),
                clock = clock,
            )
            vm.uiState.test {
                dispatcher.scheduler.advanceUntilIdle()
                val state = expectMostRecentItem()
                assertThat(state.renewals.single().relativeDate).isEqualTo("Tra 3 giorni")
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            Locale.setDefault(previous)
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

private class FakeRecurrings(
    private val all: Flow<List<Recurring>> = flowOf(emptyList()),
) : RecurringRepository {
    override fun observeAll() = all
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
        listOf(Category(1, "Alimentari", CategoryKind.EXPENSE, "groceries", 1)),
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

private class FakeAccounts(
    private val accounts: List<Account> = emptyList(),
) : AccountRepository {
    override fun observeAll(includeArchived: Boolean): Flow<List<Account>> = flowOf(accounts)
    override suspend fun get(id: Long): Account? = accounts.firstOrNull { it.id == id }
    override suspend fun upsert(account: Account): Long = account.id
    override suspend fun archive(id: Long, archived: Boolean) = Unit
}
