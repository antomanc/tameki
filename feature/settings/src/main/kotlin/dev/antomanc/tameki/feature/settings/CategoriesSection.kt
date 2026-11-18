package dev.antomanc.tameki.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.DefaultCategories
import dev.antomanc.tameki.core.ui.components.CategoryIcon
import dev.antomanc.tameki.core.ui.components.SectionHeader
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

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(MaterialTheme.spacing.gutter),
    ) {
        item { SectionHeader(title = stringResource(R.string.settings_accounts)) }
        items(state.accounts.filter { !it.archived }, key = { "a-${it.id}" }) { account ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { editingAccount = account }
                    .padding(vertical = MaterialTheme.spacing.tight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${account.displayName()} · ${account.currency.value}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                TextButton(onClick = { onArchiveAccount(account.id, true) }) {
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
                Modifier
                    .fillMaxWidth()
                    .clickable { editingCategory = category }
                    .padding(vertical = MaterialTheme.spacing.tight),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.between),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryIcon(
                    categoryKey = category.icon,
                    tint = Color(category.color),
                    size = 24.dp,
                )
                Text(
                    category.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onArchiveCategory(category.id, true) }) {
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
            onColorIndex = { index ->
                val color = DefaultCategories.colorArgb(index)
                val updated = editCat.copy(color = color)
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

@Composable
private fun RenameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    showStylePickers: Boolean = false,
    selectedIcon: String? = null,
    onIcon: (String) -> Unit = {},
    onColorIndex: (Int) -> Unit = {},
) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.tight)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showStylePickers) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoryColors.keys.take(8).forEach { key ->
                            CategoryIcon(
                                categoryKey = key,
                                size = 28.dp,
                                modifier = Modifier.clickable { onIcon(key) },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoryColors.all.take(8).forEachIndexed { index, hue ->
                            Surface(
                                onClick = { onColorIndex(index) },
                                color = hue.light,
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp),
                            ) {
                                Box(Modifier.size(28.dp))
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
