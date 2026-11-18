package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.ui.R
import dev.antomanc.tameki.core.ui.format.rememberMoneyFormatter
import dev.antomanc.tameki.core.ui.theme.spacing
import dev.antomanc.tameki.core.ui.theme.tamekiColors

/**
 * One saving opportunity: what it is, how much it is worth over a year, and the single action that
 * captures it. Never a scolding: the yearly amount sits on the title row in leaf, the colour of
 * money kept, in whole units ("€419/anno").
 *
 * The card is deliberately compact: a mark, one title, at most one short supporting line, the
 * yearly amount, one text action and a close icon. No hero amount block, no second button: the
 * cards in a ranked list must read as rows, not as a page each. The amount shares the title row at
 * normal sizes; past 150% font scale it moves under the title, right-aligned, so the title is never
 * squeezed into an ellipsis.
 *
 * @param yearlySaving the estimated saving. It must come from real data; null hides the amount
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
    val formatter = rememberMoneyFormatter()
    val fontScale = LocalDensity.current.fontScale
    val stackSaving = fontScale > 1.5f
    val savingText = yearlySaving?.let {
        stringResource(R.string.ui_insight_yearly_amount, formatter.formatWhole(it))
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.padding(
                start = spacing.group,
                end = spacing.between,
                top = spacing.between,
                bottom = 2.dp,
            ),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                CategoryAvatar(
                    categoryKey = categoryKey,
                    size = 32.dp,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Spacer(Modifier.width(spacing.between))
                Column(Modifier.weight(1f)) {
                    if (stackSaving) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (savingText != null) {
                            Text(
                                text = savingText,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.tamekiColors.leaf,
                                textAlign = TextAlign.End,
                                softWrap = false,
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (savingText != null) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = savingText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.tamekiColors.leaf,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                    if (!body.isNullOrBlank()) {
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.ui_dismiss),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
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
            InsightCard(
                title = "Two streaming services overlap",
                yearlySaving = Money(11988, CurrencyCode("EUR")),
                actionLabel = "Compare",
                onAction = {},
                onDismiss = {},
                categoryKey = "streaming",
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
            actionLabel = "Compare",
            onAction = {},
            onDismiss = {},
            categoryKey = "streaming",
        )
    }
}
