package dev.antomanc.tameki.feature.settings

import android.app.LocaleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.provider.Settings as PlatformSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.repository.CsvColumns
import dev.antomanc.tameki.core.ui.components.SectionHeader
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.spacing
import java.io.File
import java.io.FileOutputStream
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onOpenAutomationDocs: () -> Unit = {},
    onOpenDebugGallery: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onOpen = viewModel::open,
        onBackRoot = {
            if (state.section == SettingsSection.ROOT) onBack?.invoke()
            else viewModel.backToRoot()
        },
        onSetBaseCurrency = viewModel::setBaseCurrency,
        onSetTheme = viewModel::setTheme,
        onSetDynamicColor = viewModel::setDynamicColor,
        onSetLock = viewModel::setLock,
        onSetReminders = viewModel::setReminders,
        onSetDefaultRemindDays = viewModel::setDefaultRemindDays,
        onSetCaptureEnabled = viewModel::setCaptureEnabled,
        onOpenCaptureGrant = viewModel::openCaptureGrant,
        onRefreshCapture = viewModel::refreshCaptureGranted,
        onSetAllowlistNone = viewModel::setAllowlistNone,
        onSetAllowlistDefault = viewModel::setAllowlistDefault,
        onToggleBanking = viewModel::toggleBankingApp,
        onDownloadNano = viewModel::downloadNano,
        onExportBackup = viewModel::exportBackup,
        onRequestRestore = viewModel::requestRestoreConfirm,
        onDismissRestore = viewModel::dismissRestoreConfirm,
        onRestore = viewModel::restoreBackup,
        onScheduleAutoBackup = viewModel::scheduleAutoBackup,
        onCancelAutoBackup = viewModel::cancelAutoBackup,
        onExportCsv = viewModel::exportCsv,
        onExportJson = viewModel::exportJson,
        onLoadCsv = viewModel::loadCsvForMapping,
        onSetCsvMapping = viewModel::setCsvMapping,
        onPreviewCsv = viewModel::previewCsv,
        onImportCsv = viewModel::importCsv,
        onSetManualRate = viewModel::setManualRate,
        onSetSilent = viewModel::setSilentAutomation,
        onOpenAutomationDocs = onOpenAutomationDocs,
        onOpenDebugGallery = onOpenDebugGallery,
        onClearSnackbar = viewModel::clearSnackbar,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOpen: (SettingsSection) -> Unit,
    onBackRoot: () -> Unit,
    onSetBaseCurrency: (String) -> Unit,
    onSetTheme: (String) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetLock: (Boolean) -> Unit,
    onSetReminders: (Boolean) -> Unit,
    onSetDefaultRemindDays: (Int) -> Unit,
    onSetCaptureEnabled: (Boolean) -> Unit,
    onOpenCaptureGrant: () -> Unit,
    onRefreshCapture: () -> Unit,
    onSetAllowlistNone: () -> Unit,
    onSetAllowlistDefault: () -> Unit,
    onToggleBanking: (String, Boolean) -> Unit,
    onDownloadNano: () -> Unit,
    onExportBackup: (File, CharArray) -> Unit,
    onRequestRestore: () -> Unit,
    onDismissRestore: () -> Unit,
    onRestore: (File, CharArray) -> Unit,
    onScheduleAutoBackup: (String, CharArray, Int) -> Unit,
    onCancelAutoBackup: () -> Unit,
    onExportCsv: (File) -> Unit,
    onExportJson: (File) -> Unit,
    onLoadCsv: (File, List<String>) -> Unit,
    onSetCsvMapping: (String, String?) -> Unit,
    onPreviewCsv: () -> Unit,
    onImportCsv: () -> Unit,
    onSetManualRate: (String, String, String) -> Unit,
    onSetSilent: (Boolean) -> Unit,
    onOpenAutomationDocs: () -> Unit,
    onOpenDebugGallery: () -> Unit,
    onClearSnackbar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(state.snackbar) {
        val key = state.snackbar ?: return@LaunchedEffect
        val msg = when (key) {
            "backup_exported" -> context.getString(R.string.settings_backup_exported)
            "backup_restored" -> context.getString(R.string.settings_backup_restored)
            "backup_failed" -> context.getString(R.string.settings_backup_failed)
            "auto_backup_on" -> context.getString(R.string.settings_auto_backup_on)
            "csv_exported" -> context.getString(R.string.settings_csv_exported)
            "json_exported" -> context.getString(R.string.settings_json_exported)
            "csv_imported" -> context.getString(R.string.settings_csv_imported)
            "rate_saved" -> context.getString(R.string.settings_rate_saved)
            else -> key
        }
        snackbar.showSnackbar(msg)
        onClearSnackbar()
    }

    val title = when (state.section) {
        SettingsSection.ROOT -> stringResource(R.string.settings_title)
        SettingsSection.GENERAL -> stringResource(R.string.settings_general)
        SettingsSection.SECURITY -> stringResource(R.string.settings_security)
        SettingsSection.NOTIFICATIONS -> stringResource(R.string.settings_notifications)
        SettingsSection.RECEIPTS -> stringResource(R.string.settings_receipts)
        SettingsSection.BACKUP -> stringResource(R.string.settings_backup)
        SettingsSection.DATA -> stringResource(R.string.settings_data)
        SettingsSection.CSV_IMPORT -> stringResource(R.string.settings_csv_import)
        SettingsSection.CATEGORIES -> stringResource(R.string.settings_categories_accounts)
        SettingsSection.AUTOMATION -> stringResource(R.string.settings_automation)
        SettingsSection.ABOUT -> stringResource(R.string.settings_about)
        SettingsSection.EXCHANGE -> stringResource(R.string.settings_exchange)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    TextButton(onClick = onBackRoot) {
                        Text(stringResource(R.string.settings_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when (state.section) {
            SettingsSection.ROOT -> RootList(
                padding = padding,
                onOpen = onOpen,
                isDebug = state.isDebug,
            )
            SettingsSection.GENERAL -> GeneralSection(
                padding, state, onSetBaseCurrency, onSetTheme, onSetDynamicColor,
            )
            SettingsSection.SECURITY -> SecuritySection(padding, state, onSetLock)
            SettingsSection.NOTIFICATIONS -> NotificationsSection(
                padding, state, onSetReminders, onSetDefaultRemindDays, onSetCaptureEnabled,
                onOpenCaptureGrant, onRefreshCapture, onSetAllowlistNone, onSetAllowlistDefault,
                onToggleBanking,
            )
            SettingsSection.RECEIPTS -> ReceiptsSection(padding, state, onDownloadNano)
            SettingsSection.BACKUP -> BackupSection(
                padding, state, onExportBackup, onRequestRestore, onDismissRestore, onRestore,
                onScheduleAutoBackup, onCancelAutoBackup,
            )
            SettingsSection.DATA -> DataSection(
                padding, onExportCsv, onExportJson, onLoadCsv,
                onOpenExchange = { onOpen(SettingsSection.EXCHANGE) },
            )
            SettingsSection.CSV_IMPORT -> CsvImportSection(
                padding, state, onSetCsvMapping, onPreviewCsv, onImportCsv,
            )
            SettingsSection.CATEGORIES -> CategoriesSection(padding, state)
            SettingsSection.AUTOMATION -> AutomationSection(
                padding, state, onSetSilent, onOpenAutomationDocs,
            )
            SettingsSection.ABOUT -> AboutSection(padding, state, onOpenDebugGallery)
            SettingsSection.EXCHANGE -> ExchangeSection(padding, state, onSetManualRate)
        }
    }
}

@Composable
private fun RootList(
    padding: PaddingValues,
    onOpen: (SettingsSection) -> Unit,
    isDebug: Boolean,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(MaterialTheme.spacing.gutter),
    ) {
        item { NavRow(stringResource(R.string.settings_general)) { onOpen(SettingsSection.GENERAL) } }
        item { NavRow(stringResource(R.string.settings_security)) { onOpen(SettingsSection.SECURITY) } }
        item { NavRow(stringResource(R.string.settings_notifications)) { onOpen(SettingsSection.NOTIFICATIONS) } }
        item { NavRow(stringResource(R.string.settings_receipts)) { onOpen(SettingsSection.RECEIPTS) } }
        item { NavRow(stringResource(R.string.settings_backup)) { onOpen(SettingsSection.BACKUP) } }
        item { NavRow(stringResource(R.string.settings_data)) { onOpen(SettingsSection.DATA) } }
        item { NavRow(stringResource(R.string.settings_categories_accounts)) { onOpen(SettingsSection.CATEGORIES) } }
        item { NavRow(stringResource(R.string.settings_automation)) { onOpen(SettingsSection.AUTOMATION) } }
        item { NavRow(stringResource(R.string.settings_about)) { onOpen(SettingsSection.ABOUT) } }
        if (isDebug) {
            // gallery is opened from About; keep root clean
        }
    }
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = MaterialTheme.spacing.rowPadding),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = MaterialTheme.spacing.tight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun GeneralSection(
    padding: PaddingValues,
    state: SettingsUiState,
    onSetBaseCurrency: (String) -> Unit,
    onSetTheme: (String) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var currency by remember(state.baseCurrency) { mutableStateOf(state.baseCurrency.value) }
    Column(
        Modifier.fillMaxSize().padding(padding).padding(MaterialTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.group),
    ) {
        OutlinedTextField(
            value = currency,
            onValueChange = { currency = it.uppercase() },
            label = { Text(stringResource(R.string.settings_base_currency)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Button(onClick = { onSetBaseCurrency(currency) }) {
            Text(stringResource(R.string.settings_save))
        }
        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.tight)) {
            listOf("system", "light", "dark").forEach { theme ->
                val label = when (theme) {
                    "light" -> stringResource(R.string.settings_theme_light)
                    "dark" -> stringResource(R.string.settings_theme_dark)
                    else -> stringResource(R.string.settings_theme_system)
                }
                OutlinedButton(onClick = { onSetTheme(theme) }) { Text(label) }
            }
        }
        SwitchRow(stringResource(R.string.settings_dynamic_color), state.dynamicColor, onSetDynamicColor)
        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.tight)) {
            OutlinedButton(onClick = { setAppLocales(context, "en") }) {
                Text(stringResource(R.string.settings_lang_en))
            }
            OutlinedButton(onClick = { setAppLocales(context, "it") }) {
                Text(stringResource(R.string.settings_lang_it))
            }
            OutlinedButton(onClick = { setAppLocales(context, null) }) {
                Text(stringResource(R.string.settings_lang_system))
            }
        }
    }
}

private fun setAppLocales(context: android.content.Context, tag: String?) {
    if (Build.VERSION.SDK_INT >= 33) {
        val lm = context.getSystemService(LocaleManager::class.java)
        lm.applicationLocales =
            if (tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
    } else {
        context.startActivity(
            Intent(PlatformSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            },
        )
    }
}

@Composable
private fun SecuritySection(
    padding: PaddingValues,
    state: SettingsUiState,
    onSetLock: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(padding).padding(MaterialTheme.spacing.gutter)) {
        SwitchRow(stringResource(R.string.settings_biometric_lock), state.lockEnabled, onSetLock)
    }
}

@Composable
private fun NotificationsSection(
    padding: PaddingValues,
    state: SettingsUiState,
    onSetReminders: (Boolean) -> Unit,
    onSetDefaultRemindDays: (Int) -> Unit,
    onSetCaptureEnabled: (Boolean) -> Unit,
    onOpenCaptureGrant: () -> Unit,
    onRefreshCapture: () -> Unit,
    onSetAllowlistNone: () -> Unit,
    onSetAllowlistDefault: () -> Unit,
    onToggleBanking: (String, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val notifPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) { onRefreshCapture() }
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(MaterialTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.tight),
    ) {
        item {
            SwitchRow(stringResource(R.string.settings_renewal_reminders), state.remindersEnabled, onSetReminders)
        }
        item {
            var days by remember(state.defaultRemindDays) {
                mutableStateOf(state.defaultRemindDays.toString())
            }
            OutlinedTextField(
                value = days,
                onValueChange = { days = it.filter { ch -> ch.isDigit() } },
                label = { Text(stringResource(R.string.settings_remind_days)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { days.toIntOrNull()?.let(onSetDefaultRemindDays) }) {
                Text(stringResource(R.string.settings_save))
            }
        }
        item {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 33) {
                    notifPerm.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }) { Text(stringResource(R.string.settings_request_notifications)) }
        }
        item {
            SectionHeader(title = stringResource(R.string.settings_capture))
            SwitchRow(stringResource(R.string.settings_capture_enable), state.captureEnabled, onSetCaptureEnabled)
            if (!state.captureGranted) {
                OutlinedButton(onClick = onOpenCaptureGrant) {
                    Text(stringResource(R.string.settings_capture_grant))
                }
            } else {
                Text(
                    stringResource(R.string.settings_capture_granted),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.tight)) {
                OutlinedButton(onClick = onSetAllowlistDefault) {
                    Text(stringResource(R.string.settings_capture_defaults))
                }
                OutlinedButton(onClick = onSetAllowlistNone) {
                    Text(stringResource(R.string.settings_capture_none))
                }
            }
        }
        items(state.bankingApps, key = { it.packageName }) { app ->
            val checked = !state.allowlistNone && (
                state.allowlist.isEmpty() || app.packageName in state.allowlist
                )
            SwitchRow(app.label, checked) { onToggleBanking(app.packageName, it) }
        }
    }
}

@Composable
private fun ReceiptsSection(
    padding: PaddingValues,
    state: SettingsUiState,
    onDownloadNano: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(MaterialTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.group),
    ) {
        val status = when (state.nanoStatus) {
            NanoStatusUi.AVAILABLE -> stringResource(R.string.settings_nano_available)
            NanoStatusUi.DOWNLOADABLE -> stringResource(R.string.settings_nano_downloadable)
            NanoStatusUi.DOWNLOADING -> stringResource(R.string.settings_nano_downloading)
            NanoStatusUi.UNSUPPORTED -> stringResource(R.string.settings_nano_unsupported)
        }
        Text(stringResource(R.string.settings_gemini_nano), style = MaterialTheme.typography.titleSmall)
        Text(status, style = MaterialTheme.typography.bodyMedium)
        if (state.nanoStatus == NanoStatusUi.DOWNLOADABLE) {
            Button(onClick = onDownloadNano) { Text(stringResource(R.string.settings_nano_download)) }
        }
        Text(
            stringResource(R.string.settings_ocr_always),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BackupSection(
    padding: PaddingValues,
    state: SettingsUiState,
    onExportBackup: (File, CharArray) -> Unit,
    onRequestRestore: () -> Unit,
    onDismissRestore: () -> Unit,
    onRestore: (File, CharArray) -> Unit,
    onScheduleAutoBackup: (String, CharArray, Int) -> Unit,
    onCancelAutoBackup: () -> Unit,
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var pendingRestore by remember { mutableStateOf<File?>(null) }
    var freq by remember(state.autoBackupFreqDays) { mutableStateOf(state.autoBackupFreqDays.toString()) }

    val createDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri: Uri? ->
        if (uri == null || password.isEmpty()) return@rememberLauncherForActivityResult
        val tmp = File(context.cacheDir, "tameki-backup.tameki")
        onExportBackup(tmp, password.toCharArray())
        context.contentResolver.openOutputStream(uri)?.use { out ->
            tmp.inputStream().use { it.copyTo(out) }
        }
    }
    val openDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val tmp = File(context.cacheDir, "tameki-restore.tameki")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tmp).use { input.copyTo(it) }
        }
        pendingRestore = tmp
        onRequestRestore()
    }
    val openTree = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri == null || password.isEmpty()) return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
        onScheduleAutoBackup(uri.toString(), password.toCharArray(), freq.toIntOrNull() ?: 7)
    }

    Column(
        Modifier.fillMaxSize().padding(padding).padding(MaterialTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.group),
    ) {
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.settings_backup_password)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { createDoc.launch("tameki-backup.tameki") }) {
            Text(stringResource(R.string.settings_export_now))
        }
        OutlinedButton(onClick = { openDoc.launch(arrayOf("*/*")) }) {
            Text(stringResource(R.string.settings_restore))
        }
        SectionHeader(title = stringResource(R.string.settings_auto_backup))
        OutlinedTextField(
            value = freq,
            onValueChange = { freq = it.filter { ch -> ch.isDigit() } },
            label = { Text(stringResource(R.string.settings_auto_backup_freq)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { openTree.launch(null) }) {
            Text(stringResource(R.string.settings_auto_backup_folder))
        }
        if (state.autoBackupUri != null) {
            OutlinedButton(onClick = onCancelAutoBackup) {
                Text(stringResource(R.string.settings_auto_backup_cancel))
            }
        }
        state.autoBackupLastSuccess?.let {
            Text(
                stringResource(R.string.settings_auto_backup_ok, it.toString()),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        state.autoBackupLastFailure?.let {
            Text(
                stringResource(
                    R.string.settings_auto_backup_fail,
                    state.autoBackupLastFailureReason ?: it.toString(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (state.confirmRestore && pendingRestore != null) {
        AlertDialog(
            onDismissRequest = {
                onDismissRestore()
                pendingRestore = null
            },
            title = { Text(stringResource(R.string.settings_restore_confirm_title)) },
            text = { Text(stringResource(R.string.settings_restore_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    val file = pendingRestore
                    pendingRestore = null
                    if (file != null) onRestore(file, password.toCharArray())
                }) { Text(stringResource(R.string.settings_restore)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDismissRestore()
                    pendingRestore = null
                }) { Text(stringResource(R.string.settings_cancel)) }
            },
        )
    }
}

@Composable
private fun DataSection(
    padding: PaddingValues,
    onExportCsv: (File) -> Unit,
    onExportJson: (File) -> Unit,
    onLoadCsv: (File, List<String>) -> Unit,
    onOpenExchange: () -> Unit,
) {
    val context = LocalContext.current
    val createCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val tmp = File(context.cacheDir, "export.csv")
        onExportCsv(tmp)
        context.contentResolver.openOutputStream(uri)?.use { out -> tmp.inputStream().use { it.copyTo(out) } }
    }
    val createJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val tmp = File(context.cacheDir, "export.json")
        onExportJson(tmp)
        context.contentResolver.openOutputStream(uri)?.use { out -> tmp.inputStream().use { it.copyTo(out) } }
    }
    val openCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val tmp = File(context.cacheDir, "import.csv")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tmp).use { input.copyTo(it) }
        }
        val headers = tmp.bufferedReader().use { it.readLine() }?.split(',')?.map { it.trim() }.orEmpty()
        onLoadCsv(tmp, headers)
    }
    Column(
        Modifier.fillMaxSize().padding(padding).padding(MaterialTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.group),
    ) {
        Button(onClick = { openCsv.launch(arrayOf("text/*", "text/csv")) }) {
            Text(stringResource(R.string.settings_import_csv))
        }
        OutlinedButton(onClick = { createCsv.launch("tameki-export.csv") }) {
            Text(stringResource(R.string.settings_export_csv))
        }
        OutlinedButton(onClick = { createJson.launch("tameki-export.json") }) {
            Text(stringResource(R.string.settings_export_json))
        }
        OutlinedButton(onClick = onOpenExchange) {
            Text(stringResource(R.string.settings_exchange))
        }
    }
}

@Composable
private fun CsvImportSection(
    padding: PaddingValues,
    state: SettingsUiState,
    onSetCsvMapping: (String, String?) -> Unit,
    onPreviewCsv: () -> Unit,
    onImportCsv: () -> Unit,
) {
    val columns = listOf(
        CsvColumns.DATE, CsvColumns.AMOUNT, CsvColumns.CURRENCY, CsvColumns.MERCHANT,
        CsvColumns.CATEGORY, CsvColumns.NOTE, CsvColumns.ACCOUNT, CsvColumns.KIND, CsvColumns.TAGS,
    )
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(MaterialTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.tight),
    ) {
        items(columns) { col ->
            var value by remember(state.csvMapping[col]) {
                mutableStateOf(state.csvMapping[col].orEmpty())
            }
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                    onSetCsvMapping(col, it.ifBlank { null })
                },
                label = { Text(col) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    if (state.csvHeaders.isNotEmpty()) {
                        Text(state.csvHeaders.joinToString(", "))
                    }
                },
            )
        }
        item {
            Button(onClick = onPreviewCsv) { Text(stringResource(R.string.settings_csv_preview)) }
        }
        state.csvPreview?.let { preview ->
            item {
                Text(
                    stringResource(R.string.settings_csv_preview_count, preview.rows.size, preview.problems.size),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            items(preview.problems.take(5)) { problem ->
                Text(
                    "${problem.lineNumber}: ${problem.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            item {
                Button(onClick = onImportCsv) { Text(stringResource(R.string.settings_csv_import_action)) }
            }
        }
    }
}

@Composable
private fun CategoriesSection(padding: PaddingValues, state: SettingsUiState) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(MaterialTheme.spacing.gutter),
    ) {
        item { SectionHeader(title = stringResource(R.string.settings_accounts)) }
        items(state.accounts, key = { "a-${it.id}" }) { account ->
            Text(
                "${account.name} · ${account.currency.value}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.tight),
            )
        }
        item { SectionHeader(title = stringResource(R.string.settings_categories)) }
        items(state.categories, key = { "c-${it.id}" }) { category ->
            Text(
                category.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.tight),
            )
        }
    }
}

@Composable
private fun AutomationSection(
    padding: PaddingValues,
    state: SettingsUiState,
    onSetSilent: (Boolean) -> Unit,
    onOpenAutomationDocs: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(MaterialTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.group),
    ) {
        SwitchRow(stringResource(R.string.settings_silent_saves), state.silentAutomation, onSetSilent)
        TextButton(onClick = onOpenAutomationDocs) {
            Text(stringResource(R.string.settings_automation_docs))
        }
    }
}

@Composable
private fun AboutSection(
    padding: PaddingValues,
    state: SettingsUiState,
    onOpenDebugGallery: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(MaterialTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.group),
    ) {
        Text(stringResource(R.string.settings_version, state.versionName))
        Text(stringResource(R.string.settings_license))
        Text(
            stringResource(R.string.settings_source),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (state.isDebug) {
            OutlinedButton(onClick = onOpenDebugGallery) {
                Text(stringResource(R.string.settings_debug_gallery))
            }
        }
    }
}

@Composable
private fun ExchangeSection(
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
        OutlinedTextField(from, { from = it.uppercase() }, label = { Text("From") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(to, { to = it.uppercase() }, label = { Text("To") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(rate, { rate = it }, label = { Text(stringResource(R.string.settings_rate)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onSetManualRate(from, to, rate) }) {
            Text(stringResource(R.string.settings_save))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F0EA)
@Composable
private fun SettingsRootPreview() {
    TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
        SettingsScreen(
            state = sampleSettingsState(),
            onOpen = {},
            onBackRoot = {},
            onSetBaseCurrency = {},
            onSetTheme = {},
            onSetDynamicColor = {},
            onSetLock = {},
            onSetReminders = {},
            onSetDefaultRemindDays = {},
            onSetCaptureEnabled = {},
            onOpenCaptureGrant = {},
            onRefreshCapture = {},
            onSetAllowlistNone = {},
            onSetAllowlistDefault = {},
            onToggleBanking = { _, _ -> },
            onDownloadNano = {},
            onExportBackup = { _, _ -> },
            onRequestRestore = {},
            onDismissRestore = {},
            onRestore = { _, _ -> },
            onScheduleAutoBackup = { _, _, _ -> },
            onCancelAutoBackup = {},
            onExportCsv = {},
            onExportJson = {},
            onLoadCsv = { _, _ -> },
            onSetCsvMapping = { _, _ -> },
            onPreviewCsv = {},
            onImportCsv = {},
            onSetManualRate = { _, _, _ -> },
            onSetSilent = {},
            onOpenAutomationDocs = {},
            onOpenDebugGallery = {},
            onClearSnackbar = {},
        )
    }
}

internal fun sampleSettingsState() = SettingsUiState(
    baseCurrency = CurrencyCode("EUR"),
    versionName = "0.1.0",
)
