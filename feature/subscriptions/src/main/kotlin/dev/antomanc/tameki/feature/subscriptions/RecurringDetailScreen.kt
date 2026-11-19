package dev.antomanc.tameki.feature.subscriptions

import android.content.Intent
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Worth
import dev.antomanc.tameki.core.ui.components.AmountText
import dev.antomanc.tameki.core.ui.components.BrandMark
import dev.antomanc.tameki.core.ui.components.SectionHeader
import dev.antomanc.tameki.core.ui.format.rememberMoneyFormatter
import dev.antomanc.tameki.core.ui.theme.TamekiType
import dev.antomanc.tameki.core.ui.theme.spacing
import dev.antomanc.tameki.core.ui.theme.tamekiColors
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

/** Detail of one recurring payment. [onOpenUrl] lets the app open the cancel page in a browser. */
@Composable
fun SubscriptionDetailRoute(
    id: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringDetailViewModel = koinViewModel(),
    catalog: BrandCatalog = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(id) { viewModel.prepare(id) }
    androidx.compose.runtime.LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }
    RecurringDetailScreen(
        state = state,
        catalog = catalog,
        onBack = onBack,
        onEdit = { onEdit(id) },
        onRate = viewModel::rate,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onCancel = {
            state.cancelUrl?.let(onOpenUrl)
            viewModel.cancel()
        },
        onDelete = viewModel::delete,
        modifier = modifier,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecurringDetailScreen(
    state: RecurringDetailUiState,
    catalog: BrandCatalog,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onRate: (Worth) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val formatter = rememberMoneyFormatter()
    val dateFormatter = rememberDateFormatter()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val brand = catalog.find(state.brandKey)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = state.name,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.subs_back))
                    }
                },
                actions = {
                    if (state.found) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.subs_detail_menu),
                                )
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                if (state.isPaused) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.subs_resume)) },
                                        onClick = {
                                            menuOpen = false
                                            onResume()
                                        },
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.subs_pause)) },
                                        onClick = {
                                            menuOpen = false
                                            onPause()
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.subs_delete)) },
                                    onClick = {
                                        menuOpen = false
                                        confirmDelete = true
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { inner ->
        if (state.loading) {
            Box(
                Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (!state.found) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(spacing.gutter),
            ) {
                Text(
                    text = stringResource(R.string.subs_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(inner).fillMaxSize(),
            contentPadding = PaddingValues(
                start = spacing.gutter,
                end = spacing.gutter,
                top = spacing.group,
                bottom = spacing.section,
            ),
        ) {
            item(key = "identity") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandMark(name = state.name, size = 56.dp, color = brand?.accent)
                    Spacer(Modifier.width(spacing.group))
                    Column {
                        Text(
                            text = stringResource(
                                R.string.subs_detail_cycle,
                                typeLabel(state.type),
                                statusLabel(state.status),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (state.trialEndsOn != null && !state.isCancelled) {
                            Text(
                                text = stringResource(
                                    R.string.subs_trial_ends_on,
                                    dateFormatter.format(state.trialEndsOn),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.tamekiColors.amberInk,
                            )
                        }
                    }
                }
            }

            item(key = "cost") {
                Column(Modifier.padding(top = spacing.group)) {
                    Text(
                        text = stringResource(
                            if (state.cycle.unit == CycleUnit.YEAR && state.cycle.every == 1) {
                                R.string.subs_cost_year
                            } else {
                                R.string.subs_cost_month
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AmountText(
                        amount = if (state.cycle.unit == CycleUnit.YEAR && state.cycle.every == 1) state.yearly else state.monthly,
                        style = TamekiType.heroCompact,
                        dimMinorUnits = true,
                        omitZeroMinorUnits = true,
                        animateChanges = false,
                    )
                    Text(
                        text = stringResource(R.string.subs_detail_year, formatter.formatWhole(state.yearly)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.sharedWith > 1) {
                        Text(
                            text = stringResource(
                                R.string.subs_your_share,
                                formatter.format(state.yourMonthly, omitZeroFraction = true),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (state.priceHistory.size >= 2) {
                item(key = "prices") {
                    SectionHeader(title = stringResource(R.string.subs_price_history))
                    PriceHistoryChart(points = state.priceHistory)
                }
            }

            item(key = "next") {
                SectionHeader(title = stringResource(R.string.subs_next_charges))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.nextDates.forEach { date ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = dateFormatter.format(date),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = relativeDateLabel(date, state.today),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item(key = "worth") {
                SectionHeader(title = stringResource(R.string.subs_worth))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Worth.entries.forEach { worth ->
                        FilterChip(
                            selected = state.worth == worth,
                            onClick = { onRate(worth) },
                            label = { Text(worthLabel(worth)) },
                            shape = CircleShape,
                        )
                    }
                }
            }

            if (!state.isCancelled) {
                item(key = "actions") {
                    Row(
                        modifier = Modifier.padding(top = spacing.group),
                        horizontalArrangement = Arrangement.spacedBy(spacing.tight),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledTonalButton(onClick = onEdit) {
                            Text(stringResource(R.string.subs_edit))
                        }
                        OutlinedButton(onClick = onCancel) {
                            Text(stringResource(R.string.subs_cancel_subscription))
                        }
                    }
                }
            }

            item(key = "facts") {
                Column(Modifier.padding(top = spacing.group)) {
                    if (!state.note.isNullOrBlank()) {
                        Text(
                            text = state.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val facts = listOfNotNull(state.categoryName, state.accountName)
                    if (facts.isNotEmpty()) {
                        Text(
                            text = facts.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.subs_delete_title, state.name)) },
            text = { Text(stringResource(R.string.subs_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                ) {
                    Text(stringResource(R.string.subs_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.subs_dialog_cancel))
                }
            },
        )
    }
}

/**
 * The price over time as a quiet step chart: dots in ink, a hairline joining them, the first and
 * last price written underneath. No axis, no grid — the numbers are the axis.
 */
@Composable
private fun PriceHistoryChart(points: List<PricePointUi>, modifier: Modifier = Modifier) {
    if (points.size < 2) return
    val formatter = rememberMoneyFormatter()
    val dateFormatter = rememberDateFormatter()
    val ink = MaterialTheme.colorScheme.onSurface
    val line = MaterialTheme.colorScheme.outline
    val least = points.minOf { it.amount.minor }
    val most = points.maxOf { it.amount.minor }
    val span = (most - least).coerceAtLeast(1L)
    val description = stringResource(
        R.string.subs_price_history,
    ) + ": " + points.joinToString("; ") {
        "${dateFormatter.format(it.since)} ${formatter.format(it.amount)}"
    }

    Column(modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clearAndSetSemantics { contentDescription = description },
        ) {
            val inset = 10.dp.toPx()
            val usableWidth = (size.width - inset * 2).coerceAtLeast(1f)
            val usableHeight = (size.height - inset * 2).coerceAtLeast(1f)
            val step = if (points.size > 1) usableWidth / (points.size - 1) else 0f
            val offsets = points.mapIndexed { index, point ->
                val fraction = (point.amount.minor - least).toFloat() / span.toFloat()
                Offset(
                    x = inset + step * index,
                    y = size.height - inset - usableHeight * fraction,
                )
            }
            if (offsets.size > 1) {
                val path = Path().apply {
                    moveTo(offsets.first().x, offsets.first().y)
                    offsets.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, color = line, style = Stroke(width = 1.5.dp.toPx()))
            }
            offsets.forEach { offset ->
                drawCircle(color = ink, radius = 3.5.dp.toPx(), center = offset)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatter.format(points.first().amount),
                style = TamekiType.amountTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatter.format(points.last().amount),
                style = TamekiType.amountTiny,
                color = if (points.last().amount.minor > points.first().amount.minor) {
                    MaterialTheme.tamekiColors.amberInk
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        Text(
            text = "${dateFormatter.format(points.first().since)} – ${dateFormatter.format(points.last().since)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )
    }
}

/** Kept for the preview and the screenshot test: the same sample detail the list links to. */
internal fun sampleDetailState(today: java.time.LocalDate = java.time.LocalDate.of(2026, 11, 18)): RecurringDetailUiState {
    val eur = dev.antomanc.tameki.core.model.CurrencyCode("EUR")
    fun money(minor: Long): Money = Money(minor, eur)
    return RecurringDetailUiState(
        loading = false,
        found = true,
        id = 1,
        name = "Netflix",
        brandKey = "netflix",
        type = RecurringType.SUBSCRIPTION,
        status = RecurringStatus.ACTIVE,
        amount = money(1399),
        monthly = money(1399),
        yearly = money(16788),
        yourMonthly = money(1399),
        yourYearly = money(16788),
        priceHistory = listOf(
            PricePointUi(money(1099), today.minusMonths(20)),
            PricePointUi(money(1299), today.minusMonths(9)),
            PricePointUi(money(1399), today.minusMonths(2)),
        ),
        nextDates = listOf(today.plusDays(1), today.plusMonths(1).plusDays(1), today.plusMonths(2).plusDays(1)),
        worth = Worth.LOVE,
        categoryName = "Streaming",
        accountName = "Carta",
        today = today,
    )
}
