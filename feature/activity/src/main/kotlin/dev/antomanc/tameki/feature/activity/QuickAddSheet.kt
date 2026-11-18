package dev.antomanc.tameki.feature.activity

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.Split
import dev.antomanc.tameki.core.ui.components.AmountText
import dev.antomanc.tameki.core.ui.components.CategoryChip
import dev.antomanc.tameki.core.ui.components.Numpad
import dev.antomanc.tameki.core.ui.components.SegmentedKind
import dev.antomanc.tameki.core.ui.components.TransactionKind
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.TamekiType
import dev.antomanc.tameki.core.ui.theme.spacing
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    state: QuickAddUiState,
    onDismiss: () -> Unit,
    onSelectKind: (TransactionKind) -> Unit,
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onSelectCategory: (Long) -> Unit,
    onSelectAccount: (Long) -> Unit,
    onSelectToAccount: (Long) -> Unit,
    onSetDate: (LocalDate) -> Unit,
    onSetNote: (String?) -> Unit,
    onToggleNote: (Boolean) -> Unit,
    onToggleSplit: (Boolean) -> Unit,
    onSetSplits: (List<Split>) -> Unit,
    onAttachPhoto: (String) -> Unit,
    onReadReceipt: () -> Unit,
    onSave: () -> Unit,
) {
    if (!state.visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        QuickAddContent(
            state = state,
            onSelectKind = onSelectKind,
            onDigit = onDigit,
            onDecimal = onDecimal,
            onBackspace = onBackspace,
            onSelectCategory = onSelectCategory,
            onSelectAccount = onSelectAccount,
            onSelectToAccount = onSelectToAccount,
            onSetDate = onSetDate,
            onSetNote = onSetNote,
            onToggleNote = onToggleNote,
            onToggleSplit = onToggleSplit,
            onSetSplits = onSetSplits,
            onAttachPhoto = onAttachPhoto,
            onReadReceipt = onReadReceipt,
            onSave = onSave,
        )
    }
    if (state.showSplitEditor) {
        SplitEditorSheet(
            total = state.money(),
            categories = state.categories,
            splits = state.splits,
            onDismiss = { onToggleSplit(false) },
            onSave = onSetSplits,
        )
    }
}

@Composable
fun QuickAddContent(
    state: QuickAddUiState,
    onSelectKind: (TransactionKind) -> Unit,
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onSelectCategory: (Long) -> Unit,
    onSelectAccount: (Long) -> Unit,
    onSelectToAccount: (Long) -> Unit,
    onSetDate: (LocalDate) -> Unit,
    onSetNote: (String?) -> Unit,
    onToggleNote: (Boolean) -> Unit,
    onToggleSplit: (Boolean) -> Unit,
    onSetSplits: (List<Split>) -> Unit,
    onAttachPhoto: (String) -> Unit,
    onReadReceipt: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    var photoMenu by remember { mutableStateOf(false) }
    var accountMenu by remember { mutableStateOf(false) }
    var toAccountMenu by remember { mutableStateOf(false) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap == null) return@rememberLauncherForActivityResult
        val tmp = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        FileOutputStream(tmp).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out) }
        onAttachPhoto(tmp.absolutePath)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val tmp = File(context.cacheDir, "picked_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tmp.outputStream().use { output -> input.copyTo(output) }
        }
        onAttachPhoto(tmp.absolutePath)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.gutter)
            .padding(bottom = spacing.group),
    ) {
        AmountText(
            amount = state.money(),
            style = TamekiType.hero,
            dimMinorUnits = true,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(spacing.group))
        SegmentedKind(selected = state.kind, onSelect = onSelectKind)
        Spacer(Modifier.height(spacing.group))
        if (state.kind != TransactionKind.TRANSFER) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.categories.take(12).forEach { cat ->
                    CategoryChip(
                        name = cat.name,
                        categoryKey = cat.icon,
                        selected = cat.id == state.selectedCategoryId,
                        onClick = { onSelectCategory(cat.id) },
                    )
                }
            }
            Spacer(Modifier.height(spacing.tight))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val dateLabel = state.date?.let {
                if (it == LocalDate.now()) {
                    stringResource(R.string.activity_chip_today)
                } else {
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(it)
                }
            } ?: stringResource(R.string.activity_chip_today)
            AssistChip(onClick = { onSetDate(LocalDate.now()) }, label = { Text(dateLabel) })

            val accountName = state.accounts.firstOrNull { it.id == state.accountId }?.name
                ?: stringResource(R.string.activity_chip_account)
            if (state.kind == TransactionKind.TRANSFER) {
                val toName = state.accounts.firstOrNull { it.id == state.toAccountId }?.name
                    ?: stringResource(R.string.activity_chip_to_account)
                TransferAccountsControl(
                    fromName = accountName,
                    toName = toName,
                    onFromClick = { accountMenu = true },
                    onToClick = { toAccountMenu = true },
                )
                DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }) {
                    state.accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                accountMenu = false
                                onSelectAccount(account.id)
                            },
                        )
                    }
                }
                DropdownMenu(expanded = toAccountMenu, onDismissRequest = { toAccountMenu = false }) {
                    state.accounts.filter { it.id != state.accountId }.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                toAccountMenu = false
                                onSelectToAccount(account.id)
                            },
                        )
                    }
                }
            } else {
                AssistChip(onClick = { accountMenu = true }, label = { Text(accountName) })
                DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }) {
                    state.accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                accountMenu = false
                                onSelectAccount(account.id)
                            },
                        )
                    }
                }
            }

            AssistChip(
                onClick = { onToggleNote(true) },
                label = {
                    Text(
                        state.note?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.activity_chip_note),
                    )
                },
            )
            AssistChip(
                onClick = { photoMenu = true },
                label = {
                    Text(
                        if (state.receiptPath != null) {
                            stringResource(R.string.activity_chip_photo_on)
                        } else {
                            stringResource(R.string.activity_chip_photo)
                        },
                    )
                },
            )
            DropdownMenu(expanded = photoMenu, onDismissRequest = { photoMenu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.activity_photo_take)) },
                    onClick = {
                        photoMenu = false
                        takePicture.launch(null)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.activity_photo_pick)) },
                    onClick = {
                        photoMenu = false
                        pickImage.launch("image/*")
                    },
                )
                if (state.receiptPath != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.activity_photo_read)) },
                        onClick = {
                            photoMenu = false
                            onReadReceipt()
                        },
                    )
                }
            }
            if (state.kind != TransactionKind.TRANSFER) {
                AssistChip(
                    onClick = { onToggleSplit(true) },
                    label = {
                        Text(
                            if (state.splits.isEmpty()) {
                                stringResource(R.string.activity_chip_split)
                            } else {
                                stringResource(R.string.activity_chip_split_on, state.splits.size)
                            },
                        )
                    },
                )
            }
        }
        if (state.receiptEngineLabel != null) {
            Text(
                text = stringResource(R.string.activity_receipt_engine, state.receiptEngineLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
        if (state.showNoteEditor) {
            Spacer(Modifier.height(spacing.tight))
            var draft by remember(state.note) { mutableStateOf(state.note.orEmpty()) }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.activity_chip_note)) },
                singleLine = true,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onSetNote(draft) }) {
                    Text(stringResource(R.string.activity_save))
                }
            }
        }
        Spacer(Modifier.height(spacing.group))
        Numpad(onDigit = onDigit, onDecimal = onDecimal, onBackspace = onBackspace)
        Spacer(Modifier.height(spacing.group))
        Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onSave()
            },
            enabled = state.canSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(stringResource(R.string.activity_save))
        }
    }
}

/**
 * The direction of a transfer as one control: "Carta → Conto". Two tap targets, one pill: the
 * arrow says what the two names mean, so they never read as two unrelated account chips.
 */
@Composable
private fun TransferAccountsControl(
    fromName: String,
    toName: String,
    onFromClick: () -> Unit,
    onToClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onFromClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(text = fromName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.activity_chip_to_account),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onToClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(text = toName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitEditorSheet(
    total: Money,
    categories: List<Category>,
    splits: List<Split>,
    onDismiss: () -> Unit,
    onSave: (List<Split>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var lines by remember(splits) {
        mutableStateOf(
            splits.ifEmpty {
                listOf(Split(categories.firstOrNull()?.id ?: 0L, total))
            },
        )
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(MaterialTheme.spacing.gutter)) {
            Text(
                text = stringResource(R.string.activity_split_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(12.dp))
            lines.forEachIndexed { index, split ->
                Text(
                    text = categories.firstOrNull { it.id == split.categoryId }?.name ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                )
                AmountText(amount = split.amount, style = TamekiType.amountSmall)
                if (index < lines.lastIndex) Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSave(lines) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.activity_save))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Preview(name = "Quick-add empty", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 411)
@Composable
internal fun QuickAddEmptyPreview() {
    TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
        QuickAddContent(
            state = sampleQuickAddEmpty(),
            onSelectKind = {},
            onDigit = {},
            onDecimal = {},
            onBackspace = {},
            onSelectCategory = {},
            onSelectAccount = {},
            onSelectToAccount = {},
            onSetDate = {},
            onSetNote = {},
            onToggleNote = {},
            onToggleSplit = {},
            onSetSplits = {},
            onAttachPhoto = {},
            onReadReceipt = {},
            onSave = {},
        )
    }
}

@Preview(name = "Quick-add filled", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 411)
@Composable
internal fun QuickAddFilledPreview() {
    TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
        QuickAddContent(
            state = sampleQuickAddFilled(),
            onSelectKind = {},
            onDigit = {},
            onDecimal = {},
            onBackspace = {},
            onSelectCategory = {},
            onSelectAccount = {},
            onSelectToAccount = {},
            onSetDate = {},
            onSetNote = {},
            onToggleNote = {},
            onToggleSplit = {},
            onSetSplits = {},
            onAttachPhoto = {},
            onReadReceipt = {},
            onSave = {},
        )
    }
}

@Preview(name = "Quick-add transfer", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 411)
@Composable
internal fun QuickAddTransferPreview() {
    TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
        QuickAddContent(
            state = sampleQuickAddTransfer(),
            onSelectKind = {},
            onDigit = {},
            onDecimal = {},
            onBackspace = {},
            onSelectCategory = {},
            onSelectAccount = {},
            onSelectToAccount = {},
            onSetDate = {},
            onSetNote = {},
            onToggleNote = {},
            onToggleSplit = {},
            onSetSplits = {},
            onAttachPhoto = {},
            onReadReceipt = {},
            onSave = {},
        )
    }
}

internal fun sampleAccounts(): List<Account> {
    val eur = CurrencyCode("EUR")
    return listOf(
        Account(1, "Carta", eur, AccountType.CARD),
        Account(2, "Contanti", eur, AccountType.CASH),
        Account(3, "Conto", eur, AccountType.BANK),
    )
}

internal fun sampleCategories(): List<Category> = listOf(
    Category(1, "Alimentari", CategoryKind.EXPENSE, "groceries", 0xFF4CAF50),
    Category(2, "Caffè", CategoryKind.EXPENSE, "coffee", 0xFF795548),
    Category(3, "Trasporti", CategoryKind.EXPENSE, "transport", 0xFF2196F3),
    Category(4, "Ristorante", CategoryKind.EXPENSE, "restaurant", 0xFFE91E63),
)

internal fun sampleQuickAddEmpty(): QuickAddUiState = QuickAddUiState(
    visible = true,
    accounts = sampleAccounts(),
    accountId = 1,
    categories = sampleCategories(),
    date = LocalDate.of(2026, 11, 18),
    currency = CurrencyCode("EUR"),
)

internal fun sampleQuickAddFilled(): QuickAddUiState = sampleQuickAddEmpty().copy(
    amount = AmountDraft.fromMoney(Money(1250, CurrencyCode("EUR"))),
    selectedCategoryId = 2,
    merchant = "Bar Centrale",
    note = "Cappuccino",
    canSave = true,
)

internal fun sampleQuickAddTransfer(): QuickAddUiState = sampleQuickAddEmpty().copy(
    kind = TransactionKind.TRANSFER,
    amount = AmountDraft.fromMoney(Money(50000, CurrencyCode("EUR"))),
    accountId = 1,
    toAccountId = 3,
    canSave = true,
)
