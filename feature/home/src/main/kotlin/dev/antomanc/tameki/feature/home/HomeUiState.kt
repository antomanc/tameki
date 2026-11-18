package dev.antomanc.tameki.feature.home

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.insight.Insight
import dev.antomanc.tameki.core.ui.components.RecurringFlag

data class HomeRenewal(
    val recurring: Recurring,
    val relativeDate: String,
    val flags: Set<RecurringFlag>,
)

data class HomeInsight(
    val insight: Insight,
    val title: String,
    val body: String,
    val actionLabel: String,
    val categoryKey: String,
)

data class HomeTransaction(
    val transaction: Transaction,
    val title: String,
    val categoryKey: String,
    val meta: String?,
    val income: Boolean,
)

data class HomeUiState(
    val loading: Boolean = true,
    val baseCurrency: CurrencyCode = CurrencyCode("EUR"),
    val hero: AvailableToSpend? = null,
    val expectedByToday: Money? = null,
    /** Foreign amounts that could not be converted for the hero totals. */
    val unconverted: List<Money> = emptyList(),
    val renewals: List<HomeRenewal> = emptyList(),
    val hasMoreRenewals: Boolean = false,
    val topInsight: HomeInsight? = null,
    val pendingInboxCount: Int = 0,
    val recent: List<HomeTransaction> = emptyList(),
)
