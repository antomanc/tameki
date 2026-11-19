package dev.antomanc.tameki.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.budget.AvailableToSpend
import dev.antomanc.tameki.core.model.budget.HeroMode
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
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
import dev.antomanc.tameki.core.ui.components.FadingScrollRow
import dev.antomanc.tameki.core.ui.components.InsightCard
import dev.antomanc.tameki.core.ui.components.PaceBar
import dev.antomanc.tameki.core.ui.components.RecurringRow
import dev.antomanc.tameki.core.ui.components.SectionHeader
import dev.antomanc.tameki.core.ui.components.TransactionRow
import dev.antomanc.tameki.core.ui.format.rememberMoneyFormatter
import dev.antomanc.tameki.core.ui.seed.displayName
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
    onOpenAccount: (Long) -> Unit = { onOpenActivity() },
    onOpenBudget: () -> Unit = {},
    onOpenTransaction: (Long) -> Unit = {},
    onOpenSubscription: (Long) -> Unit = { _ -> onOpenSubscriptions() },
    onAddTransaction: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onOpenSettings = onOpenSettings,
        onOpenActivity = onOpenActivity,
        onOpenAccount = onOpenAccount,
        onOpenInbox = onOpenInbox,
        onSeeAllRenewals = onOpenSubscriptions,
        onDismissInsight = viewModel::dismissInsight,
        onInsightAction = {
            when (val insight = state.topInsight?.insight) {
                is Insight.UnusedSubscription -> onOpenSubscription(insight.recurringId)
                is Insight.OverlappingSubscriptions -> onOpenSubscriptions()
                is Insight.AnnualPlanSaving -> onOpenSubscription(insight.recurringId)
                is Insight.PriceIncrease -> onOpenSubscription(insight.recurringId)
                is Insight.TrialEnding -> onOpenSubscription(insight.recurringId)
                is Insight.CategorySpike -> onOpenActivity()
                is Insight.SmallFrequentSpend -> onOpenActivity()
                is Insight.BudgetAtRisk -> onOpenBudget()
                is Insight.NoSpendStreak -> viewModel.dismissInsight()
                null -> Unit
            }
        },
        onOpenBudget = onOpenBudget,
        onOpenTransaction = onOpenTransaction,
        onOpenSubscription = onOpenSubscription,
        onAddTransaction = onAddTransaction,
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
    onOpenAccount: (Long) -> Unit = { onOpenActivity() },
    onDismissInsight: () -> Unit = {},
    onInsightAction: () -> Unit = onSeeAllRenewals,
    onOpenBudget: () -> Unit = {},
    onOpenTransaction: (Long) -> Unit = {},
    onOpenSubscription: (Long) -> Unit = { onSeeAllRenewals() },
    onAddTransaction: () -> Unit = {},
) {
    val spacing = MaterialTheme.spacing
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        stringResource(R.string.home_title),
                        modifier = Modifier.semantics { heading() },
                    )
                },
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val twoColumn = maxWidth >= 840.dp
            if (twoColumn) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.gutter),
                    horizontalArrangement = Arrangement.spacedBy(spacing.section),
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(
                            top = spacing.group,
                            bottom = spacing.section + 72.dp,
                        ),
                    ) {
                        homeSummaryItems(
                            state = state,
                            onOpenBudget = onOpenBudget,
                            onOpenAccount = onOpenAccount,
                            onOpenInbox = onOpenInbox,
                            onDismissInsight = onDismissInsight,
                            onInsightAction = onInsightAction,
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(
                            top = spacing.group,
                            bottom = spacing.section + 72.dp,
                        ),
                    ) {
                        homeActivityItems(
                            state = state,
                            onSeeAllRenewals = onSeeAllRenewals,
                            onOpenSubscription = onOpenSubscription,
                            onOpenActivity = onOpenActivity,
                            onOpenTransaction = onOpenTransaction,
                            onAddTransaction = onAddTransaction,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = spacing.gutter,
                        end = spacing.gutter,
                        top = spacing.group,
                        bottom = spacing.section + 72.dp,
                    ),
                ) {
                    homeSummaryItems(
                        state = state,
                        onOpenBudget = onOpenBudget,
                        onOpenAccount = onOpenAccount,
                        onOpenInbox = onOpenInbox,
                        onDismissInsight = onDismissInsight,
                        onInsightAction = onInsightAction,
                    )
                    homeActivityItems(
                        state = state,
                        onSeeAllRenewals = onSeeAllRenewals,
                        onOpenSubscription = onOpenSubscription,
                        onOpenActivity = onOpenActivity,
                        onOpenTransaction = onOpenTransaction,
                        onAddTransaction = onAddTransaction,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.homeSummaryItems(
    state: HomeUiState,
    onOpenBudget: () -> Unit,
    onOpenAccount: (Long) -> Unit,
    onOpenInbox: () -> Unit,
    onDismissInsight: () -> Unit,
    onInsightAction: () -> Unit,
) {
    item(key = "hero") {
        AnimatedVisibility(
            visible = state.hero != null,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
        ) {
            HeroBlock(state = state, onOpenBudget = onOpenBudget)
        }
    }
    if (state.accountBalances.isNotEmpty()) {
        item(key = "accounts") {
            AccountsBalanceStrip(
                balances = state.accountBalances,
                onOpenAccount = onOpenAccount,
                modifier = Modifier.animateItem(),
            )
        }
    }
    if (state.pendingInboxCount > 0) {
        item(key = "inbox") {
            InboxCard(
                count = state.pendingInboxCount,
                onClick = onOpenInbox,
                modifier = Modifier.animateItem().padding(top = MaterialTheme.spacing.group),
            )
        }
    }
    val insight = state.topInsight
    if (insight != null) {
        item(key = "insight") {
            Column(Modifier.animateItem()) {
                SectionHeader(
                    title = stringResource(R.string.home_insight),
                    supporting = stringResource(R.string.home_insight_support),
                )
                AnimatedContent(
                    targetState = insight,
                    contentKey = { it.insight.id },
                    transitionSpec = {
                        (fadeIn(tween(220, delayMillis = 60)) + slideInHorizontally { it / 8 })
                            .togetherWith(fadeOut(tween(120)) + slideOutHorizontally { -it / 8 })
                            .using(SizeTransform(clip = false))
                    },
                    label = "home-insight",
                ) { shown ->
                    val copy = shown.localizedCopy()
                    InsightCard(
                        title = copy.title,
                        body = copy.body,
                        actionLabel = copy.actionLabel,
                        onAction = onInsightAction,
                        onDismiss = onDismissInsight,
                        yearlySaving = shown.insight.yearlySaving,
                        categoryKey = shown.categoryKey,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.homeActivityItems(
    state: HomeUiState,
    onSeeAllRenewals: () -> Unit,
    onOpenSubscription: (Long) -> Unit,
    onOpenActivity: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onAddTransaction: () -> Unit,
) {
    if (state.renewals.isNotEmpty()) {
        item(key = "renewals-header") {
            SectionHeader(
                modifier = Modifier.animateItem(),
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
        items(state.renewals, key = { "r-${it.recurring.id}" }) { row ->
            RecurringRow(
                modifier = Modifier.animateItem(),
                name = row.recurring.name,
                amount = row.recurring.amount,
                relativeDate = row.relativeDate,
                flags = row.flags,
                income = row.recurring.type == RecurringType.INCOME,
                converted = row.converted,
                brandColor = row.brandColor,
                onClick = { onOpenSubscription(row.recurring.id) },
            )
        }
    }
    if (state.recent.isEmpty() && !state.loading) {
        item(key = "empty") {
            EmptyState(
                modifier = Modifier.animateItem(),
                title = stringResource(R.string.home_empty_title),
                body = stringResource(R.string.home_empty_body),
                categoryKey = "other",
                actionLabel = stringResource(R.string.home_add_movement),
                onAction = onAddTransaction,
            )
        }
    } else if (state.recent.isNotEmpty()) {
        item(key = "recent-header") {
            SectionHeader(
                modifier = Modifier.animateItem(),
                title = stringResource(R.string.home_recent),
                actionLabel = stringResource(R.string.home_see_all),
                onAction = onOpenActivity,
            )
        }
        items(state.recent, key = { "t-${it.transaction.id}" }) { row ->
            TransactionRow(
                modifier = Modifier.animateItem(),
                title = row.title,
                amount = row.transaction.amount,
                categoryKey = row.categoryKey,
                converted = row.converted,
                meta = row.meta,
                income = row.income,
                onClick = { onOpenTransaction(row.transaction.id) },
            )
        }
    }
}

@Composable
private fun AccountsBalanceStrip(
    balances: List<HomeAccountBalance>,
    onOpenAccount: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.home_accounts),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = spacing.tight),
        )
        FadingScrollRow(
            modifier = Modifier.fillMaxWidth(),
            fadeColor = MaterialTheme.colorScheme.background,
            horizontalArrangement = Arrangement.spacedBy(spacing.between),
        ) {
            balances.forEach { item ->
                AccountBalanceCard(
                    item = item,
                    onClick = { onOpenAccount(item.account.id) },
                )
            }
        }
    }
}

@Composable
private fun AccountBalanceCard(
    item: HomeAccountBalance,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = Modifier
            .widthIn(min = 148.dp, max = 220.dp)
            .testTag("home_account_card_${item.account.id}"),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = accountTypeIcon(item.account.type),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = item.account.displayName(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AmountText(
                amount = item.balance,
                style = MaterialTheme.typography.titleMedium,
                color = if (item.balance.minor < 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                dimMinorUnits = true,
                omitZeroMinorUnits = true,
            )
        }
    }
}

private fun accountTypeIcon(type: AccountType) = when (type) {
    AccountType.CASH -> Icons.Outlined.AccountBalanceWallet
    AccountType.CARD -> Icons.Outlined.CreditCard
    AccountType.BANK -> Icons.Outlined.AccountBalance
    AccountType.SAVINGS -> Icons.Outlined.Savings
    AccountType.OTHER -> Icons.Outlined.AccountBalanceWallet
}

@Composable
private fun HeroBlock(state: HomeUiState, onOpenBudget: () -> Unit) {
    val spacing = MaterialTheme.spacing
    val hero = state.hero ?: return
    val formatter = rememberMoneyFormatter(baseCurrency = state.baseCurrency)
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
        val reference = hero.budgetLimit ?: hero.incomeThisMonth.takeIf { !it.isZero }
        if (reference != null && hero.mode == HeroMode.AVAILABLE && !reference.isZero) {
            Text(
                text = stringResource(
                    if (hero.budgetLimit != null) {
                        R.string.home_hero_of_budget
                    } else {
                        R.string.home_hero_of_income
                    },
                    formatter.formatWhole(reference),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (hero.mode == HeroMode.AVAILABLE && (!hero.spentThisMonth.isZero || !hero.upcomingRecurring.isZero)) {
            Text(
                text = stringResource(
                    R.string.home_hero_breakdown,
                    formatter.format(hero.spentThisMonth, omitZeroFraction = true),
                    formatter.format(hero.upcomingRecurring, omitZeroFraction = true),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        state.unconverted.forEach { amount ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    dev.antomanc.tameki.core.ui.R.string.ui_unconverted_amount,
                    formatter.format(amount),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (hero.mode == HeroMode.SPENT_ONLY) {
            Spacer(Modifier.height(spacing.group))
            FilledTonalButton(onClick = onOpenBudget) {
                Text(stringResource(R.string.home_set_budget))
            }
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
        Spacer(Modifier.height(spacing.group))
    }
}

@Composable
private fun InboxCard(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().testTag("home_inbox_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.cardPadding, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = pluralStringResource(R.plurals.home_inbox_pending, count, count),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        accountBalances = listOf(
            HomeAccountBalance(
                account = Account(id = 1, name = "Card", currency = eur, type = AccountType.CARD, seedKey = "card"),
                balance = Money(1_420_50, eur),
            ),
            HomeAccountBalance(
                account = Account(id = 2, name = "Cash", currency = eur, type = AccountType.CASH, seedKey = "cash"),
                balance = Money(185_00, eur),
            ),
        ),
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
                flags = emptySet(),
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
            kind = HomeInsightKind.Unused("Prime Video", Worth.RARELY),
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
