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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
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
 * Labels never wrap and never break inside a word: at a large font scale the label style shrinks
 * instead, so "Giroconto" stays one readable line in a third of the width.
 */
@Composable
fun SegmentedKind(
    selected: TransactionKind,
    onSelect: (TransactionKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = TransactionKind.entries
    val fontScale = LocalDensity.current.fontScale
    val labelStyle = MaterialTheme.typography.labelLarge.let { base ->
        if (fontScale <= 1.3f) {
            base
        } else {
            // Cap the rendered size at 130% of the design size; the segment grows in height if needed.
            val size = base.fontSize.value * 1.3f / fontScale
            base.copy(fontSize = size.sp, lineHeight = (size * 1.25f).sp)
        }
    }
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
            SegmentedButton(
                selected = selected == kind,
                onClick = { onSelect(kind) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = colors,
                icon = {},
                label = {
                    Text(
                        fontWeight = if (selected == kind) FontWeight.SemiBold else FontWeight.Normal,
                        style = labelStyle,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
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
