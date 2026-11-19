package dev.antomanc.tameki.feature.aibridge

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.ai.Problem
import dev.antomanc.tameki.core.model.ai.ProblemCode
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.ui.components.CategoryChip
import dev.antomanc.tameki.core.ui.components.RecurringRow
import dev.antomanc.tameki.core.ui.components.SelectableChip
import dev.antomanc.tameki.core.ui.components.TransactionRow
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.spacing
import dev.antomanc.tameki.core.ui.theme.tamekiColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.koin.androidx.compose.koinViewModel

@Composable
fun PasteResultRoute(
    initialText: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AiBridgeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var clipboardPrefillDone by remember { mutableStateOf(false) }
    LaunchedEffect(initialText) {
        if (!initialText.isNullOrBlank()) {
            viewModel.prefillPaste(initialText)
            clipboardPrefillDone = true
        }
    }
    LaunchedEffect(clipboardPrefillDone) {
        if (clipboardPrefillDone) return@LaunchedEffect
        if (!state.pasteText.isBlank()) {
            clipboardPrefillDone = true
            return@LaunchedEffect
        }
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        if (text.isNotBlank()) {
            viewModel.prefillPaste(text)
        }
        clipboardPrefillDone = true
    }
    PasteResultScreen(
        state = state,
        onPasteTextChange = viewModel::setPasteText,
        onParse = viewModel::parsePaste,
        onSetCategory = viewModel::setRowCategory,
        onSetAccount = viewModel::setRowAccount,
        onImport = viewModel::importAll,
        onClearSnackbar = viewModel::clearSnackbar,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasteResultScreen(
    state: AiBridgeUiState,
    onPasteTextChange: (String) -> Unit,
    onParse: () -> Unit,
    onSetCategory: (String, Long?) -> Unit,
    onSetAccount: (String, Long?) -> Unit,
    onImport: () -> Unit,
    onClearSnackbar: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val imported = stringResource(R.string.ai_imported)
    val spacing = MaterialTheme.spacing
    var textExpanded by remember { mutableStateOf(!state.parsed) }
    var categoryPickerFor by remember { mutableStateOf<DraftRowUi?>(null) }
    var accountPickerFor by remember { mutableStateOf<DraftRowUi?>(null) }

    LaunchedEffect(state.parsed) {
        if (state.parsed) textExpanded = false
    }
    LaunchedEffect(state.snackbar) {
        if (state.snackbar == "imported") {
            snackbar.showSnackbar(imported)
            onClearSnackbar()
        }
    }

    val importCount = state.importableCount

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // The paste field and the pickers share this screen with the pinned Import button.
        modifier = modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = stringResource(R.string.ai_paste_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.ai_navigate_up),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface {
                Button(
                    onClick = onImport,
                    enabled = !state.importing && importCount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.gutter, vertical = spacing.group),
                ) {
                    Text(stringResource(R.string.ai_import_n, importCount))
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = spacing.gutter,
                vertical = spacing.group,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.tight),
        ) {
            item {
                OutlinedTextField(
                    value = state.pasteText,
                    onValueChange = {
                        onPasteTextChange(it)
                        textExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.ai_paste_hint)) },
                    minLines = if (textExpanded) 6 else 4,
                    maxLines = if (textExpanded) 12 else 4,
                    readOnly = state.parsed && !textExpanded,
                )
                if (state.parsed && !textExpanded) {
                    TextButton(onClick = { textExpanded = true }) {
                        Text(stringResource(R.string.ai_edit_paste))
                    }
                }
                Spacer(Modifier.height(spacing.tight))
            }
            items(state.draftRows, key = { it.id }) { row ->
                DraftPreviewRow(
                    row = row,
                    onCategoryClick = { categoryPickerFor = row },
                    // Subscriptions have no account field in the import; only movements can be fixed here.
                    onAccountClick = if (row.isSubscription || state.accounts.isEmpty()) {
                        null
                    } else {
                        { accountPickerFor = row }
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }

    val picker = categoryPickerFor
    if (picker != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { categoryPickerFor = null },
            sheetState = sheetState,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.gutter)
                    .padding(bottom = spacing.section),
                verticalArrangement = Arrangement.spacedBy(spacing.tight),
            ) {
                Text(
                    text = stringResource(R.string.ai_pick_category),
                    style = MaterialTheme.typography.titleMedium,
                )
                state.categories.forEach { category ->
                    CategoryChip(
                        name = category.name,
                        categoryKey = category.icon,
                        selected = category.id == picker.categoryId,
                        onClick = {
                            onSetCategory(picker.id, category.id)
                            categoryPickerFor = null
                        },
                    )
                }
            }
        }
    }

    val accountPicker = accountPickerFor
    if (accountPicker != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { accountPickerFor = null },
            sheetState = sheetState,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.gutter)
                    .padding(bottom = spacing.section),
                verticalArrangement = Arrangement.spacedBy(spacing.tight),
            ) {
                Text(
                    text = stringResource(R.string.ai_pick_account),
                    style = MaterialTheme.typography.titleMedium,
                )
                state.accounts.forEach { account ->
                    SelectableChip(
                        selected = account.id == accountPicker.accountId,
                        onClick = {
                            onSetAccount(accountPicker.id, account.id)
                            accountPickerFor = null
                        },
                        label = account.name,
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftPreviewRow(
    row: DraftRowUi,
    onCategoryClick: () -> Unit,
    onAccountClick: (() -> Unit)?,
) {
    val spacing = MaterialTheme.spacing
    val amber = MaterialTheme.tamekiColors.amberInk
    val amount = row.amount
    val dateLabel = row.date?.let {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(it)
    }
    val meta = listOfNotNull(row.accountName, dateLabel).joinToString(" · ").ifBlank { null }

    Column(Modifier.fillMaxWidth()) {
        if (amount != null) {
            if (row.isSubscription) {
                RecurringRow(
                    name = row.title,
                    amount = amount,
                    relativeDate = dateLabel.orEmpty(),
                    detail = row.categoryName,
                    onClick = null,
                )
            } else {
                TransactionRow(
                    title = row.title,
                    amount = amount,
                    categoryKey = row.categoryKey,
                    meta = meta,
                    onClick = null,
                )
            }
        } else {
            Text(row.title, style = MaterialTheme.typography.titleMedium)
        }
        Row(
            modifier = Modifier.padding(top = spacing.tight),
            horizontalArrangement = Arrangement.spacedBy(spacing.tight),
        ) {
            CategoryChip(
                name = row.categoryName ?: stringResource(R.string.ai_category),
                categoryKey = row.categoryKey,
                selected = row.categoryId != null,
                onClick = onCategoryClick,
            )
            if (onAccountClick != null) {
                SelectableChip(
                    selected = row.accountId != null,
                    onClick = onAccountClick,
                    label = row.accountName ?: stringResource(R.string.ai_account),
                )
            }
        }
        row.problems.forEach { problem ->
            Text(
                text = localizedProblem(problem),
                style = MaterialTheme.typography.bodySmall,
                color = amber,
                modifier = Modifier.padding(top = spacing.tight),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F0EA)
@Composable
private fun PastePreview() {
    TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
        PasteResultScreen(
            state = samplePasteState(),
            onPasteTextChange = {},
            onParse = {},
            onSetCategory = { _, _ -> },
            onSetAccount = { _, _ -> },
            onImport = {},
            onClearSnackbar = {},
            onBack = {},
        )
    }
}

internal fun samplePasteState() = AiBridgeUiState(
    pasteText = """{"tameki":1,"transactions":[{"amount":"12.50","merchant":"Esselunga","category":"GroceriesX"}]}""",
    parsed = true,
    draftRows = listOf(
        DraftRowUi(
            id = "tx-0",
            title = "Esselunga",
            amount = Money(1250, CurrencyCode("EUR")),
            categoryId = null,
            categoryKey = "other",
            categoryName = null,
            accountId = 1,
            accountName = "Carta",
            date = LocalDate.of(2026, 11, 18),
            problems = listOf(
                Problem(
                    "transactions[0]",
                    "Unknown category “GroceriesX”",
                    ProblemCode.UNKNOWN_CATEGORY,
                    "GroceriesX",
                ),
            ),
            isSubscription = false,
        ),
    ),
    categories = listOf(Category(1, "Spesa", CategoryKind.EXPENSE, "groceries", 1L)),
    accounts = listOf(
        Account(1, "Carta", CurrencyCode("EUR"), AccountType.CARD),
    ),
)
