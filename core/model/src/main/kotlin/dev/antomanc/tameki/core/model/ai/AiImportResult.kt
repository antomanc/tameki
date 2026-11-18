package dev.antomanc.tameki.core.model.ai

import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import java.time.LocalDate

data class Problem(
    val path: String,
    val message: String,
)

data class SubscriptionDraft(
    val name: String,
    val amount: Money,
    val cycle: Cycle,
    val nextDate: LocalDate?,
    val trialEndsOn: LocalDate?,
    val categoryHint: String?,
    val cancelUrl: String?,
)

/**
 * Lenient AI paste outcome: usable drafts plus per-path problems for the preview UI.
 * Never throws — malformed chatbot output must still show something actionable.
 */
data class AiImportResult(
    val transactions: List<TransactionDraft> = emptyList(),
    val subscriptions: List<SubscriptionDraft> = emptyList(),
    val problems: List<Problem> = emptyList(),
)
