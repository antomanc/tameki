package dev.antomanc.tameki.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.Worth
import dev.antomanc.tameki.core.model.insight.Insight
import dev.antomanc.tameki.core.ui.components.AmountText
import dev.antomanc.tameki.core.ui.components.EmptyState
import dev.antomanc.tameki.core.ui.components.InsightCard
import dev.antomanc.tameki.core.ui.components.PaceBar
import dev.antomanc.tameki.core.ui.components.RecurringFlag
import dev.antomanc.tameki.core.ui.components.RecurringRow
import dev.antomanc.tameki.core.ui.components.SectionHeader
import dev.antomanc.tameki.core.ui.components.TransactionRow
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.TamekiType
import dev.antomanc.tameki.core.ui.theme.spacing
import java.time.LocalDate
import java.time.LocalDateTime
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeRoute(
    onOpenSettings: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenInbox: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onOpenSettings = onOpenSettings,
        onOpenActivity = onOpenActivity,
        onOpenInbox = onOpenInbox,
        onSeeAllRenewals = onOpenSubscriptions,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenSettings: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenInbox: () -> Unit,
    onSeeAllRenewals: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissInsight: () -> Unit = {},
    onInsightAction: () -> Unit = onSeeAllRenewals,
) {
    val spacing = MaterialTheme.spacing
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.home_settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = spacing.gutter,
                end = spacing.gutter,
                bottom = spacing.section,
            ),
        ) {
            item { HeroBlock(state = state) }
            if (state.pendingInboxCount > 0) {
                item {
                    InboxCard(
                        count = state.pendingInboxCount,
                        onClick = onOpenInbox,
                        modifier = Modifier.padding(top = spacing.group),
                    )
                }
            }
            if (state.renewals.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.home_upcoming),
                        supporting = stringResource(R.string.home_upcoming_support),
                        actionLabel = if (state.hasMoreRenewals) {
                            stringResource(R.string.home_see_all)
                        } else {
                            null
                        },
                        onAction = if (state.hasMoreRenewals) onSeeAllRenewals else null,
                    )
                }
                items(state.renewals, key = { it.recurring.id }) { row ->
                    RecurringRow(
                        name = row.recurring.name,
                        amount = row.recurring.amount,
                        relativeDate = row.relativeDate,
                        flags = row.flags,
                        onClick = onSeeAllRenewals,
                    )
                }
            }
            val insight = state.topInsight
            if (insight != null) {
                item {
                    SectionHeader(title = stringResource(R.string.home_insight))
                    InsightCard(
                        title = insight.title,
                        body = insight.body,
                        actionLabel = insight.actionLabel,
                        onAction = onInsightAction,
                        onDismiss = onDismissInsight,
                        yearlySaving = insight.insight.yearlySaving,
                        categoryKey = insight.categoryKey,
                    )
                }
            }
            item {
                SectionHeader(
                    title = stringResource(R.string.home_recent),
                    actionLabel = stringResource(R.string.home_see_all),
                    onAction = onOpenActivity,
                )
            }
            if (state.recent.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        title = stringResource(R.string.home_empty_title),
                        body = stringResource(R.string.home_empty_body),
                        categoryKey = "other",
                    )
                }
            } else {
                items(state.recent, key = { it.transaction.id }) { row ->
                    TransactionRow(
                        title = row.title,
                        amount = row.transaction.amount,
                        categoryKey = row.categoryKey,
                        meta = row.meta,
                        income = row.income,
                        onClick = onOpenActivity,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBlock(state: HomeUiState) {
    val spacing = MaterialTheme.spacing
    val hero = state.hero ?: return
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(
                when (hero.mode) {
                    HeroMode.AVAILABLE -> R.string.home_hero_available
                    HeroMode.SPENT_ONLY -> R.string.home_hero_spent
                },
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.tight))
        AmountText(
            amount = hero.amount,
            style = TamekiType.hero,
            dimMinorUnits = true,
            omitZeroMinorUnits = true,
        )
        if (hero.mode == HeroMode.SPENT_ONLY) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.home_set_budget),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val expected = state.expectedByToday
        if (expected != null) {
            Spacer(Modifier.height(spacing.group))
            PaceBar(
                spent = hero.spentThisMonth,
                expected = expected,
                budget = hero.budgetLimit,
            )
        }
        Spacer(Modifier.height(spacing.heroBottom))
    }
}

@Composable
private fun InboxCard(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(MaterialTheme.spacing.cardPadding)) {
            Text(
                text = stringResource(R.string.home_inbox_title, count),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.home_inbox_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onClick) {
                Text(stringResource(R.string.home_inbox_action))
            }
        }
    }
}

@Preview(name = "Today · light", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 411, heightDp = 891)
@Composable
private fun HomePreviewLight() {
    TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
        HomeScreen(
            state = sampleHomeState(),
            onOpenSettings = {},
            onOpenActivity = {},
            onOpenInbox = {},
            onSeeAllRenewals = {},
        )
    }
}

internal fun sampleHomeState(): HomeUiState {
    val eur = CurrencyCode("EUR")
    val today = LocalDate.of(2026, 11, 18)
    return HomeUiState(
        loading = false,
        baseCurrency = eur,
        hero = AvailableToSpend(
            amount = Money(42850, eur),
            mode = HeroMode.AVAILABLE,
            spentThisMonth = Money(51200, eur),
            upcomingRecurring = Money(7950, eur),
            budgetLimit = Money(102000, eur),
            incomeThisMonth = Money.zero(eur),
        ),
        expectedByToday = Money(61200, eur),
        renewals = listOf(
            HomeRenewal(
                recurring = Recurring(
                    id = 1,
                    type = RecurringType.SUBSCRIPTION,
                    name = "Netflix",
                    amount = Money(1349, eur),
                    cycle = Cycle(1, CycleUnit.MONTH),
                    startDate = today.minusMonths(3),
                    nextDate = today.plusDays(3),
                ),
                relativeDate = "tra 3 giorni",
                flags = setOf(RecurringFlag.WORTH_IT),
            ),
            HomeRenewal(
                recurring = Recurring(
                    id = 2,
                    type = RecurringType.BILL,
                    name = "TIM",
                    amount = Money(1490, eur),
                    cycle = Cycle(1, CycleUnit.MONTH),
                    startDate = today.minusMonths(10),
                    nextDate = today.plusDays(5),
                ),
                relativeDate = "tra 5 giorni",
                flags = emptySet(),
            ),
        ),
        hasMoreRenewals = true,
        topInsight = HomeInsight(
            insight = Insight.UnusedSubscription(3, Worth.RARELY, Money(11988, eur)),
            title = "Prime Video",
            body = "Raro uso",
            actionLabel = "Rivedi",
            categoryKey = "streaming",
        ),
        pendingInboxCount = 2,
        recent = listOf(
            HomeTransaction(
                transaction = Transaction(
                    id = 10,
                    kind = TransactionKind.EXPENSE,
                    amount = Money(4320, eur),
                    accountId = 1,
                    merchant = "Esselunga",
                    occurredAt = LocalDateTime.of(2026, 11, 18, 18, 42),
                ),
                title = "Esselunga",
                categoryKey = "groceries",
                meta = "Carta",
                income = false,
            ),
            HomeTransaction(
                transaction = Transaction(
                    id = 11,
                    kind = TransactionKind.EXPENSE,
                    amount = Money(150, eur),
                    accountId = 1,
                    merchant = "Bar Centrale",
                    occurredAt = LocalDateTime.of(2026, 11, 18, 8, 10),
                ),
                title = "Bar Centrale",
                categoryKey = "coffee",
                meta = null,
                income = false,
            ),
        ),
    )
}
