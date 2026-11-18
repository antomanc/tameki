package dev.antomanc.tameki.feature.aibridge

import dev.antomanc.tameki.core.model.ai.SubscriptionDraft
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryMatcher
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.core.model.entity.TransactionSource
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Resolves AI drafts against local categories/accounts for preview and import. */
object AiDraftMapper {
    data class ResolvedTransaction(
        val draft: TransactionDraft,
        val categoryId: Long?,
        val accountId: Long?,
        val problems: List<String>,
    )

    data class ResolvedSubscription(
        val draft: SubscriptionDraft,
        val categoryId: Long?,
        val problems: List<String>,
    )

    fun resolveTransaction(
        draft: TransactionDraft,
        categories: List<Category>,
        accounts: List<Account>,
        merchantMemory: Map<String, Long> = emptyMap(),
        path: String = "transaction",
    ): ResolvedTransaction {
        val problems = mutableListOf<String>()
        val amount = draft.amount
        if (amount == null || amount.minor <= 0) {
            problems += "Missing amount"
        }
        val categoryId = draft.categoryHint?.let { hint ->
            CategoryMatcher.match(hint, categories, merchantMemory, draft.merchant)
        }
        if (draft.categoryHint != null && categoryId == null) {
            problems += "Unknown category “${draft.categoryHint}”"
        }
        val accountId = draft.accountHint?.let { hint ->
            accounts.firstOrNull { it.name.equals(hint, ignoreCase = true) }?.id
        } ?: accounts.firstOrNull()?.id
        if (draft.accountHint != null && accounts.none { it.name.equals(draft.accountHint, ignoreCase = true) }) {
            problems += "Unknown account “${draft.accountHint}”"
        }
        if (accountId == null) {
            problems += "No account"
        }
        return ResolvedTransaction(draft, categoryId, accountId, problems)
    }

    fun resolveSubscription(
        draft: SubscriptionDraft,
        categories: List<Category>,
    ): ResolvedSubscription {
        val problems = mutableListOf<String>()
        if (draft.name.isBlank()) problems += "Missing name"
        if (draft.amount.minor <= 0) problems += "Missing amount"
        val categoryId = draft.categoryHint?.let { CategoryMatcher.match(it, categories, emptyMap(), null) }
        if (draft.categoryHint != null && categoryId == null) {
            problems += "Unknown category “${draft.categoryHint}”"
        }
        return ResolvedSubscription(draft, categoryId, problems)
    }

    fun toTransaction(
        resolved: ResolvedTransaction,
        clock: Clock,
        source: TransactionSource = TransactionSource.AI_IMPORT,
    ): Transaction? {
        val amount = resolved.draft.amount ?: return null
        val accountId = resolved.accountId ?: return null
        if (amount.minor <= 0) return null
        val occurred = resolved.draft.occurredAt
            ?: LocalDateTime.of(LocalDate.now(clock), LocalTime.NOON)
        return Transaction(
            kind = resolved.draft.kind,
            amount = amount,
            accountId = accountId,
            categoryId = resolved.categoryId,
            merchant = resolved.draft.merchant,
            note = resolved.draft.note,
            occurredAt = occurred,
            source = source,
        )
    }

    fun toRecurring(
        resolved: ResolvedSubscription,
        clock: Clock,
        defaultRemindDays: Int = 2,
    ): Recurring {
        val today = LocalDate.now(clock)
        val next = resolved.draft.nextDate ?: today.plusMonths(1)
        val start = today
        val trialEnds = resolved.draft.trialEndsOn
        val status = if (trialEnds != null && trialEnds.isAfter(today)) {
            RecurringStatus.TRIAL
        } else {
            RecurringStatus.ACTIVE
        }
        return Recurring(
            type = RecurringType.SUBSCRIPTION,
            name = resolved.draft.name,
            amount = resolved.draft.amount,
            cycle = resolved.draft.cycle,
            startDate = start,
            nextDate = next,
            trialEndsOn = resolved.draft.trialEndsOn,
            status = status,
            categoryId = resolved.categoryId,
            cancelUrl = resolved.draft.cancelUrl,
            remindDaysBefore = defaultRemindDays,
        )
    }
}
