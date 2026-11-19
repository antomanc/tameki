package dev.antomanc.tameki.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.budget.BudgetPace
import dev.antomanc.tameki.core.model.budget.Pace
import dev.antomanc.tameki.core.model.budget.PaceStatus
import dev.antomanc.tameki.core.model.entity.Budget
import dev.antomanc.tameki.core.model.entity.BudgetPeriod
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.Goal
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.Worth
import dev.antomanc.tameki.core.model.insight.InsightInput
import dev.antomanc.tameki.core.model.insight.InsightRules
import dev.antomanc.tameki.core.model.insight.SavingsInsights
import dev.antomanc.tameki.core.model.money.ExchangeRates
import dev.antomanc.tameki.core.model.money.sumConverting
import dev.antomanc.tameki.core.model.money.toBaseOrNull
import dev.antomanc.tameki.core.model.recurring.CostNormalizer
import dev.antomanc.tameki.core.model.repository.BudgetRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.GoalRepository
import dev.antomanc.tameki.core.model.repository.InsightSnoozeRepository
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionFilter
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The structured content of an insight card; the screen turns it into a localised sentence. */
sealed interface InsightKind {
    data class Unused(val name: String, val worth: Worth) : InsightKind
    data class Overlap(val categoryName: String?) : InsightKind
    data class AnnualPlan(val name: String, val percent: Int) : InsightKind
    data class PriceIncrease(val name: String, val old: Money, val new: Money) : InsightKind
    data class TrialEnding(val name: String, val daysLeft: Int) : InsightKind
    data class CategorySpike(val categoryName: String, val percent: Int, val thisMonth: Money, val average: Money) : InsightKind
    data class SmallFrequent(val label: String, val count: Int, val monthly: Money) : InsightKind
    data class BudgetAtRisk(
        val budgetName: String,
        val spent: Money,
        val limit: Money,
        val expected: Money,
    ) : InsightKind
    data class NoSpendStreak(val days: Int) : InsightKind
}

/** Where the card's one action goes. The app owns what each of these opens. */
sealed interface InsightAction {
    data class OpenSubscription(val id: Long) : InsightAction
    data object ReviewSubscriptions : InsightAction
    data class SetBudget(val id: Long) : InsightAction
    data class OpenCategory(val id: Long) : InsightAction
    data object OpenActivity : InsightAction
}

data class InsightCardUi(
    val id: String,
    val kind: InsightKind,
    val saving: Money?,
    val action: InsightAction,
    /** The core:ui category hue/icon key the card wears, or "other". */
    val categoryKey: String = "other",
)

/** A budget row: the pace numbers the bar needs, already in the base currency. */
data class BudgetUi(
    val id: Long,
    val name: String,
    val spent: Money,
    val limit: Money,
    val expected: Money,
    val status: PaceStatus,
    val period: BudgetPeriod,
    val categoryIds: Set<Long>,
    val rollover: Boolean,
)

/** A category offered when creating a budget. */
data class CategoryOptionUi(val id: Long, val name: String, val categoryKey: String)

data class GoalUi(
    val id: Long,
    val name: String,
    val saved: Money,
    val target: Money,
    val deadline: LocalDate?,
)

/** One month of the six-month spending chart. */
data class MonthSpend(val month: YearMonth, val amount: Money)

/** One category of this month's spending; [categoryId] is null for uncategorised expenses. */
data class CategorySpend(
    val categoryId: Long?,
    val categoryKey: String,
    val label: String?,
    val amount: Money,
)

/**
 * The Save tab: the one number that matters (possible yearly saving), the ranked opportunities
 * behind it, then budgets, goals and the trends. All money is in the base currency.
 */
data class InsightsUiState(
    val loading: Boolean = true,
    val currency: CurrencyCode = CurrencyCode("EUR"),
    val today: LocalDate = LocalDate.of(1970, 1, 1),
    val totalYearlySaving: Money = Money.zero(CurrencyCode("EUR")),
    val insights: List<InsightCardUi> = emptyList(),
    val budgets: List<BudgetUi> = emptyList(),
    val goals: List<GoalUi> = emptyList(),
    val months: List<MonthSpend> = emptyList(),
    val categories: List<CategorySpend> = emptyList(),
    val categoryOptions: List<CategoryOptionUi> = emptyList(),
    val unconverted: List<Money> = emptyList(),
    /**
     * Increments on every budget or goal delete; the screen keys its Undo snackbar on it, then
     * acknowledges the event so a tab return or rotation does not replay it. 0 means "nothing".
     */
    val deleteGeneration: Long = 0L,
)

/**
 * Builds the Save screen from repositories. Insight rules stay in [InsightRules]; the ranking and
 * every saving figure come from [SavingsInsights], so snoozing a card changes the hero honestly
 * instead of hiding a number that is still there.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(
    transactions: TransactionRepository,
    private val recurrings: RecurringRepository,
    private val budgets: BudgetRepository,
    private val goals: GoalRepository,
    private val categories: CategoryRepository,
    private val settings: SettingsRepository,
    private val snoozes: InsightSnoozeRepository,
    private val exchangeRates: ExchangeRates,
    private val clock: Clock,
) : ViewModel() {
    private data class Core(
        val recurrings: List<Recurring>,
        val budgets: List<Budget>,
        val goals: List<Goal>,
        val categories: List<Category>,
        val rules: InsightRules,
    )

    private val today: LocalDate get() = LocalDate.now(clock)

    private val deleteGeneration = MutableStateFlow(0L)

    /** What the last delete removed, kept while its snackbar is up so Undo can put it back. */
    private sealed interface PendingDelete {
        data class BudgetDelete(val budget: Budget) : PendingDelete
        data class GoalDelete(val goal: Goal) : PendingDelete
    }

    private var pendingDelete: PendingDelete? = null

    private val core: Flow<Core> = combine(
        recurrings.observeAll(),
        budgets.observeAll(),
        goals.observeAll(),
        categories.observeAll(),
        settings.observeInsightRules(),
    ) { recurrings, budgets, goals, categories, rules ->
        Core(recurrings, budgets, goals, categories, rules)
    }

    /**
     * Six months of expenses, from the first day of the fifth month before this one to today: the
     * range feeds the trend chart, the trailing average and every budget period.
     */
    private val expensesWindow: Flow<List<Transaction>> = settings.observeBaseCurrency()
        .flatMapLatest {
            val from = today.withDayOfMonth(1).minusMonths(5)
            transactions.observe(TransactionFilter(from = from, to = today))
        }

    val uiState: StateFlow<InsightsUiState> = combine(
        core,
        expensesWindow,
        settings.observeBaseCurrency(),
        snoozes.observeSnoozed(today),
        deleteGeneration,
    ) { core, transactions, currency, snoozed, deletes ->
        Quad(core, transactions, currency, snoozed, deletes)
    }.mapLatest { (core, transactions, currency, snoozed, deletes) ->
        build(core, transactions, currency, snoozed).copy(deleteGeneration = deletes)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InsightsUiState(),
    )

    private data class Quad(
        val core: Core,
        val transactions: List<Transaction>,
        val currency: CurrencyCode,
        val snoozed: Set<String>,
        val deleteGeneration: Long,
    )

    /** Puts one card away for 30 days. The date is stored, so it returns without a scheduler. */
    fun snooze(insightId: String) {
        viewModelScope.launch { snoozes.snooze(insightId, today.plusDays(30)) }
    }

    fun saveBudget(
        id: Long?,
        name: String,
        categoryIds: Set<Long>,
        limit: Money,
        period: BudgetPeriod,
        rollover: Boolean,
    ) {
        viewModelScope.launch {
            budgets.upsert(
                Budget(
                    id = id ?: 0L,
                    name = name.trim(),
                    categoryIds = categoryIds,
                    limit = limit,
                    period = period,
                    rollover = rollover,
                ),
            )
        }
    }

    /**
     * Delete is immediate but reversible, like Activity: keep the row and raise an Undo event.
     * The snapshot and the counter land in one main-thread turn after the row is gone, so an
     * acknowledgement for an earlier delete can never drop this one's Undo.
     */
    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            val budget = budgets.get(id) ?: return@launch
            budgets.delete(id)
            pendingDelete = PendingDelete.BudgetDelete(budget)
            deleteGeneration.update { it + 1 }
        }
    }

    /** Goals are archived rather than dropped, so Undo can restore the exact row. */
    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            val goal = goals.get(id) ?: return@launch
            goals.archive(id, true)
            pendingDelete = PendingDelete.GoalDelete(goal)
            deleteGeneration.update { it + 1 }
        }
    }

    fun undoDelete() {
        when (val pending = pendingDelete) {
            is PendingDelete.BudgetDelete -> {
                pendingDelete = null
                viewModelScope.launch { budgets.upsert(pending.budget) }
            }
            is PendingDelete.GoalDelete -> {
                pendingDelete = null
                viewModelScope.launch { goals.archive(pending.goal.id, false) }
            }
            null -> Unit
        }
    }

    /** Kept for callers that only know budgets; the pending row decides what is restored. */
    fun undoDeleteBudget() = undoDelete()

    fun undoDeleteGoal() = undoDelete()

    /**
     * The Undo snackbar for [generation] was shown and resolved. Zero the counter so returning to
     * the tab or rotating does not replay the event, and drop the snapshot when the user let the
     * snackbar go. A newer delete is left untouched.
     */
    fun acknowledgeDelete(generation: Long) {
        if (deleteGeneration.value != generation) return
        pendingDelete = null
        deleteGeneration.value = 0L
    }

    fun saveGoal(id: Long?, name: String, target: Money, deadline: LocalDate?) {
        viewModelScope.launch {
            val existing = id?.let { goals.get(it) }
            goals.upsert(
                Goal(
                    id = existing?.id ?: 0L,
                    name = name.trim(),
                    target = target,
                    saved = existing?.saved ?: Money.zero(target.currency),
                    deadline = deadline,
                    icon = existing?.icon ?: "other",
                ),
            )
        }
    }

    /** Adds money to a goal; a goal can pass its target, and the bar simply reads 100%. */
    fun addToGoal(id: Long, amount: Money) {
        viewModelScope.launch {
            val goal = goals.get(id) ?: return@launch
            if (goal.saved.currency != amount.currency) return@launch
            goals.upsert(goal.copy(saved = goal.saved + amount))
        }
    }

    private suspend fun build(
        core: Core,
        allTransactions: List<Transaction>,
        currency: CurrencyCode,
        snoozed: Set<String>,
    ): InsightsUiState {
        val day = today
        runCatching { exchangeRates.refreshRemoteRates(currency) }
        val monthStart = day.withDayOfMonth(1)
        val monthEnd = day.withDayOfMonth(day.lengthOfMonth())
        val normalized = allTransactions.mapNotNull { tx ->
            val converted = exchangeRates.toBaseOrNull(tx.amount, currency, tx.occurredAt.toLocalDate())
                ?: return@mapNotNull if (tx.amount.currency == currency) tx else null
            if (converted == tx.amount) tx else tx.copy(amount = converted)
        }
        val unconverted = sumConverting(
            allTransactions.map { it.amount },
            currency,
            day,
            exchangeRates,
        ).unconverted
        val expenses = normalized.filter { it.kind == TransactionKind.EXPENSE }
        fun inRange(from: LocalDate, to: LocalDate) = expenses.filter {
            val date = it.occurredAt.toLocalDate()
            !date.isBefore(from) && !date.isAfter(to)
        }

        val thisMonth = inRange(monthStart, day)
        val trailingStart = monthStart.minusMonths(3)
        val trailing = inRange(trailingStart, monthStart.minusDays(1))

        val recurringById = core.recurrings.associateBy { it.id }
        val categoryById = core.categories.associateBy { it.id }

        val budgetRows = core.budgets
            .filter { it.limit.currency == currency }
            .map { budget -> budgetRow(budget, expenses, day) }
        val paces = budgetRows.associate { it.id to Pace(it.spent, it.limit, it.spent, it.status) }

        // Without any recorded expense a "streak" would be an invention, not a fact.
        val streak = if (expenses.isEmpty()) 0 else noSpendStreak(expenses, day)
        val insights = SavingsInsights.compute(
            InsightInput(
                recurrings = core.recurrings,
                transactionsThisMonth = thisMonth,
                transactionsTrailingThreeMonths = trailing,
                budgets = core.budgets,
                budgetPaces = paces,
                noSpendStreakDays = streak,
                rules = core.rules,
            ),
            day,
        ).filterNot { it.id in snoozed }

        val cards = insights
            .mapNotNull { insight ->
                card(
                    insight = insight,
                    recurringById = recurringById,
                    categoryById = categoryById,
                    budgetsById = budgetRows.associateBy { it.id },
                    today = day,
                )
            }
            .take(MaxCards)

        return InsightsUiState(
            loading = false,
            currency = currency,
            today = day,
            totalYearlySaving = SavingsInsights.totalYearlySaving(insights, currency),
            insights = cards,
            budgets = budgetRows,
            goals = core.goals.map {
                GoalUi(it.id, it.name, it.saved, it.target, it.deadline)
            },
            months = monthTotals(expenses, day, months = Months, currency = currency),
            categories = categoryTotals(thisMonth, categoryById, limit = 6, currency = currency),
            categoryOptions = core.categories
                .filter { it.kind == CategoryKind.EXPENSE }
                .map { CategoryOptionUi(it.id, it.resolvedName(), it.icon) },
            unconverted = unconverted,
        )
    }

    private fun budgetRow(budget: Budget, expenses: List<Transaction>, today: LocalDate): BudgetUi {
        val (start, end) = periodBounds(budget.period, today)
        val spent = sumExpenses(expenses, budget.categoryIds, start, today, budget.limit.currency)
        val previousStart = if (budget.period == BudgetPeriod.WEEK) start.minusWeeks(1) else start.minusMonths(1)
        val previousEnd = start.minusDays(1)
        val previousSpent = sumExpenses(expenses, budget.categoryIds, previousStart, previousEnd, budget.limit.currency)
        val previousUnspent = Money(
            (budget.limit.minor - previousSpent.minor).coerceAtLeast(0L),
            budget.limit.currency,
        )
        val pace = BudgetPace.evaluate(
            budget = budget,
            spentSoFar = spent,
            periodStart = start,
            periodEnd = end,
            today = today,
            previousPeriodUnspent = previousUnspent,
        )
        return BudgetUi(
            id = budget.id,
            name = budget.name,
            spent = pace.spent,
            limit = pace.limit,
            expected = expectedByToday(pace.limit, start, end, today),
            status = pace.status,
            period = budget.period,
            categoryIds = budget.categoryIds,
            rollover = budget.rollover,
        )
    }

    private fun card(
        insight: dev.antomanc.tameki.core.model.insight.Insight,
        recurringById: Map<Long, Recurring>,
        categoryById: Map<Long, Category>,
        budgetsById: Map<Long, BudgetUi>,
        today: LocalDate,
    ): InsightCardUi? {
        fun categoryName(id: Long): String = categoryById[id]?.name ?: "—"
        fun recurringCategoryKey(id: Long): String =
            recurringById[id]?.categoryId?.let { categoryById[it]?.icon } ?: "other"
        return when (insight) {
            is dev.antomanc.tameki.core.model.insight.Insight.UnusedSubscription -> {
                val name = recurringById[insight.recurringId]?.name ?: return null
                InsightCardUi(
                    id = insight.id,
                    kind = InsightKind.Unused(name, insight.worth),
                    saving = insight.yearlySaving,
                    action = InsightAction.OpenSubscription(insight.recurringId),
                    categoryKey = recurringCategoryKey(insight.recurringId),
                )
            }
            is dev.antomanc.tameki.core.model.insight.Insight.OverlappingSubscriptions -> InsightCardUi(
                id = insight.id,
                kind = InsightKind.Overlap(categoryById[insight.categoryId]?.name),
                saving = insight.yearlySaving,
                action = InsightAction.ReviewSubscriptions,
                categoryKey = categoryById[insight.categoryId]?.icon ?: "other",
            )
            is dev.antomanc.tameki.core.model.insight.Insight.AnnualPlanSaving -> {
                val recurring = recurringById[insight.recurringId] ?: return null
                val yearly = CostNormalizer.yearly(recurring)
                val percent = percentOf(insight.yearlySaving.minor, yearly.minor)
                InsightCardUi(
                    id = insight.id,
                    kind = InsightKind.AnnualPlan(recurring.name, percent),
                    saving = insight.yearlySaving,
                    action = InsightAction.OpenSubscription(insight.recurringId),
                    categoryKey = recurringCategoryKey(insight.recurringId),
                )
            }
            is dev.antomanc.tameki.core.model.insight.Insight.PriceIncrease -> {
                val name = recurringById[insight.recurringId]?.name ?: return null
                InsightCardUi(
                    id = insight.id,
                    kind = InsightKind.PriceIncrease(name, insight.old, insight.new),
                    saving = insight.yearlySaving,
                    action = InsightAction.OpenSubscription(insight.recurringId),
                    categoryKey = recurringCategoryKey(insight.recurringId),
                )
            }
            is dev.antomanc.tameki.core.model.insight.Insight.TrialEnding -> {
                val name = recurringById[insight.recurringId]?.name ?: return null
                InsightCardUi(
                    id = insight.id,
                    kind = InsightKind.TrialEnding(
                        name = name,
                        daysLeft = ChronoUnit.DAYS.between(today, insight.endsOn).toInt(),
                    ),
                    saving = insight.yearlySaving,
                    action = InsightAction.OpenSubscription(insight.recurringId),
                    categoryKey = recurringCategoryKey(insight.recurringId),
                )
            }
            is dev.antomanc.tameki.core.model.insight.Insight.CategorySpike -> InsightCardUi(
                id = insight.id,
                kind = InsightKind.CategorySpike(
                    categoryName = categoryName(insight.categoryId),
                    percent = percentOf(insight.thisMonth.minor - insight.average.minor, insight.average.minor),
                    thisMonth = insight.thisMonth,
                    average = insight.average,
                ),
                saving = insight.yearlySaving,
                action = InsightAction.OpenCategory(insight.categoryId),
                categoryKey = categoryById[insight.categoryId]?.icon ?: "other",
            )
            is dev.antomanc.tameki.core.model.insight.Insight.SmallFrequentSpend -> {
                val categoryId = insight.merchantOrCategory
                    .removePrefix("category:")
                    .takeIf { insight.merchantOrCategory.startsWith("category:") }
                    ?.toLongOrNull()
                InsightCardUi(
                    id = insight.id,
                    kind = InsightKind.SmallFrequent(
                        label = categoryId?.let { categoryName(it) } ?: insight.merchantOrCategory,
                        count = insight.count,
                        monthly = insight.monthly,
                    ),
                    saving = insight.yearlySaving,
                    action = categoryId
                        ?.let { InsightAction.OpenCategory(it) }
                        ?: InsightAction.OpenActivity,
                    categoryKey = categoryId?.let { categoryById[it]?.icon } ?: "other",
                )
            }
            is dev.antomanc.tameki.core.model.insight.Insight.BudgetAtRisk -> {
                val budget = budgetsById[insight.budgetId] ?: return null
                InsightCardUi(
                    id = insight.id,
                    kind = InsightKind.BudgetAtRisk(
                        budgetName = budget.name,
                        spent = budget.spent,
                        limit = budget.limit,
                        expected = budget.expected,
                    ),
                    saving = null,
                    action = InsightAction.SetBudget(insight.budgetId),
                    categoryKey = "other",
                )
            }
            is dev.antomanc.tameki.core.model.insight.Insight.NoSpendStreak -> InsightCardUi(
                id = insight.id,
                kind = InsightKind.NoSpendStreak(insight.days),
                saving = null,
                action = InsightAction.OpenActivity,
            )
        }
    }

    private companion object {
        const val MaxCards = 6
        const val Months = 6
    }
}
