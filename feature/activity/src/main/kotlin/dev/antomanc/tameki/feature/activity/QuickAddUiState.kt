package dev.antomanc.tameki.feature.activity

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.ui.components.TransactionKind
import java.time.LocalDate

/**
 * Where the sheet is in its life. Legal transitions:
 * Hidden → Editing (open) · Editing ⇄ ReadingReceipt (scan) · Editing → Saving → Hidden (save)
 * · any → Hidden (dismiss, delete). Input is ignored while [Saving], so a double tap books once.
 */
enum class SheetPhase { Hidden, Editing, ReadingReceipt, Saving }

/** What a save will do with the form. */
sealed interface QuickAddMode {
    /** A new movement typed by the user (or prefilled by a deep link, scan or AI). */
    data object New : QuickAddMode

    /** Editing a stored movement. */
    data class Edit(val id: Long) : QuickAddMode

    /** Reviewing a captured payment; saving books it and accepts the inbox item. */
    data class FromInbox(val inboxId: Long) : QuickAddMode
}

/**
 * A one-shot message for the app's snackbar host. [token] makes every notice distinct, so two
 * saves in a row raise two snackbars and an acknowledgement for an old one can never drop a newer
 * one's Undo.
 */
sealed interface QuickAddNotice {
    val token: Long

    data class Saved(override val token: Long, val variant: SavedVariant) : QuickAddNotice
    data class Deleted(override val token: Long) : QuickAddNotice
    data class ReceiptFailed(override val token: Long) : QuickAddNotice

    /** Whether the snackbar offers Undo. */
    val undoable: Boolean get() = this !is ReceiptFailed
}

/** How a saved edit treated the splits it could not show. */
enum class SavedVariant { Plain, SplitsScaled, SplitsDropped }

data class QuickAddUiState(
    val phase: SheetPhase = SheetPhase.Hidden,
    val mode: QuickAddMode = QuickAddMode.New,
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
    val showNoteEditor: Boolean = false,
    val notice: QuickAddNotice? = null,
) {
    fun money(): Money = amount.toMoney(currency)

    val visible: Boolean get() = phase != SheetPhase.Hidden

    /** Calm in-sheet progress while OCR / Gemini Nano runs after a scan. */
    val readingReceipt: Boolean get() = phase == SheetPhase.ReadingReceipt

    val editingId: Long? get() = (mode as? QuickAddMode.Edit)?.id

    /** The form is complete: an account, a positive amount, and for a transfer a different target. */
    val formValid: Boolean
        get() = accountId != null &&
            money().minor > 0L &&
            (kind != TransactionKind.TRANSFER || (toAccountId != null && toAccountId != accountId))

    /** Save is offered: a valid form while the user is editing (not reading, not already saving). */
    val canSave: Boolean get() = formValid && phase == SheetPhase.Editing
}
