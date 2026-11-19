package dev.antomanc.tameki.feature.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.BudgetPeriod
import dev.antomanc.tameki.core.ui.components.MoneyField
import dev.antomanc.tameki.core.ui.format.rememberMoneyFormatter
import dev.antomanc.tameki.core.ui.theme.spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Add or edit a budget. Categories are an optional set: none means "all expenses", which the chip
 * row says in words instead of leaving the user guessing what empty means.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun BudgetSheet(
    initial: BudgetUi?,
    categories: List<CategoryOptionUi>,
    currency: CurrencyCode,
    onDismiss: () -> Unit,
    onSave: (String, Set<Long>, Money, BudgetPeriod, Boolean) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val spacing = MaterialTheme.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var limit by remember { mutableStateOf(initial?.limit) }
    var period by remember { mutableStateOf(initial?.period ?: BudgetPeriod.MONTH) }
    var rollover by remember { mutableStateOf(initial?.rollover ?: false) }
    var selected by remember { mutableStateOf(initial?.categoryIds.orEmpty()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.gutter)
                .padding(bottom = spacing.section),
        ) {
            Text(
                text = stringResource(
                    if (initial == null) R.string.insights_budget_new else R.string.insights_budget_edit,
                ),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(spacing.group))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.insights_budget_name)) },
                singleLine = true,
            )
            Spacer(Modifier.height(spacing.group))
            MoneyField(
                value = limit,
                onValueChange = { limit = it },
                currency = currency,
                label = stringResource(R.string.insights_budget_limit),
            )
            Spacer(Modifier.height(spacing.group))
            Text(
                text = stringResource(R.string.insights_budget_categories),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selected.isEmpty(),
                        onClick = { selected = emptySet() },
                        label = { Text(stringResource(R.string.insights_budget_all)) },
                        shape = CircleShape,
                    )
                }
                items(categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = category.id in selected,
                        onClick = {
                            selected = if (category.id in selected) {
                                selected - category.id
                            } else {
                                selected + category.id
                            }
                        },
                        label = { Text(category.name) },
                        shape = CircleShape,
                    )
                }
            }
            Spacer(Modifier.height(spacing.group))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = period == BudgetPeriod.MONTH,
                    onClick = { period = BudgetPeriod.MONTH },
                    label = { Text(stringResource(R.string.insights_budget_month)) },
                    shape = CircleShape,
                )
                FilterChip(
                    selected = period == BudgetPeriod.WEEK,
                    onClick = { period = BudgetPeriod.WEEK },
                    label = { Text(stringResource(R.string.insights_budget_week)) },
                    shape = CircleShape,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.group),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.insights_budget_rollover),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(checked = rollover, onCheckedChange = { rollover = it })
            }
            Spacer(Modifier.height(spacing.group))
            Button(
                onClick = { limit?.let { onSave(name, selected, it, period, rollover) } },
                enabled = name.isNotBlank() && limit != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.insights_save))
            }
            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.insights_delete))
                }
            }
        }
    }
}

/** Add or edit a goal: a name, a target, an optional deadline. Saved money is only added later. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoalSheet(
    initial: GoalUi?,
    currency: CurrencyCode,
    onDismiss: () -> Unit,
    onSave: (String, Money, LocalDate?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val spacing = MaterialTheme.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val formatter = rememberDateFormatter()
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var target by remember { mutableStateOf(initial?.target) }
    var deadline by remember { mutableStateOf(initial?.deadline) }
    var picking by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.gutter)
                .padding(bottom = spacing.section),
        ) {
            Text(
                text = stringResource(
                    if (initial == null) R.string.insights_goal_new else R.string.insights_goal_edit,
                ),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(spacing.group))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.insights_goal_name)) },
                singleLine = true,
            )
            Spacer(Modifier.height(spacing.group))
            MoneyField(
                value = target,
                onValueChange = { target = it },
                currency = currency,
                label = stringResource(R.string.insights_goal_target),
            )
            Spacer(Modifier.height(spacing.group))
            OutlinedButton(onClick = { picking = true }) {
                Text(
                    text = deadline?.let(formatter::format)
                        ?: stringResource(R.string.insights_goal_no_deadline),
                )
            }
            Spacer(Modifier.height(spacing.group))
            Button(
                onClick = { target?.let { onSave(name, it, deadline) } },
                enabled = name.isNotBlank() && target != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.insights_save))
            }
            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.insights_delete))
                }
            }
        }
    }

    if (picking) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (deadline ?: LocalDate.now())
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            deadline = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        picking = false
                    },
                ) {
                    Text(stringResource(R.string.insights_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deadline = null
                        picking = false
                    },
                ) {
                    Text(stringResource(R.string.insights_goal_no_deadline))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Adding money to a goal is a sheet with one field: no ceremony for a two-tap action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddMoneySheet(
    goal: GoalUi,
    currency: CurrencyCode,
    onDismiss: () -> Unit,
    onAdd: (Money) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val formatter = rememberMoneyFormatter()
    var amount by remember { mutableStateOf<Money?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.gutter)
                .padding(bottom = spacing.section),
        ) {
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.insights_goal_progress,
                    formatter.formatWhole(goal.saved),
                    formatter.formatWhole(goal.target),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(spacing.group))
            MoneyField(
                value = amount,
                onValueChange = { amount = it },
                currency = currency,
                label = stringResource(R.string.insights_goal_amount),
            )
            Spacer(Modifier.height(spacing.group))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.tight)) {
                listOf(1000L, 2000L, 5000L, 10000L).forEach { minor ->
                    OutlinedButton(
                        onClick = { amount = Money(minor, currency) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    ) {
                        Text(formatter.formatWhole(Money(minor, currency)))
                    }
                }
            }
            Spacer(Modifier.height(spacing.group))
            Button(
                onClick = { amount?.let(onAdd) },
                enabled = amount != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.insights_goal_add_money))
            }
        }
    }
}

/** Dates in the sheets follow the locale, like everywhere else. */
@Composable
private fun rememberDateFormatter(): DateTimeFormatter {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val locale = configuration.locales[0]
    return remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
}
