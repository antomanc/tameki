package dev.antomanc.tameki.feature.activity

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Category
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
    /**
     * The ranker's real-signal suggestion, kept apart from [selectedCategoryId] so the sheet can
     * mark it without inventing a selection. Null when the order is layout only. It stays marked
     * while it is also selected (the usual case, since the ranker preselects it); picking another
     * chip drops it, because a recommendation the user declined is not a suggestion any more.
     */
    val suggestedCategoryId: Long? = null,
    val accounts: List<Account> = emptyList(),
    val accountId: Long? = null,
    val toAccountId: Long? = null,
    val date: LocalDate? = null,
    /** The injected clock's today; the date chips compare against it, never against the device. */
    val today: LocalDate? = null,
    val note: String? = null,
    val merchant: String? = null,
    val receiptPath: String? = null,
    val receiptId: Long? = null,
    val receiptEngineLabel: String? = null,
    /**
     * How many stored splits the transaction being edited carries. The sheet has no split editor,
     * but it must say so before an amount edit rescales them; zero for a new entry.
     */
    val splitCount: Int = 0,
    /** Calm in-sheet progress while OCR / Gemini Nano runs after a scan. */
    val readingReceipt: Boolean = false,
    val showNoteEditor: Boolean = false,
    val canSave: Boolean = false,
    val snackbar: String? = null,
    /**
     * Incremented by every delete, so the screen raises a fresh Undo snackbar each time; the
     * screen acknowledges it back to 0 once shown, so a tab return or rotation cannot replay it.
     */
    val deleteGeneration: Long = 0L,
    /** The row a create-save added; Undo deletes it. Null for an edit, which restores instead. */
    val lastSavedId: Long? = null,
) {
    fun money(): Money = amount.toMoney(currency)
}
