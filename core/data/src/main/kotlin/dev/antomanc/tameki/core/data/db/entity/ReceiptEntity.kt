package dev.antomanc.tameki.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Receipt
import dev.antomanc.tameki.core.model.entity.ReceiptItem
import java.time.Instant

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val imagePath: String,
    val rawText: String? = null,
    val createdAt: Instant,
)

@Entity(
    tableName = "receipt_items",
    primaryKeys = ["receiptId", "ordinal"],
    indices = [Index("receiptId")],
)
data class ReceiptItemEntity(
    val receiptId: Long,
    val ordinal: Int,
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val quantity: Double? = null, // money-ok: quantity, not currency
) {
    fun toDomain(): ReceiptItem = ReceiptItem(
        name = name,
        amount = Money(amountMinor, CurrencyCode(currency)),
        quantity = quantity,
    )
}

fun ReceiptEntity.toDomain(items: List<ReceiptItemEntity>): Receipt = Receipt(
    id = id,
    imagePath = imagePath,
    rawText = rawText,
    items = items.sortedBy { it.ordinal }.map { it.toDomain() },
    createdAt = createdAt,
)
