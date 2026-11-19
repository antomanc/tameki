package dev.antomanc.tameki.feature.settings

import android.app.LocaleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.provider.Settings as PlatformSettings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.biometric.BiometricManager
import dev.antomanc.tameki.core.model.money.CurrencyCatalog
import dev.antomanc.tameki.core.ui.theme.spacing

/** True only when the phone can actually ask for a biometric or the screen-lock PIN. */
@Composable
internal fun rememberLockAvailable(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GeneralSection(
    padding: PaddingValues,
    state: SettingsUiState,
    onSetBaseCurrency: (String) -> Unit,
    onSetTheme: (String) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val spacing = MaterialTheme.spacing
    val locale = LocalConfiguration.current.locales[0]
    var pickingCurrency by remember { mutableStateOf(false) }
    val selectedLangTag = remember(context, locale) { currentAppLocaleTag(context) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(spacing.group),
    ) {
        Text(stringResource(R.string.settings_base_currency), style = MaterialTheme.typography.titleSmall)
        Surface(
            onClick = { pickingCurrency = true },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = spacing.group, vertical = spacing.between),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.between),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CurrencyExchange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = currencyDisplayName(state.baseCurrency),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = state.baseCurrency.value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.settings_change),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
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
                ) { Text(label, maxLines = 1) }
            }
        }

        SwitchRow(stringResource(R.string.settings_dynamic_color), state.dynamicColor, onSetDynamicColor)

        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleSmall)
        val langOptions = listOf(
            null to stringResource(R.string.settings_lang_system),
            "it" to stringResource(R.string.settings_lang_it),
            "en" to stringResource(R.string.settings_lang_en),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            langOptions.forEachIndexed { index, (tag, label) ->
                val selected = when (tag) {
                    null -> selectedLangTag == null
                    else -> selectedLangTag?.startsWith(tag, ignoreCase = true) == true
                }
                SegmentedButton(
                    selected = selected,
                    onClick = { setAppLocales(context, tag) },
                    shape = SegmentedButtonDefaults.itemShape(index, langOptions.size),
                ) { Text(label, maxLines = 1) }
            }
        }
    }

    if (pickingCurrency) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var query by remember { mutableStateOf("") }
        val allCurrencies = remember(state.baseCurrency, locale) {
            CurrencyCatalog.options(locale, preferred = state.baseCurrency.value)
        }
        val filtered = remember(allCurrencies, query) {
            if (query.isBlank()) allCurrencies.take(20)
            else allCurrencies.filter { CurrencyCatalog.matchesQuery(it, query) }.take(20)
        }
        ModalBottomSheet(
            onDismissRequest = { pickingCurrency = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.gutter)
                    .padding(bottom = spacing.section),
                verticalArrangement = Arrangement.spacedBy(spacing.between),
            ) {
                Text(
                    text = stringResource(R.string.settings_base_currency),
                    style = MaterialTheme.typography.titleLarge,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_currency_search)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.settings_cancel),
                                )
                            }
                        }
                    } else {
                        null
                    },
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.tight),
                ) {
                    items(filtered, key = { it.code }) { option ->
                        val selected = option.code == state.baseCurrency.value
                        Surface(
                            onClick = {
                                onSetBaseCurrency(option.code)
                                pickingCurrency = false
                            },
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            contentColor = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            border = if (selected) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = spacing.group, vertical = spacing.between),
                                horizontalArrangement = Arrangement.spacedBy(spacing.between),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.width(44.dp)) {
                                    Text(
                                        text = option.symbol,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                        maxLines = 1,
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(option.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        option.code,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun currentAppLocaleTag(context: android.content.Context): String? {
    if (Build.VERSION.SDK_INT >= 33) {
        val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
        if (locales != null && !locales.isEmpty) {
            return locales[0]?.language
        }
    }
    return null
}

internal fun setAppLocales(context: android.content.Context, tag: String?) {
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
internal fun SecuritySection(
    padding: PaddingValues,
    state: SettingsUiState,
    onSetLock: (Boolean) -> Unit,
) {
    val lockAvailable = rememberLockAvailable()
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.gutter),
    ) {
        SwitchRow(
            label = stringResource(R.string.settings_biometric_lock),
            checked = state.lockEnabled,
            onChecked = onSetLock,
            supporting = when {
                state.lockEnabled -> stringResource(R.string.settings_active)
                !lockAvailable -> stringResource(R.string.settings_biometric_unavailable)
                else -> stringResource(R.string.settings_inactive)
            },
            enabled = state.lockEnabled || lockAvailable,
        )
    }
}
