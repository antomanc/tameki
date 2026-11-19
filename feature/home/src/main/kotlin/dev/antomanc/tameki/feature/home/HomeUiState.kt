package dev.antomanc.tameki.feature.home

import androidx.compose.ui.graphics.Color
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.budget.AvailableToSpend
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.insight.Insight
import dev.antomanc.tameki.core.ui.components.RecurringFlag

data class HomeAccountBalance(
    val account: Account,
    val balance: Money,
)

data class HomeRenewal(
    val recurring: Recurring,
    val relativeDate: String,
    val flags: Set<RecurringFlag>,
    /** The brand's monogram wash, same source as the Recurring tab. */
    val brandColor: Color? = null,
    /** The same charge in the base currency when [recurring] is foreign; null without a rate. */
    val converted: Money? = null,
)

data class HomeInsight(
    val insight: Insight,
    val kind: HomeInsightKind,
    val categoryKey: String,
)

data class HomeTransaction(
    val transaction: Transaction,
    val title: String,
    val categoryKey: String,
    val meta: String?,
    val income: Boolean,
    /** The same amount in the base currency when foreign; null when no rate is known. */
    val converted: Money? = null,
)

data class HomeUiState(
    val loading: Boolean = true,
    val baseCurrency: CurrencyCode = CurrencyCode("EUR"),
    val hero: AvailableToSpend? = null,
    val expectedByToday: Money? = null,
    /** Foreign amounts that could not be converted for the hero totals. */
    val unconverted: List<Money> = emptyList(),
    val accountBalances: List<HomeAccountBalance> = emptyList(),
    val renewals: List<HomeRenewal> = emptyList(),
    val hasMoreRenewals: Boolean = false,
    val topInsight: HomeInsight? = null,
    val pendingInboxCount: Int = 0,
    val recent: List<HomeTransaction> = emptyList(),
)
