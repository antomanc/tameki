package dev.antomanc.tameki.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.budget.AvailableToSpendCalculator
import dev.antomanc.tameki.core.model.coroutines.runCatchingCancellable
import dev.antomanc.tameki.core.model.entity.Account
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
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.BudgetRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.InboxRepository
import dev.antomanc.tameki.core.model.repository.InsightSnoozeRepository
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionFilter
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import dev.antomanc.tameki.core.ui.brand.BrandCatalog
import dev.antomanc.tameki.core.ui.components.RecurringFlag
import dev.antomanc.tameki.core.ui.format.RelativeDates
import dev.antomanc.tameki.core.ui.format.TimeOfDay
import dev.antomanc.tameki.core.ui.format.TransactionTitle
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val transactions: TransactionRepository,
    private val recurrings: RecurringRepository,
    private val budgets: BudgetRepository,
    private val categories: CategoryRepository,
    private val accounts: AccountRepository,
    private val inbox: InboxRepository,
    private val snoozes: InsightSnoozeRepository,
    private val settings: SettingsRepository,
    private val exchangeRates: ExchangeRates,
    private val clock: Clock,
    private val brandCatalog: BrandCatalog = BrandCatalog(emptyList()),
) : ViewModel() {

    private val today: LocalDate get() = LocalDate.now(clock)

    init {
        // The hero must not wait for the network. Rates refresh in the background; the flow below
        // emits whatever is cached immediately and again when the rates land.
        viewModelScope.launch {
            settings.observeBaseCurrency().distinctUntilChanged().collect { base ->
                runCatchingCancellable { exchangeRates.refreshRemoteRates(base) }
            }
        }
    }

    private data class Core(
        val budgets: List<Budget>,
        val recurrings: List<Recurring>,
        val categories: List<Category>,
        val pending: List<InboxItem>,
        val rules: InsightRules,
        val dismissedInsights: Set<String>,
    )

    val uiState: StateFlow<HomeUiState> = combine(
        settings.observeBaseCurrency(),
        combine(
            combine(
                budgets.observeAll(),
                recurrings.observeAll(),
                categories.observeAll(),
            ) { b, r, c -> Triple(b, r, c) },
            combine(
                inbox.observe(InboxStatus.PENDING),
                settings.observeInsightRules(),
                snoozes.observeSnoozed(today),
            ) { p, rules, dismissed -> Triple(p, rules, dismissed) },
        ) { catalog, extras ->
            Core(
                budgets = catalog.first,
                recurrings = catalog.second,
                categories = catalog.third,
                pending = extras.first,
                rules = extras.second,
                dismissedInsights = extras.third,
            )
        },
        settings.observeBaseCurrency().flatMapLatest { monthTransactions() },
        recentTransactions(),
        accounts.observeAll(),
    ) { base, core, monthTx, recentTx, accountList ->
        Quad(base, core, monthTx, recentTx, accountList)
    }.mapLatest { (base, core, monthTx, recentTx, accountList) ->
        val day = today
        val normalizedMonth = normalizeTransactions(monthTx, base)
        // Three full months before this one: the denominator for a category spike. Passing this
        // month's own rows made every average equal today's spend, so no spike could ever fire.
        val trailingStart = day.withDayOfMonth(1).minusMonths(3)
        val trailingEnd = day.withDayOfMonth(1).minusDays(1)
        val normalizedTrailing = normalizeTransactions(
            recentTx.filter {
                val date = it.occurredAt.toLocalDate()
                !date.isBefore(trailingStart) && !date.isAfter(trailingEnd)
            },
            base,
        )
        val normalizedRecurring = normalizeRecurrings(core.recurrings, base, day)
        // Rows keep their original amount; these maps only add the base-currency value beside it.
        // A missing rate stays missing: no row invents a converted figure.
        val recentConverted = recentTx.mapNotNull { tx ->
            if (tx.amount.currency == base) return@mapNotNull null
            exchangeRates.toBaseOrNull(tx.amount, base, tx.occurredAt.toLocalDate())
                ?.let { tx.id to it }
        }.toMap()
        val recurringConverted = core.recurrings.mapNotNull { recurring ->
            if (recurring.amount.currency == base) return@mapNotNull null
            exchangeRates.toBaseOrNull(recurring.amount, base, day)?.let { recurring.id to it }
        }.toMap()
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
        val renewals = nextSevenDays(core.recurrings, day, recurringConverted)
        val insights = SavingsInsights.compute(
            InsightInput(
                recurrings = normalizedRecurring,
                transactionsThisMonth = normalizedMonth,
                transactionsTrailingThreeMonths = normalizedTrailing,
                budgets = core.budgets,
                rules = core.rules,
            ),
            day,
        ).filterNot { it.id in core.dismissedInsights }
        val catById = core.categories.associateBy { it.id }
        val recById = core.recurrings.associateBy { it.id }
        val accById = accountList.associateBy { it.id }
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
                    // Same quiet meta as Activity: which account paid, and when.
                    meta = listOfNotNull(
                        accById[tx.accountId]?.resolvedName()?.takeIf { it.isNotBlank() },
                        TimeOfDay.short(tx.occurredAt),
                    ).joinToString(" · "),
                    income = tx.kind == TransactionKind.INCOME,
                    converted = recentConverted[tx.id],
                )
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    /**
     * Puts the card away for thirty days — the same snooze the Save tab uses — so the next-best
     * insight can take its place without resurrecting one the user already dismissed, and the
     * choice survives process death and tab switches.
     */
    fun dismissInsight() {
        val id = uiState.value.topInsight?.insight?.id ?: return
        viewModelScope.launch { snoozes.snooze(id, today.plusDays(30)) }
    }

    private data class Quad(
        val base: CurrencyCode,
        val core: Core,
        val monthTx: List<Transaction>,
        val recentTx: List<Transaction>,
        val accounts: List<Account>,
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

    private fun nextSevenDays(
        list: List<Recurring>,
        day: LocalDate,
        converted: Map<Long, Money>,
    ): List<HomeRenewal> {
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
                    brandColor = brandCatalog.find(r.brandKey)?.accent,
                    converted = converted[r.id],
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
