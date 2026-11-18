package dev.antomanc.tameki.core.data.db.entity

import androidx.room.Embedded
import androidx.room.Relation
import dev.antomanc.tameki.core.model.entity.Transaction

data class TransactionAggregate(
    @Embedded val transaction: TransactionEntity,
    @Relation(parentColumn = "id", entityColumn = "transactionId")
    val splits: List<TransactionSplitEntity>,
    @Relation(parentColumn = "id", entityColumn = "transactionId")
    val tags: List<TransactionTagEntity>,
) {
    fun toDomain(): Transaction = transaction.toDomain(splits, tags)
}
