package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.ui.theme.spacing

/**
 * A dead end, refused: what goes here, why it is empty, and the action that fills it. Left aligned
 * like the rest of the app — a centred "nothing here" with a sad face is not an explanation.
 *
 * The caller supplies horizontal gutters and vertical breathing room.
 *
 * @param categoryKey shows the category's own mark above the title; null for a neutral state.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    categoryKey: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val spacing = MaterialTheme.spacing
    Column(modifier = modifier.widthIn(max = 420.dp)) {
        if (categoryKey != null) {
            CategoryAvatar(categoryKey = categoryKey, size = 56.dp)
            Spacer(Modifier.height(spacing.group))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(spacing.group))
            FilledTonalButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Preview(name = "Empty state · light", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 380)
@Composable
private fun EmptyStateLightPreview() {
    PreviewSurface {
        EmptyState(
            title = "No subscriptions yet",
            body = "Add Netflix, the phone bill or the gym: Tameki turns them into a monthly cost and tells you which one is worth keeping.",
            categoryKey = "streaming",
            actionLabel = "Add a recurring payment",
            onAction = {},
        )
    }
}

@Preview(name = "Empty state · dark", showBackground = true, backgroundColor = 0xFF141513, widthDp = 380)
@Composable
private fun EmptyStateDarkPreview() {
    PreviewSurface(dark = true) {
        EmptyState(
            title = "Nothing to review",
            body = "When a subscription looks unused, it will wait for you here.",
            categoryKey = "other",
        )
    }
}
