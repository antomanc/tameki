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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun PasteResultRoute(
    initialText: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AiBridgeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(initialText) {
        if (!initialText.isNullOrBlank()) viewModel.prefillPaste(initialText)
    }
    val context = LocalContext.current
    PasteResultScreen(
        state = state,
        onPasteTextChange = viewModel::setPasteText,
        onPasteClipboard = {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
            viewModel.setPasteText(text)
            if (text.isNotBlank()) viewModel.parsePaste()
        },
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
    onPasteClipboard: () -> Unit,
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
    LaunchedEffect(state.snackbar) {
        if (state.snackbar == "imported") {
            snackbar.showSnackbar(imported)
            onClearSnackbar()
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_paste_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.ai_back)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(MaterialTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.group),
        ) {
            item {
                OutlinedTextField(
                    value = state.pasteText,
                    onValueChange = onPasteTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MaterialTheme.spacing.section * 4),
                    placeholder = { Text(stringResource(R.string.ai_paste_hint)) },
                    minLines = 6,
                )
                Spacer(Modifier.height(MaterialTheme.spacing.tight))
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.tight)) {
                    OutlinedButton(onClick = onPasteClipboard) {
                        Text(stringResource(R.string.ai_paste_clipboard))
                    }
                    Button(onClick = onParse) {
                        Text(stringResource(R.string.ai_parse))
                    }
                }
            }
            items(state.draftRows, key = { it.id }) { row ->
                DraftRow(
                    row = row,
                    categories = state.categories,
                    accounts = state.accounts,
                    onSetCategory = { onSetCategory(row.id, it) },
                    onSetAccount = { onSetAccount(row.id, it) },
                )
            }
            if (state.draftRows.isNotEmpty()) {
                item {
                    val n = state.draftRows.size
                    Button(
                        onClick = onImport,
                        enabled = !state.importing && n > 0,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.ai_import_n, n))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftRow(
    row: DraftRowUi,
    categories: List<Category>,
    accounts: List<Account>,
    onSetCategory: (Long?) -> Unit,
    onSetAccount: (Long?) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(row.title, style = MaterialTheme.typography.titleSmall)
        if (row.amountLabel.isNotBlank()) {
            Text(row.amountLabel, style = MaterialTheme.typography.bodyLarge)
        }
        row.problems.forEach { problem ->
            Text(
                problem,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(MaterialTheme.spacing.tight))
        HintDropdown(
            label = stringResource(R.string.ai_category),
            options = categories.map { it.id to it.name },
            selectedId = row.categoryId,
            onSelect = onSetCategory,
        )
        if (!row.isSubscription) {
            Spacer(Modifier.height(MaterialTheme.spacing.tight))
            HintDropdown(
                label = stringResource(R.string.ai_account),
                options = accounts.map { it.id to it.name },
                selectedId = row.accountId,
                onSelect = onSetAccount,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HintDropdown(
    label: String,
    options: List<Pair<Long, String>>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second.orEmpty()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                )
            }
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
            onPasteClipboard = {},
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
    pasteText = """{"tameki":1,"transactions":[]}""",
    draftRows = listOf(
        DraftRowUi(
            id = "tx-0",
            title = "Esselunga",
            amountLabel = "12.50 EUR",
            categoryId = null,
            accountId = 1,
            problems = listOf("Unknown category “GroceriesX”"),
            isSubscription = false,
        ),
    ),
    categories = listOf(Category(1, "Spesa", CategoryKind.EXPENSE, "groceries", 1L)),
    accounts = listOf(
        Account(1, "Carta", dev.antomanc.tameki.core.model.CurrencyCode("EUR"),
            dev.antomanc.tameki.core.model.entity.AccountType.CARD),
    ),
)
