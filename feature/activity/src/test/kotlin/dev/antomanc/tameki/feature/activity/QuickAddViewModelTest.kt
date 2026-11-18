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
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.TransactionSource
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
        merchantMemory: Map<String, Long> = emptyMap(),
        transactions: FakeTransactionRepository = FakeTransactionRepository(),
        receiptReader: ReceiptReader = FakeReceiptReader,
    ): QuickAddViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        return QuickAddViewModel(
            transactions = transactions,
            accounts = FakeAccountRepository(accounts),
            categories = FakeCategoryRepository(categories, merchantMemory),
            receipts = FakeReceiptRepository(),
            inbox = FakeInboxRepository(),
            settings = FakeSettingsRepository(eur),
            receiptReader = receiptReader,
            clock = clock,
            appContext = app,
        )
    }

    private fun storedTransaction(id: Long = 7L) = Transaction(
        id = id,
        kind = TransactionKind.EXPENSE,
        amount = Money(1_000, eur),
        accountId = 1,
        occurredAt = today.atTime(9, 0),
    )

    private fun writeTempJpeg(): String {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val file = java.io.File(app.cacheDir, "test_scan_${System.nanoTime()}.jpg")
        file.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
        return file.absolutePath
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

    @Test
    fun applyCapturedReceipt_prefillsDraftAndEnablesSave() = runTest {
        val reader = object : ReceiptReader {
            override suspend fun read(imagePath: String) = ReceiptReadResult(
                draft = TransactionDraft(
                    amount = Money(1_250, eur),
                    merchant = "Bar Centrale",
                ),
                rawText = "Bar",
                engine = ReceiptEngine.GEMINI_NANO,
            )
        }
        val vm = viewModel(receiptReader = reader)
        advanceUntilIdle()
        vm.applyCapturedReceipt(writeTempJpeg(), fillEmptyOnly = false)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertThat(state.visible).isTrue()
        assertThat(state.readingReceipt).isFalse()
        assertThat(state.money().minor).isEqualTo(1_250L)
        assertThat(state.merchant).isEqualTo("Bar Centrale")
        assertThat(state.receiptPath).isNotNull()
        assertThat(state.receiptId).isEqualTo(1L)
        assertThat(state.receiptEngineLabel).isEqualTo("Gemini Nano")
        assertThat(state.canSave).isTrue()
    }

    @Test
    fun applyCapturedReceipt_fillEmptyOnly_doesNotOverwriteAmount() = runTest {
        val reader = object : ReceiptReader {
            override suspend fun read(imagePath: String) = ReceiptReadResult(
                draft = TransactionDraft(
                    amount = Money(9_999, eur),
                    merchant = "FromScan",
                ),
                rawText = "x",
                engine = ReceiptEngine.ML_KIT_OCR,
            )
        }
        val vm = viewModel(receiptReader = reader)
        advanceUntilIdle()
        vm.openNew()
        advanceUntilIdle()
        vm.digit('5')
        vm.digit('0')
        advanceUntilIdle()
        vm.applyCapturedReceipt(writeTempJpeg(), fillEmptyOnly = true)
        advanceUntilIdle()
        val state = vm.uiState.value
        // Numpad "50" without decimal → €50.00 (5000 minor).
        assertThat(state.money().minor).isEqualTo(5_000L)
        assertThat(state.merchant).isEqualTo("FromScan")
        assertThat(state.receiptPath).isNotNull()
    }

    @Test
    fun applyCapturedReceipt_readFailure_attachesPhotoAndHints() = runTest {
        val reader = object : ReceiptReader {
            override suspend fun read(imagePath: String): ReceiptReadResult {
                error("unavailable")
            }
        }
        val vm = viewModel(receiptReader = reader)
        advanceUntilIdle()
        vm.applyCapturedReceipt(writeTempJpeg(), fillEmptyOnly = false)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertThat(state.visible).isTrue()
        assertThat(state.receiptPath).isNotNull()
        assertThat(state.money().minor).isEqualTo(0L)
        assertThat(state.snackbar).isEqualTo("receipt_read_failed")
        assertThat(state.canSave).isFalse()
    }

    @Test
    fun openNewWithoutMerchantLeavesCategoryUnselected() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.openNew()
        advanceUntilIdle()
        // Ranking is only a layout order: a cold start must not guess a category.
        assertThat(vm.uiState.value.categories).isNotEmpty()
        assertThat(vm.uiState.value.selectedCategoryId).isNull()
    }

    @Test
    fun openFromDraftWithMerchantMemorySelectsTheRememberedCategory() = runTest {
        val vm = viewModel(
            categories = listOf(
                Category(1, "Groceries", CategoryKind.EXPENSE, "groceries", 0),
                Category(2, "Coffee", CategoryKind.EXPENSE, "coffee", 0),
            ),
            merchantMemory = mapOf("Bar Centrale" to 2L),
        )
        advanceUntilIdle()
        vm.openFromDraft(TransactionDraft(amount = Money(150, eur), merchant = "Bar Centrale"))
        advanceUntilIdle()
        assertThat(vm.uiState.value.selectedCategoryId).isEqualTo(2L)
    }

    @Test
    fun savePreservesEditProvenance() = runTest {
        val original = storedTransaction().copy(
            categoryId = 1L,
            occurredAt = today.atTime(9, 15),
            source = TransactionSource.NOTIFICATION,
            tags = setOf("work", "reimbursable"),
            recurringId = 42L,
        )
        val stored = mutableMapOf(7L to original)
        val vm = viewModel(transactions = FakeTransactionRepository(stored))
        advanceUntilIdle()
        vm.openEdit(7)
        advanceUntilIdle()
        vm.digit('9')
        // The date chip may still move the day; the edit must not invent a new time of day.
        vm.setDate(today.minusDays(1))
        vm.save()
        advanceUntilIdle()

        val saved = stored.getValue(7L)
        assertThat(saved.amount.minor).isNotEqualTo(original.amount.minor)
        assertThat(saved.occurredAt).isEqualTo(today.minusDays(1).atTime(9, 15))
        assertThat(saved.source).isEqualTo(TransactionSource.NOTIFICATION)
        assertThat(saved.tags).containsExactly("work", "reimbursable")
        assertThat(saved.recurringId).isEqualTo(42L)
        assertThat(saved.categoryId).isEqualTo(1L)
    }

    @Test
    fun deleteCurrent_removesTheEditedTransactionAndDismisses() = runTest {
        val stored = mutableMapOf(7L to storedTransaction())
        val transactions = FakeTransactionRepository(stored)
        val vm = viewModel(transactions = transactions)
        advanceUntilIdle()
        vm.openEdit(7)
        advanceUntilIdle()
        vm.deleteCurrent()
        advanceUntilIdle()
        val state = vm.uiState.value
        assertThat(state.visible).isFalse()
        assertThat(state.editingId).isNull()
        assertThat(state.deleteGeneration).isEqualTo(1L)
        assertThat(transactions.contains(7)).isFalse()
    }

    @Test
    fun undoDelete_restoresTheTransaction() = runTest {
        val stored = mutableMapOf(7L to storedTransaction())
        val transactions = FakeTransactionRepository(stored)
        val vm = viewModel(transactions = transactions)
        advanceUntilIdle()
        vm.openEdit(7)
        advanceUntilIdle()
        vm.deleteCurrent()
        advanceUntilIdle()
        vm.undoDelete()
        advanceUntilIdle()
        assertThat(transactions.contains(7)).isTrue()
        assertThat(vm.uiState.value.snackbar).isNull()
    }

    @Test
    fun clearReceipt_removesAttachment() = runTest {
        val reader = object : ReceiptReader {
            override suspend fun read(imagePath: String) = ReceiptReadResult(
                draft = TransactionDraft(amount = Money(100, eur)),
                rawText = null,
                engine = ReceiptEngine.HEURISTIC,
            )
        }
        val vm = viewModel(receiptReader = reader)
        advanceUntilIdle()
        vm.applyCapturedReceipt(writeTempJpeg(), fillEmptyOnly = false)
        advanceUntilIdle()
        vm.clearReceipt()
        val state = vm.uiState.value
        assertThat(state.receiptPath).isNull()
        assertThat(state.receiptId).isNull()
        assertThat(state.receiptEngineLabel).isNull()
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
    private val memory: Map<String, Long> = emptyMap(),
) : CategoryRepository {
    override fun observeAll(includeArchived: Boolean): Flow<List<Category>> = flowOf(categories)
    override suspend fun get(id: Long): Category? = categories.firstOrNull { it.id == id }
    override suspend fun upsert(category: Category): Long = category.id
    override suspend fun archive(id: Long, archived: Boolean) = Unit
    override fun observeMerchantCategoryMemory(): Flow<Map<String, Long>> = flowOf(memory)
    override suspend fun rememberMerchantCategory(merchant: String, categoryId: Long) = Unit
}

private class FakeTransactionRepository(
    private val stored: MutableMap<Long, Transaction> = mutableMapOf(),
) : TransactionRepository {
    override fun observe(filter: TransactionFilter): Flow<List<Transaction>> =
        flowOf(stored.values.toList())
    override fun observeDayTotals(filter: TransactionFilter): Flow<List<DayTotal>> = flowOf(emptyList())
    override suspend fun get(id: Long): Transaction? = stored[id]
    override suspend fun upsert(transaction: Transaction): Long {
        val id = if (transaction.id == 0L) (stored.keys.maxOrNull() ?: 0L) + 1 else transaction.id
        stored[id] = transaction.copy(id = id)
        return id
    }
    override suspend fun delete(id: Long) {
        stored.remove(id)
    }
    fun contains(id: Long): Boolean = stored.containsKey(id)
}

private class FakeReceiptRepository : ReceiptRepository {
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
