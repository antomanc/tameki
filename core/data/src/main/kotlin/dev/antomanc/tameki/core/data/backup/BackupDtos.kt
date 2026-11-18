package dev.antomanc.tameki.core.data.backup

import kotlinx.serialization.Serializable

const val BACKUP_DATA_VERSION = 1

@Serializable
data class BackupEnvelope(
    val version: Int = BACKUP_DATA_VERSION,
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val merchantMemory: List<MerchantMemoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList(),
    val receipts: List<ReceiptDto> = emptyList(),
    val recurring: List<RecurringDto> = emptyList(),
    val budgets: List<BudgetDto> = emptyList(),
    val goals: List<GoalDto> = emptyList(),
    val inbox: List<InboxDto> = emptyList(),
    val exchangeRates: List<ExchangeRateDto> = emptyList(),
)

@Serializable
data class AccountDto(
    val id: Long,
    val name: String,
    val currency: String,
    val type: String,
    val archived: Boolean,
    val sortOrder: Int,
    val seedKey: String? = null,
    val customName: String? = null,
)

@Serializable
data class CategoryDto(
    val id: Long,
    val name: String,
    val kind: String,
    val icon: String,
    val color: Long,
    val parentId: Long? = null,
    val archived: Boolean = false,
    val seedKey: String? = null,
    val customName: String? = null,
)

@Serializable
data class MerchantMemoryDto(val merchant: String, val categoryId: Long)

@Serializable
data class TransactionDto(
    val id: Long,
    val kind: String,
    val amountMinor: Long,
    val currency: String,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val merchant: String? = null,
    val note: String? = null,
    val tags: List<String> = emptyList(),
    val occurredAt: String,
    val receiptId: Long? = null,
    val recurringId: Long? = null,
    val source: String,
    val splits: List<SplitDto> = emptyList(),
)

@Serializable
data class SplitDto(
    val categoryId: Long,
    val amountMinor: Long,
    val currency: String,
    val note: String? = null,
)

@Serializable
data class ReceiptDto(
    val id: Long,
    val imagePath: String,
    val imageEntry: String? = null,
    val rawText: String? = null,
    val createdAt: String,
    val items: List<ReceiptItemDto> = emptyList(),
)

@Serializable
data class ReceiptItemDto(
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val quantity: Double? = null, // money-ok: quantity
)

@Serializable
data class RecurringDto(
    val id: Long,
    val type: String,
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val cycleEvery: Int,
    val cycleUnit: String,
    val startDate: String,
    val nextDate: String,
    val trialEndsOn: String? = null,
    val status: String,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val cancelUrl: String? = null,
    val note: String? = null,
    val worth: String? = null,
    /** ISO date the rating was given; older backups do not have it. */
    val worthRatedOn: String? = null,
    val remindDaysBefore: Int = 2,
    val autoLog: Boolean = false,
    val annualAlternativeMinor: Long? = null,
    val annualAlternativeCurrency: String? = null,
    val brandKey: String? = null,
    val sharedWith: Int = 1,
    val priceHistory: List<PricePointDto> = emptyList(),
)

@Serializable
data class PricePointDto(val amountMinor: Long, val currency: String, val since: String)

@Serializable
data class BudgetDto(
    val id: Long,
    val name: String,
    val limitMinor: Long,
    val currency: String,
    val period: String,
    val rollover: Boolean,
    val categoryIds: List<Long> = emptyList(),
)

@Serializable
data class GoalDto(
    val id: Long,
    val name: String,
    val targetMinor: Long,
    val targetCurrency: String,
    val savedMinor: Long,
    val savedCurrency: String,
    val deadline: String? = null,
    val icon: String,
    val archived: Boolean,
)

@Serializable
data class InboxDto(
    val id: Long,
    val draftJson: String,
    val origin: String,
    val rawText: String,
    val receivedAt: String,
    val status: String,
)

@Serializable
data class ExchangeRateDto(
    val baseCurrency: String,
    val quoteCurrency: String,
    val rateDate: String,
    val rate: String,
    val source: String,
)
