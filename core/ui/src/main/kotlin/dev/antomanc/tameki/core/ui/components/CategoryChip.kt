package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.ui.theme.CategoryColors

/**
 * A category offered for one tap, most-predicted first. The hue is the category's fixed hue, so a
 * chip and the row it later produces share a colour.
 *
 * Selection is a tinted wash plus a hairline of the same hue; unselected chips are tonal paper, not
 * outlined boxes, so a row of twenty candidates still reads as one quiet strip.
 */
@Composable
fun CategoryChip(
    name: String,
    categoryKey: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hue = CategoryColors.hueFor(categoryKey).color()
    val shape = CircleShape
    Surface(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .semantics { this.selected = selected }
            .heightIn(min = 40.dp),
        shape = shape,
        color = if (selected) {
            hue.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = if (selected) BorderStroke(1.dp, hue.copy(alpha = 0.55f)) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryIcon(categoryKey = categoryKey, tint = hue, size = 18.dp)
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(name = "Category chips · light", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 360)
@Composable
private fun CategoryChipLightPreview() {
    PreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryChip("Groceries", "groceries", selected = true, onClick = {})
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
            CategoryChip("Coffee", "coffee", selected = false, onClick = {})
        }
    }
}
