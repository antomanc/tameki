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
import dev.antomanc.tameki.core.model.money.toBaseOrNull
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
    private val exchangeRates: ExchangeRates,
    private val clock: Clock,
) : ViewModel() {

    private val filters = MutableStateFlow(ActivityFilters())
    private val queryText = MutableStateFlow("")
    private var queryJob: Job? = null
    private val focusInbox = MutableStateFlow(false)
    private val showFilters = MutableStateFlow(false)

    /** What the current snackbar's Undo reverts. One offer at a time: a new event replaces it. */
    private sealed interface UndoOffer {
        val notice: ActivityNotice

        data class Delete(override val notice: ActivityNotice.Deleted, val tx: Transaction) : UndoOffer
        data class Accept(
            override val notice: ActivityNotice.InboxAccepted,
            val transactionId: Long,
            val inboxItemId: Long,
        ) : UndoOffer
    }

    private val undo = MutableStateFlow<UndoOffer?>(null)
    private var nextToken = 0L

    /** Inbox items whose one-tap confirm is being written; a second tap on them is ignored. */
    private val confirming = MutableStateFlow<Set<Long>>(emptySet())

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
        val queryText: String,
        val focusInbox: Boolean,
        val showFilters: Boolean,
        val notice: ActivityNotice?,
        val confirming: Set<Long> = emptySet(),
    )

    private data class Catalog(
        val inbox: List<InboxItem>,
        val categories: List<Category>,
        val accounts: List<Account>,
        val base: CurrencyCode,
    )

    /** The day a preset resolves against, and the filter it resolved to that day. */
    private data class Window(val day: LocalDate, val repositoryFilter: TransactionFilter)

    private val window: Flow<Window> = combine(filters, today) { f, day ->
        Window(day = day, repositoryFilter = f.toRepo(day))
    }

    val uiState: StateFlow<ActivityUiState> = combine(
        combine(
            combine(filters, queryText, focusInbox, showFilters, undo) { f, text, focus, show, offer ->
                Shell(f, text, focus, show, offer?.notice)
            },
            confirming,
        ) { shell, inFlight -> shell.copy(confirming = inFlight) },
        combine(
            inbox.observe(InboxStatus.PENDING),
            categories.observeAll(),
            accounts.observeAll(),
            settings.observeBaseCurrency(),
        ) { pending, cats, accs, base ->
            Catalog(pending, cats, accs, base)
        },
    ) { shell, catalog ->
        ActivityUiState(
            filters = shell.filters,
            queryText = shell.queryText,
            showFilters = shell.showFilters,
            focusInbox = shell.focusInbox,
            inbox = catalog.inbox.map { item ->
                val hint = item.draft.categoryHint
                InboxRow(
                    item = item,
                    title = item.draft.merchant ?: hint ?: item.rawText.take(40),
                    amount = item.draft.amount,
                    // The icon comes from the category the hint resolves to, so an Italian
                    // "Alimentari" shows the groceries glyph rather than a missing key.
                    categoryKey = matchCategory(hint, catalog.categories)?.icon ?: "other",
                    confirming = item.id in shell.confirming,
                )
            },
            categories = catalog.categories,
            accounts = catalog.accounts,
            notice = shell.notice,
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

    /**
     * The field shows every key at once; the list query follows 200 ms after the last key, so
     * typing a word does not restart the journal's queries once per letter. Clearing is immediate.
     */
    fun setQuery(query: String) {
        queryText.value = query
        queryJob?.cancel()
        if (query.isEmpty()) {
            filters.update { it.copy(query = "") }
            return
        }
        queryJob = viewModelScope.launch {
            delay(QUERY_DEBOUNCE_MS)
            filters.update { it.copy(query = query) }
        }
    }

    /** Applies a category facet from another screen (insight bar, budget shortcut). */
    fun setCategoryFilter(categoryId: Long) {
        filters.update { it.copy(categoryId = categoryId) }
    }

    /** Applies an account facet from another screen (account balance card on Today). */
    fun setAccountFilter(accountId: Long) {
        filters.update { it.copy(accountId = accountId) }
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
        if (next.query != queryText.value) queryText.value = next.query
        showFilters.value = false
    }

    fun delete(transactionId: Long) {
        viewModelScope.launch {
            val tx = transactions.get(transactionId) ?: return@launch
            transactions.delete(transactionId)
            // A token, not a sticky flag: two swipes in a row are two events, and each one raises
            // its own Undo snackbar. The offer replaces any earlier one.
            undo.value = UndoOffer.Delete(ActivityNotice.Deleted(++nextToken), tx)
        }
    }

    fun dismissInbox(id: Long) {
        viewModelScope.launch { inbox.setStatus(id, InboxStatus.DISMISSED) }
    }

    /**
     * One-tap confirm for a capture that already carries an amount: match the hinted account and
     * category, book it, mark the inbox item accepted, and raise Undo. A draft with no amount is
     * not confirmable without inventing a number, so the caller opens the editor instead.
     *
     * @return false when [item] cannot be booked here, so the screen falls back to review.
     */
    fun confirmInbox(item: InboxItem): Boolean {
        val draft = item.draft
        // A transfer needs a destination the capture never carries; review it instead.
        if (draft.kind == TransactionKind.TRANSFER) return false
        val amount = draft.amount ?: return false
        val accountList = uiState.value.accounts
        // Decided synchronously from the loaded catalog: with no account the editor opens, rather
        // than a tap that silently does nothing.
        val account = draft.accountHint?.let { hint ->
            accountList.firstOrNull {
                it.name.equals(hint, ignoreCase = true) ||
                    it.customName?.equals(hint, ignoreCase = true) == true
            }
        } ?: accountList.firstOrNull() ?: return false
        // Already being written: the second tap is the same confirm, not a second booking.
        if (item.id in confirming.value) return true
        confirming.update { it + item.id }
        viewModelScope.launch {
            try {
                // Claim first: the notification's own "Add" may be racing this tap.
                if (!inbox.claim(item.id, InboxStatus.PENDING, InboxStatus.ACCEPTED)) return@launch
                val category = matchCategory(draft.categoryHint, categories.observeAll().first())
                val transactionId = transactions.upsert(
                    Transaction(
                        kind = draft.kind,
                        amount = amount,
                        accountId = account.id,
                        categoryId = category?.id,
                        merchant = draft.merchant,
                        note = draft.note,
                        occurredAt = draft.occurredAt ?: LocalDateTime.now(clock),
                        source = item.origin,
                    ),
                )
                undo.value = UndoOffer.Accept(ActivityNotice.InboxAccepted(++nextToken), transactionId, item.id)
            } finally {
                confirming.update { it - item.id }
            }
        }
        return true
    }

    /** Undo on the snackbar for [token]: reverts exactly the event that snackbar offered. */
    fun undo(token: Long) {
        val offer = undo.value?.takeIf { it.notice.token == token } ?: return
        undo.value = null
        viewModelScope.launch {
            when (offer) {
                is UndoOffer.Delete -> transactions.upsert(offer.tx)
                is UndoOffer.Accept -> {
                    transactions.delete(offer.transactionId)
                    inbox.setStatus(offer.inboxItemId, InboxStatus.PENDING)
                }
            }
        }
    }

    /**
     * The snackbar for [token] resolved. The notice is cleared so a tab return cannot replay it,
     * and its Undo expires. A newer event is left untouched.
     */
    fun acknowledgeNotice(token: Long) {
        undo.update { if (it?.notice?.token == token) null else it }
    }

    private fun matchCategory(hint: String?, categoryList: List<Category>): Category? {
        if (hint.isNullOrBlank()) return null
        val key = hint.lowercase().replace(' ', '_')
        return categoryList.firstOrNull {
            it.name.equals(hint, ignoreCase = true) ||
                it.customName?.equals(hint, ignoreCase = true) == true ||
                it.icon.equals(key, ignoreCase = true)
        }
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
        private const val QUERY_DEBOUNCE_MS = 200L

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
                // Same sign convention as the rows: income is "+", spending is "−".
                // DayTotal keeps the two magnitudes apart, so the net is income minus expense.
                val dayNets = totalsByDate[date].orEmpty().map { it.income - it.expense }
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
                        // The row keeps the original; a known rate adds the base value beside it.
                        converted = if (tx.amount.currency == base) {
                            null
                        } else {
                            rates.toBaseOrNull(tx.amount, base, date)
                        },
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
