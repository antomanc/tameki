package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.ui.R
import dev.antomanc.tameki.core.ui.theme.TamekiType
import dev.antomanc.tameki.core.ui.theme.spacing
import dev.antomanc.tameki.core.ui.theme.tamekiColors

/**
 * One saving opportunity: what it is, how much it is worth over a year, and the single action that
 * captures it. Never a scolding: the yearly amount is the loudest thing on the card and it sits in
 * leaf, the colour of money kept.
 *
 * Restraint is part of the contract: [body] is at most one short line of supporting fact
 * ("Yearly plan is 17% cheaper."), never a paragraph explaining the screen. If the title already
 * says it, the body can be empty.
 *
 * @param yearlySaving the estimated saving. It must come from real data; null hides the line
 *   instead of inventing a number.
 * @param onDismiss dismisses (or snoozes) the insight. Always available: nothing nags twice.
 */
@Composable
fun InsightCard(
    title: String,
    body: String? = null,
    actionLabel: String,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    yearlySaving: Money? = null,
    categoryKey: String = "other",
) {
    val spacing = MaterialTheme.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(spacing.cardPadding)) {
            Row(verticalAlignment = Alignment.Top) {
                CategoryAvatar(categoryKey = categoryKey, size = 36.dp)
                Spacer(Modifier.width(spacing.between))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!body.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.padding(start = 4.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.ui_dismiss),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (yearlySaving != null) {
                Spacer(Modifier.height(spacing.group))
                Text(
                    text = stringResource(R.string.ui_insight_yearly_prefix),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    AmountText(
                        amount = yearlySaving,
                        style = TamekiType.amount,
                        color = MaterialTheme.tamekiColors.leaf,
                        dimMinorUnits = true,
                        omitZeroMinorUnits = true,
                        animateChanges = false,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.ui_insight_yearly_suffix),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(spacing.group))
            FilledTonalButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Preview(name = "Insight · light", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 380)
@Composable
private fun InsightCardLightPreview() {
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InsightCard(
                title = "Switch Spotify to the yearly plan",
                body = "Yearly plan is 17% cheaper.",
                yearlySaving = Money(3480, CurrencyCode("EUR")),
                actionLabel = "See the plan",
                onAction = {},
                onDismiss = {},
                categoryKey = "music",
            )
        }
    }
}

@Preview(name = "Insight · dark", showBackground = true, backgroundColor = 0xFF141513, widthDp = 380)
@Composable
private fun InsightCardDarkPreview() {
    PreviewSurface(dark = true) {
        InsightCard(
            title = "Two streaming services overlap",
            body = "One was unused for eight weeks.",
            yearlySaving = Money(11988, CurrencyCode("EUR")),
            actionLabel = "Review subscriptions",
            onAction = {},
            onDismiss = {},
            categoryKey = "streaming",
        )
    }
}
