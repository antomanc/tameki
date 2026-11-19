package dev.antomanc.tameki.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.ui.format.FormattedMoney
import dev.antomanc.tameki.core.ui.format.MoneyFormatter
import dev.antomanc.tameki.core.ui.format.MoneySign
import dev.antomanc.tameki.core.ui.format.rememberMoneyFormatter
import dev.antomanc.tameki.core.ui.theme.TABULAR_FIGURES
import dev.antomanc.tameki.core.ui.theme.TamekiType
import dev.antomanc.tameki.core.ui.theme.motion
import dev.antomanc.tameki.core.ui.theme.rememberMotionEnabled
import dev.antomanc.tameki.core.ui.theme.tamekiColors

/** At and above this size an amount is a hero figure and gets the superscript currency symbol. */
private const val HeroThresholdSp = 36f

/** The hero symbol is a little under half the height of the digits, as premium banking apps do. */
private const val HeroSymbolRatio = 0.45f

/** Hanken Grotesk's cap height as a share of the em: what the superscript symbol aligns to. */
private const val CapHeightRatio = 0.72f

/** The smallest a hero figure may shrink to, so a long amount is measured, never clipped. */
private const val HeroMinimumSp = 26f

/**
 * An amount of money, formatted for the current locale: tabular figures, a currency symbol that is
 * lighter than the number, optional dimmed minor units, and a quiet odometer roll when the amount
 * changes.
 *
 * Two layouts, chosen from the type size:
 * - **Hero figures** (36 sp and up) set the symbol at 45% of the digit size, lifted so its cap
 *   height meets the top of the digits — superscript, like a bank statement figure. A hero amount
 *   that would not fit the width it is given scales down from its own size instead of snapping to
 *   a smaller fixed style, so at 200% font scale it still grows with the system setting until the
 *   width is used up. It is measured, never clipped or ellipsized.
 * - **Everything else** sets the symbol and the sign at the same size as the digits, on the same
 *   baseline, with the symbol in [androidx.compose.material3.ColorScheme.onSurfaceVariant] so the
 *   number stays the loudest thing in the row. Never a subscript.
 *
 * TalkBack reads one full amount, sign included, instead of "euro, one, comma, two".
 *
 * A custom [color] is dimmed for the symbol and minor units only while the dimmed tone still
 * clears the contrast floor on the surface; semantic inks such as leaf stay at full strength when
 * fading them would push an income figure below AA.
 *
 * @param sign whether to show "-" / "+". Use [MoneySign.EXPLICIT] for income.
 * @param dimMinorUnits render the cents slightly quieter, as the hero figures do.
 * @param omitZeroMinorUnits hide ",00" when the amount is whole; never hides real cents.
 * @param animateChanges roll the digits when the value changes. Skipped when the system asks for
 *   reduced motion.
 */
@Composable
fun AmountText(
    amount: Money,
    modifier: Modifier = Modifier,
    style: TextStyle = TamekiType.amount,
    color: Color = Color.Unspecified,
    sign: MoneySign = MoneySign.AUTO,
    dimMinorUnits: Boolean = false,
    omitZeroMinorUnits: Boolean = false,
    animateChanges: Boolean = true,
    formatter: MoneyFormatter = rememberMoneyFormatter(),
    contentDescription: String? = null,
) {
    val parts = remember(amount, formatter, sign) { formatter.parts(amount, sign) }
    val baseStyle = remember(style) { style.copy(fontFeatureSettings = TABULAR_FIGURES) }
    val ink = if (color == Color.Unspecified) LocalContentColor.current else color
    val hero = style.fontSize.value >= HeroThresholdSp
    // WCAG calls a hero figure large text (3:1); everything smaller, including the superscript
    // currency symbol, needs 4.5:1. A custom ink is dimmed only as far as still clears that bar;
    // semantic colours such as leafInk are already near the floor, and fading them to 72% took
    // income to ~3.1:1 on paper. Quiet is a bonus, never a reason to fail AA.
    val quietFloor = if (hero) 3f else 4.5f
    val backdrop = MaterialTheme.colorScheme.surface
    val symbolInk = if (color == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        remember(ink, backdrop, quietFloor) {
            quietInk(ink, backdrop, minContrast = quietFloor, alpha = 0.72f)
        }
    }
    val quieterInk = remember(ink, backdrop, quietFloor) {
        quietInk(ink, backdrop, minContrast = quietFloor, alpha = 0.76f)
    }
    val showFraction = parts.fraction != null && !(omitZeroMinorUnits && parts.hasZeroFraction)
    val motionEnabled = rememberMotionEnabled()
    val measurer = rememberTextMeasurer()
    val spoken = contentDescription ?: parts.text

    var previousMinor by remember { mutableLongStateOf(amount.minor) }
    val increasing = amount.minor >= previousMinor
    SideEffect { previousMinor = amount.minor }

    BoxWithConstraints(
        modifier = modifier.clearAndSetSemantics { this.contentDescription = spoken },
        propagateMinConstraints = false,
    ) {
        val available = if (constraints.hasBoundedWidth) constraints.maxWidth else null
        val fontScale = LocalDensity.current.fontScale
        val resolvedStyle = if (hero && available != null) {
            remember(parts, baseStyle, available, showFraction, fontScale) {
                heroStyleThatFits(
                    measurer = measurer,
                    parts = parts,
                    showFraction = showFraction,
                    base = baseStyle,
                    maxWidthPx = available,
                    fontScale = fontScale,
                )
            }
        } else {
            baseStyle
        }
        AmountRow(
            parts = parts,
            style = resolvedStyle,
            hero = hero,
            ink = ink,
            symbolInk = symbolInk,
            minorInk = if (dimMinorUnits) quieterInk else ink,
            showFraction = showFraction,
            increasing = increasing,
            animate = animateChanges && motionEnabled,
        )
    }
}

/**
 * How the pieces of an amount are laid out on one line: sign, symbol, digits, cents. [hero] only
 * changes how the symbol is set; digits and cents share a size in both modes.
 */
@Composable
private fun AmountRow(
    parts: FormattedMoney,
    style: TextStyle,
    hero: Boolean,
    ink: Color,
    symbolInk: Color,
    minorInk: Color,
    showFraction: Boolean,
    increasing: Boolean,
    animate: Boolean,
) {
    // Hero figures align the tops of the glyph lines, which lands the smaller symbol's cap height
    // on the digits' cap height; every other size shares one baseline with the digits.
    val alignment = if (hero) Alignment.Top else Alignment.Bottom
    Row(verticalAlignment = alignment) {
        if (parts.sign.isNotEmpty()) {
            DigitText(text = parts.sign, style = style, color = ink)
        }
        if (!parts.symbolAfter && parts.symbol.isNotEmpty()) {
            CurrencySymbol(parts.symbol, style, symbolInk, hero)
        }
        RollingDigits(
            text = parts.integer,
            style = style,
            color = ink,
            increasing = increasing,
            animate = animate,
        )
        val fraction = parts.fraction
        if (showFraction && fraction != null) {
            RollingDigits(
                text = parts.decimalSeparator + fraction,
                style = style,
                color = minorInk,
                increasing = increasing,
                animate = animate,
            )
        }
        if (parts.symbolAfter && parts.symbol.isNotEmpty()) {
            CurrencySymbol(parts.symbol, style, symbolInk, hero)
        }
    }
}

/**
 * The currency symbol: lighter than the number and, in a hero figure, small and set at the cap
 * height. In every other size it is exactly as large as the digits and sits on their baseline.
 */
@Composable
private fun CurrencySymbol(text: String, style: TextStyle, color: Color, hero: Boolean) {
    if (!hero) {
        DigitText(text = text, style = style, color = color)
        return
    }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val size = style.fontSize.value.takeIf { it > 0f } ?: 16f
    val symbolSize = (size * HeroSymbolRatio).coerceAtLeast(10f)
    val symbolStyle = style.copy(
        fontSize = symbolSize.sp,
        lineHeight = (symbolSize * 1.05f).sp,
        fontWeight = FontWeight.Normal,
        fontFeatureSettings = TABULAR_FIGURES,
    )
    // Top-aligned boxes do not align cap heights when the two sizes differ. Measure both baselines
    // and shift the symbol by exactly the cap-height difference, so its top meets the digits' top.
    val lift = remember(style, symbolStyle, text) {
        val digitsBaseline = measurer.measure(AnnotatedString("0"), style, softWrap = false).firstBaseline
        val symbolBaseline = measurer
            .measure(AnnotatedString(text.ifEmpty { "€" }), symbolStyle, softWrap = false)
            .firstBaseline
        val digitsPx = with(density) { style.fontSize.toPx() }
        val symbolPx = with(density) { symbolStyle.fontSize.toPx() }
        val px = (digitsBaseline - symbolBaseline) + CapHeightRatio * (symbolPx - digitsPx)
        with(density) { px.toDp() }
    }
    Text(
        text = text,
        style = symbolStyle,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.offset(y = lift),
    )
}

@Composable
private fun DigitText(text: String, style: TextStyle, color: Color) {
    Text(text = text, style = style, color = color, maxLines = 1, softWrap = false)
}

/**
 * The largest hero style that fits [maxWidthPx] and the user's font scale. Android 14's
 * non-linear scaling leaves very large text almost unchanged (a 60 sp hero grows ~10% at 200%),
 * which breaks "one number first"; so the hero deliberately grows toward the requested scale (up
 * to 1.5×) as long as it fits, then shrinks continuously for long amounts. Never below
 * [HeroMinimumSp].
 */
private fun heroStyleThatFits(
    measurer: TextMeasurer,
    parts: FormattedMoney,
    showFraction: Boolean,
    base: TextStyle,
    maxWidthPx: Int,
    fontScale: Float,
): TextStyle {
    val text = buildString {
        append(parts.sign)
        append(parts.symbol)
        append(parts.integer)
        if (showFraction && parts.fraction != null) {
            append(parts.decimalSeparator)
            append(parts.fraction)
        }
    }
    fun widthOf(candidate: TextStyle): Int =
        measurer.measure(AnnotatedString(text), candidate, softWrap = false, maxLines = 1).size.width

    val baseSize = base.fontSize.value.takeIf { it > 0f } ?: HeroMinimumSp
    fun sized(scale: Float): TextStyle {
        val size = (baseSize * scale).coerceAtLeast(HeroMinimumSp)
        return base.copy(fontSize = size.sp, lineHeight = (size * 1.06f).sp)
    }

    val baseWidth = widthOf(base)
    if (baseWidth <= maxWidthPx) {
        val desired = fontScale.coerceIn(1f, MaxHeroScale)
        if (desired <= 1f) return base
        if (widthOf(sized(desired)) <= maxWidthPx) return sized(desired)
        var tooLittle = 1f
        var enough = desired
        repeat(10) {
            val middle = (tooLittle + enough) / 2f
            if (widthOf(sized(middle)) <= maxWidthPx) tooLittle = middle else enough = middle
        }
        return sized(tooLittle)
    }

    val ratio = (maxWidthPx.toFloat() / baseWidth.toFloat()).coerceIn(HeroMinimumSp / baseSize, 1f)
    return sized(ratio)
}

/** A hero may grow half again with the system font scale, then width takes over. */
private const val MaxHeroScale = 1.5f

/**
 * The quiet tone for a custom ink: [ink] at [alpha] over [backdrop], but only while that still
 * clears [minContrast]. When it does not, the full-strength ink is returned instead — a symbol
 * that is slightly too loud is readable; a faded one at 3:1 is not.
 *
 * The result is opaque, so a composited quiet ink cannot change again when it is drawn on a tonal
 * card whose background differs slightly from [backdrop].
 */
internal fun quietInk(ink: Color, backdrop: Color, minContrast: Float, alpha: Float): Color {
    val quiet = ink.copy(alpha = alpha).compositeOver(backdrop)
    return if (contrastRatio(quiet, backdrop) >= minContrast) quiet else ink
}

/**
 * Digits that roll. Each digit keeps a tabular cell width so nothing around it moves, which is
 * exactly why the whole type scale carries `tnum`.
 */
@Composable
private fun RollingDigits(
    text: String,
    style: TextStyle,
    color: Color,
    increasing: Boolean,
    animate: Boolean,
) {
    if (!animate) {
        Text(text = text, style = style, color = color, maxLines = 1, softWrap = false)
        return
    }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val digitWidth: Dp = remember(style, density) {
        val zeroPair = measurer.measure(AnnotatedString("00"), style, softWrap = false).size.width
        with(density) { (zeroPair / 2f).toDp() }
    }
    val roll = MaterialTheme.motion.amountRoll
    val rollOffset = spring<IntOffset>(dampingRatio = roll.dampingRatio, stiffness = roll.stiffness)

    Row(verticalAlignment = Alignment.Bottom) {
        text.forEachIndexed { index, character ->
            key(index) {
                if (!character.isDigit()) {
                    Text(
                        text = character.toString(),
                        style = style,
                        color = color,
                        maxLines = 1,
                        softWrap = false,
                    )
                } else {
                    AnimatedContent(
                        targetState = character,
                        modifier = Modifier.width(digitWidth),
                        transitionSpec = {
                            val up = increasing
                            val enter = slideInVertically(animationSpec = rollOffset) { height ->
                                if (up) height else -height
                            } + fadeIn(tween(durationMillis = 110))
                            val exit = slideOutVertically(animationSpec = rollOffset) { height ->
                                if (up) -height else height
                            } + fadeOut(tween(durationMillis = 90))
                            enter togetherWith exit
                        },
                        label = "amount digit",
                    ) { digit ->
                        Text(
                            text = digit.toString(),
                            style = style,
                            color = color,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 380)
@Composable
private fun AmountTextLightPreview() {
    PreviewSurface {
        Column {
            AmountText(Money(128450, CurrencyCode("EUR")), style = TamekiType.hero, dimMinorUnits = true)
            AmountText(Money(4999, CurrencyCode("EUR")), style = TamekiType.amount)
            AmountText(Money(2200, CurrencyCode("EUR")), style = TamekiType.amount, sign = MoneySign.EXPLICIT, color = MaterialTheme.tamekiColors.leaf)
            AmountText(Money(-1530, CurrencyCode("EUR")), style = TamekiType.amountSmall)
            AmountText(Money(120000, CurrencyCode("EUR")), style = TamekiType.amount, omitZeroMinorUnits = true)
        }
    }
}

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF141513)
@Composable
private fun AmountTextDarkPreview() {
    PreviewSurface(dark = true) {
        AmountText(Money(128450, CurrencyCode("EUR")), style = TamekiType.hero, dimMinorUnits = true)
    }
}
