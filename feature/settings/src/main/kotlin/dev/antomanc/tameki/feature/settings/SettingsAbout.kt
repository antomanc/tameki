package dev.antomanc.tameki.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.antomanc.tameki.core.ui.theme.spacing

@Composable
internal fun AutomationSection(
    padding: PaddingValues,
    state: SettingsUiState,
    onSetSilent: (Boolean) -> Unit,
    onOpenAutomationDocs: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.group),
    ) {
        SwitchRow(stringResource(R.string.settings_silent_saves), state.silentAutomation, onSetSilent)
        TextButton(onClick = onOpenAutomationDocs) {
            Text(stringResource(R.string.settings_automation_docs))
        }
    }
}

@Composable
internal fun AboutSection(
    padding: PaddingValues,
    state: SettingsUiState,
    onOpenDebugGallery: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.gutter),
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
