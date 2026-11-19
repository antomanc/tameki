package dev.antomanc.tameki.feature.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import dev.antomanc.tameki.core.ui.theme.spacing
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class BackupPasswordAction { EXPORT, RESTORE, AUTO }

/**
 * Export creates a secret the user must be able to type again later; restore only consumes one.
 * Pure so the rule is testable without a dialog: non-empty and identical.
 */
internal fun backupPasswordReady(password: String, confirmation: String): Boolean =
    password.isNotEmpty() && password == confirmation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupSection(
    padding: PaddingValues,
    state: SettingsUiState,
    onExportBackup: suspend (File, CharArray, suspend (File) -> Boolean) -> Boolean,
    onRequestRestore: () -> Unit,
    onDismissRestore: () -> Unit,
    onRestore: (File, CharArray) -> Unit,
    onScheduleAutoBackup: (String, CharArray, Int) -> Unit,
    onCancelAutoBackup: () -> Unit,
    onRescheduleAutoBackup: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val spacing = MaterialTheme.spacing
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordAction by remember { mutableStateOf<BackupPasswordAction?>(null) }
    var pendingRestore by remember { mutableStateOf<File?>(null) }
    var pendingFolder by remember { mutableStateOf<Uri?>(null) }
    // The store survives process death and rotation; the text field does not. "Password set" and
    // the frequency switch must read the store, or a configured backup looks unconfigured.
    val passwordSet = password.isNotEmpty() || state.autoBackupPasswordStored
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
    val scope = rememberCoroutineScope()

    val createDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri: Uri? ->
        if (uri == null || password.isEmpty()) return@rememberLauncherForActivityResult
        val secret = password.toCharArray()
        scope.launch {
            val tmp = File(context.cacheDir, "tameki-backup-${System.nanoTime()}.tameki")
            try {
                // Wait for the write: copying while BackupService still streams the ZIP raced it.
                onExportBackup(tmp, secret) { copyExport(context, uri, it) }
            } finally {
                secret.fill('\u0000')
                tmp.delete()
            }
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
        if (uri == null) {
            // The picker was cancelled: the switch goes back to what is actually scheduled.
            autoEnabled = state.autoBackupUri != null
            return@rememberLauncherForActivityResult
        }
        pendingFolder = uri
        if (password.isNotEmpty()) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
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
            Row(
                Modifier.fillMaxWidth().padding(vertical = MaterialTheme.spacing.tight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.settings_auto_backup),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = autoEnabled,
                    onCheckedChange = { enabled ->
                        autoEnabled = enabled
                        if (!enabled) onCancelAutoBackup()
                        else openTree.launch(null)
                    },
                )
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
                                    when {
                                        uri == null -> Unit
                                        password.isNotEmpty() -> onScheduleAutoBackup(
                                            uri,
                                            password.toCharArray(),
                                            days,
                                        )
                                        // A password may already be in the encrypted store: let the
                                        // ViewModel reschedule with it instead of silently doing
                                        // nothing or asking for it again.
                                        else -> onRescheduleAutoBackup(days)
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
                            if (passwordSet) {
                                stringResource(R.string.settings_backup_password_set)
                            } else {
                                stringResource(R.string.settings_backup_password_unset)
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
        // A new dialog starts with an empty confirmation: a stale match must never unlock export.
        LaunchedEffect(action) { passwordConfirm = "" }
        // Export is the one flow that creates a secret, so it is also the one that confirms it.
        val exportPasswordReady = backupPasswordReady(password, passwordConfirm)
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
                    if (action == BackupPasswordAction.EXPORT) {
                        val mismatch = passwordConfirm.isNotEmpty() && passwordConfirm != password
                        OutlinedTextField(
                            value = passwordConfirm,
                            onValueChange = { passwordConfirm = it },
                            label = {
                                Text(stringResource(R.string.settings_backup_password_confirm))
                            },
                            singleLine = true,
                            isError = mismatch,
                            supportingText = if (mismatch) {
                                {
                                    Text(
                                        stringResource(
                                            R.string.settings_backup_password_mismatch,
                                        ),
                                    )
                                }
                            } else {
                                null
                            },
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
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
                                    runCatching { context.contentResolver.takePersistableUriPermission(folder, flags) }
                                    onScheduleAutoBackup(folder.toString(), password.toCharArray(), freqDays)
                                    pendingFolder = null
                                } else {
                                    // "Change password" on a configured backup: same folder, new
                                    // password. Before, this branch silently did nothing.
                                    state.autoBackupUri?.let { current ->
                                        onScheduleAutoBackup(current, password.toCharArray(), freqDays)
                                    }
                                }
                            }
                        }
                    },
                    enabled = if (action == BackupPasswordAction.EXPORT) {
                        exportPasswordReady
                    } else {
                        password.isNotEmpty()
                    },
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

/** Copies a finished export into the document the user picked, away from the main thread. */
internal suspend fun copyExport(context: android.content.Context, uri: Uri, source: File): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { it.copyTo(out) }
            } ?: error("Could not open the export destination")
        }.isSuccess
    }

@Composable
internal fun formatBackupInstant(instant: Instant): String {
    val local = instant.atZone(ZoneId.systemDefault())
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(local)
}
