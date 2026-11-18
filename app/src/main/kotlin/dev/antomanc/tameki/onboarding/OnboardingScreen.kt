package dev.antomanc.tameki.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.antomanc.tameki.R
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.spacing

@Composable
fun OnboardingScreen(
    initialCurrency: String,
    onFinish: (currency: String, importCsv: Boolean) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableIntStateOf(0) }
    var currency by remember { mutableStateOf(initialCurrency) }

    Column(
        modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.group),
    ) {
        TextButton(onClick = onSkip) { Text(stringResource(R.string.app_onboarding_skip)) }
        when (step) {
            0 -> {
                Text(stringResource(R.string.app_onboarding_currency), style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it.uppercase() },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.app_onboarding_next))
                }
            }
            1 -> {
                Text(stringResource(R.string.app_onboarding_accounts), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.app_onboarding_accounts_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { step = 2 }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.app_onboarding_next))
                }
            }
            else -> {
                Text(stringResource(R.string.app_onboarding_data), style = MaterialTheme.typography.headlineSmall)
                Button(
                    onClick = { onFinish(currency, true) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.app_onboarding_import_csv))
                }
                OutlinedButton(
                    onClick = { onFinish(currency, false) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.app_onboarding_start_empty))
                }
            }
        }
        Spacer(Modifier.height(MaterialTheme.spacing.section))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F0EA)
@Composable
private fun OnboardingPreview() {
    TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
        OnboardingScreen(initialCurrency = "EUR", onFinish = { _, _ -> }, onSkip = {})
    }
}
