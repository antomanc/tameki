package dev.antomanc.tameki.feature.activity

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
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
        onClearSnackbar = viewModel::clearSnackbar,
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
    onClearSnackbar: () -> Unit,
    onEdit: (Long) -> Unit,
    onAcceptInbox: (InboxItem) -> Unit,
    onDismissInbox: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val snackbarHost = remember { SnackbarHostState() }
    val savedLabel = stringResource(R.string.activity_deleted)
    val undoLabel = stringResource(R.string.activity_undo)

    LaunchedEffect(state.snackbar) {
        if (state.snackbar == "deleted") {
            val result = snackbarHost.showSnackbar(message = savedLabel, actionLabel = undoLabel)
            if (result == SnackbarResult.ActionPerformed) onUndoDelete()
            onClearSnackbar()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(stringResource(R.string.activity_title)) },
                actions = {
                    IconButton(onClick = onOpenFilters) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
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
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                )
                Spacer(Modifier.height(spacing.tight))
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
                items(
                    count = paging.itemCount,
                    key = paging.itemKey { item ->
                        when (item) {
                            is ActivityListItem.Header -> "h-${item.date}"
                            is ActivityListItem.Row -> "t-${item.transaction.id}"
                        }
                    },
                ) { index ->
                    when (val item = paging[index]) {
                        is ActivityListItem.Header -> StickyDay(item, state.baseCurrency)
                        is ActivityListItem.Row -> SwipeRow(item, onDelete, onEdit)
                        null -> Unit
                    }
                }
            } else {
                items(staticItems, key = {
                    when (it) {
                        is ActivityListItem.Header -> "h-${it.date}"
                        is ActivityListItem.Row -> "t-${it.transaction.id}"
                    }
                }) { item ->
                    when (item) {
                        is ActivityListItem.Header -> StickyDay(item, state.baseCurrency)
                        is ActivityListItem.Row -> SwipeRow(item, onDelete, onEdit)
                    }
                }
            }
            val empty = (paging == null && staticItems.none { it is ActivityListItem.Row }) ||
                (paging != null && paging.itemCount == 0 && state.inbox.isEmpty())
            if (empty) {
                item {
                    EmptyState(
                        title = stringResource(R.string.activity_empty_title),
                        body = stringResource(R.string.activity_empty_body),
                        categoryKey = "other",
                    )
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
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                onDelete(item.transaction.id)
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
    ) {
        TransactionRow(
            title = item.title,
            amount = item.transaction.amount,
            categoryKey = item.categoryKey,
            meta = item.meta,
            income = item.income,
            onClick = { onEdit(item.transaction.id) },
        )
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
            amount = row.amount ?: Money.zero(CurrencyCode("EUR")),
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
        Column(Modifier.padding(MaterialTheme.spacing.gutter)) {
            Text(stringResource(R.string.activity_filters), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.activity_filter_category), style = MaterialTheme.typography.labelLarge)
            FilterChip(
                selected = draft.categoryId == null,
                onClick = { draft = draft.copy(categoryId = null) },
                label = { Text(stringResource(R.string.activity_filter_any)) },
            )
            categories.take(8).forEach { cat ->
                FilterChip(
                    selected = draft.categoryId == cat.id,
                    onClick = { draft = draft.copy(categoryId = cat.id) },
                    label = { Text(cat.displayName()) },
                )
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
            Spacer(Modifier.height(16.dp))
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
            onClearSnackbar = {},
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
