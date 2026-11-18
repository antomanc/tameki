package dev.antomanc.tameki.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
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
import dev.antomanc.tameki.core.model.money.ExchangeRates
import dev.antomanc.tameki.core.model.money.sumConverting
import dev.antomanc.tameki.core.model.money.toBaseOrNull
import dev.antomanc.tameki.core.model.recurring.RecurrenceCalculator
import dev.antomanc.tameki.core.model.repository.BudgetRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.InboxRepository
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionFilter
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import dev.antomanc.tameki.core.ui.components.RecurringFlag
import dev.antomanc.tameki.core.ui.format.RelativeDates
import dev.antomanc.tameki.core.ui.format.TransactionTitle
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val transactions: TransactionRepository,
    private val recurrings: RecurringRepository,
    private val budgets: BudgetRepository,
    private val categories: CategoryRepository,
    private val inbox: InboxRepository,
    private val settings: SettingsRepository,
    private val exchangeRates: ExchangeRates,
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
        Quad(base, core, monthTx, recentTx)
    }.mapLatest { (base, core, monthTx, recentTx) ->
        val day = today
        runCatching { exchangeRates.refreshRemoteRates(base) }
        val normalizedMonth = normalizeTransactions(monthTx, base)
        val normalizedRecurring = normalizeRecurrings(core.recurrings, base, day)
        val unconverted = collectUnconverted(monthTx, core.recurrings, base, day)
        val hero = AvailableToSpendCalculator.compute(
            budgets = core.budgets,
            transactionsThisMonth = normalizedMonth,
            recurrings = normalizedRecurring,
            today = day,
            baseCurrency = base,
        )
        val pool = hero.budgetLimit ?: hero.incomeThisMonth.takeIf { !it.isZero }
        val expected = pool?.let { AvailableToSpendCalculator.expectedByToday(it, day) }
        val renewals = nextSevenDays(core.recurrings, day)
        val insights = SavingsInsights.compute(
            InsightInput(
                recurrings = normalizedRecurring,
                transactionsThisMonth = normalizedMonth,
                transactionsTrailingThreeMonths = normalizedMonth,
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
                categoryName = { id -> catById[id]?.resolvedName() },
            )
        }
        HomeUiState(
            loading = false,
            baseCurrency = base,
            hero = hero,
            expectedByToday = expected,
            unconverted = unconverted,
            renewals = renewals.take(3),
            hasMoreRenewals = renewals.size > 3,
            topInsight = top,
            pendingInboxCount = core.pending.size,
            recent = recentTx.take(5).map { tx ->
                val cat = tx.categoryId?.let { catById[it] }
                HomeTransaction(
                    transaction = tx,
                    title = TransactionTitle.of(
                        merchant = tx.merchant,
                        category = cat,
                        account = null,
                        note = tx.note,
                        occurredAt = tx.occurredAt,
                    ),
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

    private data class Quad(
        val base: CurrencyCode,
        val core: Core,
        val monthTx: List<Transaction>,
        val recentTx: List<Transaction>,
    )

    private suspend fun normalizeTransactions(
        list: List<Transaction>,
        base: CurrencyCode,
    ): List<Transaction> = list.mapNotNull { tx ->
        val converted = exchangeRates.toBaseOrNull(tx.amount, base, tx.occurredAt.toLocalDate())
            ?: return@mapNotNull if (tx.amount.currency == base) tx else null
        if (converted == tx.amount) tx else tx.copy(amount = converted)
    }

    private suspend fun normalizeRecurrings(
        list: List<Recurring>,
        base: CurrencyCode,
        day: LocalDate,
    ): List<Recurring> = list.mapNotNull { r ->
        val converted = exchangeRates.toBaseOrNull(r.amount, base, day)
            ?: return@mapNotNull if (r.amount.currency == base) r else null
        if (converted == r.amount) r else r.copy(amount = converted)
    }

    private suspend fun collectUnconverted(
        monthTx: List<Transaction>,
        recurrings: List<Recurring>,
        base: CurrencyCode,
        day: LocalDate,
    ): List<Money> {
        val amounts = buildList {
            monthTx.forEach { add(it.amount) }
            recurrings.forEach { add(it.amount) }
        }
        return sumConverting(amounts, base, day, exchangeRates).unconverted
    }

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

    private fun relative(today: LocalDate, date: LocalDate): String =
        RelativeDates.inDays(ChronoUnit.DAYS.between(today, date).toInt())

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
            Worth.RARELY -> out += RecurringFlag.RARELY_USED
            Worth.UNUSED -> out += RecurringFlag.UNUSED
            Worth.LOVE, Worth.OK, null -> Unit
        }
        return out
    }
}
