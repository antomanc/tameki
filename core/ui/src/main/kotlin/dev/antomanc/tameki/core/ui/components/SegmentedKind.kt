package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
 * Expense / income / transfer as one Material segmented control. Expense is first and selected by
 * default because it is what almost every entry is; the other two are one tap away and never look
 * more important than that.
 *
 * Labels follow the system font scale, including 200%: the segment grows in height and a long
 * label wraps instead of the type shrinking behind the user's back. The selected segment carries a
 * 2 dp ink border so its state clears non-text contrast even where the tinted container is faint.
 */
@Composable
fun SegmentedKind(
    selected: TransactionKind,
    onSelect: (TransactionKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = TransactionKind.entries
    val colors = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        activeBorderColor = Color.Transparent,
        inactiveContainerColor = Color.Transparent,
        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, kind ->
            val isSelected = selected == kind
            SegmentedButton(
                selected = isSelected,
                onClick = { onSelect(kind) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = colors,
                // The container tint alone is ~1.2:1 on paper. The border is the state the eye
                // can see; the tint only softens it.
                border = SegmentedButtonDefaults.borderStroke(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    width = if (isSelected) 2.dp else 1.dp,
                ),
                icon = {},
                label = {
                    Text(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        softWrap = true,
                        text = stringResource(
                            when (kind) {
                                TransactionKind.EXPENSE -> R.string.ui_kind_expense
                                TransactionKind.INCOME -> R.string.ui_kind_income
                                TransactionKind.TRANSFER -> R.string.ui_kind_transfer
                            },
                        ),
                    )
                },
            )
        }
    }
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
