package dev.antomanc.tameki.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * Tameki's colour world: warm paper neutrals, ink for text and spending, and three fixed data
 * colours that never follow the wallpaper. Dynamic colour is allowed to own surfaces and accents;
 * anything that carries meaning (leaf / amber / over, category hues) is fixed and tested here.
 */

/** Launcher adaptive-icon background — shared by the onboarding brand mark and store graphics. */
val BrandMarkBackground = Color(0xFF2E6B4A)

/** Warm-paper static light scheme: the fallback when dynamic colour is off or unavailable. */
internal val PaperLightScheme = lightColorScheme(
    primary = Color(0xFF2F4A3A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E7D9),
    onPrimaryContainer = Color(0xFF16301F),
    inversePrimary = Color(0xFFA9CDB3),
    secondary = Color(0xFF5F5B50),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2DBCB),
    onSecondaryContainer = Color(0xFF1E1C16),
    tertiary = Color(0xFF7A4B12),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6E2C0),
    onTertiaryContainer = Color(0xFF2A1902),
    background = Color(0xFFF2F0EA),
    onBackground = Color(0xFF1C1D1A),
    surface = Color(0xFFF2F0EA),
    onSurface = Color(0xFF1C1D1A),
    surfaceVariant = Color(0xFFE3E0D6),
    onSurfaceVariant = Color(0xFF5B5A52),
    surfaceTint = Color(0xFF2F4A3A),
    surfaceContainerLowest = Color(0xFFFCFBF7),
    surfaceContainerLow = Color(0xFFFAF8F3),
    surfaceContainer = Color(0xFFEFEDE6),
    surfaceContainerHigh = Color(0xFFE8E5DD),
    surfaceContainerHighest = Color(0xFFE0DDD4),
    outline = Color(0xFF7C7A70),
    outlineVariant = Color(0xFFCBC7BB),
    error = Color(0xFFB3352A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF7DAD4),
    onErrorContainer = Color(0xFF410E06),
    inverseSurface = Color(0xFF30312D),
    inverseOnSurface = Color(0xFFF2F0EA),
    scrim = Color(0xFF000000),
)

/** Warm-paper static dark scheme: near-black paper, never a pure invert. */
internal val PaperDarkScheme = darkColorScheme(
    primary = Color(0xFFA9CDB3),
    onPrimary = Color(0xFF16301F),
    primaryContainer = Color(0xFF2B4433),
    onPrimaryContainer = Color(0xFFC9E5D2),
    inversePrimary = Color(0xFF2F4A3A),
    secondary = Color(0xFFCCC5B6),
    onSecondary = Color(0xFF322E25),
    secondaryContainer = Color(0xFF47433A),
    onSecondaryContainer = Color(0xFFE9E3D5),
    tertiary = Color(0xFFF2C389),
    onTertiary = Color(0xFF452A05),
    tertiaryContainer = Color(0xFF613F14),
    onTertiaryContainer = Color(0xFFFFDFB0),
    background = Color(0xFF141513),
    onBackground = Color(0xFFECEAE3),
    surface = Color(0xFF141513),
    onSurface = Color(0xFFECEAE3),
    surfaceVariant = Color(0xFF2B2C29),
    onSurfaceVariant = Color(0xFFB9B7AC),
    surfaceTint = Color(0xFFA9CDB3),
    surfaceContainerLowest = Color(0xFF0D0E0C),
    surfaceContainerLow = Color(0xFF1B1C1A),
    surfaceContainer = Color(0xFF202220),
    surfaceContainerHigh = Color(0xFF292B27),
    surfaceContainerHighest = Color(0xFF33352F),
    outline = Color(0xFF908F86),
    outlineVariant = Color(0xFF45473F),
    error = Color(0xFFFFB4A8),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFECEAE3),
    inverseOnSurface = Color(0xFF31322E),
    scrim = Color(0xFF000000),
)

/**
 * Colours that carry meaning. They stay identical whatever the wallpaper does: leaf means kept,
 * saved or on track; amber means attention; over means a budget was actually exceeded.
 */
@Immutable
data class TamekiColors(
    /** Saved / kept / under budget. */
    val leaf: Color,
    /** [leaf] nudged for small text, so a 12–15 sp label still clears 4.5:1 on paper. */
    val leafInk: Color,
    /** Attention: at risk, trial ending, price increase. Never an error. */
    val amber: Color,
    /** Over budget only. Spending itself is ink, never red. */
    val over: Color,
    /** Neutral track behind bars and meters. */
    val track: Color,
    /** Marker for "where you should be today" on a pace bar. */
    val marker: Color,
)

internal val PaperLightColors = TamekiColors(
    leaf = Color(0xFF2E7D4F),
    leafInk = Color(0xFF2B6F49),
    amber = Color(0xFFB7791F),
    over = Color(0xFFC2412D),
    track = Color(0x1F1C1D1A),
    marker = Color(0xFF5B5A52),
)

internal val PaperDarkColors = TamekiColors(
    leaf = Color(0xFF6FCF97),
    leafInk = Color(0xFF6FCF97),
    amber = Color(0xFFF2B544),
    over = Color(0xFFFF7A66),
    track = Color(0x29ECEAE3),
    marker = Color(0xFFB9B7AC),
)

/** Holds [TamekiColors] for the current scheme. Read it through [MaterialTheme.tamekiColors]. */
val LocalTamekiColors = staticCompositionLocalOf { PaperLightColors }

/**
 * One of the sixteen fixed category hues. Each carries a light and a dark variant; both are tuned
 * to read at least 3:1 against the matching warm-paper surface, so they work as icons, bars and
 * small labels. The dark variant is not a brightened copy of the light one: it is re-mixed for the
 * near-black paper so it stays calm instead of glowing.
 */
@Immutable
data class CategoryHue(val light: Color, val dark: Color) {
    /** The variant that belongs on the current surface. */
    @Composable
    @ReadOnlyComposable
    fun color(): Color = if (MaterialTheme.colorScheme.isDarkScheme()) dark else light
}

/** The 16 fixed category hues, plus the key → hue mapping used by every screen. */
object CategoryColors {
    val forest = CategoryHue(Color(0xFF2A6446), Color(0xFF79C79B))
    val teal = CategoryHue(Color(0xFF1B6A66), Color(0xFF5FC7C1))
    val sky = CategoryHue(Color(0xFF1F628F), Color(0xFF7BBCE3))
    val blue = CategoryHue(Color(0xFF34508C), Color(0xFF9BB2E8))
    val slate = CategoryHue(Color(0xFF495A69), Color(0xFFA7BAC9))
    val violet = CategoryHue(Color(0xFF5F4C9A), Color(0xFFB4A4E8))
    val plum = CategoryHue(Color(0xFF7A3C70), Color(0xFFD59DCA))
    val magenta = CategoryHue(Color(0xFF8A3A63), Color(0xFFE39DBF))
    val rose = CategoryHue(Color(0xFF903553), Color(0xFFEDA3B3))
    val clay = CategoryHue(Color(0xFF98402A), Color(0xFFEE9C84))
    val terracotta = CategoryHue(Color(0xFF81502C), Color(0xFFDCAF86))
    val brown = CategoryHue(Color(0xFF6A4F37), Color(0xFFC8AC8D))
    val olive = CategoryHue(Color(0xFF5A6026), Color(0xFFC1C97C))
    val ochre = CategoryHue(Color(0xFF755611), Color(0xFFDCC274))
    val sage = CategoryHue(Color(0xFF4A6A55), Color(0xFFA6C7AD))
    val graphite = CategoryHue(Color(0xFF55534C), Color(0xFFB9B6AA))

    /** All sixteen, in palette order. */
    val all: List<CategoryHue> = listOf(
        forest, teal, sky, blue, slate, violet, plum, magenta,
        rose, clay, terracotta, brown, olive, ochre, sage, graphite,
    )

    /** Every category key the UI knows. Unknown keys fall back to [graphite]. */
    val keys: List<String> = listOf(
        "groceries", "restaurant", "coffee", "transport", "fuel", "home", "utilities", "phone",
        "internet", "health", "pharmacy", "sport", "clothes", "beauty", "gifts", "travel",
        "entertainment", "streaming", "music", "games", "education", "kids", "pets", "insurance",
        "taxes", "bank", "salary", "refund", "investment", "other",
    )

    private val byKey: Map<String, CategoryHue> = mapOf(
        "groceries" to olive,
        "restaurant" to clay,
        "coffee" to brown,
        "transport" to blue,
        "fuel" to slate,
        "home" to terracotta,
        "utilities" to teal,
        "phone" to slate,
        "internet" to sky,
        "health" to forest,
        "pharmacy" to forest,
        "sport" to sage,
        "clothes" to rose,
        "beauty" to magenta,
        "gifts" to rose,
        "travel" to sky,
        "entertainment" to violet,
        "streaming" to plum,
        "music" to magenta,
        "games" to violet,
        "education" to ochre,
        "kids" to sage,
        "pets" to brown,
        "insurance" to slate,
        "taxes" to graphite,
        "bank" to teal,
        "salary" to forest,
        "refund" to teal,
        "investment" to teal,
        "other" to graphite,
    )

    /** The hue for a category key; [graphite] for anything unknown, never a random colour. */
    fun hueFor(key: String): CategoryHue = byKey[key] ?: graphite

    /** The hue for a brand or account name, so a monogram keeps the same colour everywhere. */
    fun hueForName(name: String): CategoryHue = all[Math.floorMod(name.lowercase().hashCode(), all.size)]
}

/** True for dark schemes, including dynamic ones: category hues are picked from this. */
internal fun ColorScheme.isDarkScheme(): Boolean = surface.luminance() < 0.5f

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
