package dev.antomanc.tameki.feature.activity

import androidx.paging.LoadState
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.InboxItem
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.repository.DayTotal
import java.time.LocalDate

/**
 * The period presets under search. A preset stores *which* window was asked for, never the bounds
 * themselves: "This month" must mean the current month whenever the query runs, otherwise the
 * window freezes on the day it was clicked and starts cutting today's rows off at midnight while
 * the chip still claims the current month.
 */
enum class ActivityPeriod(val labelRes: Int) {
    ANY(R.string.activity_period_any),
    THIS_MONTH(R.string.activity_period_month),
    LAST_30_DAYS(R.string.activity_period_last_30_days),
    THIS_YEAR(R.string.activity_period_year);

    /** The absolute window this preset stands for on [today]; the query resolves it every time. */
    fun bounds(today: LocalDate): Pair<LocalDate?, LocalDate?> = when (this) {
        ANY -> null to null
        THIS_MONTH -> today.withDayOfMonth(1) to today
        LAST_30_DAYS -> today.minusDays(29) to today
        THIS_YEAR -> today.withDayOfYear(1) to today
    }

    /** Selection is the preset, not a pair of dates: it survives the midnight rollover. */
    fun isSelected(filters: ActivityFilters): Boolean = filters.period == this
}

data class ActivityFilters(
    val period: ActivityPeriod = ActivityPeriod.ANY,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val source: TransactionSource? = null,
    val tag: String? = null,
    val query: String = "",
)

/**
 * Facets the filter sheet owns: category, account, source and tag. The period is a filter the
 * sheet cannot show, so it stays out of this set and its chips remain the period's only UI.
 */
internal val ActivityFilters.hasSheetFacets: Boolean
    get() = categoryId != null || accountId != null || source != null || tag != null

/** The period preset narrows the list even though the sheet does not own it. */
internal val ActivityFilters.hasPeriod: Boolean
    get() = period != ActivityPeriod.ANY

/** True when anything other than the free-text query narrows the list. */
internal val ActivityFilters.hasFacets: Boolean
    get() = hasPeriod || hasSheetFacets

/**
 * One count per sheet-owned facet. The period is deliberately excluded: its control is not in the
 * sheet, so badge + sheet must agree on what "3 filters" means.
 */
internal val ActivityFilters.facetCount: Int
    get() = listOf(categoryId != null, accountId != null, source != null, tag != null).count { it }

/**
 * Drops the sheet's own facets and keeps both the query and the period. The header's Clear
 * filters and the sheet's reset share this: a button that cannot show the period chips must not
 * change them under the user's feet. "Clear filters" never erases what was typed either; the
 * search field has its own trailing clear button.
 */
internal fun ActivityFilters.clearedSheetFacets(): ActivityFilters =
    copy(categoryId = null, accountId = null, source = null, tag = null)

/** The empty state's way out when only a period preset hides every row. */
internal fun ActivityFilters.clearedPeriod(): ActivityFilters = copy(period = ActivityPeriod.ANY)

/** What the empty state is hiding: the search text, the sheet facets, the period, or a mix. */
internal enum class ActivityNoMatch { SEARCH, FILTERS, PERIOD, BOTH }

internal val ActivityFilters.noMatch: ActivityNoMatch?
    get() = when {
        query.isNotBlank() && hasFacets -> ActivityNoMatch.BOTH
        query.isNotBlank() -> ActivityNoMatch.SEARCH
        hasSheetFacets -> ActivityNoMatch.FILTERS
        hasPeriod -> ActivityNoMatch.PERIOD
        else -> null
    }

/** What the list area says when it holds no rows. Loading and failure are never "empty". */
internal enum class ActivityEmptyKind { BASE, NO_MATCH, ERROR }

/**
 * The one decision about an empty list, kept out of the composable so the tests can pin it. A
 * refresh in progress says nothing yet, a failed refresh offers Retry instead of "no movements",
 * and only a loaded, row-less list may let the filters explain themselves.
 */
internal fun activityEmptyKind(
    hasRows: Boolean,
    refresh: LoadState,
    noMatch: ActivityNoMatch?,
): ActivityEmptyKind? = when {
    hasRows -> null
    refresh is LoadState.Loading -> null
    refresh is LoadState.Error -> ActivityEmptyKind.ERROR
    noMatch != null -> ActivityEmptyKind.NO_MATCH
    else -> ActivityEmptyKind.BASE
}

sealed interface ActivityListItem {
    data class Header(
        val date: LocalDate,
        val label: String,
        val net: Money,
        val unconverted: List<Money> = emptyList(),
    ) : ActivityListItem
    data class Row(
        val transaction: Transaction,
        val title: String,
        val categoryKey: String,
        val meta: String?,
        val income: Boolean,
        /** The amount in the base currency when [transaction] is foreign and a rate exists. */
        val converted: Money? = null,
    ) : ActivityListItem
}

data class InboxRow(
    val item: InboxItem,
    val title: String,
    val amount: Money?,
    val categoryKey: String,
)

data class ActivityUiState(
    val filters: ActivityFilters = ActivityFilters(),
    val showFilters: Boolean = false,
    val focusInbox: Boolean = false,
    val inbox: List<InboxRow> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val dayTotals: List<DayTotal> = emptyList(),
    /**
     * Increments on every delete; the screen keys its Undo snackbar on it, then acknowledges the
     * event so a tab return or rotation does not replay it. 0 means "nothing to show".
     */
    val deleteGeneration: Long = 0L,
    val pendingDeleteId: Long? = null,
    val baseCurrency: CurrencyCode = CurrencyCode("EUR"),
)
