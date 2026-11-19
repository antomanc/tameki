package dev.antomanc.tameki.core.model.entity

import dev.antomanc.tameki.core.model.Money
import java.time.LocalDateTime

enum class TransactionKind {
    EXPENSE,
    INCOME,
    TRANSFER,
}

/** Provenance for inbox review, CSV migration and automation audit. */
enum class TransactionSource {
    MANUAL,
    RECEIPT,
    NOTIFICATION,
    AI_IMPORT,
    AUTOMATION,
    RECURRING,
    CSV_IMPORT,

    /**
     * The balance the user already had when they started. Booked as money in so the journal can
     * show it, but it is not earnings: no spending pool may count it as this month's income.
     */
    OPENING_BALANCE,
}

/**
 * True when the row is money earned this month. An opening balance records a snapshot of an
 * account, not income, so the Today hero and monthly summaries must leave it out.
 */
fun Transaction.countsAsIncome(): Boolean =
    kind == TransactionKind.INCOME && source != TransactionSource.OPENING_BALANCE


data class Split(
    val categoryId: Long,
    val amount: Money,
    val note: String? = null,
)

/**
 * A booked movement. [amount] is always positive; direction comes from [kind].
 * Splits are empty or must sum to [amount] — enforced by writers, not this type.
 */
data class Transaction(
    val id: Long = 0L,
    val kind: TransactionKind,
    val amount: Money,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val splits: List<Split> = emptyList(),
    val merchant: String? = null,
    val note: String? = null,
    val tags: Set<String> = emptySet(),
    val occurredAt: LocalDateTime,
    val receiptId: Long? = null,
    val recurringId: Long? = null,
    val source: TransactionSource = TransactionSource.MANUAL,
)
