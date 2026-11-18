package dev.antomanc.tameki.feature.aibridge

import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.ai.Problem
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Category
import java.time.LocalDate

enum class AiPromptKind {
    RECEIPT,
    NATURAL_LANGUAGE,
    SUBSCRIPTIONS,
    MONTH_REVIEW,
}

data class PromptCardUi(
    val kind: AiPromptKind,
    val prompt: String,
)

data class DraftRowUi(
    val id: String,
    val title: String,
    val amount: Money?,
    val categoryId: Long?,
    val categoryKey: String,
    val categoryName: String?,
    val accountId: Long?,
    val accountName: String?,
    val date: LocalDate?,
    val problems: List<Problem>,
    val isSubscription: Boolean,
) {
    val hasBlockingProblem: Boolean
        get() = problems.any { it.code.blocking }
}

data class AiBridgeUiState(
    val prompts: List<PromptCardUi> = emptyList(),
    val installedChatbots: List<ChatbotApps.Chatbot> = emptyList(),
    val pasteText: String = "",
    val draftRows: List<DraftRowUi> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val snackbar: String? = null,
    val importing: Boolean = false,
    val importedCount: Int = 0,
    val parsed: Boolean = false,
) {
    val importableCount: Int
        get() = draftRows.count { !it.hasBlockingProblem }
}
