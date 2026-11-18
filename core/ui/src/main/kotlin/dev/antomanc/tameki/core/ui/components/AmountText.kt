package dev.antomanc.tameki.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.ui.format.MoneyFormatter
import dev.antomanc.tameki.core.ui.format.MoneySign
import dev.antomanc.tameki.core.ui.format.rememberMoneyFormatter
import dev.antomanc.tameki.core.ui.theme.TABULAR_FIGURES
import dev.antomanc.tameki.core.ui.theme.TamekiType
import dev.antomanc.tameki.core.ui.theme.motion
import dev.antomanc.tameki.core.ui.theme.rememberMotionEnabled
import dev.antomanc.tameki.core.ui.theme.tamekiColors

/**
 * An amount of money, formatted for the current locale: tabular figures, a currency symbol that is
 * smaller and lighter than the number, optional dimmed minor units, and a quiet odometer roll when
 * the amount changes.
 *
 * The visual is split into parts, so it never ellipsizes: the caller gives it a width and the
 * amount either fits or the layout wraps around it. TalkBack reads one full amount, sign included,
 * instead of "euro, one, comma, two".
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
    val textStyle = remember(style) { style.copy(fontFeatureSettings = TABULAR_FIGURES) }
    val ink = if (color == Color.Unspecified) LocalContentColor.current else color
    val quieterInk = ink.copy(alpha = 0.76f)
    val showFraction = parts.fraction != null && !(omitZeroMinorUnits && parts.hasZeroFraction)
    val motionEnabled = rememberMotionEnabled()

    var previousMinor by remember { mutableLongStateOf(amount.minor) }
    val increasing = amount.minor >= previousMinor
    SideEffect { previousMinor = amount.minor }

    val spoken = contentDescription ?: parts.text
    Row(
        modifier = modifier.clearAndSetSemantics { this.contentDescription = spoken },
        verticalAlignment = Alignment.Bottom,
    ) {
        val prefix = buildString {
            append(parts.sign)
            if (!parts.symbolAfter) append(parts.symbol)
        }
        if (prefix.isNotEmpty() && !parts.symbolAfter) {
            Affix(prefix, textStyle, ink)
        }
        RollingDigits(
            text = parts.integer,
            style = textStyle,
            color = ink,
            increasing = increasing,
            animate = animateChanges && motionEnabled,
        )
        val fraction = parts.fraction
        if (showFraction && fraction != null) {
            RollingDigits(
                text = parts.decimalSeparator + fraction,
                style = textStyle,
                color = if (dimMinorUnits) quieterInk else ink,
                increasing = increasing,
                animate = animateChanges && motionEnabled,
            )
        }
        if (parts.symbolAfter && parts.symbol.isNotEmpty()) {
            Affix(parts.symbol, textStyle, ink)
        }
    }
}

/** The currency symbol (and the sign when it is a prefix): smaller and lighter than the digits. */
@Composable
private fun Affix(text: String, style: TextStyle, color: Color) {
    val size = style.fontSize.value.takeIf { it > 0f } ?: 16f
    val affixSize = (size * 0.68f).coerceAtLeast(11f)
    Text(
        text = text,
        style = style.copy(
            fontSize = affixSize.sp,
            lineHeight = (affixSize * 1.05f).sp,
            fontWeight = FontWeight.Normal,
            fontFeatureSettings = TABULAR_FIGURES,
        ),
        color = color.copy(alpha = 0.85f),
        maxLines = 1,
        softWrap = false,
    )
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

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFF2F0EA)
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
