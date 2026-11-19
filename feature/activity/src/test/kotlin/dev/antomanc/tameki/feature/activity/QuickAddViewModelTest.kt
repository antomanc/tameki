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
import dev.antomanc.tameki.core.model.entity.Split
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.scan.api.ReceiptEngine
import dev.antomanc.tameki.scan.api.ReceiptReadResult
import dev.antomanc.tameki.scan.api.ReceiptReader
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
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
            // The test main dispatcher drives "IO" too, so advanceUntilIdle finishes receipt copies.
            io = kotlinx.coroutines.Dispatchers.Main,
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
        assertThat(state.notice).isInstanceOf(QuickAddNotice.ReceiptFailed::class.java)
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
        vm.save()
        advanceUntilIdle()

        val saved = stored.getValue(7L)
        assertThat(saved.amount.minor).isNotEqualTo(original.amount.minor)
        // The edit must not invent a new time of day, provenance or tags.
        assertThat(saved.occurredAt).isEqualTo(original.occurredAt)
        assertThat(saved.source).isEqualTo(TransactionSource.NOTIFICATION)
        assertThat(saved.tags).containsExactly("work", "reimbursable")
        assertThat(saved.recurringId).isEqualTo(42L)
        assertThat(saved.categoryId).isEqualTo(1L)
    }

    @Test
    fun saveMovedToAnotherDayDropsTheRecurringLink() = runTest {
        val original = storedTransaction().copy(
            occurredAt = today.atTime(9, 15),
            recurringId = 42L,
        )
        val stored = mutableMapOf(7L to original)
        val vm = viewModel(transactions = FakeTransactionRepository(stored))
        advanceUntilIdle()
        vm.openEdit(7)
        advanceUntilIdle()
        vm.setDate(today.minusDays(1))
        vm.save()
        advanceUntilIdle()

        val saved = stored.getValue(7L)
        // A recurring occurrence is anchored to its day; moving the day detaches the row instead
        // of colliding with the unique (recurringId, occurredAt) occurrence.
        assertThat(saved.occurredAt).isEqualTo(today.minusDays(1).atTime(9, 15))
        assertThat(saved.recurringId).isNull()
    }

    @Test
    fun saveChangedAmountRescalesTheSplitInsteadOfDroppingIt() = runTest {
        val original = storedTransaction().copy(
            splits = listOf(
                Split(categoryId = 1L, amount = Money(600, eur)),
                Split(categoryId = 2L, amount = Money(400, eur)),
            ),
        )
        val stored = mutableMapOf(7L to original)
        val vm = viewModel(transactions = FakeTransactionRepository(stored))
        advanceUntilIdle()
        vm.openEdit(7)
        advanceUntilIdle()
        assertThat(vm.uiState.value.splitCount).isEqualTo(2)
        vm.digit('5')
        vm.save()
        advanceUntilIdle()

        val saved = stored.getValue(7L)
        assertThat(saved.amount.minor).isEqualTo(10_500L)
        // A split is a breakdown of the amount: it still sums to the new total, and the user is
        // told it was rescaled instead of finding the breakdown silently gone.
        assertThat(saved.splits.sumOf { it.amount.minor }).isEqualTo(10_500L)
        assertThat(saved.splits.map { it.categoryId }).containsExactly(1L, 2L).inOrder()
        assertThat(saved.splits.map { it.amount.minor }).containsExactly(6_300L, 4_200L).inOrder()
        assertThat((vm.uiState.value.notice as QuickAddNotice.Saved).variant).isEqualTo(SavedVariant.SplitsScaled)
    }

    @Test
    fun changingKindDropsAnIncompatibleSplitAndSaysSo() = runTest {
        val splits = listOf(
            Split(categoryId = 1L, amount = Money(600, eur)),
            Split(categoryId = 2L, amount = Money(400, eur)),
        )
        val stored = mutableMapOf(7L to storedTransaction().copy(splits = splits))
        val vm = viewModel(transactions = FakeTransactionRepository(stored))
        advanceUntilIdle()
        vm.openEdit(7)
        advanceUntilIdle()
        vm.selectKind(dev.antomanc.tameki.core.ui.components.TransactionKind.INCOME)
        vm.save()
        advanceUntilIdle()

        assertThat(stored.getValue(7L).kind).isEqualTo(TransactionKind.INCOME)
        assertThat(stored.getValue(7L).splits).isEmpty()
        assertThat((vm.uiState.value.notice as QuickAddNotice.Saved).variant).isEqualTo(SavedVariant.SplitsDropped)
    }

    @Test
    fun saveUnchangedAmountKeepsItsSplit() = runTest {
        val splits = listOf(
            Split(categoryId = 1L, amount = Money(600, eur)),
            Split(categoryId = 2L, amount = Money(400, eur)),
        )
        val stored = mutableMapOf(7L to storedTransaction().copy(splits = splits))
        val vm = viewModel(transactions = FakeTransactionRepository(stored))
        advanceUntilIdle()
        vm.openEdit(7)
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()

        assertThat(stored.getValue(7L).splits).isEqualTo(splits)
    }

    @Test
    fun saveChangedCategoryKeepsItsSplitAndMovesOnlyTheParent() = runTest {
        val splits = listOf(
            Split(categoryId = 1L, amount = Money(600, eur)),
            Split(categoryId = 2L, amount = Money(400, eur)),
        )
        val stored = mutableMapOf(7L to storedTransaction().copy(categoryId = 1L, splits = splits))
        val vm = viewModel(
            categories = listOf(
                Category(1, "Groceries", CategoryKind.EXPENSE, "groceries", 0),
                Category(2, "Restaurant", CategoryKind.EXPENSE, "restaurant", 1),
            ),
            transactions = FakeTransactionRepository(stored),
        )
        advanceUntilIdle()
        vm.openEdit(7)
        advanceUntilIdle()
        vm.selectCategory(2)
        vm.save()
        advanceUntilIdle()

        val saved = stored.getValue(7L)
        assertThat(saved.categoryId).isEqualTo(2L)
        // The breakdown is still the user's attribution; changing the headline category does not
        // erase money from the categories every insight reads.
        assertThat(saved.splits).isEqualTo(splits)
    }

    @Test
    fun pickingAnotherCategoryDropsTheSuggestionMarker() = runTest {
        val vm = viewModel(
            categories = listOf(
                Category(1, "Groceries", CategoryKind.EXPENSE, "groceries", 0),
                Category(2, "Restaurant", CategoryKind.EXPENSE, "restaurant", 1),
            ),
            merchantMemory = mapOf("Esselunga" to 1L),
        )
        advanceUntilIdle()
        vm.openFromDraft(
            TransactionDraft(
                kind = TransactionKind.EXPENSE,
                amount = Money(1_000, eur),
                merchant = "Esselunga",
            ),
        )
        advanceUntilIdle()
        assertThat(vm.uiState.value.suggestedCategoryId).isEqualTo(1L)
        assertThat(vm.uiState.value.selectedCategoryId).isEqualTo(1L)

        vm.selectCategory(2)

        // The marker labelled a recommendation the user just declined: keeping it would leave two
        // chips claiming attention at once, one of them for a choice nobody made.
        assertThat(vm.uiState.value.selectedCategoryId).isEqualTo(2L)
        assertThat(vm.uiState.value.suggestedCategoryId).isNull()
    }

    @Test
    fun undoAfterEditRestoresTheOriginalRow() = runTest {
        val original = storedTransaction().copy(
            categoryId = 1L,
            occurredAt = today.atTime(9, 15),
            tags = setOf("work", "reimbursable"),
            note = "before",
        )
        val stored = mutableMapOf(7L to original)
        val transactions = FakeTransactionRepository(stored)
        val vm = viewModel(transactions = transactions)
        advanceUntilIdle()
        vm.openEdit(7)
        advanceUntilIdle()
        vm.digit('5')
        vm.save()
        advanceUntilIdle()
        val notice = vm.uiState.value.notice as QuickAddNotice.Saved
        assertThat(notice.variant).isEqualTo(SavedVariant.Plain)
        assertThat(stored.getValue(7L).amount.minor).isEqualTo(10_500L)

        // An edit undo restores the snapshot rather than deleting the row.
        vm.undo(notice.token)
        advanceUntilIdle()

        assertThat(transactions.contains(7)).isTrue()
        assertThat(stored.getValue(7L)).isEqualTo(original)
    }

    @Test
    fun undoAfterCreateDeletesTheNewRow() = runTest {
        val stored = mutableMapOf<Long, Transaction>()
        val transactions = FakeTransactionRepository(stored)
        val vm = viewModel(transactions = transactions)
        advanceUntilIdle()
        vm.openNew()
        advanceUntilIdle()
        vm.digit('5')
        vm.digit('0')
        vm.save()
        advanceUntilIdle()
        val notice = vm.uiState.value.notice as QuickAddNotice.Saved
        assertThat(stored).hasSize(1)

        vm.undo(notice.token)
        advanceUntilIdle()

        assertThat(stored).isEmpty()
    }

    @Test
    fun deleteCurrentDropsThePendingEditUndoOfAnEarlierSave() = runTest {
        val stored = mutableMapOf(
            7L to storedTransaction(7),
            8L to storedTransaction(8),
        )
        val transactions = FakeTransactionRepository(stored)
        val vm = viewModel(transactions = transactions)
        advanceUntilIdle()
        vm.openEdit(7)
        advanceUntilIdle()
        vm.digit('5')
        vm.save()
        advanceUntilIdle()
        val saved = vm.uiState.value.notice as QuickAddNotice.Saved

        // "Saved · Undo" is still up when the user opens another row and deletes it.
        vm.openEdit(8)
        advanceUntilIdle()
        vm.deleteCurrent()
        advanceUntilIdle()
        assertThat(vm.uiState.value.notice).isInstanceOf(QuickAddNotice.Deleted::class.java)

        // The earlier edit's Undo went with it: its token no longer reverts the saved change,
        // and the deleted row stays deleted.
        vm.undo(saved.token)
        advanceUntilIdle()
        assertThat(stored.getValue(7L).amount.minor).isEqualTo(10_500L)
        assertThat(transactions.contains(8)).isFalse()
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
        assertThat(state.notice).isInstanceOf(QuickAddNotice.Deleted::class.java)
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
        vm.undo(checkNotNull(vm.uiState.value.notice).token)
        advanceUntilIdle()
        assertThat(transactions.contains(7)).isTrue()
    }

    @Test
    fun acknowledgeDeleteZeroesTheEventSoItCannotReplayAndDropsUndo() = runTest {
        val stored = mutableMapOf(7L to storedTransaction())
        val transactions = FakeTransactionRepository(stored)
        val vm = viewModel(transactions = transactions)
        advanceUntilIdle()
        vm.openEdit(7)
        advanceUntilIdle()
        vm.deleteCurrent()
        advanceUntilIdle()
        val token = checkNotNull(vm.uiState.value.notice).token

        // The screen showed the snackbar and the user let it go: no replay on tab return or
        // rotation, and no stale snapshot behind a later Undo.
        vm.acknowledgeNotice(token)
        assertThat(vm.uiState.value.notice).isNull()
        vm.undo(token)
        advanceUntilIdle()
        assertThat(transactions.contains(7)).isFalse()
    }

    @Test
    fun staleAcknowledgeDoesNotConsumeANewerDelete() = runTest {
        val stored = mutableMapOf(7L to storedTransaction())
        val transactions = FakeTransactionRepository(stored)
        val vm = viewModel(transactions = transactions)
        advanceUntilIdle()
        vm.openEdit(7)
        advanceUntilIdle()
        vm.deleteCurrent()
        advanceUntilIdle()

        val token = checkNotNull(vm.uiState.value.notice).token
        vm.acknowledgeNotice(token - 1)
        assertThat(vm.uiState.value.notice?.token).isEqualTo(token)

        vm.undo(token)
        advanceUntilIdle()
        assertThat(transactions.contains(7)).isTrue()
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

    @Test
    fun aDoubleTapOnSaveBooksOnce() = runTest {
        val stored = mutableMapOf<Long, Transaction>()
        val vm = viewModel(transactions = FakeTransactionRepository(stored))
        advanceUntilIdle()
        vm.openNew()
        advanceUntilIdle()
        vm.digit('9')
        vm.save()
        vm.save()
        advanceUntilIdle()
        assertThat(stored).hasSize(1)
    }

    @Test
    fun aDatabaseWriteWhileEditingKeepsTheFormsCurrency() = runTest {
        val usd = dev.antomanc.tameki.core.model.CurrencyCode("USD")
        val stored = mutableMapOf(7L to storedTransaction().copy(amount = Money(1_200, usd)))
        val transactions = FakeTransactionRepository(stored)
        val vm = viewModel(transactions = transactions)
        advanceUntilIdle()
        vm.openEdit(7)
        advanceUntilIdle()
        // Any write re-emits the catalog; the edited amount must stay in dollars.
        transactions.upsert(storedTransaction(8))
        advanceUntilIdle()
        assertThat(vm.uiState.value.currency).isEqualTo(usd)
    }
}
