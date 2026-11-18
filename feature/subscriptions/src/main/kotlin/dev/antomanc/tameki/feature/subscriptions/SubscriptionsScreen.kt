package dev.antomanc.tameki.feature.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Worth
import dev.antomanc.tameki.core.ui.components.AmountText
import dev.antomanc.tameki.core.ui.components.EmptyState
import dev.antomanc.tameki.core.ui.components.RecurringFlag
import dev.antomanc.tameki.core.ui.components.RecurringRow
import dev.antomanc.tameki.core.ui.components.SectionHeader
import dev.antomanc.tameki.core.ui.format.rememberMoneyFormatter
import dev.antomanc.tameki.core.ui.theme.TamekiType
import dev.antomanc.tameki.core.ui.theme.spacing
import dev.antomanc.tameki.core.ui.theme.tamekiColors
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

/**
 * Entry point of the Recurring tab. The app owns navigation: this composable renders the list and
 * reports an opened row, the add action and the review action through callbacks.
 */
@Composable
fun SubscriptionsRoute(
    onOpenSubscription: (Long) -> Unit,
    onAddRecurring: () -> Unit,
    onReviewSubscriptions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionsViewModel = koinViewModel(),
    catalog: BrandCatalog = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SubscriptionsScreen(
        state = state,
        catalog = catalog,
        onTogglePeriod = viewModel::togglePeriod,
        onSelectDay = viewModel::selectDay,
        onTypeFilter = viewModel::setTypeFilter,
        onStatusFilter = viewModel::setStatusFilter,
        onOpenSubscription = onOpenSubscription,
        onAddRecurring = onAddRecurring,
        onReview = onReviewSubscriptions,
        modifier = modifier,
    )
}

/**
 * The Recurring tab, stateless: hero monthly (or yearly) cost, counts per type, the 30-day strip,
 * the two filter rows and the list sorted by next charge. Every action is a callback.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubscriptionsScreen(
    state: SubscriptionsUiState,
    catalog: BrandCatalog,
    onTogglePeriod: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onTypeFilter: (RecurringType?) -> Unit,
    onStatusFilter: (StatusFilter) -> Unit,
    onOpenSubscription: (Long) -> Unit,
    onAddRecurring: () -> Unit,
    onReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val formatter = rememberMoneyFormatter()
    val periodHint = stringResource(
        if (state.period == CostPeriod.MONTH) {
            R.string.subs_period_switch_yearly
        } else {
            R.string.subs_period_switch_monthly
        },
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.gutter,
            end = spacing.gutter,
            top = spacing.group,
            bottom = spacing.section,
        ),
    ) {
        item(key = "hero") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = state.period == CostPeriod.YEAR,
                        role = Role.Button,
                        onValueChange = { onTogglePeriod() },
                    ),
            ) {
                Text(
                    text = stringResource(
                        if (state.period == CostPeriod.MONTH) {
                            R.string.subs_monthly_cost
                        } else {
                            R.string.subs_yearly_cost
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AmountText(
                    amount = state.total,
                    style = TamekiType.hero,
                    dimMinorUnits = true,
                    omitZeroMinorUnits = true,
                    animateChanges = true,
                    contentDescription = "${formatter.format(state.total, omitZeroFraction = true)}. $periodHint",
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        R.string.subs_counts,
                        pluralStringResource(
                            R.plurals.subs_count_subscriptions,
                            state.subscriptionCount,
                            state.subscriptionCount,
                        ),
                        pluralStringResource(
                            R.plurals.subs_count_bills,
                            state.billCount,
                            state.billCount,
                        ),
                        pluralStringResource(
                            R.plurals.subs_count_income,
                            state.incomeCount,
                            state.incomeCount,
                        ),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.canReview) {
            item(key = "review") {
                TextButton(
                    onClick = onReview,
                    modifier = Modifier.padding(top = spacing.tight),
                ) {
                    Text(stringResource(R.string.subs_review))
                }
            }
        }

        item(key = "strip") {
            CalendarStrip(
                days = state.days,
                today = state.today,
                onSelectDay = onSelectDay,
                modifier = Modifier.padding(top = spacing.group),
            )
        }

        item(key = "filters") {
            Column(Modifier.padding(top = spacing.group)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TypeFilterChip(
                        label = stringResource(R.string.subs_filter_subscriptions),
                        selected = state.typeFilter == RecurringType.SUBSCRIPTION,
                        onClick = { onTypeFilter(RecurringType.SUBSCRIPTION) },
                    )
                    TypeFilterChip(
                        label = stringResource(R.string.subs_filter_bills),
                        selected = state.typeFilter == RecurringType.BILL,
                        onClick = { onTypeFilter(RecurringType.BILL) },
                    )
                    TypeFilterChip(
                        label = stringResource(R.string.subs_filter_income),
                        selected = state.typeFilter == RecurringType.INCOME,
                        onClick = { onTypeFilter(RecurringType.INCOME) },
                    )
                }
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusFilter.entries.forEach { status ->
                        TypeFilterChip(
                            label = stringResource(
                                when (status) {
                                    StatusFilter.ACTIVE -> R.string.subs_filter_active
                                    StatusFilter.PAUSED -> R.string.subs_filter_paused
                                    StatusFilter.CANCELLED -> R.string.subs_filter_cancelled
                                },
                            ),
                            selected = state.statusFilter == status,
                            onClick = { onStatusFilter(status) },
                        )
                    }
                }
            }
        }

        item(key = "upcoming") {
            SectionHeader(
                title = stringResource(R.string.subs_upcoming),
                actionLabel = stringResource(R.string.subs_add),
                onAction = onAddRecurring,
            )
        }

        if (state.rows.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = stringResource(
                        if (state.isEmpty) R.string.subs_empty_title else R.string.subs_empty_filtered,
                    ),
                    body = stringResource(
                        if (state.isEmpty) R.string.subs_empty_body else R.string.subs_empty_filtered_body,
                    ),
                    categoryKey = if (state.isEmpty) "streaming" else null,
                    actionLabel = if (state.isEmpty) stringResource(R.string.subs_empty_action) else null,
                    onAction = if (state.isEmpty) onAddRecurring else null,
                    modifier = Modifier.padding(top = spacing.group),
                )
            }
        } else {
            items(state.rows, key = { it.id }) { row ->
                RecurringListRow(
                    row = row,
                    today = state.today,
                    catalog = catalog,
                    onClick = { onOpenSubscription(row.id) },
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun TypeFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = CircleShape,
    )
}

/**
 * The next thirty days, one small cell each. A dot marks a renewal; today is outlined; a tapped
 * day wears the ink and filters the list under it. Cells read as one date sentence to TalkBack.
 */
@Composable
private fun CalendarStrip(
    days: List<StripDay>,
    today: LocalDate,
    onSelectDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = rememberLocale()
    val dateFormatter = rememberDateFormatter()
    val ink = MaterialTheme.colorScheme.onSurface
    val selectedFill = MaterialTheme.colorScheme.surfaceContainerHigh
    val renewalDays = days.count { it.hasRenewal }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(days, key = { it.date.toString() }) { day ->
            val weekday = day.date.dayOfWeek.getDisplayName(JavaTextStyle.NARROW, locale)
            val description = stringResource(
                if (day.hasRenewal) R.string.subs_day_renewal_a11y else R.string.subs_day_a11y,
                dateFormatter.format(day.date),
            )
            Column(
                modifier = Modifier
                    .widthIn(min = 40.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (day.isSelected) selectedFill else Color.Transparent)
                    .toggleable(
                        value = day.isSelected,
                        role = Role.Button,
                        onValueChange = { onSelectDay(day.date) },
                    )
                    .clearAndSetSemantics { contentDescription = description }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = weekday,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (day.isSelected || day.isToday) ink else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (day.hasRenewal) {
                                if (day.isSelected) ink else MaterialTheme.tamekiColors.marker
                            } else {
                                Color.Transparent
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun RecurringListRow(
    row: RecurringRowUi,
    today: LocalDate,
    catalog: BrandCatalog,
    onClick: () -> Unit,
) {
    val formatter = rememberMoneyFormatter()
    val brand = catalog.find(row.brandKey)
    val detail = if (row.cycle.unit != CycleUnit.MONTH || row.cycle.every != 1) {
        stringResource(
            R.string.subs_detail_cycle,
            formatter.formatWhole(row.yearly),
            cycleLabel(row.cycle),
        )
    } else {
        null
    }
    RecurringRow(
        name = row.name,
        amount = row.amount,
        relativeDate = relativeDateLabel(row.nextDate, today),
        flags = row.flags,
        detail = detail,
        brandColor = brand?.accent,
        onClick = onClick,
    )
}

@Preview(name = "Recurring · light", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 400)
@Composable
private fun SubscriptionsLightPreview() {
    SubscriptionsPreview(dark = false)
}

@Preview(name = "Recurring · dark", showBackground = true, backgroundColor = 0xFF141513, widthDp = 400)
@Composable
private fun SubscriptionsDarkPreview() {
    SubscriptionsPreview(dark = true)
}

@Composable
private fun SubscriptionsPreview(dark: Boolean) {
    dev.antomanc.tameki.core.ui.theme.TamekiTheme(darkTheme = dark, dynamicColor = false, motionEnabled = false) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            SubscriptionsScreen(
                state = sampleSubscriptionsState(),
                catalog = sampleCatalog,
                onTogglePeriod = {},
                onSelectDay = {},
                onTypeFilter = {},
                onStatusFilter = {},
                onOpenSubscription = {},
                onAddRecurring = {},
                onReview = {},
            )
        }
    }
}

/** The Italian sample the previews and screenshot tests share. */
internal val sampleCatalog: BrandCatalog = BrandCatalog(
    listOf(
        Brand("netflix", "Netflix", "#E50914", "streaming"),
        Brand("spotify", "Spotify", "#1DB954", "music"),
        Brand("dazn", "DAZN", "#0A0A0A", "sport", type = "SUBSCRIPTION"),
        Brand("iliad", "iliad", "#E2001A", "phone", type = "BILL"),
        Brand("enel", "Enel Energia", "#E4002B", "utilities", type = "BILL"),
        Brand("disney_plus", "Disney+", "#113CCF", "streaming"),
        Brand("mcfit", "McFit", "#FFD400", "sport"),
        Brand("tim", "TIM", "#0057B8", "phone", type = "BILL"),
    ),
)

internal fun sampleSubscriptionsState(today: LocalDate = LocalDate.of(2026, 11, 18)): SubscriptionsUiState {
    val eur = CurrencyCode("EUR")
    fun money(minor: Long) = Money(minor, eur)
    return SubscriptionsUiState(
        loading = false,
        currency = eur,
        today = today,
        period = CostPeriod.MONTH,
        totalMonthly = money(20374),
        totalYearly = money(244488),
        subscriptionCount = 6,
        billCount = 4,
        incomeCount = 1,
        days = (0 until 30).map { offset ->
            val date = today.plusDays(offset.toLong())
            StripDay(
                date = date,
                hasRenewal = offset in setOf(1, 3, 8, 12, 17, 24, 28),
                isToday = offset == 0,
                isSelected = false,
            )
        },
        statusFilter = StatusFilter.ACTIVE,
        rows = listOf(
            RecurringRowUi(1, "Netflix", "netflix", money(1399), money(16788), Cycle(1, CycleUnit.MONTH), today.plusDays(1), RecurringType.SUBSCRIPTION, RecurringStatus.ACTIVE, null, setOf(RecurringFlag.PRICE_UP, RecurringFlag.WORTH_IT)),
            RecurringRowUi(2, "Spotify", "spotify", money(1099), money(13188), Cycle(1, CycleUnit.MONTH), today.plusDays(3), RecurringType.SUBSCRIPTION, RecurringStatus.ACTIVE, null, setOf(RecurringFlag.WORTH_IT)),
            RecurringRowUi(3, "Disney+", "disney_plus", money(899), money(10788), Cycle(1, CycleUnit.MONTH), today.plusDays(3), RecurringType.SUBSCRIPTION, RecurringStatus.TRIAL, null, setOf(RecurringFlag.TRIAL_ENDING)),
            RecurringRowUi(4, "DAZN", "dazn", money(3499), money(41988), Cycle(1, CycleUnit.MONTH), today.plusDays(8), RecurringType.SUBSCRIPTION, RecurringStatus.ACTIVE, null, setOf(RecurringFlag.RARELY_USED)),
            RecurringRowUi(5, "iliad", "iliad", money(999), money(11988), Cycle(1, CycleUnit.MONTH), today.plusDays(12), RecurringType.BILL, RecurringStatus.ACTIVE, null, emptySet()),
            RecurringRowUi(6, "Enel Energia", "enel", money(7800), money(46800), Cycle(2, CycleUnit.MONTH), today.plusDays(17), RecurringType.BILL, RecurringStatus.ACTIVE, null, emptySet()),
            RecurringRowUi(7, "McFit", "mcfit", money(3900), money(46800), Cycle(1, CycleUnit.MONTH), today.plusDays(24), RecurringType.SUBSCRIPTION, RecurringStatus.ACTIVE, null, setOf(RecurringFlag.UNUSED)),
        ),
        canReview = true,
    )
}
