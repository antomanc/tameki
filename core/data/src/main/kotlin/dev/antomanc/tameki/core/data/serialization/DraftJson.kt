package dev.antomanc.tameki.core.data.serialization

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.ReceiptItem
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.core.model.entity.TransactionKind
import java.time.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
internal data class DraftDto(
    val kind: String = TransactionKind.EXPENSE.name,
    val amountMinor: Long? = null,
    val currency: String? = null,
    val merchant: String? = null,
    val categoryHint: String? = null,
    val occurredAt: String? = null,
    val note: String? = null,
    val accountHint: String? = null,
    val items: List<ReceiptItemDto> = emptyList(),
)

@Serializable
internal data class ReceiptItemDto(
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val quantity: Double? = null, // money-ok: quantity
)

internal object DraftJson {
    fun encode(draft: TransactionDraft): String = json.encodeToString(draft.toDto())

    fun decode(raw: String): TransactionDraft = json.decodeFromString<DraftDto>(raw).toDomain()
}

private fun TransactionDraft.toDto(): DraftDto = DraftDto(
    kind = kind.name,
    amountMinor = amount?.minor,
    currency = amount?.currency?.value,
    merchant = merchant,
    categoryHint = categoryHint,
    occurredAt = occurredAt?.toString(),
    note = note,
    accountHint = accountHint,
    items = items.map {
        ReceiptItemDto(
            name = it.name,
            amountMinor = it.amount.minor,
            currency = it.amount.currency.value,
            quantity = it.quantity,
        )
    },
)

private fun DraftDto.toDomain(): TransactionDraft = TransactionDraft(
    kind = TransactionKind.valueOf(kind),
    amount = if (amountMinor != null && currency != null) {
        Money(amountMinor, CurrencyCode(currency))
    } else {
        null
    },
    merchant = merchant,
    categoryHint = categoryHint,
    occurredAt = occurredAt?.let(LocalDateTime::parse),
    note = note,
    accountHint = accountHint,
    items = items.map {
        ReceiptItem(
            name = it.name,
            amount = Money(it.amountMinor, CurrencyCode(it.currency)),
            quantity = it.quantity,
        )
    },
)
