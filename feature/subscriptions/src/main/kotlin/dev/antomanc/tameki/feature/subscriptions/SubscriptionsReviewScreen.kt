package dev.antomanc.tameki.feature.subscriptions

import dev.antomanc.tameki.core.ui.brand.BrandCatalog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.LoadingIndicator
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import dev.antomanc.tameki.core.ui.components.OneShotSnackbar
import org.koin.androidx.compose.koinViewModel

/** The "Review subscriptions" flow, one subscription at a time. */
@Composable
fun SubscriptionsReviewRoute(
    onDone: () -> Unit,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit = {},
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
        onChoose = { decision, item ->
            // Cancelling with a known page opens the provider's own flow first. The status is
            // still marked cancelled, and the summary calls the amount a possible saving.
            if (decision == ReviewDecision.CANCEL) {
                item.cancelUrl?.let(onOpenUrl)
            }
            viewModel.choose(decision, item.id)
        },
        onUndoCancel = viewModel::undoCancel,
        onAcknowledgeCancel = viewModel::acknowledgeCancelUndo,
        onDone = onDone,
        modifier = modifier,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SubscriptionsReviewScreen(
    state: SubscriptionsReviewUiState,
    catalog: BrandCatalog,
    onBack: () -> Unit,
    onChoose: (ReviewDecision, ReviewItem) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onUndoCancel: (Long) -> Unit = {},
    onAcknowledgeCancel: (Long) -> Unit = {},
) {
    val snackbar = remember { SnackbarHostState() }
    OneShotSnackbar(
        token = state.undoToken,
        host = snackbar,
        message = stringResource(R.string.subs_review_cancelled),
        actionLabel = stringResource(R.string.subs_review_undo),
        onAction = onUndoCancel,
        onResolved = onAcknowledgeCancel,
    )
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Text(
                        stringResource(R.string.subs_review_title),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.subs_back))
                    }
                },
            )
        },
    ) { inner ->
        // Each answer slides the next subscription in from the end; an Undo slides the restored
        // one back from the start. The key is the phase plus the item on screen.
        val slide = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
        val fadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        AnimatedContent(
            targetState = state.phase to state.current?.id,
            modifier = Modifier.padding(inner).fillMaxSize(),
            transitionSpec = {
                val order = state.items.map { it.id }
                val back = (initialState.first == ReviewPhase.Summary && targetState.first == ReviewPhase.Asking) ||
                    (order.indexOf(targetState.second) in 0 until order.indexOf(initialState.second))
                (slideInHorizontally(slide) { w -> if (back) -w / 3 else w / 3 } + fadeIn(fadeSpec))
                    .togetherWith(slideOutHorizontally(slide) { w -> if (back) w / 3 else -w / 3 } + fadeOut(fadeSpec))
            },
            label = "review-step",
        ) { (phase, _) ->
          Box(Modifier.fillMaxSize()) {
            when {
                phase == ReviewPhase.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
                phase == ReviewPhase.Summary -> ReviewSummary(state = state, onDone = onDone)
                state.current == null -> EmptyState(
                    title = stringResource(R.string.subs_review_empty_title),
                    body = stringResource(R.string.subs_review_empty_body),
                    categoryKey = "other",
                    actionLabel = stringResource(R.string.subs_review_finish),
                    onAction = onDone,
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
}

@Composable
private fun ReviewStep(
    state: SubscriptionsReviewUiState,
    catalog: BrandCatalog,
    onChoose: (ReviewDecision, ReviewItem) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val item = state.current ?: return
    val haptics = LocalHapticFeedback.current
    val progress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "review-progress",
    )
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
            progress = { progress },
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
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.subs_detail_cycle,
                        cycleLabel(item.cycle),
                        stringResource(R.string.subs_detail_year, formatter.formatWhole(item.yearly)),
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
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
            // Savings-first hierarchy: the yearly cost above makes the stake visible, and
            // cancelling is the strongest action, never the quietest one. Ink, not error red:
            // red belongs only to an over-budget amount.
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onChoose(ReviewDecision.CANCEL, item)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Text(stringResource(R.string.subs_review_cancel))
            }
            FilledTonalButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onChoose(ReviewDecision.KEEP, item)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.subs_review_keep))
            }
            OutlinedButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onChoose(ReviewDecision.RARELY, item)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.subs_review_rarely))
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
        queue = items,
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
        queue = items,
        kept = 1,
        rarely = 1,
        cancelled = 2,
        yearlySaving = money(58776),
        today = today,
        decidedIds = items.map { it.id }.toSet(),
    )
}
