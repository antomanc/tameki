package dev.antomanc.tameki.core.model.entity

import dev.antomanc.tameki.core.model.Money
import java.time.Instant


data class ReceiptItem(
    val name: String,
    val amount: Money,
    val quantity: Double? = null, // money-ok: count/weight, not currency
)

/**
 * Photo + OCR payload attached to a transaction.
 * [rawText] is kept so the user can re-parse after engine upgrades without retaking the photo.
 */
data class Receipt(
    val id: Long = 0L,
    val imagePath: String,
    val rawText: String? = null,
    val items: List<ReceiptItem> = emptyList(),
    val createdAt: Instant,
)
