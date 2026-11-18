package dev.antomanc.tameki.feature.aibridge

import dev.antomanc.tameki.core.model.ai.Problem
import dev.antomanc.tameki.core.model.ai.ProblemCode
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
        val problems: List<Problem>,
    )

    data class ResolvedSubscription(
        val draft: SubscriptionDraft,
        val categoryId: Long?,
        val problems: List<Problem>,
    )

    fun resolveTransaction(
        draft: TransactionDraft,
        categories: List<Category>,
        accounts: List<Account>,
        merchantMemory: Map<String, Long> = emptyMap(),
        path: String = "transaction",
    ): ResolvedTransaction {
        val problems = mutableListOf<Problem>()
        val amount = draft.amount
        if (amount == null || amount.minor <= 0) {
            problems += Problem(path, "Missing amount", ProblemCode.MISSING_AMOUNT)
        }
        val categoryId = draft.categoryHint?.let { hint ->
            CategoryMatcher.match(hint, categories, merchantMemory, draft.merchant)
        }
        if (draft.categoryHint != null && categoryId == null) {
            problems += Problem(
                path,
                "Unknown category “${draft.categoryHint}”",
                ProblemCode.UNKNOWN_CATEGORY,
                draft.categoryHint,
            )
        }
        val accountId = draft.accountHint?.let { hint ->
            accounts.firstOrNull { it.name.equals(hint, ignoreCase = true) }?.id
        } ?: accounts.firstOrNull()?.id
        if (draft.accountHint != null && accounts.none { it.name.equals(draft.accountHint, ignoreCase = true) }) {
            problems += Problem(
                path,
                "Unknown account “${draft.accountHint}”",
                ProblemCode.UNKNOWN_ACCOUNT,
                draft.accountHint,
            )
        }
        if (accountId == null) {
            problems += Problem(path, "No account", ProblemCode.NO_ACCOUNT)
        }
        return ResolvedTransaction(draft, categoryId, accountId, problems)
    }

    fun resolveSubscription(
        draft: SubscriptionDraft,
        categories: List<Category>,
        path: String = "subscription",
    ): ResolvedSubscription {
        val problems = mutableListOf<Problem>()
        if (draft.name.isBlank()) {
            problems += Problem(path, "Missing name", ProblemCode.MISSING_NAME)
        }
        if (draft.amount.minor <= 0) {
            problems += Problem(path, "Missing amount", ProblemCode.MISSING_AMOUNT)
        }
        val categoryId = draft.categoryHint?.let { CategoryMatcher.match(it, categories, emptyMap(), null) }
        if (draft.categoryHint != null && categoryId == null) {
            problems += Problem(
                path,
                "Unknown category “${draft.categoryHint}”",
                ProblemCode.UNKNOWN_CATEGORY,
                draft.categoryHint,
            )
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
        if (resolved.problems.any { it.code.blocking }) return null
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
    ): Recurring? {
        if (resolved.problems.any { it.code.blocking }) return null
        if (resolved.draft.name.isBlank() || resolved.draft.amount.minor <= 0) return null
        val today = LocalDate.now(clock)
        val next = resolved.draft.nextDate ?: today.plusMonths(1)
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
            startDate = today,
            nextDate = next,
            trialEndsOn = resolved.draft.trialEndsOn,
            status = status,
            categoryId = resolved.categoryId,
            cancelUrl = resolved.draft.cancelUrl,
            remindDaysBefore = defaultRemindDays,
        )
    }
}

/** Whether a problem blocks import until the user fixes it. */
internal val ProblemCode.blocking: Boolean
    get() = when (this) {
        ProblemCode.MISSING_AMOUNT,
        ProblemCode.CANNOT_PARSE_AMOUNT,
        ProblemCode.AMOUNT_TYPE,
        ProblemCode.UNKNOWN_CATEGORY,
        ProblemCode.UNKNOWN_ACCOUNT,
        ProblemCode.NO_ACCOUNT,
        ProblemCode.MISSING_NAME,
        ProblemCode.NO_JSON,
        ProblemCode.INVALID_JSON,
        ProblemCode.MISSING_VERSION,
        ProblemCode.VERSION_NOT_NUMBER,
        ProblemCode.UNSUPPORTED_VERSION,
        -> true
        else -> false
    }
