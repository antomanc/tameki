package dev.antomanc.tameki.feature.activity

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.InboxItem
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.repository.DayTotal
import java.time.LocalDate

data class ActivityFilters(
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val source: TransactionSource? = null,
    val tag: String? = null,
    val query: String = "",
)

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
    /** The injected clock's today: period chips compute their bounds from it, never from the device. */
    val today: LocalDate? = null,
)
