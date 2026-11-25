package dev.antomanc.tameki.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.antomanc.tameki.core.ui.R

/** What a transaction does to the balance. */
enum class TransactionKind {
    /** Money out. The default, and the reason the sheet opens. */
    EXPENSE,

    /** Money in. */
    INCOME,

    /** A move between two own accounts: neither spending nor income. */
    TRANSFER,
}

/**
 * Expense / income / transfer as one Material 3 Expressive connected button group. Expense is
 * first and selected by default because it is what almost every entry is; the other two are one
 * tap away and never look more important than that.
 */
@Composable
fun SegmentedKind(
    selected: TransactionKind,
    onSelect: (TransactionKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    ConnectedToggleGroup(
        options = TransactionKind.entries,
        selected = selected,
        onSelect = onSelect,
        label = { kind ->
            stringResource(
                when (kind) {
                    TransactionKind.EXPENSE -> R.string.ui_kind_expense
                    TransactionKind.INCOME -> R.string.ui_kind_income
                    TransactionKind.TRANSFER -> R.string.ui_kind_transfer
                },
            )
        },
        modifier = modifier,
    )
}

@Preview(name = "Kind · light", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 360)
@Composable
private fun SegmentedKindLightPreview() {
    PreviewSurface {
        val selected = remember { mutableStateOf(TransactionKind.EXPENSE) }
        SegmentedKind(selected = selected.value, onSelect = { selected.value = it })
    }
}

@Preview(name = "Kind · dark", showBackground = true, backgroundColor = 0xFF141513, widthDp = 360)
@Composable
private fun SegmentedKindDarkPreview() {
    PreviewSurface(dark = true) {
        SegmentedKind(selected = TransactionKind.INCOME, onSelect = {})
    }
}
