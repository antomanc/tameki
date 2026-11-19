package dev.antomanc.tameki.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.budget.Pace
import dev.antomanc.tameki.core.model.budget.PaceStatus
import dev.antomanc.tameki.core.model.entity.BudgetPeriod
import dev.antomanc.tameki.core.model.entity.Worth
import dev.antomanc.tameki.core.ui.components.AmountText
import dev.antomanc.tameki.core.ui.components.CategoryBar
import dev.antomanc.tameki.core.ui.components.CategoryBars
import dev.antomanc.tameki.core.ui.components.EmptyState
import dev.antomanc.tameki.core.ui.components.InsightCard
import dev.antomanc.tameki.core.ui.components.MonthBar
import dev.antomanc.tameki.core.ui.components.MonthlyBars
import dev.antomanc.tameki.core.ui.components.PaceBar
import dev.antomanc.tameki.core.ui.components.SectionHeader
import dev.antomanc.tameki.core.ui.format.rememberMoneyFormatter
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.TamekiType
import dev.antomanc.tameki.core.ui.theme.spacing
import dev.antomanc.tameki.core.ui.theme.tamekiColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import org.koin.androidx.compose.koinViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import dev.antomanc.tameki.core.ui.components.OneShotSnackbar
import dev.antomanc.tameki.core.ui.components.DelayedLoading

/**
 * Entry point of the Save tab. The app owns navigation: opening a subscription, the review flow
 * and a category's transactions all leave through callbacks.
 */
@Composable
fun InsightsRoute(
    snackbarHostState: SnackbarHostState,
    onOpenSubscription: (Long) -> Unit,
    onReviewSubscriptions: () -> Unit,
    onOpenCategory: (Long) -> Unit,
    onOpenActivity: () -> Unit,
    openBudget: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onSnooze = viewModel::snooze,
        onOpenSubscription = onOpenSubscription,
        onReviewSubscriptions = onReviewSubscriptions,
        onOpenCategory = onOpenCategory,
        onOpenActivity = onOpenActivity,
        onSaveBudget = viewModel::saveBudget,
        onDeleteBudget = viewModel::deleteBudget,
        onUndoDelete = viewModel::undoDelete,
        onAcknowledgeDelete = viewModel::acknowledgeDelete,
        onSaveGoal = viewModel::saveGoal,
        onDeleteGoal = viewModel::deleteGoal,
        onAddToGoal = viewModel::addToGoal,
        openBudget = openBudget,
        modifier = modifier,
    )
}

/** Where a sheet is in the Save screen: closed, or editing one of the three forms. */
private sealed interface Sheet {
    data object None : Sheet
    data class Budget(val id: Long?) : Sheet
    data class Goal(val id: Long?) : Sheet
    data class AddMoney(val goal: GoalUi) : Sheet
}

/**
 * The Save tab, stateless. One hero number, the ranked cards with one action each, then budgets,
 * goals and the two charts. Sheets keep their own text fields; everything that touches data goes
 * back through a callback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    state: InsightsUiState,
    onSnooze: (String) -> Unit,
    onOpenSubscription: (Long) -> Unit,
    onReviewSubscriptions: () -> Unit,
    onOpenCategory: (Long) -> Unit,
    onOpenActivity: () -> Unit,
    onSaveBudget: (Long?, String, Set<Long>, Money, BudgetPeriod, Boolean) -> Unit,
    onDeleteBudget: (Long) -> Unit,
    onSaveGoal: (Long?, String, Money, LocalDate?) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onAddToGoal: (Long, Money) -> Unit,
    onUndoDelete: (Long) -> Unit = {},
    onAcknowledgeDelete: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    openBudget: Boolean = false,
    /** The app's single host, owned by the root scaffold: features never draw their own. */
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val spacing = MaterialTheme.spacing
    // "Set budget" from Today is a one-shot request: the consumed flag lives in this entry's saved
    // state, so returning to the tab (which restores the entry with its old argument) does not
    // reopen the sheet the user already closed.
    var openBudgetConsumed by rememberSaveable { mutableStateOf(false) }
    var sheet by remember {
        mutableStateOf<Sheet>(if (openBudget && !openBudgetConsumed) Sheet.Budget(null) else Sheet.None)
    }
    LaunchedEffect(Unit) {
        if (openBudget && !openBudgetConsumed) sheet = Sheet.Budget(null)
        openBudgetConsumed = true
    }

    // Same contract as Activity: every delete raises its own token, acknowledged when the
    // snackbar resolves, so a tab return cannot replay an Undo for a budget that is already gone.
    OneShotSnackbar(
        token = state.undoToken,
        host = snackbarHostState,
        message = stringResource(R.string.insights_deleted),
        actionLabel = stringResource(R.string.insights_undo),
        onAction = onUndoDelete,
        onResolved = onAcknowledgeDelete,
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Same app bar as Today, Activity and Recurring: switching tabs must not move the
            // content to the top edge. The title says where you are; the hero below does the rest.
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        stringResource(R.string.insights_title),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (state.loading) {
            DelayedLoading(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = spacing.gutter,
                end = spacing.gutter,
                top = spacing.group,
                bottom = spacing.section + 72.dp,
            ),
        ) {
            // One number when there is something to save; a calm empty when there is not — never a €0 hero.
            if (!state.totalYearlySaving.isZero) {
                item(key = "hero") {
                    val formatter = rememberMoneyFormatter(baseCurrency = state.currency)
                    Column {
                        Text(
                            text = stringResource(R.string.insights_hero_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AmountText(
                            amount = state.totalYearlySaving,
                            style = TamekiType.hero,
                            color = MaterialTheme.tamekiColors.leaf,
                            dimMinorUnits = true,
                            omitZeroMinorUnits = true,
                        )
                        state.unconverted.forEach { amount ->
                            Text(
                                text = stringResource(
                                    dev.antomanc.tameki.core.ui.R.string.ui_unconverted_amount,
                                    formatter.formatWhole(amount),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (state.insights.isEmpty()) {
                item(key = "insights_empty") {
                    EmptyState(
                        title = stringResource(R.string.insights_empty_title),
                        body = stringResource(R.string.insights_empty_body),
                        actionLabel = stringResource(R.string.insights_empty_action),
                        onAction = { sheet = Sheet.Budget(null) },
                        modifier = Modifier.padding(top = spacing.group),
                    )
                }
            } else {
                items(state.insights, key = { it.id }) { card ->
                    val copy = insightCopy(card.kind)
                    InsightCard(
                        title = copy.title,
                        body = copy.body,
                        actionLabel = actionLabel(card.kind),
                        yearlySaving = card.saving,
                        onAction = {
                            when (val action = card.action) {
                                is InsightAction.OpenSubscription -> onOpenSubscription(action.id)
                                InsightAction.ReviewSubscriptions -> onReviewSubscriptions()
                                is InsightAction.OpenCategory -> onOpenCategory(action.id)
                                InsightAction.OpenActivity -> onOpenActivity()
                                is InsightAction.SetBudget -> sheet = Sheet.Budget(action.id)
                            }
                        },
                        onDismiss = { onSnooze(card.id) },
                        categoryKey = card.categoryKey,
                        modifier = Modifier.padding(top = spacing.group),
                    )
                }
            }

            item(key = "budgets_header") {
                SectionHeader(
                    title = stringResource(R.string.insights_budgets),
                    actionLabel = if (state.budgets.isEmpty()) null else stringResource(R.string.insights_add),
                    onAction = if (state.budgets.isEmpty()) null else ({ sheet = Sheet.Budget(null) }),
                )
            }
            if (state.budgets.isEmpty()) {
                item(key = "budgets_empty") {
                    EmptyText(
                        title = stringResource(R.string.insights_budget_empty),
                        body = stringResource(R.string.insights_budget_empty_body),
                        actionLabel = stringResource(R.string.insights_add),
                        onAction = { sheet = Sheet.Budget(null) },
                    )
                }
            } else {
                items(state.budgets, key = { "budget-row-${it.id}" }) { budget ->
                    BudgetRow(
                        budget = budget,
                        onClick = { sheet = Sheet.Budget(budget.id) },
                    )
                }
            }

            item(key = "goals_header") {
                SectionHeader(
                    title = stringResource(R.string.insights_goals),
                    actionLabel = if (state.goals.isEmpty()) null else stringResource(R.string.insights_add),
                    onAction = if (state.goals.isEmpty()) null else ({ sheet = Sheet.Goal(null) }),
                )
            }
            if (state.goals.isEmpty()) {
                item(key = "goals_empty") {
                    EmptyText(
                        title = stringResource(R.string.insights_goal_empty),
                        body = stringResource(R.string.insights_goal_empty_body),
                        actionLabel = stringResource(R.string.insights_add),
                        onAction = { sheet = Sheet.Goal(null) },
                    )
                }
            } else {
                items(state.goals, key = { "goal-row-${it.id}" }) { goal ->
                    GoalRow(
                        goal = goal,
                        onClick = { sheet = Sheet.Goal(goal.id) },
                        onAddMoney = { sheet = Sheet.AddMoney(goal) },
                    )
                }
            }

            item(key = "months_header") {
                // The last bar is the current month and is still filling: say so, or the shorter
                // bar reads as a real spending drop instead of an incomplete month.
                SectionHeader(
                    title = stringResource(R.string.insights_trends_months),
                    supporting = stringResource(R.string.insights_trends_so_far),
                )
            }
            item(key = "months_chart") {
                MonthlyBars(bars = state.months.map { MonthBar(monthLabel(it.month), it.amount) })
            }

            item(key = "categories_header") {
                SectionHeader(title = stringResource(R.string.insights_trends_categories))
            }
            item(key = "categories_chart") {
                CategoryBars(
                    rows = state.categories.map {
                        CategoryBar(
                            label = it.label ?: stringResource(R.string.insights_uncategorized),
                            amount = it.amount,
                            categoryKey = it.categoryKey,
                        )
                    },
                    onClick = { index ->
                        state.categories.getOrNull(index)?.categoryId?.let(onOpenCategory)
                    },
                )
            }
        }

        // The snackbar host lives in MainActivity's root scaffold: raising "Deleted · Undo" here
        // would create a second, competing host over the same event.
    }

    when (val current = sheet) {
        Sheet.None -> Unit
        is Sheet.Budget -> {
            val initial = current.id?.let { id -> state.budgets.firstOrNull { it.id == id } }
            BudgetSheet(
                initial = initial,
                categories = state.categoryOptions,
                currency = state.currency,
                onDismiss = { sheet = Sheet.None },
                onSave = { name, categoryIds, limit, period, rollover ->
                    onSaveBudget(current.id, name, categoryIds, limit, period, rollover)
                    sheet = Sheet.None
                },
                onDelete = initial?.let { budget ->
                    {
                        onDeleteBudget(budget.id)
                        sheet = Sheet.None
                    }
                },
            )
        }
        is Sheet.Goal -> {
            val initial = current.id?.let { id -> state.goals.firstOrNull { it.id == id } }
            GoalSheet(
                initial = initial,
                currency = state.currency,
                today = state.today,
                onDismiss = { sheet = Sheet.None },
                onSave = { name, target, deadline ->
                    onSaveGoal(current.id, name, target, deadline)
                    sheet = Sheet.None
                },
                onDelete = initial?.let { goal ->
                    {
                        onDeleteGoal(goal.id)
                        sheet = Sheet.None
                    }
                },
            )
        }
        is Sheet.AddMoney -> AddMoneySheet(
            goal = current.goal,
            currency = state.currency,
            onDismiss = { sheet = Sheet.None },
            onAdd = { amount ->
                onAddToGoal(current.goal.id, amount)
                sheet = Sheet.None
            },
        )
    }
}

@Composable
private fun BudgetRow(budget: BudgetUi, onClick: () -> Unit) {
    val spacing = MaterialTheme.spacing
    val formatter = rememberMoneyFormatter()
    Surface(
        onClick = onClick,
        color = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.between),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = budget.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        if (budget.period == BudgetPeriod.WEEK) {
                            R.string.insights_budget_week_label
                        } else {
                            R.string.insights_budget_month_label
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            PaceBar(spent = budget.spent, expected = budget.expected, budget = budget.limit)
            // The bar shows spent against expected; the cap itself was only a colour. Print it,
            // so "at risk" can be checked against the number that defines it.
            Text(
                text = stringResource(
                    R.string.insights_budget_limit_amount,
                    formatter.formatWhole(budget.limit),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun GoalRow(goal: GoalUi, onClick: () -> Unit, onAddMoney: () -> Unit) {
    val spacing = MaterialTheme.spacing
    val formatter = rememberMoneyFormatter()
    val ratio = if (goal.target.minor <= 0L) 0f else {
        (goal.saved.minor.toFloat() / goal.target.minor.toFloat()).coerceIn(0f, 1f)
    }
    Surface(
        color = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.between),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // The row is the edit target and the button is the quick action: siblings, so a
                // tap can never be claimed by both and TalkBack stops announcing a nested button.
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = MaterialTheme.spacing.touchTarget)
                        .clickable(onClick = onClick),
                ) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            R.string.insights_goal_progress,
                            formatter.formatWhole(goal.saved),
                            formatter.formatWhole(goal.target),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = onAddMoney,
                    modifier = Modifier.heightIn(min = MaterialTheme.spacing.touchTarget),
                ) {
                    Text(stringResource(R.string.insights_goal_add_money))
                }
            }
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = MaterialTheme.tamekiColors.leaf,
                trackColor = MaterialTheme.tamekiColors.track,
            )
        }
    }
}

@Composable
private fun EmptyText(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(Modifier.padding(top = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onAction, modifier = Modifier.padding(top = 4.dp)) {
            Text(actionLabel)
        }
    }
}

/**
 * The single verb that captures this opportunity. It names the outcome, never the destination:
 * "Disdici" on an unused subscription opens its detail, where the cancel action lives.
 */
@Composable
private fun actionLabel(kind: InsightKind): String = stringResource(
    when (kind) {
        is InsightKind.Unused -> R.string.insights_action_cancel
        is InsightKind.Overlap -> R.string.insights_action_compare
        is InsightKind.AnnualPlan -> R.string.insights_action_plan
        is InsightKind.PriceIncrease -> R.string.insights_action_review
        is InsightKind.TrialEnding -> R.string.insights_action_decide
        is InsightKind.CategorySpike -> R.string.insights_action_activity
        is InsightKind.SmallFrequent -> R.string.insights_action_activity
        is InsightKind.BudgetAtRisk -> R.string.insights_action_budget
        is InsightKind.NoSpendStreak -> R.string.insights_action_activity
    },
)

/** A card's one-sentence title and its optional one-line fact. */
private data class InsightCopy(val title: String, val body: String?)

@Composable
private fun insightCopy(kind: InsightKind): InsightCopy {
    val formatter = rememberMoneyFormatter()
    return when (kind) {
        is InsightKind.Unused -> InsightCopy(
            title = stringResource(
                if (kind.worth == Worth.UNUSED) {
                    R.string.insights_card_unused_never
                } else {
                    R.string.insights_card_unused_rarely
                },
                kind.name,
            ),
            body = null,
        )
        is InsightKind.Overlap -> InsightCopy(
            title = stringResource(R.string.insights_card_overlap),
            body = kind.categoryName?.let { stringResource(R.string.insights_card_overlap_body, it) },
        )
        is InsightKind.AnnualPlan -> InsightCopy(
            title = stringResource(R.string.insights_card_annual, kind.name),
            body = stringResource(R.string.insights_card_annual_body, kind.percent),
        )
        is InsightKind.PriceIncrease -> InsightCopy(
            title = stringResource(R.string.insights_card_price, kind.name),
            body = stringResource(
                R.string.insights_card_price_body,
                formatter.format(kind.old),
                formatter.format(kind.new),
            ),
        )
        is InsightKind.TrialEnding -> InsightCopy(
            title = if (kind.daysLeft <= 0) {
                stringResource(R.string.insights_card_trial, kind.name)
            } else {
                pluralStringResource(R.plurals.insights_card_trial_days, kind.daysLeft, kind.name, kind.daysLeft)
            },
            body = null,
        )
        is InsightKind.CategorySpike -> InsightCopy(
            title = stringResource(R.string.insights_card_spike, kind.categoryName, kind.percent),
            body = stringResource(
                R.string.insights_card_spike_body,
                formatter.formatWhole(kind.thisMonth),
                formatter.formatWhole(kind.average),
            ),
        )
        is InsightKind.SmallFrequent -> InsightCopy(
            title = stringResource(
                R.string.insights_card_frequent,
                kind.label ?: stringResource(R.string.insights_uncategorized),
                kind.count,
            ),
            body = stringResource(R.string.insights_card_frequent_body, formatter.formatWhole(kind.monthly)),
        )
        is InsightKind.BudgetAtRisk -> InsightCopy(
            title = stringResource(R.string.insights_card_budget, kind.budgetName),
            body = stringResource(
                R.string.insights_card_budget_body,
                formatter.formatWhole(kind.spent),
                formatter.formatWhole(kind.expected),
            ),
        )
        is InsightKind.NoSpendStreak -> InsightCopy(
            title = pluralStringResource(R.plurals.insights_card_streak, kind.days, kind.days),
            body = null,
        )
    }
}

@Composable
private fun monthLabel(month: YearMonth): String {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val locale = configuration.locales[0]
    return month.month.getDisplayName(JavaTextStyle.SHORT, locale)
}

@Preview(name = "Save · light", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 400, heightDp = 1400)
@Composable
private fun InsightsLightPreview() {
    TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            InsightsScreen(
                state = sampleInsightsState(),
                onSnooze = {},
                onOpenSubscription = {},
                onReviewSubscriptions = {},
                onOpenCategory = {},
                onOpenActivity = {},
                onSaveBudget = { _, _, _, _, _, _ -> },
                onDeleteBudget = {},
                onSaveGoal = { _, _, _, _ -> },
                onDeleteGoal = {},
                onAddToGoal = { _, _ -> },
            )
        }
    }
}

@Preview(name = "Save · dark", showBackground = true, backgroundColor = 0xFF141513, widthDp = 400, heightDp = 1400)
@Composable
private fun InsightsDarkPreview() {
    TamekiTheme(darkTheme = true, dynamicColor = false, motionEnabled = false) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            InsightsScreen(
                state = sampleInsightsState(),
                onSnooze = {},
                onOpenSubscription = {},
                onReviewSubscriptions = {},
                onOpenCategory = {},
                onOpenActivity = {},
                onSaveBudget = { _, _, _, _, _, _ -> },
                onDeleteBudget = {},
                onSaveGoal = { _, _, _, _ -> },
                onDeleteGoal = {},
                onAddToGoal = { _, _ -> },
            )
        }
    }
}

/** Realistic Italian sample: savings, one budget ahead, two goals, six months of bars. */
internal fun sampleInsightsState(today: LocalDate = LocalDate.of(2026, 11, 18)): InsightsUiState {
    val eur = CurrencyCode("EUR")
    fun money(minor: Long) = Money(minor, eur)
    return InsightsUiState(
        loading = false,
        currency = eur,
        today = today,
        totalYearlySaving = money(78516),
        insights = listOf(
            InsightCardUi(
                id = "unused-4",
                kind = InsightKind.Unused("DAZN", Worth.RARELY),
                saving = money(41988),
                action = InsightAction.OpenSubscription(4),
                categoryKey = "sport",
            ),
            InsightCardUi(
                id = "annual-2",
                kind = InsightKind.AnnualPlan("Spotify", 17),
                saving = money(3480),
                action = InsightAction.OpenSubscription(2),
                categoryKey = "music",
            ),
            InsightCardUi(
                id = "trial-3",
                kind = InsightKind.TrialEnding("Disney+", 3),
                saving = money(10788),
                action = InsightAction.OpenSubscription(3),
                categoryKey = "streaming",
            ),
            InsightCardUi(
                id = "budget-1",
                kind = InsightKind.BudgetAtRisk(
                    budgetName = "Alimentari",
                    spent = money(31240),
                    limit = money(35000),
                    expected = money(21000),
                ),
                saving = null,
                action = InsightAction.SetBudget(1),
            ),
        ),
        budgets = listOf(
            BudgetUi(
                id = 1,
                name = "Alimentari",
                spent = money(31240),
                limit = money(35000),
                expected = money(24500),
                status = PaceStatus.AT_RISK,
                period = BudgetPeriod.MONTH,
                categoryIds = setOf(1L),
                rollover = false,
            ),
            BudgetUi(
                id = 2,
                name = "Svago",
                spent = money(4800),
                limit = money(12000),
                expected = money(8400),
                status = PaceStatus.ON_TRACK,
                period = BudgetPeriod.MONTH,
                categoryIds = setOf(14L),
                rollover = true,
            ),
        ),
        goals = listOf(
            GoalUi(1, "Vacanza in Sicilia", money(68000), money(120000), today.plusMonths(8)),
            GoalUi(2, "MacBook", money(32000), money(150000), null),
        ),
        months = listOf(
            MonthSpend(YearMonth.of(2026, 6), money(91200)),
            MonthSpend(YearMonth.of(2026, 7), money(104500)),
            MonthSpend(YearMonth.of(2026, 8), money(98700)),
            MonthSpend(YearMonth.of(2026, 9), money(128450)),
            MonthSpend(YearMonth.of(2026, 10), money(110200)),
            MonthSpend(YearMonth.of(2026, 11), money(74200)),
        ),
        categories = listOf(
            CategorySpend(1, "groceries", "Alimentari", money(31240)),
            CategorySpend(6, "home", "Casa", money(21500)),
            CategorySpend(2, "restaurant", "Ristorante", money(14900)),
            CategorySpend(4, "transport", "Trasporti", money(8600)),
            CategorySpend(19, "streaming", "Streaming", money(3297)),
        ),
        categoryOptions = listOf(
            CategoryOptionUi(1, "Alimentari", "groceries"),
            CategoryOptionUi(2, "Ristorante", "restaurant"),
            CategoryOptionUi(6, "Casa", "home"),
            CategoryOptionUi(14, "Svago", "entertainment"),
            CategoryOptionUi(19, "Streaming", "streaming"),
        ),
    )
}
