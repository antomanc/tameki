package dev.antomanc.tameki.feature.activity

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuickAddViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val eur = CurrencyCode("EUR")
    private val today = LocalDate.of(2026, 11, 18)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private fun viewModel(
        accounts: List<Account> = listOf(
            Account(1, "Cash", eur, AccountType.CASH),
            Account(2, "Card", eur, AccountType.CARD),
        ),
        categories: List<Category> = listOf(
            Category(1, "Groceries", CategoryKind.EXPENSE, "groceries", 0),
        ),
    ): QuickAddViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        return QuickAddViewModel(
            transactions = FakeTransactionRepository(),
            accounts = FakeAccountRepository(accounts),
            categories = FakeCategoryRepository(categories),
            receipts = FakeReceiptRepository(),
            inbox = FakeInboxRepository(),
            settings = FakeSettingsRepository(eur),
            receiptReader = FakeReceiptReader,
            clock = clock,
            appContext = app,
        )
    }

    @Test
    fun openFromDraft_matchingAccountHint_selectsThatAccount() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.openFromDraft(
            TransactionDraft(
                amount = Money(1_250, eur),
                accountHint = "card",
            ),
        )
        advanceUntilIdle()
        val state = vm.uiState.value
        assertThat(state.accountId).isEqualTo(2L)
        assertThat(state.canSave).isTrue()
    }

    @Test
    fun openFromDraft_noHint_selectsFirstAccount() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.openFromDraft(TransactionDraft(amount = Money(500, eur)))
        advanceUntilIdle()
        val state = vm.uiState.value
        assertThat(state.accountId).isEqualTo(1L)
        assertThat(state.canSave).isTrue()
    }

    @Test
    fun openFromDraft_completeDraft_canSave() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.openFromDraft(
            TransactionDraft(
                amount = Money(2_000, eur),
                merchant = "Esselunga",
                accountHint = "CASH",
            ),
        )
        advanceUntilIdle()
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.visible).isTrue()
            assertThat(state.accountId).isEqualTo(1L)
            assertThat(state.canSave).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private class FakeAccountRepository(
    private val accounts: List<Account>,
) : AccountRepository {
    override fun observeAll(includeArchived: Boolean): Flow<List<Account>> = flowOf(accounts)
    override suspend fun get(id: Long): Account? = accounts.firstOrNull { it.id == id }
    override suspend fun upsert(account: Account): Long = account.id
    override suspend fun archive(id: Long, archived: Boolean) = Unit
}

private class FakeCategoryRepository(
    private val categories: List<Category>,
) : CategoryRepository {
    override fun observeAll(includeArchived: Boolean): Flow<List<Category>> = flowOf(categories)
    override suspend fun get(id: Long): Category? = categories.firstOrNull { it.id == id }
    override suspend fun upsert(category: Category): Long = category.id
    override suspend fun archive(id: Long, archived: Boolean) = Unit
    override fun observeMerchantCategoryMemory(): Flow<Map<String, Long>> = flowOf(emptyMap())
    override suspend fun rememberMerchantCategory(merchant: String, categoryId: Long) = Unit
}

private class FakeTransactionRepository : TransactionRepository {
    override fun observe(filter: TransactionFilter): Flow<List<Transaction>> = flowOf(emptyList())
    override fun observeDayTotals(filter: TransactionFilter): Flow<List<DayTotal>> = flowOf(emptyList())
    override suspend fun get(id: Long): Transaction? = null
    override suspend fun upsert(transaction: Transaction): Long = transaction.id
    override suspend fun delete(id: Long) = Unit
}

private class FakeReceiptRepository : ReceiptRepository {
    override fun observeAll(): Flow<List<Receipt>> = flowOf(emptyList())
    override suspend fun get(id: Long): Receipt? = null
    override suspend fun upsert(receipt: Receipt): Long = receipt.id
    override suspend fun delete(id: Long) = Unit
}

private class FakeInboxRepository : InboxRepository {
    override fun observe(status: InboxStatus?): Flow<List<InboxItem>> = flowOf(emptyList())
    override suspend fun get(id: Long): InboxItem? = null
    override suspend fun upsert(item: InboxItem): Long = item.id
    override suspend fun setStatus(id: Long, status: InboxStatus) = Unit
}

private class FakeSettingsRepository(
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

private object FakeReceiptReader : ReceiptReader {
    override suspend fun read(imagePath: String) = ReceiptReadResult(
        draft = TransactionDraft(),
        rawText = null,
        engine = ReceiptEngine.HEURISTIC,
    )
}
