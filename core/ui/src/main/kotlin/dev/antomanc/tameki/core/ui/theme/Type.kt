package dev.antomanc.tameki.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.antomanc.tameki.core.ui.R

private fun hanken(weight: FontWeight) = Font(
    R.font.hanken_grotesk,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Hanken Grotesk, one grotesque for display and text, the same face Ameki uses. */
val Hanken = FontFamily(
    hanken(FontWeight.Light),
    hanken(FontWeight.Normal),
    hanken(FontWeight.Medium),
    hanken(FontWeight.SemiBold),
    hanken(FontWeight.Bold),
)

/** Tabular figures: amounts line up in columns and do not jitter while they animate. */
internal const val TABULAR_FIGURES = "tnum"

private fun text(
    size: Int,
    line: Int,
    weight: FontWeight,
    tracking: Double = 0.0,
    family: FontFamily = Hanken,
    figures: String? = null,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
    fontFeatureSettings = figures,
)

/**
 * The Material type scale, carried by Hanken Grotesk. Display and headline sizes stay light: on
 * warm paper a heavy headline shouts, and Tameki never shouts about money.
 */
val TamekiTypography = Typography(
    displayLarge = text(88, 92, FontWeight.Light, -0.03, figures = TABULAR_FIGURES),
    displayMedium = text(56, 60, FontWeight.Light, -0.02, figures = TABULAR_FIGURES),
    displaySmall = text(40, 44, FontWeight.Light, -0.01, figures = TABULAR_FIGURES),
    headlineLarge = text(30, 36, FontWeight.Normal),
    headlineMedium = text(26, 32, FontWeight.Normal),
    headlineSmall = text(22, 28, FontWeight.Medium),
    titleLarge = text(20, 26, FontWeight.Medium),
    titleMedium = text(16, 22, FontWeight.Medium, figures = TABULAR_FIGURES),
    titleSmall = text(14, 20, FontWeight.Medium),
    bodyLarge = text(16, 24, FontWeight.Normal),
    bodyMedium = text(14, 20, FontWeight.Normal),
    bodySmall = text(12, 16, FontWeight.Normal),
    labelLarge = text(14, 20, FontWeight.Medium),
    labelMedium = text(12, 16, FontWeight.Medium, 0.02),
    labelSmall = text(11, 16, FontWeight.Medium, 0.06),
)

/**
 * Styles beyond the Material scale that only amounts and the numpad use. Every one of them is
 * tabular: a number that changes must not move its neighbours.
 */
object TamekiType {
    /** Hero figure on Today and Save: the one number the screen is about. */
    val hero = text(60, 64, FontWeight.Light, -0.025, figures = TABULAR_FIGURES)

    /** Hero figure when the number is long or the screen is small. */
    val heroCompact = text(42, 46, FontWeight.Light, -0.02, figures = TABULAR_FIGURES)

    /** Standard list amount, e.g. a transaction value. */
    val amount = text(20, 26, FontWeight.Medium, -0.01, figures = TABULAR_FIGURES)

    /** Smaller list amount, e.g. a day total or a recurring cost. */
    val amountSmall = text(15, 20, FontWeight.Medium, figures = TABULAR_FIGURES)

    /** Smallest amount: chart values, dense tables. Never below 12 sp. */
    val amountTiny = text(12, 16, FontWeight.Medium, figures = TABULAR_FIGURES)

    /** Numpad keys: large, light, unmissable. */
    val numpadKey = text(30, 34, FontWeight.Normal, figures = TABULAR_FIGURES)

    /** Month/category labels inside charts. */
    val chartLabel = text(11, 14, FontWeight.Medium, 0.02, figures = TABULAR_FIGURES)
}
