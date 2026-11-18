package dev.antomanc.tameki.feature.activity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.InboxStatus
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
) : ViewModel() {

    private val _state = MutableStateFlow(QuickAddUiState())
    val uiState: StateFlow<QuickAddUiState> = _state.asStateFlow()

    private var inboxItemId: Long? = null
    private var pendingDelete: Transaction? = null

    /**
     * The record being edited. Saving must change only what the user touched: the time of day,
     * provenance, tags and recurring link of the original entry survive the edit.
     */
    private var editingOriginal: Transaction? = null

    /**
     * The row exactly as it was before the last saved edit, kept while its snackbar is up. Undo
     * upserts this snapshot back; a create-undo deletes [QuickAddUiState.lastSavedId] instead.
     */
    private var pendingEditRestore: Transaction? = null

    private val scanOrchestrator = ReceiptScanOrchestrator(
        copyToReceiptStorage = { source -> copyReceipt(source).absolutePath },
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
                    val accountId = current.accountId ?: snap.accounts.firstOrNull()?.id
                    current.copy(
                        accounts = snap.accounts,
                        accountId = accountId,
                        categories = ranked,
                        currency = snap.accounts.firstOrNull { it.id == accountId }?.currency ?: snap.base,
                        canSave = canSave(current.copy(accountId = accountId, categories = ranked)),
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

    fun openNew() {
        inboxItemId = null
        editingOriginal = null
        viewModelScope.launch {
            val base = settings.observeBaseCurrency().first()
            val accountList = accounts.observeAll().first()
            val day = LocalDate.now(clock)
            _state.value = QuickAddUiState(
                visible = true,
                currency = accountList.firstOrNull()?.currency ?: base,
                accountId = accountList.firstOrNull()?.id,
                date = day,
                today = day,
                accounts = accountList,
            )
            refreshCategories()
        }
    }

    fun openEdit(transactionId: Long) {
        inboxItemId = null
        viewModelScope.launch {
            val tx = transactions.get(transactionId) ?: return@launch
            editingOriginal = tx
            _state.value = QuickAddUiState(
                visible = true,
                editingId = tx.id,
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
                canSave = true,
            )
            refreshCategories()
        }
    }

    fun openFromDraft(draft: TransactionDraft, inboxId: Long? = null) {
        inboxItemId = inboxId
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
            val prefilled = QuickAddUiState(
                visible = true,
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
            _state.value = prefilled.copy(canSave = canSave(prefilled))
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
                _state.value = QuickAddUiState(
                    visible = true,
                    readingReceipt = true,
                    currency = accountList.firstOrNull()?.currency ?: base,
                    accountId = accountList.firstOrNull()?.id,
                    date = day,
                    today = day,
                    accounts = accountList,
                )
            } else {
                _state.update { it.copy(visible = true, readingReceipt = true, snackbar = null) }
            }
            val ready = scanOrchestrator.processCaptured(sourcePath)
            applyScanReady(ready, fillEmptyOnly)
        }
    }

    fun clearReceipt() {
        _state.update {
            it.copy(receiptPath = null, receiptId = null, receiptEngineLabel = null)
        }
    }

    fun dismiss() {
        _state.update {
            it.copy(
                visible = false,
                showNoteEditor = false,
                readingReceipt = false,
            )
        }
    }

    /**
     * The snackbar resolved without Undo: drop the snapshot so a later Undo cannot restore a row
     * the user has already accepted.
     */
    fun clearSnackbar() {
        pendingEditRestore = null
        _state.update { it.copy(snackbar = null, lastSavedId = null) }
    }

    fun selectKind(kind: TransactionKind) {
        _state.update { it.copy(kind = kind, selectedCategoryId = null) }
        refreshCategories()
    }

    fun digit(d: Char) = updateAmount { it.appendDigit(d) }
    fun decimal() = updateAmount { it.appendDecimal() }
    fun backspace() = updateAmount { it.backspace() }

    fun selectCategory(id: Long) {
        _state.update { it.copy(selectedCategoryId = id, canSave = canSave(it.copy(selectedCategoryId = id))) }
    }

    fun setAccount(id: Long) {
        val currency = _state.value.accounts.firstOrNull { it.id == id }?.currency ?: _state.value.currency
        _state.update { it.copy(accountId = id, currency = currency, canSave = canSave(it.copy(accountId = id, currency = currency))) }
    }

    fun setToAccount(id: Long) {
        _state.update { it.copy(toAccountId = id, canSave = canSave(it.copy(toAccountId = id))) }
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
        if (!canSave(s)) return
        val editingId = s.editingId
        viewModelScope.launch {
            val accountId = s.accountId ?: return@launch
            val existing = editingId?.let { editingOriginal ?: transactions.get(it) }
            val day = s.date ?: LocalDate.now(clock)
            val occurred = day.atTime(
                existing?.occurredAt?.toLocalTime() ?: LocalDateTime.now(clock).toLocalTime(),
            )
            val money = s.money()
            val modelKind = s.kind.toModel()
            val dayMoved = existing != null && day != existing.occurredAt.toLocalDate()
            // Splits have no editor yet: keep a real stored breakdown untouched on edit, drop it
            // when the total changes (it would no longer sum), and never invent one on create.
            val splits = if (existing != null && money == existing.amount) {
                existing.splits
            } else {
                emptyList()
            }
            val tx = Transaction(
                id = s.editingId ?: 0L,
                kind = modelKind,
                amount = money,
                accountId = accountId,
                toAccountId = if (modelKind == ModelKind.TRANSFER) s.toAccountId else null,
                categoryId = if (modelKind == ModelKind.TRANSFER) null else s.selectedCategoryId,
                splits = splits,
                merchant = s.merchant,
                note = s.note,
                tags = existing?.tags ?: emptySet(),
                occurredAt = occurred,
                receiptId = s.receiptId,
                recurringId = existing?.recurringId?.takeIf { !dayMoved },
                source = existing?.source
                    ?: if (inboxItemId != null) TransactionSource.NOTIFICATION else TransactionSource.MANUAL,
            )
            val id = transactions.upsert(tx)
            if (!s.merchant.isNullOrBlank() && s.selectedCategoryId != null) {
                categories.rememberMerchantCategory(s.merchant, s.selectedCategoryId)
            }
            inboxItemId?.let { inbox.setStatus(it, InboxStatus.ACCEPTED) }
            inboxItemId = null
            editingOriginal = null
            // Both paths get "Saved · Undo"; what Undo means differs: an edit puts the snapshot
            // back, a create deletes the new row.
            pendingEditRestore = if (editingId != null) existing else null
            _state.update {
                it.copy(
                    visible = false,
                    editingId = null,
                    snackbar = "saved",
                    lastSavedId = if (editingId == null) id else null,
                    readingReceipt = false,
                )
            }
        }
    }

    /**
     * Undoes the save behind the current snackbar: an edit is restored from
     * [pendingEditRestore], a create deletes the row that was just added.
     */
    fun undoLastSave() {
        val restore = pendingEditRestore
        if (restore != null) {
            pendingEditRestore = null
            viewModelScope.launch {
                transactions.upsert(restore)
                _state.update { it.copy(snackbar = null) }
            }
            return
        }
        val id = _state.value.lastSavedId ?: return
        viewModelScope.launch {
            transactions.delete(id)
            _state.update { it.copy(lastSavedId = null, snackbar = null) }
        }
    }

    private fun applyScanReady(ready: ReceiptScanReady, fillEmptyOnly: Boolean) {
        val draft = ready.draft
        _state.update { current ->
            val money = draft.amount
            val nextAmount = when {
                !fillEmptyOnly && money != null -> AmountDraft.fromMoney(money)
                fillEmptyOnly && current.money().minor <= 0L && money != null -> AmountDraft.fromMoney(money)
                else -> current.amount
            }
            val nextCurrency = when {
                !fillEmptyOnly && money != null -> money.currency
                fillEmptyOnly && current.money().minor <= 0L && money != null -> money.currency
                else -> current.currency
            }
            val next = current.copy(
                visible = true,
                readingReceipt = false,
                amount = nextAmount,
                currency = nextCurrency,
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
                snackbar = if (ready.readFailed) "receipt_read_failed" else current.snackbar,
            )
            next.copy(canSave = canSave(next))
        }
        refreshCategories(hint = draft.categoryHint)
    }

    private fun noteFromItems(draft: TransactionDraft): String? {
        if (draft.items.isEmpty()) return null
        return draft.items.joinToString(separator = ", ") { it.name }.takeIf { it.isNotBlank() }
    }

    private fun updateAmount(block: (AmountDraft) -> AmountDraft) {
        _state.update { current ->
            val next = current.copy(amount = block(current.amount))
            next.copy(canSave = canSave(next))
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
            val ranked = ranking.categories
            // Never guess a category from layout alone: only a real merchant / hint / memory
            // match may preselect a chip, so a cold start keeps its amount + Save clear. An edit
            // keeps the transaction's own category, and a transfer has none.
            val preselected = if (s.kind == TransactionKind.TRANSFER) null else ranking.suggestedId
            val selected = s.selectedCategoryId ?: preselected
            _state.update {
                it.copy(
                    categories = ranked,
                    selectedCategoryId = selected,
                    suggestedCategoryId = ranking.suggestedId,
                    canSave = canSave(it.copy(categories = ranked, selectedCategoryId = selected)),
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
        viewModelScope.launch {
            val tx = transactions.get(id) ?: return@launch
            transactions.delete(id)
            // Both the snapshot and the counter land in one main-thread turn after the row is
            // gone, so an acknowledgement for an earlier delete can never drop this one's Undo.
            pendingDelete = tx
            editingOriginal = null
            // A delete supersedes the "Saved · Undo" that may still be on screen: its snapshot
            // would resurrect a row the user has just removed, so it cannot outlive the delete.
            pendingEditRestore = null
            _state.update {
                it.copy(
                    visible = false,
                    editingId = null,
                    showNoteEditor = false,
                    readingReceipt = false,
                    snackbar = null,
                    lastSavedId = null,
                    // A counter, not a sticky flag: every delete must raise its own Undo event.
                    deleteGeneration = it.deleteGeneration + 1,
                )
            }
        }
    }

    fun undoDelete() {
        val tx = pendingDelete ?: return
        pendingDelete = null
        viewModelScope.launch { transactions.upsert(tx) }
    }

    /**
     * The Undo snackbar for [generation] was shown and resolved. Zero the counter so returning to
     * a tab or rotating does not replay the same event, and drop the snapshot when the user let
     * the snackbar go. A newer delete is left untouched.
     */
    fun acknowledgeDelete(generation: Long) {
        if (_state.value.deleteGeneration != generation) return
        pendingDelete = null
        _state.update { it.copy(deleteGeneration = 0L) }
    }

    private fun canSave(s: QuickAddUiState): Boolean {
        if (s.accountId == null) return false
        if (s.money().minor <= 0L) return false
        if (s.kind == TransactionKind.TRANSFER) {
            return s.toAccountId != null && s.toAccountId != s.accountId
        }
        return true
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
        ReceiptEngine.HEURISTIC -> "Heuristic"
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
