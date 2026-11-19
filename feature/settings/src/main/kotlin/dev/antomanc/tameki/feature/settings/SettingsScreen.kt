package dev.antomanc.tameki.feature.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.ui.components.SectionHeader
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.spacing
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Currency
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onOpenAutomationDocs: () -> Unit = {},
    onOpenDebugGallery: () -> Unit = {},
    onOpenAi: () -> Unit = {},
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
        onUpsertAccount = viewModel::upsertAccount,
        onArchiveAccount = viewModel::archiveAccount,
        onUpsertCategory = viewModel::upsertCategory,
        onArchiveCategory = viewModel::archiveCategory,
        onOpenAutomationDocs = onOpenAutomationDocs,
        onOpenDebugGallery = onOpenDebugGallery,
        onOpenAi = onOpenAi,
        onClearSnackbar = viewModel::clearSnackbar,
        onRescheduleAutoBackup = viewModel::rescheduleAutoBackup,
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
    /** Suspends until the file is complete; the caller copies it only after a true result. */
    onExportBackup: suspend (File, CharArray, suspend (File) -> Boolean) -> Boolean,
    onRequestRestore: () -> Unit,
    onDismissRestore: () -> Unit,
    onRestore: (File, CharArray) -> Unit,
    onScheduleAutoBackup: (String, CharArray, Int) -> Unit,
    onCancelAutoBackup: () -> Unit,
    onExportCsv: suspend (File, suspend (File) -> Boolean) -> Boolean,
    onExportJson: suspend (File, suspend (File) -> Boolean) -> Boolean,
    onLoadCsv: (File, List<String>) -> Unit,
    onSetCsvMapping: (String, String?) -> Unit,
    onPreviewCsv: () -> Unit,
    onImportCsv: () -> Unit,
    onSetManualRate: (String, String, String) -> Unit,
    onSetSilent: (Boolean) -> Unit,
    onRescheduleAutoBackup: (Int) -> Unit = {},
    onUpsertAccount: (dev.antomanc.tameki.core.model.entity.Account) -> Unit = {},
    onArchiveAccount: (Long, Boolean) -> Unit = { _, _ -> },
    onUpsertCategory: (dev.antomanc.tameki.core.model.entity.Category) -> Unit = {},
    onArchiveCategory: (Long, Boolean) -> Unit = { _, _ -> },
    onOpenAutomationDocs: () -> Unit,
    onOpenDebugGallery: () -> Unit,
    onOpenAi: () -> Unit = {},
    onClearSnackbar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val snackbarMessage = when (state.snackbar) {
        SettingsSnackbarMessage.BACKUP_EXPORTED -> stringResource(R.string.settings_backup_exported)
        SettingsSnackbarMessage.BACKUP_RESTORED -> stringResource(R.string.settings_backup_restored)
        SettingsSnackbarMessage.BACKUP_FAILED -> stringResource(R.string.settings_backup_failed)
        SettingsSnackbarMessage.AUTO_BACKUP_ON -> stringResource(R.string.settings_auto_backup_on)
        SettingsSnackbarMessage.CSV_EXPORTED -> stringResource(R.string.settings_csv_exported)
        SettingsSnackbarMessage.JSON_EXPORTED -> stringResource(R.string.settings_json_exported)
        SettingsSnackbarMessage.EXPORT_FAILED -> stringResource(R.string.settings_export_failed)
        SettingsSnackbarMessage.CSV_IMPORTED -> stringResource(R.string.settings_csv_imported)
        SettingsSnackbarMessage.CSV_FAILED -> stringResource(R.string.settings_csv_failed)
        SettingsSnackbarMessage.RATE_SAVED -> stringResource(R.string.settings_rate_saved)
        SettingsSnackbarMessage.RATE_INVALID -> stringResource(R.string.settings_rate_invalid)
        null -> null
    }
    LaunchedEffect(state.snackbar) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        onClearSnackbar()
    }

    // Each section is an internal sub-page, not a navigation destination. System and predictive
    // back must go up one level exactly like the top-bar arrow, never leave Settings entirely.
    BackHandler(enabled = state.section != SettingsSection.ROOT) { onBackRoot() }

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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
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
        // Sub-pages move along the shared X axis: deeper pages enter from the end, Back returns
        // from the start, like every other pushed screen in the app.
        val slide = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
        val fadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        AnimatedContent(
            targetState = state.section,
            transitionSpec = {
                val forward = targetState.depth > initialState.depth
                (slideInHorizontally(slide) { w -> if (forward) w / 6 else -w / 6 } + fadeIn(fadeSpec))
                    .togetherWith(slideOutHorizontally(slide) { w -> if (forward) -w / 10 else w / 10 } + fadeOut(fadeSpec))
            },
            label = "settings-section",
        ) { section ->
        when (section) {
            SettingsSection.ROOT -> RootList(
                padding = padding,
                state = state,
                onOpen = onOpen,
                onSetLock = onSetLock,
                onOpenAi = onOpenAi,
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
                onScheduleAutoBackup, onCancelAutoBackup, onRescheduleAutoBackup,
            )
            SettingsSection.DATA -> DataSection(
                padding, onExportCsv, onExportJson, onLoadCsv,
                onOpenExchange = { onOpen(SettingsSection.EXCHANGE) },
            )
            SettingsSection.CSV_IMPORT -> CsvImportSection(
                padding, state, onSetCsvMapping, onPreviewCsv, onImportCsv,
            )
            SettingsSection.CATEGORIES -> CategoriesAccountsEditor(
                padding = padding,
                state = state,
                onUpsertAccount = onUpsertAccount,
                onArchiveAccount = onArchiveAccount,
                onUpsertCategory = onUpsertCategory,
                onArchiveCategory = onArchiveCategory,
            )
            SettingsSection.AUTOMATION -> AutomationSection(
                padding, state, onSetSilent, onOpenAutomationDocs,
            )
            SettingsSection.ABOUT -> AboutSection(padding, state, onOpenDebugGallery)
            SettingsSection.EXCHANGE -> ExchangeSection(padding, state, onSetManualRate)
        }
        }
    }
}

@Composable
internal fun RootList(
    padding: PaddingValues,
    state: SettingsUiState,
    onOpen: (SettingsSection) -> Unit,
    onSetLock: (Boolean) -> Unit,
    onOpenAi: () -> Unit = {},
) {
    val spacing = MaterialTheme.spacing
    val lockAvailable = rememberLockAvailable()
    val lockSupporting = when {
        state.lockEnabled -> stringResource(R.string.settings_active)
        !lockAvailable -> stringResource(R.string.settings_biometric_unavailable)
        else -> stringResource(R.string.settings_inactive)
    }
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
            formatRelativeBackup(state.autoBackupLastSuccess, state.now),
        )
        else -> stringResource(R.string.settings_active)
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(bottom = spacing.section),
    ) {
        item {
            SettingsNavItem(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.settings_general),
                supporting = "$currencyLabel · $themeLabel",
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
                supporting = lockSupporting,
                checked = state.lockEnabled,
                onChecked = onSetLock,
                // Without an enrolled fingerprint/PIN the lock would protect nothing: never let
                // the user turn on a lock that cannot ask for authentication. Turning it off
                // stays available if the credential was removed later.
                enabled = state.lockEnabled || lockAvailable,
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
            SectionHeader(
                title = stringResource(R.string.settings_receipts_ai),
                modifier = Modifier.padding(horizontal = spacing.gutter),
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
            SettingsNavItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_ai),
                supporting = null,
                onClick = onOpenAi,
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
                title = stringResource(R.string.settings_data),
                supporting = stringResource(R.string.settings_data_value),
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
internal fun SettingsNavItem(
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
        modifier = Modifier
            .heightIn(min = MaterialTheme.spacing.touchTarget)
            .clickable(onClick = onClick),
    )
}

@Composable
internal fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    supporting: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(supporting) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier
            .heightIn(min = MaterialTheme.spacing.touchTarget)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onChecked,
            ),
    )
}

@Composable
internal fun currencyDisplayName(code: CurrencyCode): String {
    if (code.value == "EUR") return stringResource(R.string.settings_currency_eur)
    return runCatching {
        val cur = Currency.getInstance(code.value)
        "${cur.displayName} (${cur.symbol})"
    }.getOrElse { code.value }
}

@Composable
internal fun formatRelativeBackup(instant: Instant, now: Instant): String {
    val zone = ZoneId.systemDefault()
    val local = instant.atZone(zone)
    val today = now.atZone(zone)
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(local)
    val days = ChronoUnit.DAYS.between(local.toLocalDate(), today.toLocalDate())
    return when (days) {
        0L -> stringResource(R.string.settings_backup_relative_today, time)
        1L -> stringResource(R.string.settings_backup_relative_yesterday, time)
        else -> DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(local)
    }
}

@Composable
internal fun SwitchRow(
    label: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    supporting: String? = null,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.spacing.touchTarget)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onChecked,
            )
            .padding(vertical = MaterialTheme.spacing.tight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F0EA)
@Composable
internal fun SettingsRootPreview() {
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
            onExportBackup = { _, _, _ -> true },
            onRequestRestore = {},
            onDismissRestore = {},
            onRestore = { _, _ -> },
            onScheduleAutoBackup = { _, _, _ -> },
            onCancelAutoBackup = {},
            onExportCsv = { _, _ -> true },
            onExportJson = { _, _ -> true },
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
    now = Instant.parse("2026-11-18T10:00:00Z"),
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
