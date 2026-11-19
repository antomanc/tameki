package dev.antomanc.tameki.feature.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Worth
import dev.antomanc.tameki.core.model.money.ExchangeRates
import dev.antomanc.tameki.core.model.money.sumConverting
import dev.antomanc.tameki.core.model.money.toBaseOrNull
import dev.antomanc.tameki.core.model.recurring.CostNormalizer
import dev.antomanc.tameki.core.model.recurring.SubscriptionReview
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.ui.components.RecurringFlag
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Which total the hero figure shows; tapping the figure switches it. */
enum class CostPeriod { MONTH, YEAR }

/** The three statuses the list can show; [ACTIVE] includes trials, which are still running. */
enum class StatusFilter { ACTIVE, PAUSED, CANCELLED }

/** One day of the 30-day strip: a dot when something renews, a marker when it is tapped. */
data class StripDay(
    val date: LocalDate,
    val hasRenewal: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
)

/** A recurring payment ready to be drawn: no formatting, every string is localised in the UI. */
data class RecurringRowUi(
    val id: Long,
    val name: String,
    val brandKey: String?,
    val amount: Money,
    val yearly: Money,
    val cycle: Cycle,
    val nextDate: LocalDate,
    val type: RecurringType,
    val status: RecurringStatus,
    val categoryKey: String?,
    val flags: Set<RecurringFlag>,
)

/**
 * The whole Recurring tab in one immutable value: the hero total (monthly or yearly), the counts
 * per type, the 30-day strip, the filters and the rows sorted by next charge.
 */
data class SubscriptionsUiState(
    val loading: Boolean = true,
    val currency: CurrencyCode = CurrencyCode("EUR"),
    val today: LocalDate = LocalDate.of(1970, 1, 1),
    val period: CostPeriod = CostPeriod.MONTH,
    val totalMonthly: Money = Money.zero(CurrencyCode("EUR")),
    val totalYearly: Money = Money.zero(CurrencyCode("EUR")),
    val subscriptionCount: Int = 0,
    val billCount: Int = 0,
    val incomeCount: Int = 0,
    val days: List<StripDay> = emptyList(),
    val typeFilter: RecurringType? = null,
    val statusFilter: StatusFilter = StatusFilter.ACTIVE,
    val rows: List<RecurringRowUi> = emptyList(),
    val isEmpty: Boolean = false,
    val canReview: Boolean = false,
    val unconverted: List<Money> = emptyList(),
) {
    /** The figure the hero shows for the current [period]. */
    val total: Money get() = if (period == CostPeriod.YEAR) totalYearly else totalMonthly
}

/**
 * Recurring tab: normalised totals, counts, the calendar strip and the list. Filtering is app
 * state, so it lives here next to the repository flows; all the money math is delegated to
 * [CostNormalizer] and every date comes from the injected [Clock].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionsViewModel(
    private val recurrings: RecurringRepository,
    settings: SettingsRepository,
    private val exchangeRates: ExchangeRates,
    private val clock: Clock,
) : ViewModel() {
    private data class Filters(
        val period: CostPeriod = CostPeriod.MONTH,
        val type: RecurringType? = null,
        val status: StatusFilter = StatusFilter.ACTIVE,
        val day: LocalDate? = null,
    )

    private val filters = MutableStateFlow(Filters())

    val uiState: StateFlow<SubscriptionsUiState> = combine(
        recurrings.observeAll(),
        settings.observeBaseCurrency(),
        filters,
    ) { all, currency, filter ->
        Triple(all, currency, filter)
    }.mapLatest { (all, currency, filter) ->
        build(all, currency, filter)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SubscriptionsUiState(),
    )

    fun togglePeriod() = filters.update {
        it.copy(period = if (it.period == CostPeriod.MONTH) CostPeriod.YEAR else CostPeriod.MONTH)
    }

    fun selectDay(date: LocalDate) = filters.update {
        it.copy(day = if (it.day == date) null else date)
    }

    fun setTypeFilter(type: RecurringType?) = filters.update {
        it.copy(type = if (it.type == type) null else type)
    }

    fun setStatusFilter(status: StatusFilter) = filters.update { it.copy(status = status) }

    private suspend fun build(
        all: List<Recurring>,
        currency: CurrencyCode,
        filter: Filters,
    ): SubscriptionsUiState {
        val today = LocalDate.now(clock)
        runCatching { exchangeRates.refreshRemoteRates(currency) }
        val converted = all.mapNotNull { r ->
            val amount = exchangeRates.toBaseOrNull(r.amount, currency, today)
                ?: return@mapNotNull if (r.amount.currency == currency) r else null
            if (amount == r.amount) r else r.copy(amount = amount)
        }
        val unconverted = sumConverting(all.map { it.amount }, currency, today, exchangeRates).unconverted
        val mine = converted
        val running = mine.filter { it.status == RecurringStatus.ACTIVE || it.status == RecurringStatus.TRIAL }
        val costs = running.filter { it.type != RecurringType.INCOME }
        val totalMonthly = costs.fold(Money.zero(currency)) { acc, r -> acc + CostNormalizer.monthly(r) }
        val totalYearly = costs.fold(Money.zero(currency)) { acc, r -> acc + CostNormalizer.yearly(r) }

        val renewalDates = running.map { it.nextDate }.toSet()
        val horizon = today.plusDays(StripDays.toLong())
        val days = (0 until StripDays).map { offset ->
            val date = today.plusDays(offset.toLong())
            StripDay(
                date = date,
                hasRenewal = date in renewalDates,
                isToday = offset == 0,
                isSelected = filter.day == date,
            )
        }

        val visible = mine
            .filter { matchesStatus(it, filter.status) }
            .filter { filter.type == null || it.type == filter.type }
            .filter { filter.day == null || it.nextDate == filter.day }
            .sortedWith(compareBy({ it.nextDate }, { it.name.lowercase() }))

        return SubscriptionsUiState(
            loading = false,
            currency = currency,
            today = today,
            period = filter.period,
            totalMonthly = totalMonthly,
            totalYearly = totalYearly,
            subscriptionCount = running.count { it.type == RecurringType.SUBSCRIPTION },
            billCount = running.count { it.type == RecurringType.BILL },
            incomeCount = running.count { it.type == RecurringType.INCOME },
            days = days,
            typeFilter = filter.type,
            statusFilter = filter.status,
            rows = visible.map { row(it, today) },
            isEmpty = mine.isEmpty(),
            canReview = running.any { SubscriptionReview.isDue(it, today) },
            unconverted = unconverted,
        )
    }

    private fun matchesStatus(recurring: Recurring, status: StatusFilter): Boolean =
        when (status) {
            StatusFilter.ACTIVE ->
                recurring.status == RecurringStatus.ACTIVE || recurring.status == RecurringStatus.TRIAL
            StatusFilter.PAUSED -> recurring.status == RecurringStatus.PAUSED
            StatusFilter.CANCELLED -> recurring.status == RecurringStatus.CANCELLED
        }

    private fun row(recurring: Recurring, today: LocalDate): RecurringRowUi = RecurringRowUi(
        id = recurring.id,
        name = recurring.name,
        brandKey = recurring.brandKey,
        amount = recurring.amount,
        yearly = CostNormalizer.yearly(recurring),
        cycle = recurring.cycle,
        nextDate = recurring.nextDate,
        type = recurring.type,
        status = recurring.status,
        categoryKey = null,
        flags = flagsOf(recurring, today),
    )

    private fun flagsOf(recurring: Recurring, today: LocalDate): Set<RecurringFlag> {
        val flags = mutableSetOf<RecurringFlag>()
        when (recurring.worth) {
            Worth.RARELY -> flags += RecurringFlag.RARELY_USED
            Worth.UNUSED -> flags += RecurringFlag.UNUSED
            Worth.LOVE, Worth.OK, null -> Unit
        }
        if (recurring.status == RecurringStatus.TRIAL) {
            val trial = recurring.trialEndsOn
            if (trial != null) {
                val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, trial)
                if (daysLeft in 0..7) flags += RecurringFlag.TRIAL_ENDING
            }
        }
        if (recentPriceIncrease(recurring, today)) flags += RecurringFlag.PRICE_UP
        return flags
    }

    /** A price that went up in the last 90 days: the same window the insights use. */
    private fun recentPriceIncrease(recurring: Recurring, today: LocalDate): Boolean {
        val history = recurring.priceHistory.sortedBy { it.since }
        if (history.size < 2) return false
        val latest = history.last()
        val previous = history[history.lastIndex - 1]
        if (latest.amount.currency != previous.amount.currency) return false
        if (latest.amount.minor <= previous.amount.minor) return false
        val age = ChronoUnit.DAYS.between(latest.since, today)
        return age in 0..90
    }

    private companion object {
        const val StripDays = 30
    }
}
