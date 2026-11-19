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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.IntOffset
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.R
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.money.CurrencyCatalog
import dev.antomanc.tameki.core.ui.components.MoneyField
import dev.antomanc.tameki.core.ui.navigation.NavIcons
import dev.antomanc.tameki.core.ui.theme.BrandMarkBackground
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.spacing

data class OnboardingAccountDraft(
    val name: String,
    val balance: Money? = null,
)

/** The first-run flow, in order. Back goes to the previous step; Skip is offered after Welcome. */
internal enum class OnboardingStep { Welcome, Currency, Accounts }

private val StepCount = OnboardingStep.entries.size
private val OnboardingMaxWidth = 560.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    initialCurrency: String,
    onFinish: (currency: String, accounts: List<OnboardingAccountDraft>, importCsv: Boolean) -> Unit,
    onSkip: (currency: String) -> Unit,
    modifier: Modifier = Modifier,
    onRestoreBackup: () -> Unit = { onSkip(initialCurrency) },
) {
    var step by rememberSaveable { mutableStateOf(OnboardingStep.Welcome) }
    var currency by rememberSaveable { mutableStateOf(initialCurrency) }
    var currencyQuery by rememberSaveable { mutableStateOf("") }
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

    fun goBack() {
        focusManager.clearFocus()
        if (step.ordinal > 0) step = OnboardingStep.entries[step.ordinal - 1]
    }

    BackHandler(enabled = step != OnboardingStep.Welcome) { goBack() }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    val canGoBack = step != OnboardingStep.Welcome
                    IconButton(
                        onClick = { goBack() },
                        enabled = canGoBack,
                        modifier = if (canGoBack) {
                            Modifier
                        } else {
                            Modifier
                                .alpha(0f)
                                .clearAndSetSemantics { }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.app_onboarding_back),
                        )
                    }
                },
                actions = {
                    // The action slot is always reserved: showing Skip only from step 1
                    // resized the app bar and shifted the whole screen mid-flow.
                    val canSkip = step != OnboardingStep.Welcome
                    TextButton(
                        onClick = { onSkip(currency) },
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
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = OnboardingMaxWidth)
                        .padding(horizontal = spacing.gutter)
                        .padding(top = spacing.tight, bottom = spacing.group),
                    verticalArrangement = Arrangement.spacedBy(spacing.tight),
                ) {
                    when (step) {
                        OnboardingStep.Welcome -> {
                            Button(
                                onClick = { step = OnboardingStep.Currency },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.app_onboarding_start))
                            }
                            TextButton(onClick = onRestoreBackup, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.app_onboarding_have_backup))
                            }
                        }
                        OnboardingStep.Currency -> {
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    step = OnboardingStep.Accounts
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.app_onboarding_next))
                            }
                        }
                        OnboardingStep.Accounts -> {
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
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = OnboardingMaxWidth)
                    .padding(horizontal = spacing.gutter)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.group),
            ) {
                StepDots(step = step.ordinal, total = StepCount)
                // Steps slide along the reading direction: forward from the end, back from the start.
                val slide = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                val fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                val fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        (slideInHorizontally(slide) { w -> if (forward) w / 4 else -w / 4 } + fadeIn(fadeInSpec))
                            .togetherWith(slideOutHorizontally(slide) { w -> if (forward) -w / 4 else w / 4 } + fadeOut(fadeOutSpec))
                            .using(SizeTransform(clip = false))
                    },
                    label = "onboarding-step",
                ) { shown ->
                    when (shown) {
                        OnboardingStep.Welcome -> WelcomeStep()
                        OnboardingStep.Currency -> CurrencyStep(
                            selected = currency,
                            query = currencyQuery,
                            onQueryChange = { currencyQuery = it },
                            currencies = currencies.filter { CurrencyCatalog.matchesQuery(it, currencyQuery) },
                            onSelect = { currency = it },
                        )
                        OnboardingStep.Accounts -> AccountsStep(
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
                }
                Spacer(Modifier.height(spacing.group))
            }
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
            // The current step stretches into a pill; the others stay dots. Spring-driven, so a
            // step change reads as one continuous gesture rather than a swap.
            val active = index == step
            val width by animateDpAsState(
                targetValue = if (active) 24.dp else 8.dp,
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                label = "dot-width",
            )
            val color by androidx.compose.animation.animateColorAsState(
                targetValue = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                label = "dot-color",
            )
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = width, height = 8.dp)
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
                .clip(MaterialTheme.shapes.large)
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.group),
    ) {
        Text(
            stringResource(R.string.app_onboarding_currency),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.app_onboarding_currency_search)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                )
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.app_onboarding_clear_search),
                        )
                    }
                }
            } else {
                null
            },
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
                    onClick = {
                        focusManager.clearFocus()
                        onSelect(option.code)
                    },
                    shape = MaterialTheme.shapes.medium,
                    color = if (selectedRow) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    contentColor = if (selectedRow) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    border = if (selectedRow) {
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = spacing.group, vertical = spacing.between),
                        horizontalArrangement = Arrangement.spacedBy(spacing.between),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.width(symbolSlot),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                text = option.symbol,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (selectedRow) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = option.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selectedRow) FontWeight.Medium else FontWeight.Normal,
                            )
                            Text(
                                text = option.code,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedRow) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        if (selectedRow) {
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.group),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.app_onboarding_accounts),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.app_onboarding_accounts_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.between),
        ) {
            accounts.forEachIndexed { index, account ->
                val accountIcon = when (index) {
                    0 -> Icons.Outlined.AccountBalanceWallet
                    1 -> Icons.Outlined.CreditCard
                    else -> Icons.Outlined.AccountBalance
                }
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.padding(spacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(spacing.between),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.tight),
                        ) {
                            Icon(
                                imageVector = accountIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                            OutlinedTextField(
                                value = account.name,
                                onValueChange = { onNameChange(index, it) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text(stringResource(R.string.app_onboarding_account_name)) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            )
                            if (index > 0) {
                                IconButton(onClick = { onRemove(index) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.app_onboarding_remove_account),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
        }
        OutlinedButton(
            onClick = {
                focusManager.clearFocus()
                onAdd()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.app_onboarding_add_account))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F0EA)
@Composable
private fun OnboardingPreview() {
    TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
        OnboardingScreen(initialCurrency = "EUR", onFinish = { _, _, _ -> }, onSkip = {})
    }
}
