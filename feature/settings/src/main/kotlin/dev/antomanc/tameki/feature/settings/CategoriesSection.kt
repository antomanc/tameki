package dev.antomanc.tameki.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.DefaultCategories
import dev.antomanc.tameki.core.ui.components.CategoryIcon
import dev.antomanc.tameki.core.ui.components.SectionHeader
import dev.antomanc.tameki.core.ui.seed.SeedLabels
import dev.antomanc.tameki.core.ui.seed.displayName
import dev.antomanc.tameki.core.ui.theme.CategoryColors
import dev.antomanc.tameki.core.ui.theme.spacing

/**
 * Restrained categories & accounts editor: rename, colour/icon from core:ui sets, archive, add.
 * Drag-to-reorder is intentionally deferred.
 */
@Composable
internal fun CategoriesAccountsEditor(
    padding: PaddingValues,
    state: SettingsUiState,
    onUpsertAccount: (Account) -> Unit,
    onArchiveAccount: (Long, Boolean) -> Unit,
    onUpsertCategory: (Category) -> Unit,
    onArchiveCategory: (Long, Boolean) -> Unit,
) {
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var addingCategory by remember { mutableStateOf(false) }
    var addingAccount by remember { mutableStateOf(false) }
    val touchTarget = MaterialTheme.spacing.touchTarget

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(MaterialTheme.spacing.gutter),
    ) {
        item { SectionHeader(title = stringResource(R.string.settings_accounts)) }
        items(state.accounts.filter { !it.archived }, key = { "a-${it.id}" }) { account ->
            // Row click and Archive are siblings, never nested: TalkBack hears two targets and a
            // tap can only mean one of them.
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .heightIn(min = touchTarget)
                        .clickable { editingAccount = account }
                        .padding(vertical = MaterialTheme.spacing.tight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${account.displayName()} · ${account.currency.value}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                TextButton(
                    onClick = { onArchiveAccount(account.id, true) },
                    modifier = Modifier.heightIn(min = touchTarget),
                ) {
                    Text(stringResource(R.string.settings_archive))
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { addingAccount = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_account_add))
            }
        }
        item { SectionHeader(title = stringResource(R.string.settings_categories)) }
        items(state.categories.filter { !it.archived }, key = { "c-${it.id}" }) { category ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .heightIn(min = touchTarget)
                        .clickable { editingCategory = category }
                        .padding(vertical = MaterialTheme.spacing.tight),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.between),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The stored ARGB is a legacy storage detail (importer/palette seed), not the
                    // ink to paint with: CategoryColors carries the light/dark variant that keeps
                    // ≥3:1 on both papers. Raw ARGB made Navy/Ink fail AA in dark and Mist/Sand
                    // fail in light.
                    CategoryIcon(
                        categoryKey = category.icon,
                        size = 24.dp,
                    )
                    Text(
                        category.displayName(),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                TextButton(
                    onClick = { onArchiveCategory(category.id, true) },
                    modifier = Modifier.heightIn(min = touchTarget),
                ) {
                    Text(stringResource(R.string.settings_archive))
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { addingCategory = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_category_add))
            }
        }
    }

    val editCat = editingCategory
    if (editCat != null) {
        RenameDialog(
            title = stringResource(R.string.settings_rename),
            initial = editCat.customName ?: editCat.displayName(),
            onDismiss = { editingCategory = null },
            onConfirm = { name ->
                onUpsertCategory(
                    editCat.copy(
                        customName = name.trim().ifBlank { null },
                        name = name.trim().ifBlank { editCat.name },
                    ),
                )
                editingCategory = null
            },
            showStylePickers = true,
            selectedIcon = editCat.icon,
            onIcon = { icon ->
                val updated = editCat.copy(icon = icon)
                onUpsertCategory(updated)
                editingCategory = updated
            },
        )
    }

    val editAcc = editingAccount
    if (editAcc != null) {
        RenameDialog(
            title = stringResource(R.string.settings_rename),
            initial = editAcc.customName ?: editAcc.displayName(),
            onDismiss = { editingAccount = null },
            onConfirm = { name ->
                onUpsertAccount(
                    editAcc.copy(
                        customName = name.trim().ifBlank { null },
                        name = name.trim().ifBlank { editAcc.name },
                    ),
                )
                editingAccount = null
            },
        )
    }

    if (addingCategory) {
        RenameDialog(
            title = stringResource(R.string.settings_category_add),
            initial = "",
            onDismiss = { addingCategory = false },
            onConfirm = { name ->
                val trimmed = name.trim()
                if (trimmed.isNotEmpty()) {
                    onUpsertCategory(
                        Category(
                            name = trimmed,
                            kind = CategoryKind.EXPENSE,
                            icon = "other",
                            color = DefaultCategories.colorArgb(15),
                            customName = trimmed,
                        ),
                    )
                }
                addingCategory = false
            },
        )
    }

    if (addingAccount) {
        RenameDialog(
            title = stringResource(R.string.settings_account_add),
            initial = "",
            onDismiss = { addingAccount = false },
            onConfirm = { name ->
                val trimmed = name.trim()
                if (trimmed.isNotEmpty()) {
                    onUpsertAccount(
                        Account(
                            name = trimmed,
                            currency = state.baseCurrency,
                            type = AccountType.OTHER,
                            customName = trimmed,
                            sortOrder = state.accounts.size,
                        ),
                    )
                }
                addingAccount = false
            },
        )
    }
}

/**
 * Rename (and, for categories, restyle) one row. Every icon is reachable: the body scrolls
 * instead of clipping the last rows, and each choice is a 48 dp radio target with its own TalkBack
 * label. Colours are not user-editable: the fixed category palette is tuned for both themes, so a
 * free colour picker would be a promise the chips never kept.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    showStylePickers: Boolean = false,
    selectedIcon: String? = null,
    onIcon: (String) -> Unit = {},
) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.tight),
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showStylePickers) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        CategoryColors.keys.forEach { key ->
                            val selected = key == selectedIcon
                            val label = SeedLabels.categoryRes(key)?.let { stringResource(it) } ?: key
                            StyleOption(
                                selected = selected,
                                label = stringResource(R.string.settings_icon_option, label),
                                onClick = { onIcon(key) },
                            ) {
                                CategoryIcon(categoryKey = key, size = 24.dp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(stringResource(R.string.settings_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

/** A 48 dp selectable target; the glyph or swatch inside stays small so the grid stays calm. */
@Composable
private fun StyleOption(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    val outline = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent
    Box(
        modifier = Modifier
            .size(MaterialTheme.spacing.touchTarget)
            .clip(CircleShape)
            .background(container)
            .then(
                if (selected) Modifier.border(2.dp, outline, CircleShape) else Modifier,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
