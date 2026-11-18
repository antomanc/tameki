package dev.antomanc.tameki.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.ui.R
import dev.antomanc.tameki.core.ui.format.rememberMoneyFormatter
import dev.antomanc.tameki.core.ui.theme.CategoryColors
import dev.antomanc.tameki.core.ui.theme.TamekiType
import dev.antomanc.tameki.core.ui.theme.motion
import dev.antomanc.tameki.core.ui.theme.rememberMotionEnabled
import dev.antomanc.tameki.core.ui.theme.tamekiColors

/** One month of spending: the label is already localised ("Nov", "nov"). */
@Immutable
data class MonthBar(val label: String, val amount: Money)

/**
 * A handful of months of spending as vertical bars: no axis legend, the current month in ink and
 * the others quiet, values written above the bars when they fit, and one sentence for TalkBack
 * with every month and amount.
 *
 * The highlighted bar is ink, never leaf: leaf means money saved, and a month of spending is not
 * a saving. Value labels are whole currency units (€1.285), the same shape for every bar, so the
 * row reads as one line of numbers.
 *
 * Bars are drawn with Compose Canvas; labels are real text, so they scale with the system font
 * size. Past 130% font scale the per-bar values give way to one caption about the highlighted
 * month — an amount is never ellipsized to fit a chart.
 *
 * @param highlightedIndex the month that carries the message, normally the current one.
 */
@Composable
fun MonthlyBars(
    bars: List<MonthBar>,
    modifier: Modifier = Modifier,
    highlightedIndex: Int = bars.lastIndex,
    height: Dp = 152.dp,
) {
    if (bars.isEmpty()) {
        ChartEmpty(modifier)
        return
    }
    val formatter = rememberMoneyFormatter()
    val amounts = bars.map { formatter.formatWhole(it.amount) }
    val description = stringResource(
        R.string.ui_chart_monthly_a11y,
        bars.indices.joinToString(separator = "; ") { index -> "${bars[index].label} ${amounts[index]}" },
    )
    val maximum = bars.maxOf { it.amount.minor }.coerceAtLeast(1L)
    val density = LocalDensity.current
    val showEveryValue = density.fontScale <= 1.3f
    val highlight = highlightedIndex.coerceIn(0, bars.lastIndex)
    val labelHeight = with(density) { (TamekiType.amountTiny.fontSize.value * density.fontScale).sp.toDp() }
    val labelSpace = if (showEveryValue) labelHeight + 6.dp else 0.dp
    val barArea = (height - labelSpace).coerceAtLeast(40.dp)
    val motionEnabled = rememberMotionEnabled()
    val highlightColor = MaterialTheme.colorScheme.onSurface
    val quietColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    val valueColor = MaterialTheme.colorScheme.onSurface
    val quietValueColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier.clearAndSetSemantics { contentDescription = description }) {
        if (!showEveryValue) {
            Text(
                text = "${bars[highlight].label} · ${amounts[highlight]}",
                style = TamekiType.amountTiny,
                color = valueColor,
            )
            Spacer(Modifier.height(6.dp))
        }
        Row(Modifier.fillMaxWidth().height(height)) {
            bars.forEachIndexed { index, bar ->
                val fraction = (bar.amount.minor.toFloat() / maximum.toFloat()).coerceIn(0f, 1f)
                val animated = animatedFraction(fraction, motionEnabled)
                val highlighted = index == highlight
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    if (showEveryValue) {
                        Text(
                            text = amounts[index],
                            style = TamekiType.amountTiny,
                            color = if (highlighted) valueColor else quietValueColor,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    val filledHeight = if (bar.amount.minor == 0L) 2.dp else (barArea * animated).coerceAtLeast(3.dp)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth(0.52f)
                            .height(filledHeight),
                    ) {
                        val radius = CornerRadius(4.dp.toPx())
                        drawRoundRect(
                            color = if (highlighted) highlightColor else quietColor,
                            cornerRadius = radius,
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 6.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        )
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            bars.forEachIndexed { index, bar ->
                Text(
                    text = bar.label,
                    modifier = Modifier.weight(1f),
                    style = TamekiType.chartLabel,
                    color = if (index == highlight) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** One category of spending: the label and amount are the direct labels of the horizontal bar. */
@Immutable
data class CategoryBar(val label: String, val amount: Money, val categoryKey: String)

/**
 * The category breakdown as horizontal bars, largest first, each with its fixed category hue, its
 * name and its amount at the edge of the bar. No pie, no legend, no percentages to decode: the
 * amount is the label.
 *
 * @param onClick optional row action (e.g. filter Activity by that category).
 */
@Composable
fun CategoryBars(
    rows: List<CategoryBar>,
    modifier: Modifier = Modifier,
    onClick: ((Int) -> Unit)? = null,
) {
    if (rows.isEmpty()) {
        ChartEmpty(modifier)
        return
    }
    val formatter = rememberMoneyFormatter()
    val total = rows.sumOf { it.amount.minor }.coerceAtLeast(1L)
    val maximum = rows.maxOf { it.amount.minor }.coerceAtLeast(1L)
    val motionEnabled = rememberMotionEnabled()

    Column(modifier) {
        rows.forEachIndexed { index, row ->
            val rowDescription = stringResource(
                R.string.ui_chart_category_a11y,
                row.label,
                formatter.format(row.amount),
                ((row.amount.minor * 100) / total).toInt(),
            )
            val rowContent: @Composable () -> Unit = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryIcon(categoryKey = row.categoryKey, size = 18.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = row.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        AmountText(
                            amount = row.amount,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            animateChanges = false,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    val fraction = (row.amount.minor.toFloat() / maximum.toFloat()).coerceIn(0f, 1f)
                    val animated = animatedFraction(fraction, motionEnabled)
                    val hue = CategoryColors.hueFor(row.categoryKey).color()
                    val trackColor = MaterialTheme.tamekiColors.track
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                    ) {
                        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                            val radius = CornerRadius(size.height / 2f)
                            drawRoundRect(
                                color = trackColor,
                                cornerRadius = radius,
                            )
                            if (animated > 0f) {
                                drawRoundRect(
                                    color = hue,
                                    size = Size(size.width * animated, size.height),
                                    cornerRadius = radius,
                                )
                            }
                        }
                    }
                }
            }
            if (onClick != null) {
                Surface(
                    onClick = { onClick(index) },
                    color = Color.Transparent,
                ) {
                    Box(Modifier.clearAndSetSemantics { contentDescription = rowDescription }) {
                        rowContent()
                    }
                }
            } else {
                Box(Modifier.clearAndSetSemantics { contentDescription = rowDescription }) {
                    rowContent()
                }
            }
        }
    }
}

@Composable
private fun ChartEmpty(modifier: Modifier) {
    Box(modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.ui_chart_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun animatedFraction(target: Float, enabled: Boolean): Float {
    if (!enabled) return target
    val value by animateFloatAsState(
        targetValue = target,
        animationSpec = MaterialTheme.motion.gentle,
        label = "chart bar",
    )
    return value
}

@Preview(name = "Charts · light", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 380)
@Composable
private fun ChartsLightPreview() {
    PreviewSurface {
        val eur = CurrencyCode("EUR")
        Column {
            MonthlyBars(
                bars = listOf(
                    MonthBar("Jun", Money(91200, eur)),
                    MonthBar("Jul", Money(104500, eur)),
                    MonthBar("Aug", Money(98700, eur)),
                    MonthBar("Sep", Money(128450, eur)),
                    MonthBar("Oct", Money(110200, eur)),
                    MonthBar("Nov", Money(74200, eur)),
                ),
            )
            Spacer(Modifier.height(24.dp))
            CategoryBars(
                rows = listOf(
                    CategoryBar("Groceries", Money(31240, eur), "groceries"),
                    CategoryBar("Home", Money(21500, eur), "home"),
                    CategoryBar("Restaurants", Money(14900, eur), "restaurant"),
                    CategoryBar("Transport", Money(8600, eur), "transport"),
                    CategoryBar("Streaming", Money(3297, eur), "streaming"),
                ),
            )
        }
    }
}

@Preview(name = "Charts · dark", showBackground = true, backgroundColor = 0xFF141513, widthDp = 380)
@Composable
private fun ChartsDarkPreview() {
    PreviewSurface(dark = true) {
        MonthlyBars(
            bars = listOf(
                MonthBar("Sep", Money(128450, CurrencyCode("EUR"))),
                MonthBar("Oct", Money(110200, CurrencyCode("EUR"))),
                MonthBar("Nov", Money(74200, CurrencyCode("EUR"))),
            ),
        )
    }
}
