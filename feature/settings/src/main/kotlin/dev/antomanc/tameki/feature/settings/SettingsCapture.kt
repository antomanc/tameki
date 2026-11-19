package dev.antomanc.tameki.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.antomanc.tameki.core.model.capture.PaymentNotificationParser
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.ui.components.SectionHeader
import dev.antomanc.tameki.core.ui.components.SelectableChip
import dev.antomanc.tameki.core.ui.format.rememberMoneyFormatter
import dev.antomanc.tameki.core.ui.theme.spacing
import java.time.Instant

private val REMIND_DAY_PRESETS = listOf(1, 2, 3, 5, 7)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NotificationsSection(
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
    onAddCustomCaptureApp: (String) -> Unit = {},
    onRemoveCustomCaptureApp: (String) -> Unit = {},
) {
    val spacing = MaterialTheme.spacing
    val context = LocalContext.current
    var postNotifGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val notifPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> postNotifGranted = granted }

    var showAddAppDialog by remember { mutableStateOf(false) }
    var showTestDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { onRefreshCapture() }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(
            start = spacing.gutter,
            end = spacing.gutter,
            top = spacing.tight,
            bottom = spacing.section,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.between),
    ) {
        // Renewal reminders card with instant preset chips (no detached Save button)
        item(key = "reminders") {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(spacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(spacing.between),
                ) {
                    SwitchRow(
                        label = stringResource(R.string.settings_renewal_reminders),
                        checked = state.remindersEnabled,
                        onChecked = onSetReminders,
                    )
                    if (state.remindersEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.tight)) {
                            Text(
                                text = stringResource(R.string.settings_remind_days),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(spacing.tight),
                                verticalArrangement = Arrangement.spacedBy(spacing.tight),
                            ) {
                                REMIND_DAY_PRESETS.forEach { days ->
                                    SelectableChip(
                                        selected = state.defaultRemindDays == days,
                                        onClick = { onSetDefaultRemindDays(days) },
                                        label = stringResource(R.string.settings_remind_days_chip, days),
                                    )
                                }
                            }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= 33 && !postNotifGranted) {
                        FilledTonalButton(
                            onClick = { notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_request_notifications))
                        }
                    }
                }
            }
        }

        // Payment notification capture header + main control card
        item(key = "capture-header") {
            SectionHeader(title = stringResource(R.string.settings_capture))
        }
        item(key = "capture-card") {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(spacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(spacing.between),
                ) {
                    SwitchRow(
                        label = stringResource(R.string.settings_capture_enable),
                        checked = state.captureEnabled,
                        onChecked = onSetCaptureEnabled,
                    )
                    if (!state.captureGranted) {
                        FilledTonalButton(
                            onClick = onOpenCaptureGrant,
                            modifier = Modifier.fillMaxWidth().testTag("capture_grant_button"),
                        ) {
                            Text(stringResource(R.string.settings_capture_grant))
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                stringResource(R.string.settings_capture_granted),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.tight)) {
                        OutlinedButton(onClick = onSetAllowlistDefault) {
                            Text(stringResource(R.string.settings_capture_defaults))
                        }
                        OutlinedButton(onClick = onSetAllowlistNone) {
                            Text(stringResource(R.string.settings_capture_none))
                        }
                    }
                }
            }
        }

        // Installed known banking apps
        if (state.bankingApps.isNotEmpty()) {
            item(key = "banking-header") {
                SectionHeader(title = stringResource(R.string.settings_capture_detected_banks))
            }
            items(state.bankingApps, key = { "bank-${it.packageName}" }) { app ->
                val checked = !state.allowlistNone && (
                    state.allowlist.isEmpty() || app.packageName in state.allowlist
                    )
                SwitchRow(
                    label = app.label,
                    supporting = app.packageName,
                    checked = checked,
                    onChecked = { onToggleBanking(app.packageName, it) },
                )
            }
        }

        // Custom apps (user-added + recently observed notification senders)
        item(key = "custom-header") {
            SectionHeader(
                title = stringResource(R.string.settings_capture_custom_apps),
                supporting = stringResource(R.string.settings_capture_custom_apps_help),
            )
        }
        items(state.customCaptureApps, key = { "custom-${it.packageName}" }) { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = spacing.touchTarget),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { onRemoveCustomCaptureApp(app.packageName) }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.settings_capture_remove_app, app.label),
                    )
                }
            }
        }
        if (state.recentNotificationApps.isNotEmpty()) {
            item(key = "recent-apps") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.tight)) {
                    Text(
                        text = stringResource(R.string.settings_capture_recent_apps),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(spacing.tight),
                        verticalArrangement = Arrangement.spacedBy(spacing.tight),
                    ) {
                        state.recentNotificationApps.forEach { app ->
                            AssistChip(
                                onClick = { onAddCustomCaptureApp(app.packageName) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                                    )
                                },
                                label = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            )
                        }
                    }
                }
            }
        }
        item(key = "custom-actions") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.tight)) {
                OutlinedButton(
                    onClick = { showAddAppDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("capture_add_custom_app"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_capture_add_custom))
                }
                TextButton(
                    onClick = { showTestDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("capture_test_notification"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Science,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_capture_test_action))
                }
            }
        }
    }

    if (showAddAppDialog) {
        AddCustomCaptureAppDialog(
            onDismiss = { showAddAppDialog = false },
            onAdd = { input ->
                onAddCustomCaptureApp(input)
                showAddAppDialog = false
            },
        )
    }

    if (showTestDialog) {
        NotificationParserTestDialog(
            now = state.now,
            onDismiss = { showTestDialog = false },
        )
    }
}

@Composable
private fun AddCustomCaptureAppDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_capture_add_custom)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.settings_capture_add_custom_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(R.string.settings_capture_package_or_link)) },
                    placeholder = { Text("com.latuabancaperandroid") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("capture_custom_input"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(input) },
                enabled = input.isNotBlank(),
            ) { Text(stringResource(R.string.settings_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
private fun NotificationParserTestDialog(
    now: Instant,
    onDismiss: () -> Unit,
) {
    val formatter = rememberMoneyFormatter()
    var sample by remember {
        mutableStateOf(
            "💸 Hai pagato 105,99 € con la carta *1234 il 19.11 alle ore 09:25 da WWW.AMAZON.IT.",
        )
    }
    val parsed = remember(sample, now) {
        if (sample.isBlank()) null
        else PaymentNotificationParser.parse(
            packageName = "custom.test",
            title = "",
            text = sample,
            postedAt = now,
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_capture_test_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = sample,
                    onValueChange = { sample = it },
                    label = { Text(stringResource(R.string.settings_capture_test_label)) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("capture_test_input"),
                )
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (parsed != null) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val parsedAmount = parsed?.amount
                        if (parsed != null && parsedAmount != null) {
                            val kindLabel = stringResource(
                                if (parsed.kind == TransactionKind.INCOME) {
                                    R.string.settings_capture_test_income
                                } else {
                                    R.string.settings_capture_test_expense
                                },
                            )
                            Text(
                                text = "$kindLabel · ${formatter.format(parsedAmount)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = parsed.merchant ?: stringResource(R.string.settings_capture_test_no_merchant),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.settings_capture_test_ignored),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_capture_test_done)) }
        },
    )
}

@Composable
internal fun ReceiptsSection(
    padding: PaddingValues,
    state: SettingsUiState,
    onDownloadNano: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.gutter),
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
