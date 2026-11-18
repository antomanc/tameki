package dev.antomanc.tameki.feature.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.ui.components.AmountText
import dev.antomanc.tameki.core.ui.components.BrandMark
import dev.antomanc.tameki.core.ui.components.EmptyState
import dev.antomanc.tameki.core.ui.format.rememberMoneyFormatter
import dev.antomanc.tameki.core.ui.theme.TamekiType
import dev.antomanc.tameki.core.ui.theme.spacing
import dev.antomanc.tameki.core.ui.theme.tamekiColors
import java.time.LocalDate
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

/** The "Review subscriptions" flow, one subscription at a time. */
@Composable
fun SubscriptionsReviewRoute(
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionsReviewViewModel = koinViewModel(),
    catalog: BrandCatalog = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.start() }
    SubscriptionsReviewScreen(
        state = state,
        catalog = catalog,
        onBack = onBack,
        onChoose = viewModel::choose,
        onDone = onDone,
        modifier = modifier,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsReviewScreen(
    state: SubscriptionsReviewUiState,
    catalog: BrandCatalog,
    onBack: () -> Unit,
    onChoose: (ReviewDecision) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.subs_review_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.subs_back))
                    }
                },
            )
        },
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            when {
                state.finished -> ReviewSummary(state = state, onDone = onDone)
                state.current == null -> EmptyState(
                    title = stringResource(R.string.subs_review_empty_title),
                    body = stringResource(R.string.subs_review_empty_body),
                    categoryKey = "other",
                    modifier = Modifier.padding(MaterialTheme.spacing.gutter),
                )
                else -> ReviewStep(
                    state = state,
                    catalog = catalog,
                    onChoose = onChoose,
                )
            }
        }
    }
}

@Composable
private fun ReviewStep(
    state: SubscriptionsReviewUiState,
    catalog: BrandCatalog,
    onChoose: (ReviewDecision) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val item = state.current ?: return
    val brand = catalog.find(item.brandKey)
    val formatter = rememberMoneyFormatter()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.gutter, vertical = spacing.group),
    ) {
        Text(
            text = stringResource(R.string.subs_review_progress, state.answered + 1, state.items.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth(),
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = maxHeight * 0.2f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BrandMark(name = item.name, size = 64.dp, color = brand?.accent)
                Spacer(Modifier.height(spacing.group))
                Text(
                    text = stringResource(R.string.subs_review_question, item.name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                AmountText(
                    amount = item.amount,
                    style = TamekiType.amount,
                    animateChanges = false,
                )
                Text(
                    text = stringResource(
                        R.string.subs_detail_cycle,
                        cycleLabel(item.cycle),
                        stringResource(R.string.subs_detail_year, formatter.formatWhole(item.yearly)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = relativeDateLabel(item.nextDate, state.today),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onChoose(ReviewDecision.KEEP) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.subs_review_keep))
            }
            FilledTonalButton(
                onClick = { onChoose(ReviewDecision.RARELY) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.subs_review_rarely))
            }
            OutlinedButton(
                onClick = { onChoose(ReviewDecision.CANCEL) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.subs_review_cancel))
            }
        }
        Spacer(Modifier.height(spacing.group))
    }
}

@Composable
private fun ReviewSummary(state: SubscriptionsReviewUiState, onDone: () -> Unit) {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.gutter, vertical = spacing.group),
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            Column {
                Text(
                    text = stringResource(R.string.subs_review_summary_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(spacing.group))
                Text(
                    text = stringResource(R.string.subs_review_saving),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AmountText(
                    amount = state.yearlySaving,
                    style = TamekiType.hero,
                    color = MaterialTheme.tamekiColors.leaf,
                    dimMinorUnits = true,
                    omitZeroMinorUnits = true,
                    animateChanges = false,
                )
                Spacer(Modifier.height(8.dp))
                if (state.cancelled == 0) {
                    Text(
                        text = stringResource(R.string.subs_review_summary_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = pluralStringResource(
                            R.plurals.subs_review_summary_cancelled,
                            state.cancelled,
                            state.cancelled,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = pluralStringResource(R.plurals.subs_review_summary_kept, state.kept, state.kept),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.rarely > 0) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.subs_review_rarely_summary,
                            state.rarely,
                            state.rarely,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.subs_review_finish))
        }
        Spacer(Modifier.height(spacing.group))
    }
}

/** In-progress sample: DAZN is the subscription on screen, two answers already given. */
internal fun sampleReviewState(today: LocalDate = LocalDate.of(2026, 11, 18)): SubscriptionsReviewUiState {
    val eur = CurrencyCode("EUR")
    fun money(minor: Long) = Money(minor, eur)
    val items = listOf(
        ReviewItem(1, "Netflix", "netflix", money(1399), money(16788), Cycle(1, CycleUnit.MONTH), today.plusDays(1)),
        ReviewItem(4, "DAZN", "dazn", money(3499), money(41988), Cycle(1, CycleUnit.MONTH), today.plusDays(8)),
        ReviewItem(2, "Spotify", "spotify", money(1099), money(13188), Cycle(1, CycleUnit.MONTH), today.plusDays(3)),
        ReviewItem(7, "McFit", "mcfit", money(3900), money(46800), Cycle(1, CycleUnit.MONTH), today.plusDays(24)),
    )
    return SubscriptionsReviewUiState(
        loading = false,
        items = items,
        kept = 1,
        rarely = 1,
        cancelled = 0,
        yearlySaving = Money.zero(eur),
        today = today,
        decidedIds = setOf(1L, 2L),
    )
}

/** Finished sample: two cancelled, one kept, one rarely used. */
internal fun sampleReviewSummary(today: LocalDate = LocalDate.of(2026, 11, 18)): SubscriptionsReviewUiState {
    val eur = CurrencyCode("EUR")
    fun money(minor: Long) = Money(minor, eur)
    val items = listOf(
        ReviewItem(1, "Netflix", "netflix", money(1399), money(16788), Cycle(1, CycleUnit.MONTH), today.plusDays(1)),
        ReviewItem(4, "DAZN", "dazn", money(3499), money(41988), Cycle(1, CycleUnit.MONTH), today.plusDays(8)),
        ReviewItem(2, "Spotify", "spotify", money(1099), money(13188), Cycle(1, CycleUnit.MONTH), today.plusDays(3)),
        ReviewItem(7, "McFit", "mcfit", money(3900), money(46800), Cycle(1, CycleUnit.MONTH), today.plusDays(24)),
    )
    return SubscriptionsReviewUiState(
        loading = false,
        items = items,
        kept = 1,
        rarely = 1,
        cancelled = 2,
        yearlySaving = money(58776),
        finished = true,
        today = today,
        decidedIds = items.map { it.id }.toSet(),
    )
}
