package dev.antomanc.tameki.feature.subscriptions

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.ui.components.MoneyField
import dev.antomanc.tameki.core.ui.theme.spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.koin.androidx.compose.koinViewModel

/** Add or edit a recurring payment; [onDone] fires after a successful save. */
@Composable
fun RecurringEditRoute(
    id: Long?,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringEditViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(id) { viewModel.prepare(id) }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    RecurringEditScreen(
        state = state,
        onBack = onBack,
        onName = viewModel::setName,
        onSelectBrand = viewModel::selectBrand,
        onType = viewModel::setType,
        onAmount = viewModel::setAmount,
        onCycleEvery = viewModel::setCycleEvery,
        onCycleUnit = viewModel::setCycleUnit,
        onStartDate = viewModel::setStartDate,
        onNextDate = viewModel::setNextDate,
        onTrialEnd = viewModel::setTrialEnd,
        onAccount = viewModel::setAccount,
        onCategory = viewModel::setCategory,
        onReminderDays = viewModel::setReminderDays,
        onAutoLog = viewModel::setAutoLog,
        onAnnualAlternative = viewModel::setAnnualAlternative,
        onSharedWith = viewModel::setSharedWith,
        onCancelUrl = viewModel::setCancelUrl,
        onNote = viewModel::setNote,
        onSave = viewModel::save,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecurringEditScreen(
    state: RecurringEditUiState,
    onBack: () -> Unit,
    onName: (String) -> Unit,
    onSelectBrand: (BrandSuggestion) -> Unit,
    onType: (RecurringType) -> Unit,
    onAmount: (Money?) -> Unit,
    onCycleEvery: (Int) -> Unit,
    onCycleUnit: (CycleUnit) -> Unit,
    onStartDate: (LocalDate) -> Unit,
    onNextDate: (LocalDate) -> Unit,
    onTrialEnd: (LocalDate?) -> Unit,
    onAccount: (Long?) -> Unit,
    onCategory: (Long?) -> Unit,
    onReminderDays: (Int) -> Unit,
    onAutoLog: (Boolean) -> Unit,
    onAnnualAlternative: (Money?) -> Unit,
    onSharedWith: (Int) -> Unit,
    onCancelUrl: (String) -> Unit,
    onNote: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    var datePicker by remember { mutableStateOf<DateField?>(null) }
    var cycleEveryText by remember(state.cycleEvery) { mutableStateOf(state.cycleEvery.toString()) }
    val dateFormatter = rememberDateFormatter()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        stringResource(
                            if (state.isNew) R.string.subs_edit_title_new else R.string.subs_edit_title_edit,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.subs_back))
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = state.canSave) {
                        Text(stringResource(R.string.subs_save))
                    }
                },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.padding(inner).fillMaxSize(),
            contentPadding = PaddingValues(
                start = spacing.gutter,
                end = spacing.gutter,
                top = spacing.group,
                bottom = spacing.section,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.group),
        ) {
            item(key = "name") {
                Column {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = onName,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.subs_name)) },
                        placeholder = { Text(stringResource(R.string.subs_name_hint)) },
                        singleLine = true,
                    )
                    if (state.suggestions.isNotEmpty()) {
                        SuggestionRow(state.suggestions, onSelectBrand)
                    }
                }
            }

            item(key = "type") {
                FormSection(stringResource(R.string.subs_type)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        RecurringType.entries.forEach { type ->
                            FilterChip(
                                selected = state.type == type,
                                onClick = { onType(type) },
                                label = { Text(typeLabel(type)) },
                                shape = CircleShape,
                            )
                        }
                    }
                }
            }

            item(key = "amount") {
                FormSection(stringResource(R.string.subs_amount)) {
                    MoneyField(
                        value = state.amount,
                        onValueChange = onAmount,
                        currency = state.currency,
                        isError = state.amountMissing,
                        supportingText = if (state.amountMissing) {
                            stringResource(R.string.subs_enter_amount)
                        } else {
                            null
                        },
                    )
                }
            }

            item(key = "cycle") {
                FormSection(stringResource(R.string.subs_repeats)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.subs_every))
                        OutlinedTextField(
                            value = cycleEveryText,
                            onValueChange = { raw ->
                                // Let the field be empty while editing; only a positive number
                                // reaches the form state. Coercing every keystroke to 1 made it
                                // impossible to clear and type "12".
                                cycleEveryText = raw.filter { it.isDigit() }.take(2)
                                cycleEveryText.toIntOrNull()?.takeIf { it > 0 }?.let(onCycleEvery)
                            },
                            modifier = Modifier.width(84.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(CycleUnit.entries) { unit ->
                                FilterChip(
                                    selected = state.cycleUnit == unit,
                                    onClick = { onCycleUnit(unit) },
                                    label = { Text(unitLabel(unit)) },
                                    shape = CircleShape,
                                )
                            }
                        }
                    }
                }
            }

            item(key = "dates") {
                FormSection(stringResource(R.string.subs_dates)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateButton(
                            label = stringResource(R.string.subs_start_date),
                            value = dateFormatter.format(state.startDate),
                            onClick = { datePicker = DateField.START },
                        )
                        DateButton(
                            label = stringResource(R.string.subs_next_date),
                            value = dateFormatter.format(state.nextDate),
                            onClick = { datePicker = DateField.NEXT },
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DateButton(
                                label = stringResource(R.string.subs_trial_end),
                                value = state.trialEndsOn?.let(dateFormatter::format)
                                    ?: stringResource(R.string.subs_trial_none),
                                onClick = { datePicker = DateField.TRIAL },
                                modifier = Modifier.weight(1f),
                            )
                            if (state.trialEndsOn != null) {
                                IconButton(onClick = { onTrialEnd(null) }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.subs_trial_none),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "account") {
                FormSection(stringResource(R.string.subs_account)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.accounts) { account ->
                            FilterChip(
                                selected = state.accountId == account.id,
                                onClick = { onAccount(if (state.accountId == account.id) null else account.id) },
                                label = { Text(account.name) },
                                shape = CircleShape,
                            )
                        }
                    }
                }
            }

            item(key = "category") {
                FormSection(stringResource(R.string.subs_category)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.categories) { category ->
                            FilterChip(
                                selected = state.categoryId == category.id,
                                onClick = { onCategory(if (state.categoryId == category.id) null else category.id) },
                                label = { Text(category.name) },
                                shape = CircleShape,
                            )
                        }
                    }
                }
            }

            item(key = "reminder") {
                FormSection(stringResource(R.string.subs_reminder)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(-1, 1, 2, 3, 7).forEach { days ->
                            FilterChip(
                                selected = state.reminderDays == days,
                                onClick = { onReminderDays(days) },
                                label = {
                                    Text(
                                        if (days < 0) {
                                            stringResource(R.string.subs_reminder_off)
                                        } else {
                                            androidx.compose.ui.res.pluralStringResource(
                                                R.plurals.subs_reminder_days,
                                                days,
                                                days,
                                            )
                                        },
                                    )
                                },
                                shape = CircleShape,
                            )
                        }
                    }
                }
            }

            item(key = "auto_log") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = state.autoLog, onValueChange = onAutoLog),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.subs_auto_log),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.subs_auto_log_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(spacing.tight))
                    Switch(checked = state.autoLog, onCheckedChange = null)
                }
            }

            item(key = "annual") {
                FormSection(stringResource(R.string.subs_annual_alternative)) {
                    MoneyField(
                        value = state.annualAlternative,
                        onValueChange = onAnnualAlternative,
                        currency = state.currency,
                    )
                }
            }

            item(key = "shared") {
                FormSection(stringResource(R.string.subs_shared_with)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onSharedWith(state.sharedWith - 1) }) {
                            Text("−", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            androidx.compose.ui.res.pluralStringResource(
                                R.plurals.subs_people,
                                state.sharedWith,
                                state.sharedWith,
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        IconButton(onClick = { onSharedWith(state.sharedWith + 1) }) {
                            Text("+", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }

            item(key = "cancel_url") {
                OutlinedTextField(
                    value = state.cancelUrl,
                    onValueChange = onCancelUrl,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.subs_cancel_url)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
            }

            item(key = "note") {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = onNote,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.subs_note)) },
                    minLines = 2,
                )
            }

            item(key = "save") {
                Button(
                    onClick = onSave,
                    enabled = state.canSave,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.subs_save))
                }
            }
        }
    }

    datePicker?.let { field ->
        val initial = when (field) {
            DateField.START -> state.startDate
            DateField.NEXT -> state.nextDate
            DateField.TRIAL -> state.trialEndsOn ?: state.nextDate
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { datePicker = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = pickerState.selectedDateMillis
                        datePicker = null
                        if (millis != null) {
                            val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            when (field) {
                                DateField.START -> onStartDate(date)
                                DateField.NEXT -> onNextDate(date)
                                DateField.TRIAL -> onTrialEnd(date)
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.subs_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { datePicker = null }) {
                    Text(stringResource(R.string.subs_dialog_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private enum class DateField { START, NEXT, TRIAL }

@Composable
private fun FormSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionRow(suggestions: List<BrandSuggestion>, onSelect: (BrandSuggestion) -> Unit) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            text = stringResource(R.string.subs_brand_suggestions),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            suggestions.forEach { suggestion ->
                FilterChip(
                    selected = false,
                    onClick = { onSelect(suggestion) },
                    label = { Text(suggestion.name) },
                    shape = CircleShape,
                )
            }
        }
    }
}

@Composable
private fun DateButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onClick) {
            Text(value)
        }
    }
}

@Composable
private fun unitLabel(unit: CycleUnit): String = stringResource(
    when (unit) {
        CycleUnit.MONTH -> R.string.subs_unit_month
        CycleUnit.YEAR -> R.string.subs_unit_year
        CycleUnit.WEEK -> R.string.subs_unit_week
        CycleUnit.DAY -> R.string.subs_unit_day
    },
)

/** The form as it is opened for a brand-new Spotify subscription, for previews and screenshots. */
internal fun sampleEditState(): RecurringEditUiState {
    val eur = dev.antomanc.tameki.core.model.CurrencyCode("EUR")
    val today = java.time.LocalDate.of(2026, 11, 18)
    return RecurringEditUiState(
        isNew = false,
        loading = false,
        name = "Spotify",
        type = RecurringType.SUBSCRIPTION,
        amount = Money(1099, eur),
        startDate = today.minusYears(2),
        nextDate = today.plusDays(3),
        accountId = 2,
        categoryId = 19,
        reminderDays = 2,
        currency = eur,
        accounts = listOf(AccountOption(1, "Contanti"), AccountOption(2, "Carta")),
        categories = listOf(
            CategoryOption(19, "Streaming", "streaming"),
            CategoryOption(20, "Musica", "music"),
            CategoryOption(6, "Casa", "home"),
            CategoryOption(7, "Utenze", "utilities"),
            CategoryOption(8, "Telefono", "phone"),
            CategoryOption(11, "Sport", "sport"),
        ),
        today = today,
    )
}
