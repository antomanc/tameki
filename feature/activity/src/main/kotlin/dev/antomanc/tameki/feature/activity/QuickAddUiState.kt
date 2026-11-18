package dev.antomanc.tameki.feature.activity

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.Split
import dev.antomanc.tameki.core.ui.components.TransactionKind
import java.time.LocalDate

data class QuickAddUiState(
    val visible: Boolean = false,
    val editingId: Long? = null,
    val kind: TransactionKind = TransactionKind.EXPENSE,
    val amount: AmountDraft = AmountDraft(),
    val currency: CurrencyCode = CurrencyCode("EUR"),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val accounts: List<Account> = emptyList(),
    val accountId: Long? = null,
    val toAccountId: Long? = null,
    val date: LocalDate? = null,
    /** The injected clock's today; the date chips compare against it, never against the device. */
    val today: LocalDate? = null,
    val note: String? = null,
    val merchant: String? = null,
    val splits: List<Split> = emptyList(),
    val receiptPath: String? = null,
    val receiptId: Long? = null,
    val receiptEngineLabel: String? = null,
    /** Calm in-sheet progress while OCR / Gemini Nano runs after a scan. */
    val readingReceipt: Boolean = false,
    val showSplitEditor: Boolean = false,
    val showNoteEditor: Boolean = false,
    val canSave: Boolean = false,
    val snackbar: String? = null,
    val lastSavedId: Long? = null,
) {
    fun money(): Money = amount.toMoney(currency)
}
