package dev.antomanc.tameki.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.antomanc.tameki.core.model.entity.InboxItem
import dev.antomanc.tameki.core.model.entity.InboxStatus
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.core.model.entity.TransactionSource
import java.time.Instant

@Entity(
    tableName = "inbox_items",
    indices = [Index("status"), Index("receivedAt")],
)
data class InboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val draftJson: String,
    val origin: String,
    val rawText: String,
    val receivedAt: Instant,
    val status: String = InboxStatus.PENDING.name,
)

fun InboxEntity.toDomain(draft: TransactionDraft): InboxItem = InboxItem(
    id = id,
    draft = draft,
    origin = TransactionSource.valueOf(origin),
    rawText = rawText,
    receivedAt = receivedAt,
    status = InboxStatus.valueOf(status),
)
