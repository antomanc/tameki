package dev.antomanc.tameki.feature.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import dev.antomanc.tameki.core.ui.components.SectionHeader
import dev.antomanc.tameki.core.ui.theme.spacing
import kotlinx.coroutines.launch

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
            SwitchRow(
                label = app.label,
                checked = checked,
                onChecked = { onToggleBanking(app.packageName, it) },
            )
        }
    }
}

@Composable
internal fun ReceiptsSection(
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
