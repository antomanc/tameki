package dev.antomanc.tameki.core.data.db.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Split
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.TransactionSource
import java.time.LocalDateTime

@Entity(
    tableName = "transactions",
    indices = [
        Index("occurredAt"),
        Index("accountId"),
        Index("categoryId"),
        Index("merchant"),
        Index("source"),
        Index("recurringId"),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val kind: String,
    val amountMinor: Long,
    val currency: String,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val merchant: String? = null,
    val note: String? = null,
    val occurredAt: LocalDateTime,
    val receiptId: Long? = null,
    val recurringId: Long? = null,
    val source: String = TransactionSource.MANUAL.name,
)

@Fts4(contentEntity = TransactionEntity::class)
@Entity(tableName = "transactions_fts")
data class TransactionFtsEntity(
    val merchant: String?,
    val note: String?,
)

@Entity(
    tableName = "transaction_splits",
    primaryKeys = ["transactionId", "ordinal"],
    indices = [Index("transactionId"), Index("categoryId")],
)
data class TransactionSplitEntity(
    val transactionId: Long,
    val ordinal: Int,
    val categoryId: Long,
    val amountMinor: Long,
    val currency: String,
    val note: String? = null,
) {
    fun toDomain(): Split = Split(
        categoryId = categoryId,
        amount = Money(amountMinor, CurrencyCode(currency)),
        note = note,
    )
}

@Entity(
    tableName = "transaction_tags",
    primaryKeys = ["transactionId", "tag"],
    indices = [Index("tag"), Index("transactionId")],
)
data class TransactionTagEntity(
    val transactionId: Long,
    val tag: String,
)

data class TransactionWithExtras(
    val transaction: TransactionEntity,
    val splits: List<TransactionSplitEntity>,
    val tags: List<TransactionTagEntity>,
)

fun TransactionEntity.toDomain(
    splits: List<TransactionSplitEntity> = emptyList(),
    tags: List<TransactionTagEntity> = emptyList(),
): Transaction = Transaction(
    id = id,
    kind = TransactionKind.valueOf(kind),
    amount = Money(amountMinor, CurrencyCode(currency)),
    accountId = accountId,
    toAccountId = toAccountId,
    categoryId = categoryId,
    splits = splits.sortedBy { it.ordinal }.map { it.toDomain() },
    merchant = merchant,
    note = note,
    tags = tags.map { it.tag }.toSet(),
    occurredAt = occurredAt,
    receiptId = receiptId,
    recurringId = recurringId,
    source = TransactionSource.valueOf(source),
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    kind = kind.name,
    amountMinor = amount.minor,
    currency = amount.currency.value,
    accountId = accountId,
    toAccountId = toAccountId,
    categoryId = categoryId,
    merchant = merchant,
    note = note,
    occurredAt = occurredAt,
    receiptId = receiptId,
    recurringId = recurringId,
    source = source.name,
)
