package dev.antomanc.tameki.core.model.entity

import dev.antomanc.tameki.core.model.Money
import java.time.LocalDateTime

/**
 * Unconfirmed capture from OCR, notification, AI or deep link.
 * Fields are nullable so partial parses still land in the inbox for one-tap fix-up.
 */
data class TransactionDraft(
    val kind: TransactionKind = TransactionKind.EXPENSE,
    val amount: Money? = null,
    val merchant: String? = null,
    val categoryHint: String? = null,
    val occurredAt: LocalDateTime? = null,
    val note: String? = null,
    val items: List<ReceiptItem> = emptyList(),
    val accountHint: String? = null,
)
