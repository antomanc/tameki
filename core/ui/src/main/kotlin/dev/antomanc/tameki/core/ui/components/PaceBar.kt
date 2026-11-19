package dev.antomanc.tameki.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.ui.R
import dev.antomanc.tameki.core.ui.format.rememberMoneyFormatter
import dev.antomanc.tameki.core.ui.theme.motion
import dev.antomanc.tameki.core.ui.theme.rememberMotionEnabled
import dev.antomanc.tameki.core.ui.theme.spacing
import dev.antomanc.tameki.core.ui.theme.tamekiColors

/** How spending compares with where it should be by today. */
enum class PaceStatus { ON_TRACK, AT_RISK, OVER }

/** Pure pace arithmetic, kept out of the composable so it can be tested and reused. */
object Pace {
    /**
     * [AT_RISK] means spending is ahead of the expected pace but still inside the budget; [OVER]
     * means the budget is actually exceeded. Without a budget there is no such thing as over.
     */
    fun status(spent: Long, expected: Long, budget: Long?): PaceStatus = when {
        budget != null && spent > budget -> PaceStatus.OVER
        spent > expected -> PaceStatus.AT_RISK
        else -> PaceStatus.ON_TRACK
    }

    /**
     * The value the bar is drawn against. It grows to the spent amount when spending overshoots the
     * budget, so an overspend of 10% and one of 100% do not look identical: the marker slides left.
     */
    fun scale(spent: Long, expected: Long, budget: Long?): Long =
        maxOf(budget ?: 0L, expected, spent).coerceAtLeast(1L)

    /** How much of the bar is filled, always between 0 and 1. */
    fun fill(spent: Long, expected: Long, budget: Long?): Float =
        (spent.toFloat() / scale(spent, expected, budget).toFloat()).coerceIn(0f, 1f)

    /** Where the "expected by today" marker sits, always between 0 and 1. */
    fun marker(spent: Long, expected: Long, budget: Long?): Float =
        (expected.toFloat() / scale(spent, expected, budget).toFloat()).coerceIn(0f, 1f)
}

/**
 * A slim bar showing what has been spent against where it should be by today, with a notch at the
 * expected position. Colour is the status: leaf on track, amber ahead of pace, over only once the
 * budget is actually exceeded. Labels underneath are neutral ink: being ahead of pace is not a
 * failure and never looks like one.
 *
 * @param budget the monthly (or period) budget. Without it the bar can still show pace, but never
 *   turns [PaceStatus.OVER].
 */
@Composable
fun PaceBar(
    spent: Money,
    expected: Money,
    modifier: Modifier = Modifier,
    budget: Money? = null,
    showLabels: Boolean = true,
    height: Dp = 10.dp,
) {
    val formatter = rememberMoneyFormatter()
    val status = Pace.status(spent.minor, expected.minor, budget?.minor)
    val statusColor = when (status) {
        PaceStatus.ON_TRACK -> MaterialTheme.tamekiColors.leaf
        PaceStatus.AT_RISK -> MaterialTheme.tamekiColors.amber
        PaceStatus.OVER -> MaterialTheme.tamekiColors.over
    }
    val statusLabel = stringResource(
        when (status) {
            PaceStatus.ON_TRACK -> R.string.ui_pace_on_track
            PaceStatus.AT_RISK -> R.string.ui_pace_at_risk
            PaceStatus.OVER -> R.string.ui_pace_over
        },
    )
    val spentText = formatter.format(spent, omitZeroFraction = true)
    val expectedText = formatter.format(expected, omitZeroFraction = true)
    val description = stringResource(R.string.ui_pace_a11y, spentText, expectedText, statusLabel)

    val motionEnabled = rememberMotionEnabled()
    val fill by animateFloatAsState(
        targetValue = Pace.fill(spent.minor, expected.minor, budget?.minor),
        // Reduced motion: jump to the new value instead of animating the bar.
        animationSpec = if (motionEnabled) MaterialTheme.motion.spatial else snap(),
        label = "pace fill",
    )
    val marker = Pace.marker(spent.minor, expected.minor, budget?.minor)
    val track = MaterialTheme.tamekiColors.track
    val markerColor = MaterialTheme.tamekiColors.marker
    val separation = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Box(Modifier.fillMaxWidth().height(height + 6.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val inset = 3.dp.toPx()
                val trackHeight = size.height - inset * 2
                val radius = CornerRadius(trackHeight / 2f)
                drawRoundRect(
                    color = track,
                    topLeft = Offset(0f, inset),
                    size = Size(size.width, trackHeight),
                    cornerRadius = radius,
                )
                val filled = size.width * fill
                if (filled > 0f) {
                    drawRoundRect(
                        color = statusColor,
                        topLeft = Offset(0f, inset),
                        size = Size(filled.coerceAtLeast(trackHeight), trackHeight),
                        cornerRadius = radius,
                    )
                }
                val x = (size.width * marker).coerceIn(0f, size.width)
                val notch = 2.dp.toPx()
                drawRect(
                    color = separation,
                    topLeft = Offset(x - notch * 1.4f, 0f),
                    size = Size(notch * 2.8f, size.height),
                )
                drawRoundRect(
                    color = markerColor,
                    topLeft = Offset(x - notch / 2f, 0f),
                    size = Size(notch, size.height),
                    cornerRadius = CornerRadius(notch / 2f),
                )
            }
        }
        if (showLabels) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(
                    text = stringResource(R.string.ui_pace_labels, spentText, expectedText),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(name = "Pace · light", showBackground = true, backgroundColor = 0xFFF2F0EA)
@Composable
private fun PaceBarLightPreview() {
    PreviewSurface {
        Column {
            val spacing = MaterialTheme.spacing
            PaceBar(
                spent = Money(42000, CurrencyCode("EUR")),
                expected = Money(56000, CurrencyCode("EUR")),
                budget = Money(92000, CurrencyCode("EUR")),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(spacing.group))
            PaceBar(
                spent = Money(64000, CurrencyCode("EUR")),
                expected = Money(56000, CurrencyCode("EUR")),
                budget = Money(92000, CurrencyCode("EUR")),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(spacing.group))
            PaceBar(
                spent = Money(101000, CurrencyCode("EUR")),
                expected = Money(56000, CurrencyCode("EUR")),
                budget = Money(92000, CurrencyCode("EUR")),
            )
        }
    }
}

@Preview(name = "Pace · dark", showBackground = true, backgroundColor = 0xFF141513)
@Composable
private fun PaceBarDarkPreview() {
    PreviewSurface(dark = true) {
        PaceBar(
            spent = Money(64000, CurrencyCode("EUR")),
            expected = Money(56000, CurrencyCode("EUR")),
            budget = Money(92000, CurrencyCode("EUR")),
        )
    }
}
