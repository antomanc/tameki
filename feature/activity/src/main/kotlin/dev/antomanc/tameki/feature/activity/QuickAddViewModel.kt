package dev.antomanc.tameki.feature.activity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.InboxStatus
import dev.antomanc.tameki.core.model.entity.SplitAllocator
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.core.model.entity.TransactionKind as ModelKind
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.InboxRepository
import dev.antomanc.tameki.core.model.repository.ReceiptRepository
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionFilter
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import dev.antomanc.tameki.core.ui.components.TransactionKind
import dev.antomanc.tameki.scan.api.ReceiptEngine
import dev.antomanc.tameki.scan.api.ReceiptReader
import dev.antomanc.tameki.scan.api.ReceiptScanOrchestrator
import dev.antomanc.tameki.scan.api.ReceiptScanReady
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QuickAddViewModel(
    private val transactions: TransactionRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val receipts: ReceiptRepository,
    private val inbox: InboxRepository,
    private val settings: SettingsRepository,
    private val receiptReader: ReceiptReader,
    private val clock: Clock,
    private val appContext: Context,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow(QuickAddUiState())
    val uiState: StateFlow<QuickAddUiState> = _state.asStateFlow()

    /** What the snackbar's Undo reverts, keyed by the notice token that offered it. */
    private sealed interface UndoAction {
        /** A create: Undo deletes the new row. */
        data class DeleteCreated(val id: Long) : UndoAction

        /** An edit: Undo puts the row back as it was. */
        data class Restore(val before: Transaction) : UndoAction

        /** A delete: Undo puts the deleted row back. */
        data class Reinsert(val tx: Transaction) : UndoAction
    }

    private var undoable: Pair<Long, UndoAction>? = null
    private var nextToken = 0L

    /**
     * The record being edited. Saving must change only what the user touched: the time of day,
     * provenance, tags and recurring link of the original entry survive the edit.
     */
    private var editingOriginal: Transaction? = null

    private val scanOrchestrator = ReceiptScanOrchestrator(
        copyToReceiptStorage = { source -> withContext(io) { copyReceipt(source).absolutePath } },
        upsertReceipt = { receipt -> receipts.upsert(receipt) },
        reader = receiptReader,
        clock = clock,
    )

    init {
        viewModelScope.launch {
            combine(
                accounts.observeAll(),
                categories.observeAll(),
                categories.observeMerchantCategoryMemory(),
                transactions.observe(TransactionFilter()),
                settings.observeBaseCurrency(),
            ) { accountList, categoryList, memory, recent, base ->
                CatalogSnapshot(accountList, categoryList, memory, recent.take(80), base)
            }.collect { snap ->
                val s = _state.value
                val ranked = CategoryRanker.rank(
                    categories = snap.categories,
                    kind = categoryKind(s.kind),
                    merchant = s.merchant,
                    hint = null,
                    merchantMemory = snap.memory,
                    recent = snap.recent,
                    now = LocalDateTime.now(clock),
                )
                _state.update { current ->
                    // The catalog refreshes on every write. It may fill a missing account, but the
                    // currency belongs to the form: an edit of a USD movement stays USD however
                    // often the database emits.
                    val pickedAccount = if (current.accountId == null) snap.accounts.firstOrNull() else null
                    current.copy(
                        accounts = snap.accounts,
                        accountId = current.accountId ?: pickedAccount?.id,
                        categories = ranked,
                        currency = if (pickedAccount != null && current.money().minor == 0L) {
                            pickedAccount.currency
                        } else if (!current.visible) {
                            snap.accounts.firstOrNull { it.id == current.accountId }?.currency ?: snap.base
                        } else {
                            current.currency
                        },
                    )
                }
            }
        }
    }

    private data class CatalogSnapshot(
        val accounts: List<dev.antomanc.tameki.core.model.entity.Account>,
        val categories: List<dev.antomanc.tameki.core.model.entity.Category>,
        val memory: Map<String, Long>,
        val recent: List<Transaction>,
        val base: CurrencyCode,
    )

    /** Opens a fresh sheet, keeping only the pending notice (its snackbar may still be up). */
    private fun fresh(block: QuickAddUiState.() -> QuickAddUiState): QuickAddUiState =
        QuickAddUiState(notice = _state.value.notice).block()

    fun openNew() {
        editingOriginal = null
        viewModelScope.launch {
            val base = settings.observeBaseCurrency().first()
            val accountList = accounts.observeAll().first()
            val day = LocalDate.now(clock)
            _state.value = fresh {
                copy(
                    phase = SheetPhase.Editing,
                    currency = accountList.firstOrNull()?.currency ?: base,
                    accountId = accountList.firstOrNull()?.id,
                    date = day,
                    today = day,
                    accounts = accountList,
                )
            }
            refreshCategories()
        }
    }

    fun openEdit(transactionId: Long) {
        viewModelScope.launch {
            val tx = transactions.get(transactionId) ?: return@launch
            editingOriginal = tx
            _state.value = fresh {
                copy(
                    phase = SheetPhase.Editing,
                    mode = QuickAddMode.Edit(tx.id),
                    kind = tx.kind.toUi(),
                    amount = AmountDraft.fromMoney(tx.amount),
                    currency = tx.amount.currency,
                    selectedCategoryId = tx.categoryId,
                    accountId = tx.accountId,
                    toAccountId = tx.toAccountId,
                    date = tx.occurredAt.toLocalDate(),
                    today = LocalDate.now(clock),
                    note = tx.note,
                    merchant = tx.merchant,
                    receiptId = tx.receiptId,
                    splitCount = tx.splits.size,
                    accounts = _state.value.accounts,
                )
            }
            refreshCategories()
        }
    }

    fun openFromDraft(draft: TransactionDraft, inboxId: Long? = null) {
        editingOriginal = null
        viewModelScope.launch {
            val base = settings.observeBaseCurrency().first()
            val accountList = accounts.observeAll().first()
            val money = draft.amount
            // A prefilled draft (deep link, notification, AI) needs an account like a new entry,
            // otherwise Save stays disabled: match the hint by name, else the first account.
            val account = draft.accountHint?.let { hint ->
                accountList.firstOrNull { it.resolvedName().equals(hint.trim(), ignoreCase = true) }
            } ?: accountList.firstOrNull()
            _state.value = fresh {
                copy(
                    phase = SheetPhase.Editing,
                    mode = if (inboxId != null) QuickAddMode.FromInbox(inboxId) else QuickAddMode.New,
                    kind = draft.kind.toUi(),
                    amount = money?.let { AmountDraft.fromMoney(it) } ?: AmountDraft(),
                    currency = money?.currency ?: account?.currency ?: base,
                    accountId = account?.id,
                    accounts = accountList,
                    date = draft.occurredAt?.toLocalDate() ?: LocalDate.now(clock),
                    today = LocalDate.now(clock),
                    note = draft.note,
                    merchant = draft.merchant,
                )
            }
            refreshCategories(hint = draft.categoryHint)
        }
    }

    /**
     * FAB / deep-link scan: open the sheet (with progress), store + read, prefill from the draft.
     * [fillEmptyOnly] is for the in-sheet chip — never overwrite what the user already typed.
     */
    fun applyCapturedReceipt(sourcePath: String, fillEmptyOnly: Boolean) {
        viewModelScope.launch {
            if (!fillEmptyOnly) {
                editingOriginal = null
                val base = settings.observeBaseCurrency().first()
                val accountList = accounts.observeAll().first()
                val day = LocalDate.now(clock)
                _state.value = fresh {
                    copy(
                        phase = SheetPhase.ReadingReceipt,
                        currency = accountList.firstOrNull()?.currency ?: base,
                        accountId = accountList.firstOrNull()?.id,
                        date = day,
                        today = day,
                        accounts = accountList,
                    )
                }
            } else {
                _state.update { it.copy(phase = SheetPhase.ReadingReceipt) }
            }
            val ready = try {
                scanOrchestrator.processCaptured(sourcePath)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The image could not even be stored (disk full, unreadable source): back to the
                // form with a message, never a crash or a spinner that never ends.
                null
            }
            if (ready == null) {
                _state.update { it.copy(phase = SheetPhase.Editing, notice = QuickAddNotice.ReceiptFailed(++nextToken)) }
            } else {
                applyScanReady(ready, fillEmptyOnly)
            }
        }
    }

    /** A picked or shared image could not be read at all. */
    fun reportReceiptFailed() {
        _state.update { it.copy(notice = QuickAddNotice.ReceiptFailed(++nextToken)) }
    }

    fun clearReceipt() {
        _state.update {
            it.copy(receiptPath = null, receiptId = null, receiptEngineLabel = null)
        }
    }

    fun dismiss() {
        editingOriginal = null
        _state.update {
            it.copy(phase = SheetPhase.Hidden, mode = QuickAddMode.New, showNoteEditor = false)
        }
    }

    fun selectKind(kind: TransactionKind) {
        _state.update { it.copy(kind = kind, selectedCategoryId = null) }
        refreshCategories()
    }

    fun digit(d: Char) = updateAmount { it.appendDigit(d) }
    fun decimal() = updateAmount { it.appendDecimal() }
    fun backspace() = updateAmount { it.backspace() }

    fun selectCategory(id: Long) {
        _state.update { state ->
            // The marker labels a recommendation, so it cannot outlive the user's own choice: once
            // they pick a different chip, keeping "Suggested" on another one asks them to compare
            // a recommendation they just declined with the choice they made.
            val suggested = if (state.suggestedCategoryId == id) state.suggestedCategoryId else null
            state.copy(selectedCategoryId = id, suggestedCategoryId = suggested)
        }
    }

    /**
     * Picks the account. The amount follows the account's currency only while it was in the old
     * account's currency: a foreign amount from a receipt, a draft or an edit keeps its currency.
     */
    fun setAccount(id: Long) {
        _state.update { s ->
            val previous = s.accounts.firstOrNull { it.id == s.accountId }
            val next = s.accounts.firstOrNull { it.id == id }
            val followAccount = next != null && (previous == null || s.currency == previous.currency)
            s.copy(accountId = id, currency = if (followAccount) next.currency else s.currency)
        }
    }

    fun setToAccount(id: Long) {
        _state.update { it.copy(toAccountId = id) }
    }

    fun setDate(date: LocalDate) {
        _state.update { it.copy(date = date) }
    }

    fun setNote(note: String?) {
        _state.update { it.copy(note = note?.ifBlank { null }, showNoteEditor = false) }
    }

    fun toggleNoteEditor(show: Boolean) {
        _state.update { it.copy(showNoteEditor = show) }
    }

    /**
     * Commits the sheet.
     *
     * An edit fixes only what the user touched: the original keeps its time of day (the date chip
     * may still move the day), its provenance, tags and receipt. Two invariants decide the rest:
     * - the sheet cannot edit a split, so a create always writes none; an edit keeps the stored
     *   breakdown only while the total still matches it, because a split must sum to the amount;
     * - a recurring link is anchored to one occurrence, so moving the day detaches the row
     *   (clearing [Transaction.recurringId]) instead of colliding with the unique
     *   `(recurringId, occurredAt)` occurrence.
     */
    fun save() {
        val s = _state.value
        // Only an editable, valid form saves; entering Saving makes a second tap a no-op.
        if (!s.canSave) return
        val accountId = s.accountId ?: return
        _state.update { it.copy(phase = SheetPhase.Saving) }
        val mode = s.mode
        viewModelScope.launch {
            val editingId = (mode as? QuickAddMode.Edit)?.id
            val existing = editingId?.let { editingOriginal ?: transactions.get(it) }
            val day = s.date ?: LocalDate.now(clock)
            val occurred = day.atTime(
                existing?.occurredAt?.toLocalTime() ?: LocalDateTime.now(clock).toLocalTime(),
            )
            val money = s.money()
            val modelKind = s.kind.toModel()
            val dayMoved = existing != null && day != existing.occurredAt.toLocalDate()
            val categoryId = if (modelKind == ModelKind.TRANSFER) null else s.selectedCategoryId
            // Splits have no editor yet. They are never dropped silently: a category change only
            // moves the parent category, and an amount change rescales the stored breakdown in
            // proportion (the sheet says it will). A kind change is the one incompatible edit,
            // because a transfer has no expense breakdown at all; the sheet's split line has
            // already told the user there is one. Never invent splits on create.
            val splitSource = existing?.splits.orEmpty()
            val splitsScaled = existing != null &&
                splitSource.isNotEmpty() &&
                modelKind == ModelKind.EXPENSE &&
                existing.kind == ModelKind.EXPENSE &&
                money != existing.amount
            val splits = if (existing == null || splitSource.isEmpty()) {
                emptyList()
            } else if (modelKind != ModelKind.EXPENSE || existing.kind != ModelKind.EXPENSE) {
                emptyList()
            } else if (money == existing.amount) {
                splitSource
            } else {
                SplitAllocator.scaleTo(splitSource, money)
            }
            val splitsDropped = existing != null && splitSource.isNotEmpty() && splits.isEmpty()
            val tx = Transaction(
                id = editingId ?: 0L,
                kind = modelKind,
                amount = money,
                accountId = accountId,
                toAccountId = if (modelKind == ModelKind.TRANSFER) s.toAccountId else null,
                categoryId = categoryId,
                splits = splits,
                merchant = s.merchant,
                note = s.note,
                tags = existing?.tags ?: emptySet(),
                occurredAt = occurred,
                receiptId = s.receiptId,
                recurringId = existing?.recurringId?.takeIf { !dayMoved },
                source = existing?.source
                    ?: if (mode is QuickAddMode.FromInbox) TransactionSource.NOTIFICATION else TransactionSource.MANUAL,
            )
            val id = try {
                transactions.upsert(tx)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The write failed: give the form back untouched so nothing typed is lost.
                _state.update { it.copy(phase = SheetPhase.Editing) }
                return@launch
            }
            if (!s.merchant.isNullOrBlank() && s.selectedCategoryId != null) {
                categories.rememberMerchantCategory(s.merchant, s.selectedCategoryId)
            }
            if (mode is QuickAddMode.FromInbox) inbox.setStatus(mode.inboxId, InboxStatus.ACCEPTED)
            editingOriginal = null
            // Both paths get "Saved · Undo"; what Undo means differs: an edit puts the snapshot
            // back, a create deletes the new row.
            val token = ++nextToken
            undoable = token to if (existing != null) UndoAction.Restore(existing) else UndoAction.DeleteCreated(id)
            _state.update {
                it.copy(
                    phase = SheetPhase.Hidden,
                    mode = QuickAddMode.New,
                    notice = QuickAddNotice.Saved(
                        token,
                        when {
                            splitsScaled -> SavedVariant.SplitsScaled
                            splitsDropped -> SavedVariant.SplitsDropped
                            else -> SavedVariant.Plain
                        },
                    ),
                )
            }
        }
    }

    /** Undo on the snackbar for [token]: reverts exactly the action that snackbar offered. */
    fun undo(token: Long) {
        val (offered, action) = undoable ?: return
        if (offered != token) return
        undoable = null
        viewModelScope.launch {
            when (action) {
                is UndoAction.DeleteCreated -> transactions.delete(action.id)
                is UndoAction.Restore -> transactions.upsert(action.before)
                is UndoAction.Reinsert -> transactions.upsert(action.tx)
            }
        }
    }

    /**
     * The snackbar for [token] resolved (Undo, timeout, dismissed, or the screen went away). The
     * notice is cleared so it cannot replay, and its Undo expires. A newer notice is untouched.
     */
    fun acknowledgeNotice(token: Long) {
        if (undoable?.first == token) undoable = null
        _state.update { if (it.notice?.token == token) it.copy(notice = null) else it }
    }

    private fun applyScanReady(ready: ReceiptScanReady, fillEmptyOnly: Boolean) {
        val draft = ready.draft
        _state.update { current ->
            val money = draft.amount
            val takeAmount = money != null && (!fillEmptyOnly || current.money().minor <= 0L)
            current.copy(
                phase = SheetPhase.Editing,
                amount = if (takeAmount && money != null) AmountDraft.fromMoney(money) else current.amount,
                currency = if (takeAmount && money != null) money.currency else current.currency,
                merchant = if (fillEmptyOnly) current.merchant ?: draft.merchant else draft.merchant ?: current.merchant,
                note = if (fillEmptyOnly) {
                    current.note ?: draft.note ?: noteFromItems(draft)
                } else {
                    draft.note ?: noteFromItems(draft) ?: current.note
                },
                date = if (fillEmptyOnly) {
                    current.date ?: draft.occurredAt?.toLocalDate()
                } else {
                    draft.occurredAt?.toLocalDate() ?: current.date
                },
                receiptPath = ready.receiptPath,
                receiptId = ready.receiptId,
                receiptEngineLabel = ready.engine?.let { engineLabel(it) },
                notice = if (ready.readFailed) QuickAddNotice.ReceiptFailed(++nextToken) else current.notice,
            )
        }
        refreshCategories(hint = draft.categoryHint)
    }

    private fun noteFromItems(draft: TransactionDraft): String? {
        if (draft.items.isEmpty()) return null
        return draft.items.joinToString(separator = ", ") { it.name }.takeIf { it.isNotBlank() }
    }

    private fun updateAmount(block: (AmountDraft) -> AmountDraft) {
        _state.update { current ->
            if (current.phase == SheetPhase.Saving) current else current.copy(amount = block(current.amount))
        }
    }

    private fun refreshCategories(hint: String? = null) {
        viewModelScope.launch {
            val categoryList = categories.observeAll().first()
            val memory = categories.observeMerchantCategoryMemory().first()
            val recent = transactions.observe(TransactionFilter()).first().take(80)
            val s = _state.value
            val ranking = CategoryRanker.rankWithSuggestion(
                categories = categoryList,
                kind = categoryKind(s.kind),
                merchant = s.merchant,
                hint = hint,
                merchantMemory = memory,
                recent = recent,
                now = LocalDateTime.now(clock),
            )
            // Never guess a category from layout alone: only a real merchant / hint / memory
            // match may preselect a chip, so a cold start keeps its amount + Save clear. An edit
            // keeps the transaction's own category, and a transfer has none.
            val preselected = if (s.kind == TransactionKind.TRANSFER) null else ranking.suggestedId
            _state.update {
                it.copy(
                    categories = ranking.categories,
                    selectedCategoryId = it.selectedCategoryId ?: preselected,
                    suggestedCategoryId = ranking.suggestedId,
                )
            }
        }
    }

    /**
     * Deleting from the edit sheet: the sheet is the only place where a transaction is looked at
     * in full, so it is also where "this one is wrong" belongs. Kept for Undo, since DESIGN's
     * deletes are immediate but reversible.
     */
    fun deleteCurrent() {
        val id = _state.value.editingId ?: return
        if (_state.value.phase == SheetPhase.Saving) return
        _state.update { it.copy(phase = SheetPhase.Saving) }
        viewModelScope.launch {
            val tx = transactions.get(id)
            if (tx == null) {
                _state.update { it.copy(phase = SheetPhase.Hidden, mode = QuickAddMode.New) }
                return@launch
            }
            transactions.delete(id)
            editingOriginal = null
            // A delete supersedes the "Saved · Undo" that may still be on screen: its snapshot
            // would resurrect a row the user has just removed, so the new token replaces it.
            val token = ++nextToken
            undoable = token to UndoAction.Reinsert(tx)
            _state.update {
                it.copy(
                    phase = SheetPhase.Hidden,
                    mode = QuickAddMode.New,
                    showNoteEditor = false,
                    notice = QuickAddNotice.Deleted(token),
                )
            }
        }
    }

    private fun copyReceipt(sourcePath: String): File {
        val dir = File(appContext.filesDir, "receipts").apply { mkdirs() }
        val dest = File(dir, "${UUID.randomUUID()}.jpg")
        File(sourcePath).inputStream().use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        return dest
    }

    private fun categoryKind(kind: TransactionKind): CategoryKind = when (kind) {
        TransactionKind.INCOME -> CategoryKind.INCOME
        else -> CategoryKind.EXPENSE
    }

    private fun engineLabel(engine: ReceiptEngine): String = when (engine) {
        ReceiptEngine.GEMINI_NANO -> "Gemini Nano"
        ReceiptEngine.ML_KIT_OCR -> "ML Kit"
        ReceiptEngine.HEURISTIC -> appContext.getString(R.string.activity_receipt_engine_heuristic)
    }

    private fun ModelKind.toUi(): TransactionKind = when (this) {
        ModelKind.EXPENSE -> TransactionKind.EXPENSE
        ModelKind.INCOME -> TransactionKind.INCOME
        ModelKind.TRANSFER -> TransactionKind.TRANSFER
    }

    private fun TransactionKind.toModel(): ModelKind = when (this) {
        TransactionKind.EXPENSE -> ModelKind.EXPENSE
        TransactionKind.INCOME -> ModelKind.INCOME
        TransactionKind.TRANSFER -> ModelKind.TRANSFER
    }
}
