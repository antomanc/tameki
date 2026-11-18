package dev.antomanc.tameki.feature.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.InboxItem
import dev.antomanc.tameki.core.model.entity.InboxStatus
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.DayTotal
import dev.antomanc.tameki.core.model.repository.InboxRepository
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionFilter
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModel(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val accounts: AccountRepository,
    private val inbox: InboxRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    private val filters = MutableStateFlow(ActivityFilters())
    private val focusInbox = MutableStateFlow(false)
    private val showFilters = MutableStateFlow(false)
    private val snackbar = MutableStateFlow<String?>(null)
    private val pendingDelete = MutableStateFlow<Transaction?>(null)

    private data class Shell(
        val filters: ActivityFilters,
        val focusInbox: Boolean,
        val showFilters: Boolean,
        val snackbar: String?,
        val pendingDeleteId: Long?,
    )

    private data class Catalog(
        val inbox: List<InboxItem>,
        val categories: List<Category>,
        val accounts: List<Account>,
        val base: CurrencyCode,
        val totals: List<DayTotal>,
    )

    val uiState: StateFlow<ActivityUiState> = combine(
        combine(filters, focusInbox, showFilters, snackbar, pendingDelete) { f, focus, show, snack, pending ->
            Shell(f, focus, show, snack, pending?.id)
        },
        combine(
            inbox.observe(InboxStatus.PENDING),
            categories.observeAll(),
            accounts.observeAll(),
            settings.observeBaseCurrency(),
            filters.flatMapLatest { f -> transactions.observeDayTotals(f.toRepo()) },
        ) { pending, cats, accs, base, totals ->
            Catalog(pending, cats, accs, base, totals)
        },
    ) { shell, catalog ->
        ActivityUiState(
            filters = shell.filters,
            showFilters = shell.showFilters,
            focusInbox = shell.focusInbox,
            inbox = catalog.inbox.map { item ->
                val hint = item.draft.categoryHint
                InboxRow(
                    item = item,
                    title = item.draft.merchant
                        ?: hint
                        ?: item.rawText.take(40),
                    amount = item.draft.amount,
                    categoryKey = hint?.lowercase()?.replace(' ', '_') ?: "other",
                )
            },
            categories = catalog.categories,
            accounts = catalog.accounts,
            dayTotals = catalog.totals,
            snackbar = shell.snackbar,
            pendingDeleteId = shell.pendingDeleteId,
            baseCurrency = catalog.base,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState())

    val listItems: Flow<PagingData<ActivityListItem>> = combine(
        filters,
        categories.observeAll(),
        accounts.observeAll(),
        filters.flatMapLatest { f -> transactions.observeDayTotals(f.toRepo()) },
    ) { f, cats, accs, totals ->
        PagingKey(f, cats, accs, totals)
    }.flatMapLatest { key ->
        Pager(PagingConfig(pageSize = 60, enablePlaceholders = false)) {
            object : PagingSource<Int, ActivityListItem>() {
                override fun getRefreshKey(state: PagingState<Int, ActivityListItem>): Int? = null
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ActivityListItem> =
                    try {
                        val list = transactions.observe(key.filters.toRepo()).first()
                        LoadResult.Page(
                            data = buildItems(
                                transactions = list,
                                categories = key.categories,
                                accounts = key.accounts,
                                totals = key.totals,
                                today = LocalDate.now(clock),
                            ),
                            prevKey = null,
                            nextKey = null,
                        )
                    } catch (t: Throwable) {
                        LoadResult.Error(t)
                    }
            }
        }.flow
    }.cachedIn(viewModelScope)

    fun setFocusInbox(focus: Boolean) {
        focusInbox.value = focus
    }

    fun setQuery(query: String) {
        filters.update { it.copy(query = query) }
    }

    fun openFilters(show: Boolean) {
        showFilters.value = show
    }

    fun applyFilters(next: ActivityFilters) {
        filters.value = next
        showFilters.value = false
    }

    fun clearSnackbar() {
        snackbar.value = null
    }

    fun delete(transactionId: Long) {
        viewModelScope.launch {
            val tx = transactions.get(transactionId) ?: return@launch
            pendingDelete.value = tx
            transactions.delete(transactionId)
            snackbar.value = "deleted"
        }
    }

    fun undoDelete() {
        val tx = pendingDelete.value ?: return
        viewModelScope.launch {
            transactions.upsert(tx)
            pendingDelete.value = null
            snackbar.value = null
        }
    }

    fun dismissInbox(id: Long) {
        viewModelScope.launch { inbox.setStatus(id, InboxStatus.DISMISSED) }
    }

    private data class PagingKey(
        val filters: ActivityFilters,
        val categories: List<Category>,
        val accounts: List<Account>,
        val totals: List<DayTotal>,
    )

    private fun ActivityFilters.toRepo() = TransactionFilter(
        from = from,
        to = to,
        accountId = accountId,
        categoryId = categoryId,
        text = query.ifBlank { null },
        tag = tag,
        source = source,
    )

    companion object {
        fun buildItems(
            transactions: List<Transaction>,
            categories: List<Category>,
            accounts: List<Account>,
            totals: List<DayTotal>,
            today: LocalDate,
            locale: Locale = Locale.getDefault(),
        ): List<ActivityListItem> {
            val catById = categories.associateBy { it.id }
            val accById = accounts.associateBy { it.id }
            val totalsByDate = totals.groupBy { it.date }
            val grouped = transactions
                .sortedByDescending { it.occurredAt }
                .groupBy { it.occurredAt.toLocalDate() }
            val out = mutableListOf<ActivityListItem>()
            for ((date, txs) in grouped) {
                out += ActivityListItem.Header(
                    date = date,
                    label = dayLabel(date, today, locale),
                    totals = totalsByDate[date].orEmpty(),
                )
                for (tx in txs) {
                    val cat = tx.categoryId?.let { catById[it] }
                    out += ActivityListItem.Row(
                        transaction = tx,
                        title = tx.merchant?.takeIf { it.isNotBlank() }
                            ?: cat?.name
                            ?: tx.note?.takeIf { it.isNotBlank() }
                            ?: "—",
                        categoryKey = cat?.icon ?: "other",
                        meta = accById[tx.accountId]?.name,
                        income = tx.kind == TransactionKind.INCOME,
                    )
                }
            }
            return out
        }

        fun dayLabel(date: LocalDate, today: LocalDate, locale: Locale): String = when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> {
                val dow = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                val day = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)
                "$dow · $day"
            }
        }
    }
}
