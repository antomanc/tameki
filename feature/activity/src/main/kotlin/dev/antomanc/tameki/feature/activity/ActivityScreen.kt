package dev.antomanc.tameki.feature.activity

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.InboxItem
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.ui.components.DayHeader
import dev.antomanc.tameki.core.ui.components.EmptyState
import dev.antomanc.tameki.core.ui.components.SectionHeader
import dev.antomanc.tameki.core.ui.components.TransactionRow
import dev.antomanc.tameki.core.ui.seed.displayName
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.spacing
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import org.koin.androidx.compose.koinViewModel

@Composable
fun ActivityRoute(
    openInbox: Boolean = false,
    onEditTransaction: (Long) -> Unit,
    onAcceptInbox: (InboxItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val paging = viewModel.listItems.collectAsLazyPagingItems()
    LaunchedEffect(openInbox) {
        if (openInbox) viewModel.setFocusInbox(true)
    }
    ActivityScreen(
        state = state,
        paging = paging,
        onQueryChange = viewModel::setQuery,
        onOpenFilters = { viewModel.openFilters(true) },
        onCloseFilters = { viewModel.openFilters(false) },
        onApplyFilters = viewModel::applyFilters,
        onDelete = viewModel::delete,
        onUndoDelete = viewModel::undoDelete,
        onAcknowledgeDelete = viewModel::acknowledgeDelete,
        onEdit = onEditTransaction,
        onAcceptInbox = onAcceptInbox,
        onDismissInbox = viewModel::dismissInbox,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActivityScreen(
    state: ActivityUiState,
    paging: LazyPagingItems<ActivityListItem>?,
    staticItems: List<ActivityListItem> = emptyList(),
    onQueryChange: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onCloseFilters: () -> Unit,
    onApplyFilters: (ActivityFilters) -> Unit,
    onDelete: (Long) -> Unit,
    onUndoDelete: () -> Unit,
    onAcknowledgeDelete: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onAcceptInbox: (InboxItem) -> Unit,
    onDismissInbox: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val snackbarHost = remember { SnackbarHostState() }
    val deletedLabel = stringResource(R.string.activity_deleted)
    val undoLabel = stringResource(R.string.activity_undo)
    val deleteGeneration = state.deleteGeneration

    LaunchedEffect(deleteGeneration) {
        if (deleteGeneration > 0L) {
            // Long + dismissable: a swipe is easy to trigger by accident, so Undo stays reachable
            // far longer than the default short snackbar and never traps the user. The effect is
            // keyed on a counter, so every swipe shows its own snackbar. The event is acknowledged
            // when the snackbar resolves — or when the composition goes away mid-snackbar — so a
            // tab return or a rotation cannot replay an Undo for a row that is already gone.
            try {
                val result = snackbarHost.showSnackbar(
                    message = deletedLabel,
                    actionLabel = undoLabel,
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) onUndoDelete()
            } finally {
                onAcknowledgeDelete(deleteGeneration)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            // The app-level FAB floats over this screen (FabPosition.End, 56.dp + 16.dp margin);
            // lift "Deleted · Undo" above it so "Annulla" stays visible and tappable.
            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier.padding(bottom = 80.dp),
            )
        },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(stringResource(R.string.activity_title)) },
                actions = {
                    IconButton(onClick = onOpenFilters) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = stringResource(R.string.activity_filters),
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
                bottom = spacing.section + 72.dp,
            ),
        ) {
            item {
                TextField(
                    value = state.filters.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = CircleShape,
                    placeholder = { Text(stringResource(R.string.activity_search)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        // Highest container shade: the pill must read on dark paper too.
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                )
                Spacer(Modifier.height(spacing.tight))
                state.today?.let { today ->
                    PeriodChips(
                        filters = state.filters,
                        today = today,
                        onApply = onApplyFilters,
                    )
                }
                if (state.filters.isActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { onApplyFilters(ActivityFilters()) }) {
                            Text(stringResource(R.string.activity_clear_filters))
                        }
                    }
                } else {
                    Spacer(Modifier.height(spacing.tight))
                }
            }
            if (state.inbox.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(R.string.activity_inbox))
                }
                items(state.inbox, key = { "inbox-${it.item.id}" }) { row ->
                    InboxItemRow(
                        row = row,
                        onAccept = { onAcceptInbox(row.item) },
                        onDismiss = { onDismissInbox(row.item.id) },
                    )
                }
            }
            if (paging != null) {
                // DESIGN.md wants sticky day headers. stickyHeader is a LazyListScope call, so the
                // list is built per index: peek decides Header vs Row (peek never triggers loads),
                // and the content still calls paging[index] so prefetch keeps working.
                repeat(paging.itemCount) { index ->
                    when (val peeked = paging.peek(index)) {
                        is ActivityListItem.Header -> stickyHeader(key = "h-${peeked.date}") {
                            paging[index]
                            StickyDay(peeked, state.baseCurrency)
                        }
                        is ActivityListItem.Row -> item(key = "t-${peeked.transaction.id}") {
                            paging[index]
                            SwipeRow(peeked, onDelete, onEdit, modifier = Modifier.animateItem())
                        }
                        null -> item(key = "placeholder-$index") {
                            paging[index]
                        }
                    }
                }
            } else {
                staticItems.forEach { item ->
                    when (item) {
                        is ActivityListItem.Header -> stickyHeader(key = "h-${item.date}") {
                            StickyDay(item, state.baseCurrency)
                        }
                        is ActivityListItem.Row -> item(key = "t-${item.transaction.id}") {
                            SwipeRow(item, onDelete, onEdit, modifier = Modifier.animateItem())
                        }
                    }
                }
            }
            val empty = (paging == null && staticItems.none { it is ActivityListItem.Row }) ||
                (paging != null && paging.itemCount == 0 && state.inbox.isEmpty())
            if (empty) {
                item {
                    // A filtered-out list is not an empty app: say the search failed and offer the
                    // way back, instead of telling someone with 400 transactions to tap +.
                    if (state.filters.isActive) {
                        EmptyState(
                            title = stringResource(R.string.activity_no_matches_title),
                            body = stringResource(R.string.activity_no_matches_body),
                            actionLabel = stringResource(R.string.activity_clear_filters),
                            onAction = { onApplyFilters(ActivityFilters()) },
                        )
                    } else {
                        EmptyState(
                            title = stringResource(R.string.activity_empty_title),
                            body = stringResource(R.string.activity_empty_body),
                            categoryKey = "other",
                        )
                    }
                }
            }
        }
    }

    if (state.showFilters) {
        FilterSheet(
            filters = state.filters,
            categories = state.categories,
            accounts = state.accounts,
            onDismiss = onCloseFilters,
            onApply = onApplyFilters,
        )
    }
}

@Composable
private fun StickyDay(item: ActivityListItem.Header, baseCurrency: CurrencyCode) {
    DayHeader(
        label = item.label,
        total = item.net,
        unconverted = item.unconverted,
        baseCurrency = baseCurrency,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeRow(
    item: ActivityListItem.Row,
    onDelete: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Opaque row content is required: the delete layer sits behind it, full-bleed, and must never
    // show through the row. clipToBounds keeps that layer inside the row while it slides, instead
    // of painting over the day header or the neighbouring rows.
    // Neutral highest container instead of errorContainer: DESIGN reserves red for over-budget
    // only, deletion is feedback, not an error.
    // confirmValueChange returns false after scheduling delete so the box never settles in the
    // dismissed offset while paging/Room catch up — that stuck state looked like a broken row.
    key(item.transaction.id) {
        val haptics = LocalHapticFeedback.current
        val deleteLabel = stringResource(R.string.activity_delete)
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onDelete(item.transaction.id)
                }
                false
            },
        )
        SwipeToDismissBox(
            state = dismissState,
            modifier = modifier.clipToBounds(),
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        // The label is a visual affordance for sighted users; the row itself
                        // exposes "Delete" as an accessibility action, so this layer must not
                        // become a second, non-actionable TalkBack stop.
                        .clearAndSetSemantics {}
                        .padding(horizontal = MaterialTheme.spacing.gutter),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = deleteLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            // Swiping one way only: the other direction used to delete by accident.
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true,
        ) {
            TransactionRow(
                title = item.title,
                amount = item.transaction.amount,
                categoryKey = item.categoryKey,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.background,
                meta = item.meta,
                income = item.income,
                onClick = { onEdit(item.transaction.id) },
                // TalkBack cannot swipe: the destructive action lives on the row's own merged
                // semantics, so it is reachable from the node that is actually focused.
                onDelete = { onDelete(item.transaction.id) },
            )
        }
    }
}

/** A query or a filter chip is applied: an empty list means "nothing matched", not "no data". */
private val ActivityFilters.isActive: Boolean
    get() = query.isNotBlank() || from != null || to != null || categoryId != null ||
        accountId != null || source != null || tag != null

/**
 * The period presets under search. They only set [ActivityFilters.from]/[to]; category and account
 * stay in the Tune sheet, so the two surfaces never fight over the same field.
 */
internal enum class ActivityPeriod(val labelRes: Int) {
    ANY(R.string.activity_period_any),
    THIS_MONTH(R.string.activity_period_month),
    LAST_30_DAYS(R.string.activity_period_last_30_days),
    THIS_YEAR(R.string.activity_period_year);

    fun bounds(today: LocalDate): Pair<LocalDate?, LocalDate?> = when (this) {
        ANY -> null to null
        THIS_MONTH -> today.withDayOfMonth(1) to today
        LAST_30_DAYS -> today.minusDays(29) to today
        THIS_YEAR -> today.withDayOfYear(1) to today
    }

    fun isSelected(filters: ActivityFilters, today: LocalDate): Boolean {
        val (from, to) = bounds(today)
        return filters.from == from && filters.to == to
    }
}

@Composable
private fun PeriodChips(
    filters: ActivityFilters,
    today: LocalDate,
    onApply: (ActivityFilters) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActivityPeriod.entries.forEach { preset ->
            FilterChip(
                selected = preset.isSelected(filters, today),
                onClick = {
                    val (from, to) = preset.bounds(today)
                    onApply(filters.copy(from = from, to = to))
                },
                label = { Text(stringResource(preset.labelRes)) },
            )
        }
    }
}

@Composable
private fun InboxItemRow(
    row: InboxRow,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        TransactionRow(
            title = row.title,
            amount = row.amount,
            categoryKey = row.categoryKey,
            onClick = onAccept,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAccept) { Text(stringResource(R.string.activity_accept)) }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.activity_dismiss)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    filters: ActivityFilters,
    categories: List<dev.antomanc.tameki.core.model.entity.Category>,
    accounts: List<dev.antomanc.tameki.core.model.entity.Account>,
    onDismiss: () -> Unit,
    onApply: (ActivityFilters) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember(filters) { mutableStateOf(filters) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        // Apply is pinned: accounts and categories can outgrow a short screen (or 200% text), and
        // the one action must never be what scrolls away.
        Column(
            Modifier
                .padding(MaterialTheme.spacing.gutter)
                .imePadding(),
        ) {
            Text(stringResource(R.string.activity_filters), style = MaterialTheme.typography.titleMedium)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.activity_filter_category), style = MaterialTheme.typography.labelLarge)
                // Full list in a scrolling row: a category beyond the first eight must stay reachable.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = draft.categoryId == null,
                        onClick = { draft = draft.copy(categoryId = null) },
                        label = { Text(stringResource(R.string.activity_filter_any)) },
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = draft.categoryId == cat.id,
                            onClick = { draft = draft.copy(categoryId = cat.id) },
                            label = { Text(cat.displayName()) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.activity_filter_account), style = MaterialTheme.typography.labelLarge)
                accounts.forEach { account ->
                    FilterChip(
                        selected = draft.accountId == account.id,
                        onClick = {
                            draft = draft.copy(
                                accountId = if (draft.accountId == account.id) null else account.id,
                            )
                        },
                        label = { Text(account.displayName()) },
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.activity_filter_source), style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = draft.source == null,
                        onClick = { draft = draft.copy(source = null) },
                        label = { Text(stringResource(R.string.activity_filter_any)) },
                    )
                    SourceChipValues.forEach { source ->
                        FilterChip(
                            selected = draft.source == source,
                            onClick = {
                                draft = draft.copy(source = if (draft.source == source) null else source)
                            },
                            label = { Text(stringResource(source.labelRes())) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(MaterialTheme.spacing.group))
            Button(
                onClick = { onApply(draft) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.activity_filter_apply))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** The three origins a transaction can have in everyday use; imports are rare enough to skip. */
private val SourceChipValues = listOf(
    TransactionSource.MANUAL,
    TransactionSource.NOTIFICATION,
    TransactionSource.AUTOMATION,
)

private fun TransactionSource.labelRes(): Int = when (this) {
    TransactionSource.NOTIFICATION -> R.string.activity_source_notification
    TransactionSource.AUTOMATION -> R.string.activity_source_automation
    else -> R.string.activity_source_manual
}

@Preview(name = "Activity · inbox", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 411, heightDp = 891)
@Composable
private fun ActivityPreview() {
    TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
        ActivityScreen(
            state = sampleActivityState(),
            paging = null,
            staticItems = sampleActivityItems(),
            onQueryChange = {},
            onOpenFilters = {},
            onCloseFilters = {},
            onApplyFilters = {},
            onDelete = {},
            onUndoDelete = {},
            onAcknowledgeDelete = {},
            onEdit = {},
            onAcceptInbox = {},
            onDismissInbox = {},
        )
    }
}

internal fun sampleActivityState(): ActivityUiState {
    val eur = CurrencyCode("EUR")
    return ActivityUiState(
        inbox = listOf(
            InboxRow(
                item = InboxItem(
                    id = 1,
                    draft = TransactionDraft(
                        kind = TransactionKind.EXPENSE,
                        amount = Money(1250, eur),
                        merchant = "Esselunga",
                        categoryHint = "Alimentari",
                    ),
                    origin = TransactionSource.NOTIFICATION,
                    rawText = "Pagamento di 12,50 EUR presso Esselunga",
                    receivedAt = Instant.parse("2026-11-18T10:00:00Z"),
                ),
                title = "Esselunga",
                amount = Money(1250, eur),
                categoryKey = "groceries",
            ),
        ),
        categories = sampleCategories(),
        accounts = sampleAccounts(),
        today = LocalDate.of(2026, 11, 18),
    )
}

internal fun sampleActivityItems(): List<ActivityListItem> {
    val eur = CurrencyCode("EUR")
    val day = LocalDate.of(2026, 11, 18)
    return listOf(
        ActivityListItem.Header(
            date = day,
            label = "Oggi",
            net = Money(4470, eur),
        ),
        ActivityListItem.Row(
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
        ActivityListItem.Row(
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
            meta = "Carta",
            income = false,
        ),
    )
}
