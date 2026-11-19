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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Category
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
    snackbarHostState: SnackbarHostState,
    openInbox: Boolean = false,
    categoryId: Long? = null,
    onAddMovement: () -> Unit,
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
    // A category bar opens the journal already filtered to the category it names, instead of
    // dropping the user into an unfiltered list they have to narrow again.
    LaunchedEffect(categoryId) {
        if (categoryId != null) viewModel.setCategoryFilter(categoryId)
    }
    // A preset's bounds are relative to the clock. Re-resolve them when the screen comes back
    // (opening the app the next day is the usual case) and again at midnight if it stays open,
    // so the chip's selection and the query can never disagree about what "this month" is.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshToday() }
    LaunchedEffect(Unit) { viewModel.watchToday() }
    ActivityScreen(
        state = state,
        paging = paging,
        snackbarHostState = snackbarHostState,
        onQueryChange = viewModel::setQuery,
        onOpenFilters = { viewModel.openFilters(true) },
        onCloseFilters = { viewModel.openFilters(false) },
        onApplyFilters = viewModel::applyFilters,
        onAddMovement = onAddMovement,
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
    onAddMovement: () -> Unit,
    onDelete: (Long) -> Unit,
    onUndoDelete: () -> Unit,
    onAcknowledgeDelete: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onAcceptInbox: (InboxItem) -> Unit,
    onDismissInbox: (Long) -> Unit,
    /** The app's single host, owned by the root scaffold: features never draw their own. */
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val deletedLabel = stringResource(R.string.activity_deleted)
    val undoLabel = stringResource(R.string.activity_undo)
    val deleteGeneration = state.deleteGeneration

    // One decision for the whole list area, taken outside the lazy scope. A refresh that is still
    // running says nothing, and a failed one says so instead of borrowing the words of an empty
    // list: "no movements" is a fact about the data, not about the read.
    val hasRows = paging?.let { it.itemCount > 0 } ?: staticItems.any { it is ActivityListItem.Row }
    val emptyKind = activityEmptyKind(
        hasRows = hasRows,
        refresh = paging?.loadState?.refresh ?: LoadState.NotLoading(endOfPaginationReached = true),
        noMatch = state.filters.noMatch,
    )
    // Never two "Clear filters" buttons for one tap. The no-match state names the cause and keeps
    // its own button, so the header's copy steps aside; BOTH keeps both, because there the empty
    // state clears the search, which is a different action.
    val headerClearVisible = state.filters.hasSheetFacets &&
        !(emptyKind == ActivityEmptyKind.NO_MATCH && state.filters.noMatch == ActivityNoMatch.FILTERS)

    LaunchedEffect(deleteGeneration) {
        if (deleteGeneration > 0L) {
            // Long + dismissable: a swipe is easy to trigger by accident, so Undo stays reachable
            // far longer than the default short snackbar and never traps the user. The effect is
            // keyed on a counter, so every swipe shows its own snackbar. The event is acknowledged
            // when the snackbar resolves — or when the composition goes away mid-snackbar — so a
            // tab return or a rotation cannot replay an Undo for a row that is already gone.
            try {
                // One snackbar at a time on the shared host: drop what is still up instead of
                // queueing behind it, so this Undo is the one the user can actually tap.
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
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
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(stringResource(R.string.activity_title)) },
                actions = {
                    val facetCount = state.filters.facetCount
                    IconButton(onClick = onOpenFilters) {
                        BadgedBox(
                            badge = {
                                if (facetCount > 0) {
                                    Badge(
                                        // Tonal accent, never error red: in Tameki red means an
                                        // over-budget amount, not "a filter is on".
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ) {
                                        // The count is visual; TalkBack hears it in the icon label.
                                        Text(
                                            text = facetCount.toString(),
                                            modifier = Modifier.clearAndSetSemantics {},
                                        )
                                    }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (facetCount > 0) Icons.Filled.Tune else Icons.Outlined.Tune,
                                contentDescription = if (facetCount > 0) {
                                    stringResource(R.string.activity_filters_active, facetCount)
                                } else {
                                    stringResource(R.string.activity_filters)
                                },
                            )
                        }
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
                    // Search clears itself: "Clear filters" below must never erase a typed query.
                    trailingIcon = if (state.filters.query.isNotEmpty()) {
                        {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.activity_clear_search),
                                )
                            }
                        }
                    } else {
                        null
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
                PeriodChips(filters = state.filters, onApply = onApplyFilters)
                if (state.filters.hasSheetFacets) {
                    Spacer(Modifier.height(spacing.tight))
                    ActiveFilterChips(
                        filters = state.filters,
                        categories = state.categories,
                        accounts = state.accounts,
                        onApply = onApplyFilters,
                    )
                    if (headerClearVisible) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            // The period chips above are a separate control: clearing the sheet's
                            // facets must not silently move the period selection.
                            TextButton(onClick = { onApplyFilters(state.filters.clearedSheetFacets()) }) {
                                Text(stringResource(R.string.activity_clear_filters))
                            }
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
            when (emptyKind) {
                ActivityEmptyKind.BASE -> if (state.inbox.isEmpty()) {
                    // Actionable, not decorative: no category avatar for a journal with no rows,
                    // and the button opens the same quick-add the FAB does. A pending capture in
                    // the Inbox is not a transaction, so the Inbox section above already fills
                    // the screen and this message would only repeat it.
                    item {
                        EmptyState(
                            title = stringResource(R.string.activity_empty_title),
                            actionLabel = stringResource(R.string.activity_add_movement),
                            onAction = onAddMovement,
                        )
                    }
                }
                ActivityEmptyKind.NO_MATCH -> {
                    val noMatch = state.filters.noMatch
                    if (noMatch != null) {
                        item {
                            NoMatchEmpty(
                                reason = noMatch,
                                filters = state.filters,
                                onQueryChange = onQueryChange,
                                onApplyFilters = onApplyFilters,
                            )
                        }
                    }
                }
                // A failed read is not an empty journal: say what happened and offer the retry.
                ActivityEmptyKind.ERROR -> item {
                    EmptyState(
                        title = stringResource(R.string.activity_load_failed),
                        actionLabel = stringResource(R.string.activity_retry),
                        onAction = { paging?.retry() },
                    )
                }
                null -> Unit
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
private fun NoMatchEmpty(
    reason: ActivityNoMatch,
    filters: ActivityFilters,
    onQueryChange: (String) -> Unit,
    onApplyFilters: (ActivityFilters) -> Unit,
) {
    val title = stringResource(R.string.activity_no_matches_title)
    // One pattern for every body ("No movements match …"), one action scoped to the cause the
    // body names: the button always removes what is actually hiding the rows.
    when (reason) {
        ActivityNoMatch.SEARCH -> EmptyState(
            title = title,
            body = stringResource(R.string.activity_no_matches_body),
            actionLabel = stringResource(R.string.activity_clear_search),
            onAction = { onQueryChange("") },
        )
        ActivityNoMatch.FILTERS -> EmptyState(
            title = title,
            body = stringResource(R.string.activity_no_matches_filters_body),
            actionLabel = stringResource(R.string.activity_clear_filters),
            onAction = { onApplyFilters(filters.clearedSheetFacets()) },
        )
        ActivityNoMatch.PERIOD -> EmptyState(
            title = title,
            body = stringResource(R.string.activity_no_matches_period_body),
            actionLabel = stringResource(R.string.activity_clear_period),
            onAction = { onApplyFilters(filters.clearedPeriod()) },
        )
        // Clearing the search is the smaller loss: the typed text is deliberate, the facets are
        // kept, and if the list is still empty the filters/period state offers its own way out.
        ActivityNoMatch.BOTH -> EmptyState(
            title = title,
            body = stringResource(R.string.activity_no_matches_both_body),
            actionLabel = stringResource(R.string.activity_clear_search),
            onAction = { onQueryChange("") },
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

/**
 * The period presets under search. They only set the preset; category and account stay in the
 * Tune sheet, so the two surfaces never fight over the same field. The chip is selected by the
 * preset itself, so it stays selected when the clock crosses midnight — the ViewModel resolves
 * the bounds from today, and the two can never disagree.
 */
@Composable
private fun PeriodChips(
    filters: ActivityFilters,
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
                selected = preset.isSelected(filters),
                onClick = { onApply(filters.copy(period = preset)) },
                label = { Text(stringResource(preset.labelRes)) },
            )
        }
    }
}

/**
 * One removable chip per active facet under the period presets: a filter you cannot see is a
 * filter you cannot trust, and a period is already visible in its own chip.
 */
@Composable
private fun ActiveFilterChips(
    filters: ActivityFilters,
    categories: List<Category>,
    accounts: List<Account>,
    onApply: (ActivityFilters) -> Unit,
) {
    val categoryChip = filters.categoryId
        ?.let { id -> categories.firstOrNull { it.id == id } }
        ?.let { it.displayName() to filters.copy(categoryId = null) }
    val accountChip = filters.accountId
        ?.let { id -> accounts.firstOrNull { it.id == id } }
        ?.let { it.displayName() to filters.copy(accountId = null) }
    val sourceChip = filters.source
        ?.let { stringResource(it.labelRes()) to filters.copy(source = null) }
    val tagChip = filters.tag?.let { it to filters.copy(tag = null) }
    val chips = listOfNotNull(categoryChip, accountChip, sourceChip, tagChip)
    // An id can outlive its category or account; the header's Clear filters still removes it.
    if (chips.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { (label, next) ->
            InputChip(
                selected = true,
                onClick = { onApply(next) },
                label = { Text(label) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.activity_filter_remove),
                        modifier = Modifier.size(16.dp),
                    )
                },
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
    categories: List<Category>,
    accounts: List<Account>,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.activity_filters),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                // A draft is reset without closing: the sheet must be editable both ways before
                // it is applied. Only the facets this sheet shows are cleared — the period chips
                // are not here, so touching them would be an invisible side effect. Same label as
                // the header's action, because it does the same thing.
                TextButton(
                    onClick = { draft = draft.clearedSheetFacets() },
                    enabled = draft.hasSheetFacets,
                ) {
                    Text(stringResource(R.string.activity_clear_filters))
                }
            }
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
                            // Same toggle as account and source: tapping the selected chip
                            // releases the facet instead of requiring the separate Any chip.
                            onClick = {
                                draft = draft.copy(
                                    categoryId = if (draft.categoryId == cat.id) null else cat.id,
                                )
                            },
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

/**
 * Every origin a transaction can have. Imports are not exotic: a CSV migration or an AI paste
 * creates exactly the rows someone may want to find again, so the filter is exhaustive and its
 * labels name the real provenance.
 */
private val SourceChipValues: List<TransactionSource> = TransactionSource.entries

private fun TransactionSource.labelRes(): Int = when (this) {
    TransactionSource.MANUAL -> R.string.activity_source_manual
    TransactionSource.RECEIPT -> R.string.activity_source_receipt
    TransactionSource.NOTIFICATION -> R.string.activity_source_notification
    TransactionSource.AI_IMPORT -> R.string.activity_source_ai_import
    TransactionSource.AUTOMATION -> R.string.activity_source_automation
    TransactionSource.RECURRING -> R.string.activity_source_recurring
    TransactionSource.CSV_IMPORT -> R.string.activity_source_csv_import
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
            onAddMovement = {},
            onDelete = {},
            onUndoDelete = {},
            onAcknowledgeDelete = {},
            onEdit = {},
            onAcceptInbox = {},
            onDismissInbox = {},
        )
    }
}

internal fun sampleActivityState(filters: ActivityFilters = ActivityFilters()): ActivityUiState {
    val eur = CurrencyCode("EUR")
    return ActivityUiState(
        filters = filters,
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
