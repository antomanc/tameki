package dev.antomanc.tameki.core.model.ai

import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import java.time.LocalDate

/**
 * Stable problem identity for UI localisation. [Problem.message] stays English for logs/tests;
 * screens map [code] (+ optional [arg]) to IT/EN strings.
 */
enum class ProblemCode {
    NO_JSON,
    INVALID_JSON,
    MISSING_VERSION,
    VERSION_NOT_NUMBER,
    UNSUPPORTED_VERSION,
    EXPECTED_OBJECT,
    UNKNOWN_KIND,
    MISSING_AMOUNT,
    AMOUNT_TYPE,
    CANNOT_PARSE_AMOUNT,
    MISSING_NAME,
    MISSING_CYCLE_DEFAULT,
    UNRECOGNIZED_CYCLE,
    INVALID_DATE,
    INVALID_TIME,
    UNKNOWN_CATEGORY,
    UNKNOWN_ACCOUNT,
    NO_ACCOUNT,
}

data class Problem(
    val path: String,
    val message: String,
    val code: ProblemCode,
    /** Optional detail for localisation (category/account name, raw value, version…). */
    val arg: String? = null,
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
