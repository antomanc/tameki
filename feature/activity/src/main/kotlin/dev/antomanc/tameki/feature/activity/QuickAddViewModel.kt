package dev.antomanc.tameki.feature.activity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.InboxStatus
import dev.antomanc.tameki.core.model.entity.Receipt
import dev.antomanc.tameki.core.model.entity.Split
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
import java.io.File
import java.time.Clock
import java.time.Instant
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
        viewModelScope.launch {
            val base = settings.observeBaseCurrency().first()
            val accountList = accounts.observeAll().first()
            _state.value = QuickAddUiState(
                visible = true,
                currency = accountList.firstOrNull()?.currency ?: base,
                accountId = accountList.firstOrNull()?.id,
                date = LocalDate.now(clock),
                accounts = accountList,
            )
            refreshCategories()
        }
    }

    fun openEdit(transactionId: Long) {
        inboxItemId = null
        viewModelScope.launch {
            val tx = transactions.get(transactionId) ?: return@launch
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
                note = tx.note,
                merchant = tx.merchant,
                splits = tx.splits,
                receiptId = tx.receiptId,
                canSave = true,
            )
            refreshCategories()
        }
    }

    fun openFromDraft(draft: TransactionDraft, inboxId: Long? = null) {
        inboxItemId = inboxId
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
                note = draft.note,
                merchant = draft.merchant,
            )
            _state.value = prefilled.copy(canSave = canSave(prefilled))
            refreshCategories(hint = draft.categoryHint)
        }
    }

    fun dismiss() {
        _state.update { it.copy(visible = false, showSplitEditor = false, showNoteEditor = false) }
    }

    fun clearSnackbar() {
        _state.update { it.copy(snackbar = null) }
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

    fun toggleSplitEditor(show: Boolean) {
        _state.update { it.copy(showSplitEditor = show) }
    }

    fun setSplits(splits: List<Split>) {
        _state.update { it.copy(splits = splits, showSplitEditor = false) }
    }

    fun attachPhoto(sourcePath: String) {
        viewModelScope.launch {
            val dest = copyReceipt(sourcePath)
            val receiptId = receipts.upsert(
                Receipt(
                    imagePath = dest.absolutePath,
                    createdAt = Instant.now(clock),
                ),
            )
            _state.update {
                it.copy(receiptPath = dest.absolutePath, receiptId = receiptId, receiptEngineLabel = null)
            }
        }
    }

    fun readReceipt() {
        val path = _state.value.receiptPath ?: return
        viewModelScope.launch {
            val result = receiptReader.read(path)
            val draft = result.draft
            val money = draft.amount
            _state.update { current ->
                current.copy(
                    amount = money?.let { AmountDraft.fromMoney(it) } ?: current.amount,
                    currency = money?.currency ?: current.currency,
                    merchant = draft.merchant ?: current.merchant,
                    note = draft.note ?: current.note,
                    date = draft.occurredAt?.toLocalDate() ?: current.date,
                    receiptEngineLabel = engineLabel(result.engine),
                    canSave = canSave(
                        current.copy(
                            amount = money?.let { AmountDraft.fromMoney(it) } ?: current.amount,
                            currency = money?.currency ?: current.currency,
                        ),
                    ),
                )
            }
            refreshCategories(hint = draft.categoryHint)
        }
    }

    fun save() {
        val s = _state.value
        if (!canSave(s)) return
        viewModelScope.launch {
            val accountId = s.accountId ?: return@launch
            val occurred = (s.date ?: LocalDate.now(clock)).atTime(LocalDateTime.now(clock).toLocalTime())
            val money = s.money()
            val modelKind = s.kind.toModel()
            val tx = Transaction(
                id = s.editingId ?: 0L,
                kind = modelKind,
                amount = money,
                accountId = accountId,
                toAccountId = if (modelKind == ModelKind.TRANSFER) s.toAccountId else null,
                categoryId = if (modelKind == ModelKind.TRANSFER) null else s.selectedCategoryId,
                splits = s.splits,
                merchant = s.merchant,
                note = s.note,
                occurredAt = occurred,
                receiptId = s.receiptId,
                source = if (inboxItemId != null) TransactionSource.NOTIFICATION else TransactionSource.MANUAL,
            )
            val id = transactions.upsert(tx)
            if (!s.merchant.isNullOrBlank() && s.selectedCategoryId != null) {
                categories.rememberMerchantCategory(s.merchant, s.selectedCategoryId)
            }
            inboxItemId?.let { inbox.setStatus(it, InboxStatus.ACCEPTED) }
            inboxItemId = null
            _state.update {
                it.copy(
                    visible = false,
                    snackbar = "saved",
                    lastSavedId = id,
                    showSplitEditor = false,
                )
            }
        }
    }

    fun undoLastSave() {
        val id = _state.value.lastSavedId ?: return
        viewModelScope.launch {
            transactions.delete(id)
            _state.update { it.copy(lastSavedId = null, snackbar = null) }
        }
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
            val ranked = CategoryRanker.rank(
                categories = categoryList,
                kind = categoryKind(s.kind),
                merchant = s.merchant,
                hint = hint,
                merchantMemory = memory,
                recent = recent,
                now = LocalDateTime.now(clock),
            )
            // Cold start: no preselection. A matcher hint may pick the top ranked chip.
            val selected = s.selectedCategoryId
                ?: hint?.let { ranked.firstOrNull()?.id }
            _state.update {
                it.copy(
                    categories = ranked,
                    selectedCategoryId = selected,
                    canSave = canSave(it.copy(categories = ranked, selectedCategoryId = selected)),
                )
            }
        }
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
