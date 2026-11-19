package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.ui.theme.TamekiTheme

/**
 * A single-choice chip whose selected state does not depend on the tonal fill. The M3 selected
 * container is only about 1.2:1 against paper, so the outline and the check carry the state as
 * well: the tint stays a quiet reinforcement, never the only signal.
 *
 * Every filter row in the app uses this, so "selected" looks the same on Today, Recurring and
 * Save instead of each screen inventing its own chip language.
 */
@Composable
fun SelectableChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        enabled = enabled,
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
        border = if (selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            null
        },
        shape = CircleShape,
    )
}

@Preview(name = "Selectable chip · light", showBackground = true, backgroundColor = 0xFFF2F0EA)
@Composable
private fun SelectableChipLightPreview() {
    TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
        PreviewSurface {
            androidx.compose.foundation.layout.Row {
                SelectableChip(selected = true, onClick = {}, label = "Selected")
                SelectableChip(selected = false, onClick = {}, label = "Not selected")
            }
        }
    }
}
