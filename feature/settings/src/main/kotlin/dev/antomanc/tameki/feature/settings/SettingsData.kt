package dev.antomanc.tameki.feature.settings

import dev.antomanc.tameki.core.model.coroutines.runCatchingCancellable
import dev.antomanc.tameki.core.model.csv.CsvHeader
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import dev.antomanc.tameki.core.model.money.MoneyParser
import dev.antomanc.tameki.core.model.repository.CsvColumns
import dev.antomanc.tameki.core.model.repository.CsvProblem
import dev.antomanc.tameki.core.model.repository.CsvProblemKind
import dev.antomanc.tameki.core.ui.theme.spacing
import dev.antomanc.tameki.core.ui.theme.tamekiColors
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun DataSection(
    padding: PaddingValues,
    onExportCsv: suspend (File, suspend (File) -> Boolean) -> Boolean,
    onExportJson: suspend (File, suspend (File) -> Boolean) -> Boolean,
    onLoadCsv: (File, List<String>) -> Unit,
    onOpenExchange: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val createCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val tmp = File(context.cacheDir, "export-${System.nanoTime()}.csv")
            try {
                onExportCsv(tmp) { copyExport(context, uri, it) }
            } finally {
                tmp.delete()
            }
        }
    }
    val createJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val tmp = File(context.cacheDir, "export-${System.nanoTime()}.json")
            try {
                onExportJson(tmp) { copyExport(context, uri, it) }
            } finally {
                tmp.delete()
            }
        }
    }
    val openCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            // Off the main thread, and a revoked or unreadable document is not a crash.
            val loaded = withContext(Dispatchers.IO) {
                runCatchingCancellable {
                    val tmp = File(context.cacheDir, "import.csv")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tmp).use { input.copyTo(it) }
                    } ?: error("unreadable")
                    // Semicolon and tab files (common from Italian banks) and a UTF-8 BOM still
                    // give one header per column.
                    tmp to CsvHeader.parse(tmp.bufferedReader().use { it.readLine() }.orEmpty())
                }.getOrNull()
            }
            if (loaded != null) onLoadCsv(loaded.first, loaded.second)
        }
    }
    Column(
        Modifier.fillMaxSize().padding(padding).padding(MaterialTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.group),
    ) {
        Button(onClick = { openCsv.launch(arrayOf("text/*", "text/csv")) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_import_csv))
        }
        OutlinedButton(onClick = { createCsv.launch("tameki-export.csv") }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_export_csv))
        }
        OutlinedButton(onClick = { createJson.launch("tameki-export.json") }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_export_json))
        }
        OutlinedButton(onClick = onOpenExchange, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_exchange))
        }
    }
}

@Composable
internal fun CsvImportSection(
    padding: PaddingValues,
    state: SettingsUiState,
    onSetCsvMapping: (String, String?) -> Unit,
    onPreviewCsv: () -> Unit,
    onImportCsv: () -> Unit,
) {
    val fields = listOf(
        CsvField(CsvColumns.DATE, R.string.settings_csv_field_date, required = true),
        CsvField(CsvColumns.AMOUNT, R.string.settings_csv_field_amount, required = true),
        CsvField(CsvColumns.CURRENCY, R.string.settings_csv_field_currency),
        CsvField(CsvColumns.MERCHANT, R.string.settings_csv_field_merchant),
        CsvField(CsvColumns.CATEGORY, R.string.settings_csv_field_category),
        CsvField(CsvColumns.NOTE, R.string.settings_csv_field_note),
        CsvField(CsvColumns.ACCOUNT, R.string.settings_csv_field_account),
        CsvField(CsvColumns.KIND, R.string.settings_csv_field_kind),
        CsvField(CsvColumns.TAGS, R.string.settings_csv_field_tags),
    )
    val requiredMapped = fields.filter { it.required }.all { state.csvMapping[it.column] != null }
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(MaterialTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.tight),
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_csv_map_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(fields) { field ->
            CsvFieldPicker(
                field = field,
                headers = state.csvHeaders,
                selected = state.csvMapping[field.column],
                onSelect = { onSetCsvMapping(field.column, it) },
            )
        }
        item {
            Button(
                onClick = onPreviewCsv,
                enabled = requiredMapped,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_csv_preview))
            }
        }
        if (!requiredMapped) {
            item {
                Text(
                    text = stringResource(R.string.settings_csv_required_missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        state.csvPreview?.let { preview ->
            item {
                Text(
                    text = pluralStringResource(
                        R.plurals.settings_csv_rows,
                        preview.rows.size,
                        preview.rows.size,
                    ) + " · " + pluralStringResource(
                        R.plurals.settings_csv_problems,
                        preview.problems.size,
                        preview.problems.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            items(preview.problems.take(5)) { problem ->
                val message = csvProblemText(problem)
                Text(
                    text = if (problem.lineNumber > 0) {
                        stringResource(
                            R.string.settings_csv_problem_line,
                            problem.lineNumber,
                            message,
                        )
                    } else {
                        message
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            item {
                Button(onClick = onImportCsv, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_csv_import_action))
                }
            }
        }
    }
}

/** One CSV target: a localized name, whether it is required, and the file column it reads. */
internal data class CsvField(
    val column: String,
    @param:StringRes val label: Int,
    val required: Boolean = false,
)

/** Localised copy for a structured import problem; the parser never ships English UI text. */
@Composable
internal fun csvProblemText(problem: CsvProblem): String = when (problem.kind) {
    CsvProblemKind.EMPTY_FILE -> stringResource(R.string.settings_csv_problem_empty_file)
    CsvProblemKind.MISSING_MAPPING -> stringResource(
        R.string.settings_csv_problem_missing_mapping,
        csvFieldLabel(problem.detail),
    )
    CsvProblemKind.UNPARSEABLE_DATE -> stringResource(
        R.string.settings_csv_problem_unparseable_date,
        problem.detail.orEmpty(),
    )
    CsvProblemKind.MISSING_DATE -> stringResource(R.string.settings_csv_problem_missing_date)
    CsvProblemKind.UNPARSEABLE_CURRENCY -> stringResource(
        R.string.settings_csv_problem_unparseable_currency,
        problem.detail.orEmpty(),
    )
    CsvProblemKind.UNPARSEABLE_AMOUNT -> stringResource(
        R.string.settings_csv_problem_unparseable_amount,
        problem.detail.orEmpty(),
    )
    CsvProblemKind.MISSING_AMOUNT -> stringResource(R.string.settings_csv_problem_missing_amount)
    CsvProblemKind.UNKNOWN_ACCOUNT -> stringResource(
        R.string.settings_csv_problem_unknown_account,
        problem.detail.orEmpty(),
    )
    CsvProblemKind.UNKNOWN_KIND -> stringResource(
        R.string.settings_csv_problem_unknown_kind,
        problem.detail.orEmpty(),
    )
}

@Composable
internal fun csvFieldLabel(column: String?): String = stringResource(
    when (column) {
        CsvColumns.DATE -> R.string.settings_csv_field_date
        CsvColumns.AMOUNT -> R.string.settings_csv_field_amount
        CsvColumns.CURRENCY -> R.string.settings_csv_field_currency
        CsvColumns.MERCHANT -> R.string.settings_csv_field_merchant
        CsvColumns.CATEGORY -> R.string.settings_csv_field_category
        CsvColumns.NOTE -> R.string.settings_csv_field_note
        CsvColumns.ACCOUNT -> R.string.settings_csv_field_account
        CsvColumns.KIND -> R.string.settings_csv_field_kind
        CsvColumns.TAGS -> R.string.settings_csv_field_tags
        else -> R.string.settings_csv_not_mapped
    },
)

/**
 * A field mapped by choosing one of the file's own headers, never by typing an English key. The
 * button shows the source header it will read, so the mapping reads left to right: field → column.
 */
@Composable
internal fun CsvFieldPicker(
    field: CsvField,
    headers: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(field.label),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            if (field.required) {
                Text(
                    text = stringResource(R.string.settings_csv_required),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.tamekiColors.amberInk,
                )
            }
        }
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = selected ?: stringResource(R.string.settings_csv_not_mapped),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings_csv_not_mapped)) },
                    onClick = {
                        expanded = false
                        onSelect(null)
                    },
                )
                headers.forEach { header ->
                    DropdownMenuItem(
                        text = { Text(header) },
                        onClick = {
                            expanded = false
                            onSelect(header)
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun ExchangeSection(
    padding: PaddingValues,
    state: SettingsUiState,
    onSetManualRate: (String, String, String) -> Unit,
) {
    var from by remember { mutableStateOf(state.baseCurrency.value) }
    var to by remember { mutableStateOf("USD") }
    var rate by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(padding).padding(MaterialTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.group),
    ) {
        OutlinedTextField(
            from,
            { from = it.uppercase() },
            label = { Text(stringResource(R.string.settings_base_currency)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            to,
            { to = it.uppercase() },
            label = { Text(stringResource(R.string.settings_exchange)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            rate,
            { rate = it },
            label = { Text(stringResource(R.string.settings_rate)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        // Follow the app's language, not the system one: an Italian UI offers a comma keyboard
        // even on an English phone, and "1,08" must light up Save there.
        val separator = MoneyParser.decimalSeparator(LocalConfiguration.current.locales[0])
        val parsedRate = MoneyParser.parseDecimal(rate, separator)
        val validRate = parsedRate?.signum() == 1
        Button(
            onClick = {
                val normalized = parsedRate?.stripTrailingZeros()?.toPlainString() ?: rate
                onSetManualRate(from, to, normalized)
            },
            enabled = from.isNotBlank() && to.isNotBlank() && validRate,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_save))
        }
    }
}
