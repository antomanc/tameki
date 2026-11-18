package dev.antomanc.tameki.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.antomanc.tameki.core.model.entity.Budget
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.InboxItem
import dev.antomanc.tameki.core.model.entity.InboxStatus
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.Worth
import dev.antomanc.tameki.core.model.insight.InsightInput
import dev.antomanc.tameki.core.model.insight.InsightRules
import dev.antomanc.tameki.core.model.insight.SavingsInsights
import dev.antomanc.tameki.core.model.recurring.RecurrenceCalculator
import dev.antomanc.tameki.core.model.repository.BudgetRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.InboxRepository
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionFilter
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import dev.antomanc.tameki.core.ui.components.RecurringFlag
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val transactions: TransactionRepository,
    private val recurrings: RecurringRepository,
    private val budgets: BudgetRepository,
    private val categories: CategoryRepository,
    private val inbox: InboxRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    private val today: LocalDate get() = LocalDate.now(clock)

    private data class Core(
        val budgets: List<Budget>,
        val recurrings: List<Recurring>,
        val categories: List<Category>,
        val pending: List<InboxItem>,
        val rules: InsightRules,
    )

    val uiState: StateFlow<HomeUiState> = combine(
        settings.observeBaseCurrency(),
        combine(
            budgets.observeAll(),
            recurrings.observeAll(),
            categories.observeAll(),
            inbox.observe(InboxStatus.PENDING),
            settings.observeInsightRules(),
        ) { b, r, c, p, rules -> Core(b, r, c, p, rules) },
        settings.observeBaseCurrency().flatMapLatest { monthTransactions() },
        recentTransactions(),
    ) { base, core, monthTx, recentTx ->
        val day = today
        val hero = AvailableToSpendCalculator.compute(
            budgets = core.budgets,
            transactionsThisMonth = monthTx,
            recurrings = core.recurrings,
            today = day,
            baseCurrency = base,
        )
        val pool = hero.budgetLimit ?: hero.incomeThisMonth.takeIf { !it.isZero }
        val expected = pool?.let { AvailableToSpendCalculator.expectedByToday(it, day) }
        val renewals = nextSevenDays(core.recurrings, day)
        val insights = SavingsInsights.compute(
            InsightInput(
                recurrings = core.recurrings,
                transactionsThisMonth = monthTx,
                transactionsTrailingThreeMonths = monthTx,
                budgets = core.budgets,
                rules = core.rules,
            ),
            day,
        )
        val catById = core.categories.associateBy { it.id }
        val recById = core.recurrings.associateBy { it.id }
        val top = insights.firstOrNull()?.let { insight ->
            InsightPresentation.present(
                insight = insight,
                recurringName = { id -> recById[id]?.name },
                categoryName = { id -> catById[id]?.name },
            )
        }
        HomeUiState(
            loading = false,
            baseCurrency = base,
            hero = hero,
            expectedByToday = expected,
            renewals = renewals.take(3),
            hasMoreRenewals = renewals.size > 3,
            topInsight = top,
            pendingInboxCount = core.pending.size,
            recent = recentTx.take(5).map { tx ->
                val cat = tx.categoryId?.let { catById[it] }
                HomeTransaction(
                    transaction = tx,
                    title = tx.merchant?.takeIf { it.isNotBlank() }
                        ?: cat?.name
                        ?: tx.note?.takeIf { it.isNotBlank() }
                        ?: "—",
                    categoryKey = cat?.icon ?: "other",
                    meta = null,
                    income = tx.kind == TransactionKind.INCOME,
                )
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    private fun monthTransactions(): Flow<List<Transaction>> {
        val day = today
        return transactions.observe(
            TransactionFilter(from = day.withDayOfMonth(1), to = day.withDayOfMonth(day.lengthOfMonth())),
        )
    }

    private fun recentTransactions(): Flow<List<Transaction>> = transactions.observe(TransactionFilter())

    private fun nextSevenDays(list: List<Recurring>, day: LocalDate): List<HomeRenewal> {
        val end = day.plusDays(7)
        val rows = mutableListOf<Pair<LocalDate, Recurring>>()
        for (r in list) {
            if (r.status != RecurringStatus.ACTIVE && r.status != RecurringStatus.TRIAL) continue
            val dates = RecurrenceCalculator.occurrencesBetween(r, day, end)
            for (d in dates) {
                if (!d.isBefore(day)) rows += d to r
            }
        }
        return rows
            .sortedWith(compareBy({ it.first }, { it.second.name }))
            .map { (date, r) ->
                HomeRenewal(
                    recurring = r,
                    relativeDate = relative(day, date),
                    flags = flagsFor(r, day),
                )
            }
            .distinctBy { it.recurring.id }
    }

    private fun relative(today: LocalDate, date: LocalDate): String {
        val days = ChronoUnit.DAYS.between(today, date)
        return when (days) {
            0L -> "Today"
            1L -> "Tomorrow"
            else -> "in $days days"
        }
    }

    private fun flagsFor(r: Recurring, today: LocalDate): Set<RecurringFlag> {
        val out = mutableSetOf<RecurringFlag>()
        val trial = r.trialEndsOn
        if (r.status == RecurringStatus.TRIAL && trial != null) {
            val left = ChronoUnit.DAYS.between(today, trial)
            if (left in 0..7) out += RecurringFlag.TRIAL_ENDING
        }
        if (r.priceHistory.size >= 2) {
            val last = r.priceHistory.maxBy { it.since }
            val prev = r.priceHistory.filter { it.since.isBefore(last.since) }.maxByOrNull { it.since }
            if (prev != null && last.amount.minor > prev.amount.minor) out += RecurringFlag.PRICE_UP
        }
        when (r.worth) {
            Worth.LOVE, Worth.OK -> out += RecurringFlag.WORTH_IT
            Worth.RARELY -> out += RecurringFlag.RARELY_USED
            Worth.UNUSED -> out += RecurringFlag.UNUSED
            null -> Unit
        }
        return out
    }
}
