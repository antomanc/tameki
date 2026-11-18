package dev.antomanc.tameki.feature.aibridge

import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Category

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
    val amountLabel: String,
    val categoryId: Long?,
    val accountId: Long?,
    val problems: List<String>,
    val isSubscription: Boolean,
)

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
) {
    val importableCount: Int
        get() = draftRows.count { it.problems.isEmpty() || (!it.isSubscription && it.amountLabel.isNotBlank()) }
}
