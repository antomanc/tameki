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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.repository.CsvColumns
import dev.antomanc.tameki.core.ui.components.SectionHeader
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.spacing
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Currency
import java.util.Locale
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
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = onBackRoot) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_navigate_up),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when (state.section) {
            SettingsSection.ROOT -> RootList(
                padding = padding,
                state = state,
                onOpen = onOpen,
                onSetLock = onSetLock,
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
    state: SettingsUiState,
    onOpen: (SettingsSection) -> Unit,
    onSetLock: (Boolean) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val currencyLabel = currencyDisplayName(state.baseCurrency)
    val themeLabel = when (state.theme) {
        "light" -> stringResource(R.string.settings_theme_light)
        "dark" -> stringResource(R.string.settings_theme_dark)
        else -> stringResource(R.string.settings_theme_system)
    }
    val captureSupporting = when {
        !state.captureEnabled || state.allowlistNone -> stringResource(R.string.settings_capture_none_value)
        else -> {
            val n = if (state.allowlist.isEmpty()) state.bankingApps.size else state.allowlist.size
            stringResource(R.string.settings_capture_apps, n.coerceAtLeast(0))
        }
    }
    val nanoLabel = when (state.nanoStatus) {
        NanoStatusUi.AVAILABLE -> stringResource(R.string.settings_nano_available)
        NanoStatusUi.DOWNLOADABLE -> stringResource(R.string.settings_nano_downloadable)
        NanoStatusUi.DOWNLOADING -> stringResource(R.string.settings_nano_downloading)
        NanoStatusUi.UNSUPPORTED -> stringResource(R.string.settings_nano_unsupported)
    }
    val backupSupporting = when {
        state.autoBackupUri == null -> stringResource(R.string.settings_backup_auto_off)
        state.autoBackupLastSuccess != null -> stringResource(
            R.string.settings_backup_auto_last,
            formatRelativeBackup(state.autoBackupLastSuccess),
        )
        else -> stringResource(R.string.settings_active)
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(bottom = spacing.section),
    ) {
        item {
            SectionHeader(
                title = stringResource(R.string.settings_general),
                modifier = Modifier.padding(horizontal = spacing.gutter),
            )
        }
        item {
            SettingsNavItem(
                icon = Icons.Default.CurrencyExchange,
                title = stringResource(R.string.settings_base_currency),
                supporting = currencyLabel,
                onClick = { onOpen(SettingsSection.GENERAL) },
            )
        }
        item {
            SettingsNavItem(
                icon = Icons.Default.DarkMode,
                title = stringResource(R.string.settings_theme),
                supporting = themeLabel,
                onClick = { onOpen(SettingsSection.GENERAL) },
            )
        }
        item {
            SectionHeader(
                title = stringResource(R.string.settings_security_privacy),
                modifier = Modifier.padding(horizontal = spacing.gutter),
            )
        }
        item {
            SettingsSwitchItem(
                icon = Icons.Default.Fingerprint,
                title = stringResource(R.string.settings_biometric_lock),
                supporting = stringResource(
                    if (state.lockEnabled) R.string.settings_active else R.string.settings_inactive,
                ),
                checked = state.lockEnabled,
                onChecked = onSetLock,
            )
        }
        item {
            SectionHeader(
                title = stringResource(R.string.settings_notifications),
                modifier = Modifier.padding(horizontal = spacing.gutter),
            )
        }
        item {
            SettingsNavItem(
                icon = Icons.Default.Payments,
                title = stringResource(R.string.settings_capture_enable),
                supporting = captureSupporting,
                onClick = { onOpen(SettingsSection.NOTIFICATIONS) },
            )
        }
        item {
            SettingsNavItem(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = stringResource(R.string.settings_receipts),
                supporting = nanoLabel,
                onClick = { onOpen(SettingsSection.RECEIPTS) },
            )
        }
        item {
            SectionHeader(
                title = stringResource(R.string.settings_data),
                modifier = Modifier.padding(horizontal = spacing.gutter),
            )
        }
        item {
            SettingsNavItem(
                icon = Icons.Default.Backup,
                title = stringResource(R.string.settings_auto_backup),
                supporting = backupSupporting,
                onClick = { onOpen(SettingsSection.BACKUP) },
            )
        }
        item {
            SettingsNavItem(
                icon = Icons.Default.Upload,
                title = stringResource(R.string.settings_import_csv),
                supporting = null,
                onClick = { onOpen(SettingsSection.DATA) },
            )
        }
        item {
            SettingsNavItem(
                icon = Icons.Default.Download,
                title = stringResource(R.string.settings_export_data),
                supporting = null,
                onClick = { onOpen(SettingsSection.DATA) },
            )
        }
        item {
            SettingsNavItem(
                icon = Icons.Default.Category,
                title = stringResource(R.string.settings_categories_accounts),
                supporting = null,
                onClick = { onOpen(SettingsSection.CATEGORIES) },
            )
        }
        item {
            SectionHeader(
                title = stringResource(R.string.settings_other),
                modifier = Modifier.padding(horizontal = spacing.gutter),
            )
        }
        item {
            SettingsNavItem(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = stringResource(R.string.settings_automation),
                supporting = null,
                onClick = { onOpen(SettingsSection.AUTOMATION) },
            )
        }
        item {
            SettingsNavItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_about),
                supporting = "v${state.versionName}",
                onClick = { onOpen(SettingsSection.ABOUT) },
            )
        }
    }
}

@Composable
private fun SettingsNavItem(
    icon: ImageVector,
    title: String,
    supporting: String?,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = supporting?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    supporting: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(supporting) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChecked) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
private fun currencyDisplayName(code: CurrencyCode): String {
    if (code.value == "EUR") return stringResource(R.string.settings_currency_eur)
    return runCatching {
        val cur = Currency.getInstance(code.value)
        "${cur.displayName} (${cur.symbol})"
    }.getOrElse { code.value }
}

@Composable
private fun formatRelativeBackup(instant: Instant): String {
    val zone = ZoneId.systemDefault()
    val local = instant.atZone(zone)
    val now = Instant.now().atZone(zone)
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(local)
    val days = ChronoUnit.DAYS.between(local.toLocalDate(), now.toLocalDate())
    val locale = Locale.getDefault().language
    return when (days) {
        0L -> if (locale == "it") "oggi $time" else "today $time"
        1L -> if (locale == "it") "ieri $time" else "yesterday $time"
        else -> DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(local)
    }
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
        Button(onClick = { onSetBaseCurrency(currency) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_save))
        }
        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("system", "light", "dark").forEachIndexed { index, theme ->
                val label = when (theme) {
                    "light" -> stringResource(R.string.settings_theme_light)
                    "dark" -> stringResource(R.string.settings_theme_dark)
                    else -> stringResource(R.string.settings_theme_system)
                }
                SegmentedButton(
                    selected = state.theme == theme,
                    onClick = { onSetTheme(theme) },
                    shape = SegmentedButtonDefaults.itemShape(index, 3),
                ) { Text(label) }
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            TextButton(onClick = { days.toIntOrNull()?.let(onSetDefaultRemindDays) }) {
                Text(stringResource(R.string.settings_save))
            }
        }
        item {
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        notifPerm.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.settings_request_notifications)) }
        }
        item {
            SectionHeader(title = stringResource(R.string.settings_capture))
            SwitchRow(stringResource(R.string.settings_capture_enable), state.captureEnabled, onSetCaptureEnabled)
            if (!state.captureGranted) {
                OutlinedButton(onClick = onOpenCaptureGrant, modifier = Modifier.fillMaxWidth()) {
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
            Button(onClick = onDownloadNano, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_nano_download))
            }
        }
        Text(
            stringResource(R.string.settings_ocr_always),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private enum class BackupPasswordAction { EXPORT, RESTORE, AUTO }

@OptIn(ExperimentalMaterial3Api::class)
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
    val spacing = MaterialTheme.spacing
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordAction by remember { mutableStateOf<BackupPasswordAction?>(null) }
    var pendingRestore by remember { mutableStateOf<File?>(null) }
    var pendingFolder by remember { mutableStateOf<Uri?>(null) }
    var autoEnabled by remember(state.autoBackupUri) { mutableStateOf(state.autoBackupUri != null) }
    var freqDays by remember(state.autoBackupFreqDays) {
        mutableStateOf(
            when (state.autoBackupFreqDays) {
                1 -> 1
                30 -> 30
                else -> 7
            },
        )
    }

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
        passwordAction = BackupPasswordAction.RESTORE
    }
    val openTree = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingFolder = uri
        if (password.isNotEmpty()) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            onScheduleAutoBackup(uri.toString(), password.toCharArray(), freqDays)
            pendingFolder = null
        } else {
            passwordAction = BackupPasswordAction.AUTO
        }
    }

    val statusLine = when {
        state.autoBackupLastSuccess != null -> stringResource(
            R.string.settings_backup_last,
            formatBackupInstant(state.autoBackupLastSuccess),
        )
        else -> stringResource(R.string.settings_backup_none)
    }
    val folderName = state.autoBackupUri?.let { uri ->
        Uri.parse(uri).lastPathSegment?.substringAfterLast(':') ?: uri
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(spacing.group),
    ) {
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = statusLine,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(spacing.cardPadding),
                )
            }
        }
        item {
            Button(
                onClick = { passwordAction = BackupPasswordAction.EXPORT },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_export_now))
            }
            TextButton(
                onClick = { openDoc.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_restore))
            }
        }
        item {
            SectionHeader(title = stringResource(R.string.settings_auto_backup))
            SwitchRow(stringResource(R.string.settings_auto_backup), autoEnabled) { enabled ->
                autoEnabled = enabled
                if (!enabled) onCancelAutoBackup()
                else openTree.launch(null)
            }
            if (autoEnabled) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(1 to R.string.settings_auto_freq_daily, 7 to R.string.settings_auto_freq_weekly, 30 to R.string.settings_auto_freq_monthly)
                        .forEachIndexed { index, (days, label) ->
                            SegmentedButton(
                                selected = freqDays == days,
                                onClick = {
                                    freqDays = days
                                    val uri = state.autoBackupUri
                                    if (uri != null && password.isNotEmpty()) {
                                        onScheduleAutoBackup(uri, password.toCharArray(), days)
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, 3),
                            ) { Text(stringResource(label)) }
                        }
                }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_auto_backup_folder)) },
                    supportingContent = { Text(folderName ?: "—") },
                    leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                    trailingContent = {
                        TextButton(onClick = { openTree.launch(null) }) {
                            Text(stringResource(R.string.settings_auto_backup_folder_change))
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_backup_password)) },
                    supportingContent = {
                        Text(
                            if (password.isNotEmpty()) {
                                stringResource(R.string.settings_backup_password_set)
                            } else {
                                stringResource(R.string.settings_inactive)
                            },
                        )
                    },
                    trailingContent = {
                        TextButton(onClick = { passwordAction = BackupPasswordAction.AUTO }) {
                            Text(stringResource(R.string.settings_change))
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
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
    }

    val action = passwordAction
    if (action != null) {
        AlertDialog(
            onDismissRequest = { passwordAction = null },
            title = { Text(stringResource(R.string.settings_backup_password)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.tight)) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.settings_backup_password)) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = stringResource(
                                        if (passwordVisible) {
                                            R.string.settings_backup_password_hide
                                        } else {
                                            R.string.settings_backup_password_show
                                        },
                                    ),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(R.string.settings_backup_password_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (action) {
                            BackupPasswordAction.EXPORT -> {
                                passwordAction = null
                                createDoc.launch("tameki-backup.tameki")
                            }
                            BackupPasswordAction.RESTORE -> {
                                passwordAction = null
                                onRequestRestore()
                            }
                            BackupPasswordAction.AUTO -> {
                                val folder = pendingFolder
                                passwordAction = null
                                if (folder != null) {
                                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    context.contentResolver.takePersistableUriPermission(folder, flags)
                                    onScheduleAutoBackup(folder.toString(), password.toCharArray(), freqDays)
                                    pendingFolder = null
                                }
                            }
                        }
                    },
                    enabled = password.isNotEmpty(),
                ) { Text(stringResource(R.string.settings_save)) }
            },
            dismissButton = {
                TextButton(onClick = { passwordAction = null }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
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
private fun formatBackupInstant(instant: Instant): String {
    val local = instant.atZone(ZoneId.systemDefault())
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(local)
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
            Button(onClick = onPreviewCsv, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_csv_preview))
            }
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
                Button(onClick = onImportCsv, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_csv_import_action))
                }
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
            OutlinedButton(onClick = onOpenDebugGallery, modifier = Modifier.fillMaxWidth()) {
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
        )
        Button(onClick = { onSetManualRate(from, to, rate) }, modifier = Modifier.fillMaxWidth()) {
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
    theme = "system",
    lockEnabled = true,
    captureEnabled = true,
    bankingApps = listOf(
        BankingAppUi("Bank A", "bank.a"),
        BankingAppUi("Bank B", "bank.b"),
        BankingAppUi("Bank C", "bank.c"),
    ),
    nanoStatus = NanoStatusUi.AVAILABLE,
    autoBackupUri = "content://tree/primary%3ATameki",
    autoBackupLastSuccess = Instant.parse("2026-11-17T21:10:00Z"),
)
