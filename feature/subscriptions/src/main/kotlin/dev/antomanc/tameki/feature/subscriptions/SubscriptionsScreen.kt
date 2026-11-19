package dev.antomanc.tameki.feature.subscriptions

import dev.antomanc.tameki.core.ui.components.DelayedLoading
import dev.antomanc.tameki.core.ui.brand.Brand
import dev.antomanc.tameki.core.ui.brand.BrandCatalog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import dev.antomanc.tameki.core.ui.components.FadingScrollRow
import dev.antomanc.tameki.core.ui.components.SelectableChip
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
        onClearFilters = viewModel::clearFilters,
        onOpenSubscription = onOpenSubscription,
        onAddRecurring = onAddRecurring,
        onReview = onReviewSubscriptions,
        modifier = modifier,
    )
}

/**
 * The Recurring tab, stateless: a top bar (status menu), the hero monthly (or yearly) cost, the
 * review button when something is unrated, the 30-day strip, one row of type chips and the list
 * sorted by next charge. Add lives on the empty state and the FAB long-press menu — one primary
 * action, not a second Add in the top bar. Status is a filter in the overflow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    state: SubscriptionsUiState,
    catalog: BrandCatalog,
    onTogglePeriod: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onTypeFilter: (RecurringType?) -> Unit,
    onStatusFilter: (StatusFilter) -> Unit,
    onClearFilters: () -> Unit,
    onOpenSubscription: (Long) -> Unit,
    onAddRecurring: () -> Unit,
    onReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val formatter = rememberMoneyFormatter(baseCurrency = state.currency)
    var statusMenu by remember { mutableStateOf(false) }
    val periodHint = stringResource(
        if (state.period == CostPeriod.MONTH) {
            R.string.subs_period_switch_yearly
        } else {
            R.string.subs_period_switch_monthly
        },
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        stringResource(R.string.subs_title),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { statusMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.subs_status_menu),
                            )
                        }
                        DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
                            StatusFilter.entries.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(statusLabel(status)) },
                                    onClick = {
                                        statusMenu = false
                                        onStatusFilter(status)
                                    },
                                    leadingIcon = if (state.statusFilter == status) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        // The database answers in a frame: a centred spinner for that frame is a flash, not
        // information. The list simply appears.
        if (state.loading) {
            DelayedLoading(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = spacing.gutter,
                end = spacing.gutter,
                top = spacing.group,
                bottom = spacing.section + 72.dp,
            ),
        ) {
            item(key = "hero") {
                Column(Modifier.fillMaxWidth()) {
                    // Two explicit segments, not a chevron: the chevron read as "open a menu",
                    // while this control only switches the unit of the same total.
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.widthIn(max = 240.dp),
                    ) {
                        SegmentedButton(
                            selected = state.period == CostPeriod.MONTH,
                            onClick = { if (state.period != CostPeriod.MONTH) onTogglePeriod() },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) {
                            Text(stringResource(R.string.subs_period_month))
                        }
                        SegmentedButton(
                            selected = state.period == CostPeriod.YEAR,
                            onClick = { if (state.period != CostPeriod.YEAR) onTogglePeriod() },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) {
                            Text(stringResource(R.string.subs_period_year))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    AmountText(
                        amount = state.total,
                        style = TamekiType.hero,
                        dimMinorUnits = true,
                        omitZeroMinorUnits = true,
                        animateChanges = true,
                        contentDescription = "${formatter.format(state.total, omitZeroFraction = true)}. $periodHint",
                    )
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
                    val countLabels = buildList {
                        if (state.subscriptionCount > 0) {
                            add(
                                pluralStringResource(
                                    R.plurals.subs_count_subscriptions,
                                    state.subscriptionCount,
                                    state.subscriptionCount,
                                ),
                            )
                        }
                        if (state.billCount > 0) {
                            add(
                                pluralStringResource(
                                    R.plurals.subs_count_bills,
                                    state.billCount,
                                    state.billCount,
                                ),
                            )
                        }
                        if (state.incomeCount > 0) {
                            add(
                                pluralStringResource(
                                    R.plurals.subs_count_income,
                                    state.incomeCount,
                                    state.incomeCount,
                                ),
                            )
                        }
                    }
                    joinCountLabels(countLabels)?.let { counts ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = counts,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (state.canReview) {
                item(key = "review") {
                    FilledTonalButton(
                        onClick = onReview,
                        modifier = Modifier.padding(top = spacing.group),
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
                // A scrolling row, not a wrapping one: at 200% text four chips do not fit a
                // 360 dp screen, and clipping the last one hid "Income" entirely.
                FadingScrollRow(
                    modifier = Modifier.fillMaxWidth().padding(top = spacing.group),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.statusFilter != StatusFilter.ACTIVE) {
                        InputChip(
                            selected = true,
                            onClick = { onStatusFilter(StatusFilter.ACTIVE) },
                            label = { Text(statusLabel(state.statusFilter)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.subs_status_reset),
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            shape = CircleShape,
                        )
                    }
                    // "All" is a real chip, not the absence of one: null already means all, and a
                    // row where nothing looks selected reads as a filter that failed to apply.
                    TypeFilterChip(
                        label = stringResource(R.string.subs_filter_all),
                        selected = state.typeFilter == null,
                        onClick = { onTypeFilter(null) },
                    )
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
            }

            item(key = "upcoming") {
                SectionHeader(title = stringResource(R.string.subs_upcoming))
            }

            if (state.rows.isEmpty()) {
                item(key = "empty") {
                    // Switching to Paused, or tapping a day with no renewals, must not strand the
                    // user on a list with no way back: the filtered state carries its own reset.
                    EmptyState(
                        modifier = Modifier.animateItem().padding(top = spacing.group),
                        title = stringResource(
                            if (state.isEmpty) R.string.subs_empty_title else R.string.subs_empty_filtered,
                        ),
                        body = if (state.isEmpty) stringResource(R.string.subs_empty_body) else null,
                        categoryKey = if (state.isEmpty) "streaming" else null,
                        actionLabel = if (state.isEmpty) {
                            stringResource(R.string.subs_empty_action)
                        } else {
                            stringResource(R.string.subs_clear_filters)
                        },
                        onAction = if (state.isEmpty) onAddRecurring else onClearFilters,
                    )
                }
            } else {
                // Filters and status changes move rows instead of making them jump.
                itemsIndexed(state.rows, key = { _, row -> row.id }) { index, row ->
                    Column(Modifier.animateItem()) {
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                        RecurringListRow(
                            row = row,
                            today = state.today,
                            catalog = catalog,
                            onClick = { onOpenSubscription(row.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun statusLabel(status: StatusFilter): String = stringResource(
    when (status) {
        StatusFilter.ACTIVE -> R.string.subs_filter_active
        StatusFilter.PAUSED -> R.string.subs_filter_paused
        StatusFilter.CANCELLED -> R.string.subs_filter_cancelled
    },
)

@Composable
private fun TypeFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    SelectableChip(selected = selected, onClick = onClick, label = label)
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
    // A selected day is an ink pill, not another shade of paper: surfaceContainerHigh is ~1.1:1
    // against the background, which made "selected" indistinguishable from a decorative tint.
    val selectedFill = MaterialTheme.colorScheme.inverseSurface
    val selectedInk = MaterialTheme.colorScheme.inverseOnSurface
    val renewalDays = days.count { it.hasRenewal }

    FadingScrollRow(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        fadeColor = MaterialTheme.colorScheme.background,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        days.forEach { day ->
            val weekday = day.date.dayOfWeek.getDisplayName(JavaTextStyle.NARROW, locale)
            val dateLabel = dateFormatter.format(day.date)
            val description = stringResource(
                if (day.hasRenewal) R.string.subs_day_renewal_a11y else R.string.subs_day_a11y,
                dateLabel,
            )
            Column(
                modifier = Modifier
                    .widthIn(min = 48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (day.isSelected) selectedFill else Color.Transparent)
                    .then(
                        // Today keeps a visible outline even when another day is selected: the
                        // strip always says where "now" is.
                        if (day.isToday) {
                            Modifier.border(
                                1.dp,
                                if (day.isSelected) selectedInk else ink,
                                MaterialTheme.shapes.medium,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .toggleable(
                        value = day.isSelected,
                        role = Role.Button,
                        onValueChange = { onSelectDay(day.date) },
                    )
                    // Merge, never clear: the description replaces the weekday/number text but the
                    // toggle action, role and selected state must survive for TalkBack.
                    .semantics(mergeDescendants = true) { contentDescription = description }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = weekday,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.isSelected) selectedInk else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = when {
                        day.isSelected -> selectedInk
                        day.isToday -> ink
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (day.hasRenewal) {
                                if (day.isSelected) selectedInk else MaterialTheme.tamekiColors.marker
                            } else {
                                Color.Transparent
                            },
                        )
                        .then(
                            if (day.hasRenewal) {
                                // The cell's own contentDescription already says "…, renewal";
                                // the dot is decorative so TalkBack never reads it twice.
                                Modifier.clearAndSetSemantics { }
                            } else {
                                Modifier
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
        income = row.type == RecurringType.INCOME,
        converted = row.converted,
        // A paused or cancelled plan has no next charge: printing "in 3 days" would invent one.
        relativeDate = when (row.status) {
            RecurringStatus.PAUSED -> stringResource(R.string.subs_state_paused)
            RecurringStatus.CANCELLED -> stringResource(R.string.subs_state_cancelled)
            else -> relativeDateLabel(row.nextDate, today)
        },
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
                onClearFilters = {},
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
            RecurringRowUi(1, "Netflix", "netflix", money(1399), money(16788), Cycle(1, CycleUnit.MONTH), today.plusDays(1), RecurringType.SUBSCRIPTION, RecurringStatus.ACTIVE, null, setOf(RecurringFlag.PRICE_UP)),
            RecurringRowUi(2, "Spotify", "spotify", money(1099), money(13188), Cycle(1, CycleUnit.MONTH), today.plusDays(3), RecurringType.SUBSCRIPTION, RecurringStatus.ACTIVE, null, emptySet()),
            RecurringRowUi(3, "Disney+", "disney_plus", money(899), money(10788), Cycle(1, CycleUnit.MONTH), today.plusDays(3), RecurringType.SUBSCRIPTION, RecurringStatus.TRIAL, null, setOf(RecurringFlag.TRIAL_ENDING)),
            RecurringRowUi(4, "DAZN", "dazn", money(3499), money(41988), Cycle(1, CycleUnit.MONTH), today.plusDays(8), RecurringType.SUBSCRIPTION, RecurringStatus.ACTIVE, null, setOf(RecurringFlag.RARELY_USED)),
            RecurringRowUi(5, "iliad", "iliad", money(999), money(11988), Cycle(1, CycleUnit.MONTH), today.plusDays(12), RecurringType.BILL, RecurringStatus.ACTIVE, null, emptySet()),
            RecurringRowUi(6, "Enel Energia", "enel", money(7800), money(46800), Cycle(2, CycleUnit.MONTH), today.plusDays(17), RecurringType.BILL, RecurringStatus.ACTIVE, null, emptySet()),
            RecurringRowUi(7, "McFit", "mcfit", money(3900), money(46800), Cycle(1, CycleUnit.MONTH), today.plusDays(24), RecurringType.SUBSCRIPTION, RecurringStatus.ACTIVE, null, setOf(RecurringFlag.UNUSED)),
        ),
        canReview = true,
    )
}

/** A non-default status filter: the dismissible chip is the only extra control on the filter row. */
internal fun sampleSubscriptionsStatePaused(today: LocalDate = LocalDate.of(2026, 11, 18)): SubscriptionsUiState {
    val eur = CurrencyCode("EUR")
    fun money(minor: Long) = Money(minor, eur)
    return sampleSubscriptionsState(today).copy(
        statusFilter = StatusFilter.PAUSED,
        rows = listOf(
            RecurringRowUi(
                8,
                "Prime Video",
                null,
                money(499),
                money(5988),
                Cycle(1, CycleUnit.MONTH),
                today.plusDays(9),
                RecurringType.SUBSCRIPTION,
                RecurringStatus.PAUSED,
                null,
                emptySet(),
            ),
        ),
    )
}
