package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * The heading of a section: a plain title, an optional one-line explanation, and at most one text
 * action. No kicker above it and no uppercase eyebrow: the title is the heading.
 *
 * The caller supplies horizontal gutters. Section separation is vertical space, not a divider.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            if (!supporting.isNullOrBlank()) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, modifier = Modifier.padding(start = 8.dp)) {
                Text(actionLabel)
            }
        }
    }
}

@Preview(name = "Section header · light", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 380)
@Composable
private fun SectionHeaderLightPreview() {
    PreviewSurface {
        Column {
            SectionHeader(title = "Upcoming", supporting = "Next 7 days")
            SectionHeader(title = "Recent", actionLabel = "See all", onAction = {})
        }
    }
}

@Preview(name = "Section header · dark", showBackground = true, backgroundColor = 0xFF141513, widthDp = 380)
@Composable
private fun SectionHeaderDarkPreview() {
    PreviewSurface(dark = true) {
        SectionHeader(title = "Recent", actionLabel = "See all", onAction = {})
    }
}
