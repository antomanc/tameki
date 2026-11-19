package dev.antomanc.tameki.feature.settings

import android.app.LocaleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.provider.Settings as PlatformSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.biometric.BiometricManager
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.ui.theme.spacing

/** True only when the phone can actually ask for a biometric or the screen-lock PIN. */
@Composable
internal fun rememberLockAvailable(): Boolean {
    val context = LocalContext.current
    return androidx.compose.runtime.remember(context) {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }
}

@Composable
internal fun GeneralSection(
    padding: PaddingValues,
    state: SettingsUiState,
    onSetBaseCurrency: (String) -> Unit,
    onSetTheme: (String) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var currency by remember(state.baseCurrency) { mutableStateOf(state.baseCurrency.value) }
    // An invalid code used to look saved but the ViewModel silently ignored it. The field now
    // says what is wrong instead of leaving the old currency in place without a word.
    var currencyError by remember(state.baseCurrency) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(padding).padding(MaterialTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.group),
    ) {
        OutlinedTextField(
            value = currency,
            onValueChange = {
                currency = it.uppercase()
                currencyError = false
            },
            label = { Text(stringResource(R.string.settings_base_currency)) },
            isError = currencyError,
            supportingText = if (currencyError) {
                { Text(stringResource(R.string.settings_currency_invalid)) }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Button(
            onClick = {
                if (CurrencyCode.parse(currency) == null) {
                    currencyError = true
                } else {
                    currencyError = false
                    onSetBaseCurrency(currency)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
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
    Column(Modifier.fillMaxSize().padding(padding).padding(MaterialTheme.spacing.gutter)) {
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
