package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.ui.R
import dev.antomanc.tameki.core.ui.theme.CategoryColors

/**
 * A category offered for one tap, most-predicted first. The hue is the category's fixed hue, so a
 * chip and the row it later produces share a colour.
 *
 * The states must be told apart at a glance, not by a hairline alone:
 * - selected: a strong tinted wash, a 2 dp border and a trailing check;
 * - suggested: a leading filled dot in the full category hue (≥3:1) and a spoken
 *   [stateDescription] — also while selected, when the ranker's pick is the one the user accepted;
 * - plain: tonal paper with no marker, so a row of twenty candidates still reads as one quiet strip.
 */
@Composable
fun CategoryChip(
    name: String,
    categoryKey: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    suggested: Boolean = false,
) {
    val hue = CategoryColors.hueFor(categoryKey).color()
    val shape = CircleShape
    val suggestedLabel = stringResource(R.string.ui_category_suggested)
    Surface(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .semantics {
                this.selected = selected
                // A suggestion is a state, not a label: TalkBack hears "Coffee, Suggested"
                // without the chip having to print a word next to every inferred category. It
                // stays true when the suggested chip is also the selected one: "selected,
                // suggested" is exactly what happened.
                if (suggested) stateDescription = suggestedLabel
            }
            .heightIn(min = 40.dp),
        shape = shape,
        color = if (selected) {
            hue.copy(alpha = 0.26f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
        // The selected chip is a choice: full-strength 2 dp line + check. A suggestion alone keeps
        // the quiet tonal paper, but its filled dot carries the state without a tinted wash.
        border = when {
            selected -> BorderStroke(2.dp, hue.copy(alpha = 0.90f))
            suggested -> BorderStroke(1.dp, hue.copy(alpha = 0.55f))
            else -> null
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (suggested) {
                // The visible twin of the state description: a full-hue dot always clears 3:1,
                // where a 40% hairline on tonal paper does not. A dot beside the check means
                // "the ranker's pick, and you took it", never a third unrelated state.
                Box(
                    Modifier
                        .size(8.dp)
                        .background(hue, CircleShape)
                        .clearAndSetSemantics {},
                )
            }
            CategoryIcon(categoryKey = categoryKey, tint = hue, size = 18.dp)
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = hue,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Preview(name = "Category chips · light", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 360)
@Composable
private fun CategoryChipLightPreview() {
    PreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // The ranker's pick, already accepted: dot + check, one chip.
            CategoryChip("Groceries", "groceries", selected = true, onClick = {}, suggested = true)
            CategoryChip("Coffee", "coffee", selected = false, onClick = {})
            CategoryChip("Transport", "transport", selected = false, onClick = {})
        }
    }
}

@Preview(name = "Category chips · dark", showBackground = true, backgroundColor = 0xFF141513, widthDp = 360)
@Composable
private fun CategoryChipDarkPreview() {
    PreviewSurface(dark = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryChip("Groceries", "groceries", selected = true, onClick = {})
            CategoryChip("Coffee", "coffee", selected = false, onClick = {}, suggested = true)
        }
    }
}
