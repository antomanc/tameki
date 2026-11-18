package dev.antomanc.tameki.feature.aibridge

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.ai.AiExchangeFormat
import dev.antomanc.tameki.core.model.ai.AiPrompts
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionFilter
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AiBridgeViewModel(
    private val transactions: TransactionRepository,
    private val recurrings: RecurringRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(AiBridgeUiState())
    val uiState: StateFlow<AiBridgeUiState> = _state.asStateFlow()

    private var resolvedTx: List<AiDraftMapper.ResolvedTransaction> = emptyList()
    private var resolvedSubs: List<AiDraftMapper.ResolvedSubscription> = emptyList()

    init {
        viewModelScope.launch {
            combine(
                accounts.observeAll(),
                categories.observeAll(),
                settings.observeBaseCurrency(),
            ) { accountList, categoryList, base ->
                Triple(accountList, categoryList, base)
            }.collect { (accountList, categoryList, base) ->
                val prompts = buildPrompts(categoryList.map { it.name }, accountList.map { it.name }, base)
                _state.update {
                    it.copy(
                        prompts = prompts,
                        categories = categoryList,
                        accounts = accountList,
                        installedChatbots = ChatbotApps.installed(installedPackages()),
                    )
                }
            }
        }
    }

    fun refreshChatbots() {
        _state.update { it.copy(installedChatbots = ChatbotApps.installed(installedPackages())) }
    }

    fun onPromptCopied() {
        _state.update { it.copy(snackbar = "copied") }
    }

    fun clearSnackbar() {
        _state.update { it.copy(snackbar = null) }
    }

    fun setPasteText(text: String) {
        _state.update { it.copy(pasteText = text) }
    }

    fun parsePaste() {
        viewModelScope.launch {
            val base = settings.observeBaseCurrency().first()
            val result = AiExchangeFormat.parse(_state.value.pasteText, base, clock)
            val cats = categories.observeAll().first()
            val accs = accounts.observeAll().first()
            val memory = categories.observeMerchantCategoryMemory().first()
            resolvedTx = result.transactions.mapIndexed { index, draft ->
                AiDraftMapper.resolveTransaction(draft, cats, accs, memory, "transactions[$index]")
            }
            resolvedSubs = result.subscriptions.map { AiDraftMapper.resolveSubscription(it, cats) }
            val globalProblems = result.problems.map { "${it.path}: ${it.message}" }
            val rows = buildList {
                resolvedTx.forEachIndexed { i, r ->
                    val amount = r.draft.amount
                    add(
                        DraftRowUi(
                            id = "tx-$i",
                            title = r.draft.merchant ?: r.draft.note ?: "Transaction",
                            amountLabel = amount?.let { formatMinor(it.minor, it.currency) }.orEmpty(),
                            categoryId = r.categoryId,
                            accountId = r.accountId,
                            problems = r.problems + if (i == 0) globalProblems else emptyList(),
                            isSubscription = false,
                        ),
                    )
                }
                resolvedSubs.forEachIndexed { i, r ->
                    add(
                        DraftRowUi(
                            id = "sub-$i",
                            title = r.draft.name,
                            amountLabel = formatMinor(r.draft.amount.minor, r.draft.amount.currency),
                            categoryId = r.categoryId,
                            accountId = null,
                            problems = r.problems,
                            isSubscription = true,
                        ),
                    )
                }
            }
            _state.update { it.copy(draftRows = rows, categories = cats, accounts = accs) }
        }
    }

    fun setRowCategory(rowId: String, categoryId: Long?) {
        val idx = rowId.removePrefix("tx-").toIntOrNull()
        if (idx != null && idx in resolvedTx.indices) {
            val old = resolvedTx[idx]
            resolvedTx = resolvedTx.toMutableList().also {
                it[idx] = old.copy(
                    categoryId = categoryId,
                    draft = old.draft.copy(
                        categoryHint = categoryId?.let { id ->
                            _state.value.categories.firstOrNull { c -> c.id == id }?.name
                        },
                    ),
                    problems = old.problems.filterNot { it.startsWith("Unknown category") },
                )
            }
        }
        val subIdx = rowId.removePrefix("sub-").toIntOrNull()
        if (subIdx != null && subIdx in resolvedSubs.indices) {
            val old = resolvedSubs[subIdx]
            resolvedSubs = resolvedSubs.toMutableList().also {
                it[subIdx] = old.copy(
                    categoryId = categoryId,
                    problems = old.problems.filterNot { p -> p.startsWith("Unknown category") },
                )
            }
        }
        rebuildRows()
    }

    fun setRowAccount(rowId: String, accountId: Long?) {
        val idx = rowId.removePrefix("tx-").toIntOrNull() ?: return
        if (idx !in resolvedTx.indices) return
        val old = resolvedTx[idx]
        resolvedTx = resolvedTx.toMutableList().also {
            it[idx] = old.copy(
                accountId = accountId,
                problems = old.problems.filterNot { p -> p == "No account" || p.startsWith("Unknown account") },
            )
        }
        rebuildRows()
    }

    fun importAll() {
        viewModelScope.launch {
            _state.update { it.copy(importing = true) }
            var count = 0
            for (r in resolvedTx) {
                val tx = AiDraftMapper.toTransaction(r, clock, TransactionSource.AI_IMPORT) ?: continue
                transactions.upsert(tx)
                val merchant = tx.merchant
                val categoryId = tx.categoryId
                if (!merchant.isNullOrBlank() && categoryId != null) {
                    categories.rememberMerchantCategory(merchant, categoryId)
                }
                count++
            }
            for (r in resolvedSubs) {
                if (r.draft.name.isBlank() || r.draft.amount.minor <= 0) continue
                recurrings.upsert(AiDraftMapper.toRecurring(r, clock))
                count++
            }
            resolvedTx = emptyList()
            resolvedSubs = emptyList()
            _state.update {
                it.copy(
                    importing = false,
                    draftRows = emptyList(),
                    pasteText = "",
                    importedCount = count,
                    snackbar = "imported",
                )
            }
        }
    }

    fun prefillPaste(text: String) {
        _state.update { it.copy(pasteText = text) }
        if (text.isNotBlank()) parsePaste()
    }

    private fun rebuildRows() {
        val rows = buildList {
            resolvedTx.forEachIndexed { i, r ->
                val amount = r.draft.amount
                add(
                    DraftRowUi(
                        id = "tx-$i",
                        title = r.draft.merchant ?: r.draft.note ?: "Transaction",
                        amountLabel = amount?.let { formatMinor(it.minor, it.currency) }.orEmpty(),
                        categoryId = r.categoryId,
                        accountId = r.accountId,
                        problems = r.problems,
                        isSubscription = false,
                    ),
                )
            }
            resolvedSubs.forEachIndexed { i, r ->
                add(
                    DraftRowUi(
                        id = "sub-$i",
                        title = r.draft.name,
                        amountLabel = formatMinor(r.draft.amount.minor, r.draft.amount.currency),
                        categoryId = r.categoryId,
                        accountId = null,
                        problems = r.problems,
                        isSubscription = true,
                    ),
                )
            }
        }
        _state.update { it.copy(draftRows = rows) }
    }

    private suspend fun buildPrompts(
        categoryNames: List<String>,
        accountNames: List<String>,
        base: CurrencyCode,
    ): List<PromptCardUi> {
        val locale = appContext.resources.configuration.locales[0] ?: Locale.getDefault()
        val receipt = AiPrompts.receiptExtraction(locale, categoryNames, accountNames)
        val natural = AiPrompts.naturalLanguageEntry(locale, categoryNames, accountNames)
        val subs = AiPrompts.subscriptionDiscovery(locale, categoryNames)
        val month = YearMonth.from(LocalDate.now(clock))
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        val txs = transactions.observe(
            TransactionFilter(from = monthStart, to = monthEnd),
        ).first()
        val cats = categories.observeAll().first()
        val recs = recurrings.observeAll().first()
        val summary = MonthSummaryBuilder.build(month, base, txs, cats, recs)
        val review = AiPrompts.monthReview(locale, summary)
        return listOf(
            PromptCardUi(AiPromptKind.RECEIPT, receipt),
            PromptCardUi(AiPromptKind.NATURAL_LANGUAGE, natural),
            PromptCardUi(AiPromptKind.SUBSCRIPTIONS, subs),
            PromptCardUi(AiPromptKind.MONTH_REVIEW, review),
        )
    }

    private fun installedPackages(): Set<String> {
        val pm = appContext.packageManager
        return ChatbotApps.all.mapNotNull { bot ->
            try {
                pm.getApplicationInfo(bot.packageName, 0)
                bot.packageName
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }.toSet()
    }

    private fun formatMinor(minor: Long, currency: CurrencyCode): String {
        val digits = when (currency.value) {
            "JPY" -> 0
            else -> 2
        }
        return if (digits == 0) {
            "$minor ${currency.value}"
        } else {
            val whole = minor / 100
            val frac = (minor % 100).toString().padStart(2, '0')
            "$whole.$frac ${currency.value}"
        }
    }
}
