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
import dev.antomanc.tameki.core.model.money.ExchangeRates
import dev.antomanc.tameki.core.model.money.sumConverting
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.DayTotal
import dev.antomanc.tameki.core.model.repository.InboxRepository
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionFilter
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import dev.antomanc.tameki.core.ui.format.RelativeDates
import dev.antomanc.tameki.core.ui.format.TimeOfDay
import dev.antomanc.tameki.core.ui.format.TransactionTitle
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val exchangeRates: ExchangeRates,
    private val clock: Clock,
) : ViewModel() {

    private val filters = MutableStateFlow(ActivityFilters())
    private val focusInbox = MutableStateFlow(false)
    private val showFilters = MutableStateFlow(false)
    private val deleteGeneration = MutableStateFlow(0L)
    private val pendingDelete = MutableStateFlow<Transaction?>(null)

    /**
     * The clock's day. A period preset is relative to it ("this month" is a different window
     * tomorrow), so it is state, not a value captured when the filter is built: [refreshToday]
     * and [watchToday] move it, and every query re-resolves its bounds from it.
     */
    private val today = MutableStateFlow(LocalDate.now(clock))

    /** A timer for the day change while the screen stays open; null until the screen asks. */
    private var dayWatch: Job? = null

    private data class Shell(
        val filters: ActivityFilters,
        val focusInbox: Boolean,
        val showFilters: Boolean,
        val deleteGeneration: Long,
        val pendingDeleteId: Long?,
    )

    private data class Catalog(
        val inbox: List<InboxItem>,
        val categories: List<Category>,
        val accounts: List<Account>,
        val base: CurrencyCode,
        val totals: List<DayTotal>,
    )

    /** The day a preset resolves against, and the filter it resolved to that day. */
    private data class Window(val day: LocalDate, val repositoryFilter: TransactionFilter)

    private val window: Flow<Window> = combine(filters, today) { f, day ->
        Window(day = day, repositoryFilter = f.toRepo(day))
    }

    val uiState: StateFlow<ActivityUiState> = combine(
        combine(filters, focusInbox, showFilters, deleteGeneration, pendingDelete) { f, focus, show, deletes, pending ->
            Shell(f, focus, show, deletes, pending?.id)
        },
        combine(
            inbox.observe(InboxStatus.PENDING),
            categories.observeAll(),
            accounts.observeAll(),
            settings.observeBaseCurrency(),
            window.flatMapLatest { w -> transactions.observeDayTotals(w.repositoryFilter) },
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
            deleteGeneration = shell.deleteGeneration,
            pendingDeleteId = shell.pendingDeleteId,
            baseCurrency = catalog.base,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState())

    /**
     * What the pager is keyed on. The rows themselves are part of the key: an edit that changes
     * only the merchant, note or category leaves the day totals identical, so a key without the
     * list would leave the PagingSource rendering the rows it captured when it was created.
     * A new key means a new pager generation, which is what repaints the list.
     *
     * The resolved filter and the day it was resolved on are in the key too: a preset's bounds
     * move at midnight, and the pager must follow the day instead of keeping yesterday's window.
     *
     * Internal so the ViewModel test can prove that a title edit produces a new key.
     */
    internal val pagingKeys: Flow<PagingKey> = combine(
        window,
        categories.observeAll(),
        accounts.observeAll(),
        settings.observeBaseCurrency(),
    ) { w, cats, accs, base -> ListCatalog(w, cats, accs, base) }
        .flatMapLatest { catalog ->
            combine(
                transactions.observe(catalog.window.repositoryFilter).distinctUntilChanged(),
                transactions.observeDayTotals(catalog.window.repositoryFilter),
            ) { rows, totals ->
                PagingKey(
                    filter = catalog.window.repositoryFilter,
                    day = catalog.window.day,
                    categories = catalog.categories,
                    accounts = catalog.accounts,
                    base = catalog.base,
                    totals = totals,
                    transactions = rows,
                )
            }
        }
        // Room invalidation fires for the whole table: a write outside this filter still re-runs
        // both queries. Only a key whose content actually moved may restart the pager.
        .distinctUntilChanged()

    val listItems: Flow<PagingData<ActivityListItem>> = pagingKeys
        .flatMapLatest { key ->
            Pager(PagingConfig(pageSize = 60, enablePlaceholders = false)) { pagingSource(key) }.flow
        }
        .cachedIn(viewModelScope)

    private fun pagingSource(key: PagingKey): PagingSource<Int, ActivityListItem> =
        object : PagingSource<Int, ActivityListItem>() {
            override fun getRefreshKey(state: PagingState<Int, ActivityListItem>): Int? = null

            // The whole filtered list is one page, so load only renders the key's snapshot: the
            // observation that produced it is what invalidates the pager.
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ActivityListItem> =
                try {
                    LoadResult.Page(
                        data = buildItems(
                            transactions = key.transactions,
                            categories = key.categories,
                            accounts = key.accounts,
                            totals = key.totals,
                            today = key.day,
                            base = key.base,
                            rates = exchangeRates,
                        ),
                        prevKey = null,
                        nextKey = null,
                    )
                } catch (t: Throwable) {
                    LoadResult.Error(t)
                }
        }

    fun setFocusInbox(focus: Boolean) {
        focusInbox.value = focus
    }

    fun setQuery(query: String) {
        filters.update { it.copy(query = query) }
    }

    /**
     * Re-reads the clock. The screen calls it when it comes back to the foreground: a preset's
     * bounds are relative to today, so a window resolved yesterday must not outlive the night
     * (the chip would say "This month" while the query still cut at yesterday).
     */
    fun refreshToday() {
        today.value = LocalDate.now(clock)
    }

    /**
     * Keeps today current while the screen stays open across midnight. Started by the route, not
     * by the ViewModel: no timer runs while nothing is looking at the list.
     */
    fun watchToday() {
        if (dayWatch?.isActive == true) return
        dayWatch = viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now(clock)
                val nextDay = now.toLocalDate().plusDays(1).atStartOfDay()
                delay(Duration.between(now, nextDay).toMillis().coerceAtLeast(1_000L))
                refreshToday()
            }
        }
    }

    fun openFilters(show: Boolean) {
        showFilters.value = show
    }

    fun applyFilters(next: ActivityFilters) {
        filters.value = next
        showFilters.value = false
    }

    fun delete(transactionId: Long) {
        viewModelScope.launch {
            val tx = transactions.get(transactionId) ?: return@launch
            transactions.delete(transactionId)
            // Snapshot and counter land in one main-thread turn after the row is gone, so an
            // acknowledgement for an earlier delete can never drop this one's Undo.
            pendingDelete.value = tx
            // A counter, not a sticky "deleted" flag: two swipes in a row are two events, and
            // each one must raise its own Undo snackbar.
            deleteGeneration.update { it + 1 }
        }
    }

    fun undoDelete() {
        val tx = pendingDelete.value ?: return
        pendingDelete.value = null
        viewModelScope.launch { transactions.upsert(tx) }
    }

    /**
     * The Undo snackbar for [generation] was shown and resolved. Zero the counter so returning to
     * this tab or rotating does not replay the same event, and drop the snapshot when the user
     * let the snackbar go. A delete that happened meanwhile is left untouched.
     */
    fun acknowledgeDelete(generation: Long) {
        if (deleteGeneration.value != generation) return
        pendingDelete.value = null
        deleteGeneration.value = 0L
    }

    fun dismissInbox(id: Long) {
        viewModelScope.launch { inbox.setStatus(id, InboxStatus.DISMISSED) }
    }

    private data class ListCatalog(
        val window: Window,
        val categories: List<Category>,
        val accounts: List<Account>,
        val base: CurrencyCode,
    )

    /**
     * The resolved filter, the day it was resolved on, and the catalogs it renders with: the rows
     * and totals it produced. Data-class equality is the whole invalidation contract.
     */
    internal data class PagingKey(
        val filter: TransactionFilter,
        val day: LocalDate,
        val categories: List<Category>,
        val accounts: List<Account>,
        val base: CurrencyCode,
        val totals: List<DayTotal>,
        val transactions: List<Transaction>,
    )

    private fun ActivityFilters.toRepo(day: LocalDate): TransactionFilter {
        val (from, to) = period.bounds(day)
        return TransactionFilter(
            from = from,
            to = to,
            accountId = accountId,
            categoryId = categoryId,
            text = query.ifBlank { null },
            tag = tag,
            source = source,
        )
    }

    companion object {
        suspend fun buildItems(
            transactions: List<Transaction>,
            categories: List<Category>,
            accounts: List<Account>,
            totals: List<DayTotal>,
            today: LocalDate,
            base: CurrencyCode,
            rates: ExchangeRates,
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
                val dayNets = totalsByDate[date].orEmpty().map { it.expense - it.income }
                val converted = sumConverting(dayNets, base, date, rates)
                out += ActivityListItem.Header(
                    date = date,
                    label = dayLabel(date, today, locale),
                    net = converted.converted,
                    unconverted = converted.unconverted,
                )
                for (tx in txs) {
                    val cat = tx.categoryId?.let { catById[it] }
                    val account = accById[tx.accountId]
                    out += ActivityListItem.Row(
                        transaction = tx,
                        title = TransactionTitle.of(
                            merchant = tx.merchant,
                            category = cat,
                            account = account,
                            note = tx.note,
                            occurredAt = tx.occurredAt,
                            locale = locale,
                        ),
                        categoryKey = cat?.icon ?: "other",
                        // Account and time stay on the row: two purchases on the same day are only
                        // distinguishable by when they happened, and by which card paid.
                        meta = listOfNotNull(
                            account?.customName?.takeIf { it.isNotBlank() } ?: account?.name,
                            TimeOfDay.short(tx.occurredAt, locale),
                        ).joinToString(" · "),
                        income = tx.kind == TransactionKind.INCOME,
                    )
                }
            }
            return out
        }

        fun dayLabel(date: LocalDate, today: LocalDate, locale: Locale): String = when (date) {
            today -> RelativeDates.today(locale)
            today.minusDays(1) -> RelativeDates.yesterday(locale)
            else -> {
                val dow = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                val day = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)
                "$dow · $day"
            }
        }
    }
}
