package dev.antomanc.tameki.feature.activity

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
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
import dev.antomanc.tameki.core.ui.components.AmountText
import dev.antomanc.tameki.core.ui.components.CategoryChip
import dev.antomanc.tameki.core.ui.components.Numpad
import dev.antomanc.tameki.core.ui.components.SegmentedKind
import dev.antomanc.tameki.core.ui.components.TransactionKind
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.TamekiType
import dev.antomanc.tameki.core.ui.theme.spacing
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
    onScanReceipt: () -> Unit,
    onClearReceipt: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    if (!state.visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // The content owns the gestures it needs (chip rows scroll horizontally, the numpad
        // taps); a draggable sheet made small upward drags rubber-band the whole modal.
        // Dismiss stays on scrim tap, system back and Save.
        sheetGesturesEnabled = false,
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
            onScanReceipt = onScanReceipt,
            onClearReceipt = onClearReceipt,
            onSave = onSave,
            onClose = onDismiss,
            onDelete = onDelete,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onScanReceipt: () -> Unit,
    onClearReceipt: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    val spacing = MaterialTheme.spacing
    val haptics = LocalHapticFeedback.current
    var accountMenu by remember { mutableStateOf(false) }
    var toAccountMenu by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    val today = state.today ?: state.date

    // Save is pinned: on a short screen or at 200% text the chips and the numpad scroll, while
    // the one primary action never leaves the sheet.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = spacing.group),
    ) {
        // The sheet does not drag (sheetGesturesEnabled = false), so dismissal needs a visible
        // target: Close sits above the scroll, where no amount of content can carry it away.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.gutter),
        ) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 48.dp)) {
                AmountText(
                    amount = state.money(),
                    style = TamekiType.hero,
                    dimMinorUnits = true,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.activity_close),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.gutter),
        ) {
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
                    state.categories.forEach { cat ->
                        CategoryChip(
                            name = cat.name,
                            categoryKey = cat.icon,
                            selected = cat.id == state.selectedCategoryId,
                            suggested = cat.id == state.suggestedCategoryId &&
                                cat.id != state.selectedCategoryId,
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Today and Yesterday are one tap; anything else opens the picker with the
                // current date preselected instead of silently jumping to now().
                if (today != null) {
                    val yesterday = today.minusDays(1)
                    FilterChip(
                        selected = state.date == today,
                        onClick = { onSetDate(today) },
                        label = { Text(stringResource(R.string.activity_chip_today)) },
                    )
                    FilterChip(
                        selected = state.date == yesterday,
                        onClick = { onSetDate(yesterday) },
                        label = { Text(stringResource(R.string.activity_chip_yesterday)) },
                    )
                }
                AssistChip(
                    onClick = { pickingDate = true },
                    label = {
                        Text(
                            state.date?.let {
                                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(it)
                            } ?: stringResource(R.string.activity_chip_date),
                        )
                    },
                )

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
                ReceiptChip(
                    receiptPath = state.receiptPath,
                    onScan = onScanReceipt,
                    onClear = onClearReceipt,
                )
            }
            if (state.readingReceipt) {
                Spacer(Modifier.height(spacing.tight))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = stringResource(R.string.activity_receipt_reading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
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
        }
        Spacer(Modifier.height(spacing.group))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.gutter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.tight),
        ) {
            // Deleting is regular ink, not error red: DESIGN reserves red for an over-budget amount.
            // The swipe row's "Delete" uses the same neutral onSurfaceVariant.
            if (state.editingId != null && onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text(
                        text = stringResource(R.string.activity_delete),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave()
                },
                enabled = state.canSave && !state.readingReceipt,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Text(stringResource(R.string.activity_save))
            }
        }
    }

    if (pickingDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (state.date ?: today)?.toEpochDay()?.let { it * MILLIS_PER_DAY },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onSetDate(LocalDate.ofEpochDay(millis / MILLIS_PER_DAY))
                        }
                        pickingDate = false
                    },
                ) {
                    Text(stringResource(R.string.activity_date_set))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * One chip, no menu: launch the document scanner, or show a thumbnail + clear once attached.
 * Label stays "Receipt" / "Scontrino" either way (state never replaces the label).
 */
@Composable
private fun ReceiptChip(
    receiptPath: String?,
    onScan: () -> Unit,
    onClear: () -> Unit,
) {
    val label = stringResource(R.string.activity_chip_receipt)
    if (receiptPath == null) {
        AssistChip(onClick = onScan, label = { Text(label) })
        return
    }
    val thumb = remember(receiptPath) {
        runCatching {
            BitmapFactory.decodeFile(receiptPath)?.let { src ->
                val size = 96
                android.graphics.Bitmap.createScaledBitmap(src, size, size, true)
            }
        }.getOrNull()
    }
    InputChip(
        selected = true,
        onClick = { },
        label = { Text(label) },
        avatar = {
            if (thumb != null) {
                Image(
                    bitmap = thumb.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        },
        trailingIcon = {
            IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.activity_receipt_remove),
                    modifier = Modifier.size(18.dp),
                )
            }
        },
    )
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
            onScanReceipt = {},
            onClearReceipt = {},
            onSave = {},
            onClose = {},
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
            onScanReceipt = {},
            onClearReceipt = {},
            onSave = {},
            onClose = {},
        )
    }
}

@Preview(name = "Quick-add with receipt", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 411)
@Composable
internal fun QuickAddWithReceiptPreview() {
    TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
        QuickAddContent(
            state = sampleQuickAddWithReceipt(),
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
            onScanReceipt = {},
            onClearReceipt = {},
            onSave = {},
            onClose = {},
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
            onScanReceipt = {},
            onClearReceipt = {},
            onSave = {},
            onClose = {},
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

internal fun sampleQuickAddEmpty(): QuickAddUiState {
    val day = LocalDate.of(2026, 11, 18)
    return QuickAddUiState(
        visible = true,
        accounts = sampleAccounts(),
        accountId = 1,
        categories = sampleCategories(),
        date = day,
        today = day,
        currency = CurrencyCode("EUR"),
    )
}

internal fun sampleQuickAddFilled(): QuickAddUiState = sampleQuickAddEmpty().copy(
    amount = AmountDraft.fromMoney(Money(1250, CurrencyCode("EUR"))),
    selectedCategoryId = 2,
    // A ranker hint on a different chip: the screenshots must show selected and suggested apart.
    suggestedCategoryId = 1,
    merchant = "Bar Centrale",
    note = "Cappuccino",
    canSave = true,
)

internal fun sampleQuickAddWithReceipt(): QuickAddUiState = sampleQuickAddFilled().copy(
    receiptPath = "/nonexistent/receipt.jpg",
    receiptId = 1L,
    receiptEngineLabel = "Gemini Nano",
)

internal fun sampleQuickAddTransfer(): QuickAddUiState = sampleQuickAddEmpty().copy(
    kind = TransactionKind.TRANSFER,
    amount = AmountDraft.fromMoney(Money(50000, CurrencyCode("EUR"))),
    accountId = 1,
    toAccountId = 3,
    canSave = true,
)
