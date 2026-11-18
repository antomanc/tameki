package dev.antomanc.tameki.core.model.entity

import java.time.Instant

enum class InboxStatus {
    PENDING,
    ACCEPTED,
    DISMISSED,
}

/**
 * Proposed transaction waiting for confirmation.
 * [rawText] preserves the notification/OCR/AI payload that produced [draft].
 */
data class InboxItem(
    val id: Long = 0L,
    val draft: TransactionDraft,
    val origin: TransactionSource,
    val rawText: String,
    val receivedAt: Instant,
    val status: InboxStatus = InboxStatus.PENDING,
)
