package dev.antomanc.tameki.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.R
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.money.CurrencyCatalog
import dev.antomanc.tameki.core.ui.components.MoneyField
import dev.antomanc.tameki.core.ui.navigation.NavIcons
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.spacing

data class OnboardingAccountDraft(
    val name: String,
    val balance: Money? = null,
)

private const val StepCount = 3

/** Launcher adaptive-icon background — matches the store feature graphic. */
private val BrandMarkBackground = Color(0xFF2E6B4A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    initialCurrency: String,
    onFinish: (currency: String, accounts: List<OnboardingAccountDraft>, importCsv: Boolean) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    onRestoreBackup: () -> Unit = onSkip,
) {
    var step by remember { mutableIntStateOf(0) }
    var currency by remember { mutableStateOf(initialCurrency) }
    var currencyQuery by remember { mutableStateOf("") }
    val cashLabel = stringResource(R.string.app_onboarding_account_cash)
    val cardLabel = stringResource(R.string.app_onboarding_account_card)
    val accounts = remember {
        mutableStateListOf(
            OnboardingAccountDraft(cashLabel),
            OnboardingAccountDraft(cardLabel),
        )
    }
    val spacing = MaterialTheme.spacing
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val currencies = remember(initialCurrency, locale) {
        CurrencyCatalog.options(locale, preferred = initialCurrency)
    }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = step > 0) {
        focusManager.clearFocus()
        step -= 1
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    // The action slot is always reserved: showing Skip only from step 1
                    // resized the app bar and shifted the whole screen mid-flow.
                    val canSkip = step >= 1
                    TextButton(
                        onClick = onSkip,
                        enabled = canSkip,
                        modifier = if (canSkip) {
                            Modifier
                        } else {
                            Modifier
                                .alpha(0f)
                                .clearAndSetSemantics { }
                        },
                    ) {
                        Text(stringResource(R.string.app_onboarding_skip))
                    }
                },
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.gutter)
                    .padding(bottom = spacing.group),
                verticalArrangement = Arrangement.spacedBy(spacing.tight),
            ) {
                when (step) {
                    0 -> {
                        Button(
                            onClick = { step = 1 },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.app_onboarding_start))
                        }
                        TextButton(onClick = onRestoreBackup, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.app_onboarding_have_backup))
                        }
                    }
                    1 -> {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                step = 2
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.app_onboarding_next))
                        }
                    }
                    else -> {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onFinish(currency, accounts.toList(), false)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.app_onboarding_finish))
                        }
                        TextButton(
                            onClick = {
                                focusManager.clearFocus()
                                onFinish(currency, accounts.toList(), true)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.app_onboarding_import_csv))
                        }
                    }
                }
            }
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
            StepDots(step = step, total = StepCount)
            when (step) {
                0 -> WelcomeStep()
                1 -> CurrencyStep(
                    selected = currency,
                    query = currencyQuery,
                    onQueryChange = { currencyQuery = it },
                    currencies = currencies.filter { CurrencyCatalog.matchesQuery(it, currencyQuery) },
                    onSelect = { currency = it },
                )
                else -> AccountsStep(
                    accounts = accounts,
                    currency = CurrencyCode(currency),
                    onNameChange = { index, name ->
                        accounts[index] = accounts[index].copy(name = name)
                    },
                    onBalanceChange = { index, money ->
                        accounts[index] = accounts[index].copy(balance = money)
                    },
                    onAdd = { accounts += OnboardingAccountDraft("") },
                    onRemove = { index -> accounts.removeAt(index) },
                )
            }
            Spacer(Modifier.height(spacing.group))
        }
    }
}

@Composable
private fun StepDots(step: Int, total: Int) {
    val label = stringResource(R.string.app_onboarding_step, step + 1, total)
    Row(
        Modifier
            .fillMaxWidth()
            .semantics { contentDescription = label },
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
                    .size(if (index == step) 8.dp else 6.dp)
                    .clearAndSetSemantics { },
            ) {}
        }
    }
}

@Composable
private fun WelcomeStep() {
    val spacing = MaterialTheme.spacing
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(spacing.group),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(BrandMarkBackground),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.tight)) {
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
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.between)) {
            WelcomeFact(
                icon = NavIcons.Today,
                text = stringResource(R.string.app_onboarding_fact_spend),
            )
            WelcomeFact(
                icon = NavIcons.Save,
                text = stringResource(R.string.app_onboarding_fact_save),
            )
            WelcomeFact(
                icon = Icons.Default.Lock,
                text = stringResource(R.string.app_onboarding_fact_private),
            )
        }
    }
}

@Composable
private fun WelcomeFact(icon: ImageVector, text: String) {
    val spacing = MaterialTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.between),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CurrencyStep(
    selected: String,
    query: String,
    onQueryChange: (String) -> Unit,
    currencies: List<CurrencyCatalog.Option>,
    onSelect: (String) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val focusManager = LocalFocusManager.current
    // Symbols range from "€" to "CHF" / "US$": a fixed slot keeps the names aligned,
    // and it grows with the font scale so accessibility sizes aren't clipped.
    val symbolSlot = with(LocalDensity.current) { 48.dp * fontScale }
    val visible = if (query.isBlank()) {
        val top = currencies.take(5)
        val selectedOpt = currencies.firstOrNull { it.code == selected }
        if (selectedOpt == null || top.any { it.code == selected }) {
            top
        } else {
            listOf(selectedOpt) + top.take(4)
        }
    } else {
        currencies.take(12)
    }
    Text(
        stringResource(R.string.app_onboarding_currency),
        style = MaterialTheme.typography.headlineSmall,
    )
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(stringResource(R.string.app_onboarding_currency_search)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
    )
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.tight),
    ) {
        visible.forEach { option ->
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
                    Box(Modifier.width(symbolSlot)) {
                        Text(
                            text = option.symbol,
                            style = MaterialTheme.typography.titleMedium,
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
                }
            }
        }
    }
}

@Composable
private fun AccountsStep(
    accounts: List<OnboardingAccountDraft>,
    currency: CurrencyCode,
    onNameChange: (Int, String) -> Unit,
    onBalanceChange: (Int, Money?) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val focusManager = LocalFocusManager.current
    Text(
        stringResource(R.string.app_onboarding_accounts),
        style = MaterialTheme.typography.headlineSmall,
    )
    Text(
        stringResource(R.string.app_onboarding_accounts_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    accounts.forEachIndexed { index, account ->
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(spacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(spacing.tight),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = account.name,
                        onValueChange = { onNameChange(index, it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.app_onboarding_account_name)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )
                    if (index > 0) {
                        Spacer(Modifier.width(spacing.tight))
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.app_onboarding_remove_account),
                            )
                        }
                    }
                }
                MoneyField(
                    value = account.balance,
                    onValueChange = { onBalanceChange(index, it) },
                    currency = currency,
                    label = stringResource(R.string.app_onboarding_current_balance),
                    placeholder = "0",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    TextButton(
        onClick = {
            focusManager.clearFocus()
            onAdd()
        },
    ) {
        Text(stringResource(R.string.app_onboarding_add_account))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F0EA)
@Composable
private fun OnboardingPreview() {
    TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
        OnboardingScreen(initialCurrency = "EUR", onFinish = { _, _, _ -> }, onSkip = {})
    }
}
