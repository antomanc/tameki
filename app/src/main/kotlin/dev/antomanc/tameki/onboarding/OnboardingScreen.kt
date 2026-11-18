package dev.antomanc.tameki.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.R
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.ui.components.MoneyField
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.spacing
import java.util.Currency
import java.util.Locale

private data class OnboardingAccountDraft(
    val name: String,
    val balance: Money? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    initialCurrency: String,
    onFinish: (currency: String, importCsv: Boolean) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    onRestoreBackup: () -> Unit = onSkip,
) {
    var step by remember { mutableIntStateOf(0) }
    var currency by remember { mutableStateOf(initialCurrency) }
    var currencyQuery by remember { mutableStateOf("") }
    var importChoice by remember { mutableStateOf<Boolean?>(null) }
    val cashLabel = stringResource(R.string.app_onboarding_account_cash)
    val cardLabel = stringResource(R.string.app_onboarding_account_card)
    val accounts = remember {
        mutableStateListOf(
            OnboardingAccountDraft(cashLabel),
            OnboardingAccountDraft(cardLabel),
        )
    }
    val spacing = MaterialTheme.spacing
    val currencies = remember { currencyOptions(initialCurrency) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    if (step >= 1) {
                        TextButton(onClick = onSkip) {
                            Text(stringResource(R.string.app_onboarding_skip))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.gutter)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.group),
        ) {
            StepDots(step = step, total = 4)
            when (step) {
                0 -> WelcomeStep(
                    onStart = { step = 1 },
                    onRestore = onRestoreBackup,
                )
                1 -> CurrencyStep(
                    selected = currency,
                    query = currencyQuery,
                    onQueryChange = { currencyQuery = it },
                    currencies = currencies.filter {
                        currencyQuery.isBlank() ||
                            it.code.contains(currencyQuery, ignoreCase = true) ||
                            it.name.contains(currencyQuery, ignoreCase = true)
                    },
                    onSelect = { currency = it },
                    onNext = { step = 2 },
                )
                2 -> AccountsStep(
                    accounts = accounts,
                    currency = CurrencyCode(currency),
                    onNameChange = { index, name ->
                        accounts[index] = accounts[index].copy(name = name)
                    },
                    onBalanceChange = { index, money ->
                        accounts[index] = accounts[index].copy(balance = money)
                    },
                    onAdd = { accounts += OnboardingAccountDraft("") },
                    onNext = { step = 3 },
                )
                else -> DataStep(
                    selectedImport = importChoice,
                    onSelect = { importChoice = it },
                    onFinish = {
                        onFinish(currency, importChoice == true)
                    },
                )
            }
            Spacer(Modifier.height(spacing.group))
        }
    }
}

@Composable
private fun StepDots(step: Int, total: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            Surface(
                shape = CircleShape,
                color = if (index == step) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (index == step) 8.dp else 6.dp),
            ) {}
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit, onRestore: () -> Unit) {
    val spacing = MaterialTheme.spacing
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(spacing.group),
    ) {
        Spacer(Modifier.height(spacing.section))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.app_kanji),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.app_onboarding_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.section))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.app_onboarding_start))
        }
        TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.app_onboarding_have_backup))
        }
    }
}

private data class CurrencyOption(val code: String, val name: String, val symbol: String)

private fun currencyOptions(preferred: String): List<CurrencyOption> {
    val locale = Locale.getDefault()
    val codes = (
        listOf(preferred, "EUR", "USD", "GBP", "CHF", "JPY", "CAD", "AUD") +
            Currency.getAvailableCurrencies().map { it.currencyCode }
        ).distinct()
    return codes.mapNotNull { code ->
        runCatching {
            val cur = Currency.getInstance(code)
            CurrencyOption(code, cur.getDisplayName(locale), cur.getSymbol(locale))
        }.getOrNull()
    }.sortedWith(
        compareByDescending<CurrencyOption> { it.code == preferred }
            .thenBy { it.name },
    )
}

@Composable
private fun CurrencyStep(
    selected: String,
    query: String,
    onQueryChange: (String) -> Unit,
    currencies: List<CurrencyOption>,
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Text(
        stringResource(R.string.app_onboarding_currency),
        style = MaterialTheme.typography.headlineSmall,
    )
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.app_onboarding_currency_search)) },
    )
    Column(
        Modifier
            .fillMaxWidth()
            .height(280.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.tight),
    ) {
        currencies.take(12).forEach { option ->
            val selectedRow = option.code == selected
            Surface(
                onClick = { onSelect(option.code) },
                shape = MaterialTheme.shapes.medium,
                color = if (selectedRow) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = if (selectedRow) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(horizontal = spacing.group, vertical = spacing.rowPadding),
                    horizontalArrangement = Arrangement.spacedBy(spacing.between),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(option.symbol, style = MaterialTheme.typography.titleMedium)
                    Column(Modifier.weight(1f)) {
                        Text(option.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            option.code,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.app_onboarding_next))
    }
}

@Composable
private fun AccountsStep(
    accounts: List<OnboardingAccountDraft>,
    currency: CurrencyCode,
    onNameChange: (Int, String) -> Unit,
    onBalanceChange: (Int, Money?) -> Unit,
    onAdd: () -> Unit,
    onNext: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Text(
        stringResource(R.string.app_onboarding_accounts),
        style = MaterialTheme.typography.headlineSmall,
    )
    accounts.forEachIndexed { index, account ->
        OutlinedTextField(
            value = account.name,
            onValueChange = { onNameChange(index, it) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        MoneyField(
            value = account.balance,
            onValueChange = { onBalanceChange(index, it) },
            currency = currency,
            label = stringResource(R.string.app_onboarding_starting_balance),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(spacing.tight))
    }
    TextButton(onClick = onAdd) {
        Text(stringResource(R.string.app_onboarding_add_account))
    }
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.app_onboarding_next))
    }
}

@Composable
private fun DataStep(
    selectedImport: Boolean?,
    onSelect: (Boolean) -> Unit,
    onFinish: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Text(
        stringResource(R.string.app_onboarding_data),
        style = MaterialTheme.typography.headlineSmall,
    )
    ChoiceCard(
        title = stringResource(R.string.app_onboarding_start_empty),
        selected = selectedImport == false,
        onClick = { onSelect(false) },
    )
    ChoiceCard(
        title = stringResource(R.string.app_onboarding_import_csv),
        selected = selectedImport == true,
        onClick = { onSelect(true) },
    )
    Spacer(Modifier.height(spacing.group))
    Button(
        onClick = onFinish,
        enabled = selectedImport != null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.app_onboarding_finish))
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = if (selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(spacing.cardPadding),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F0EA)
@Composable
private fun OnboardingPreview() {
    TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
        OnboardingScreen(initialCurrency = "EUR", onFinish = { _, _ -> }, onSkip = {})
    }
}
