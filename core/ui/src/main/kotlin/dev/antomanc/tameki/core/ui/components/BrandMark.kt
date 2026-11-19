package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.ui.theme.CategoryColors
import dev.antomanc.tameki.core.ui.theme.isDarkScheme

/**
 * A subscription's or account's mark: the real logo when the app ships one, otherwise a coloured
 * monogram on a wash of the same hue. The colour is derived from the name, so Netflix is always
 * the same shade on Recurring, in a notification and in the widget.
 *
 * @param logo an optional brand logo. Must be a real logo, never a placeholder illustration.
 * @param color the known brand hue, when the caller has a catalogue entry; null derives a stable
 *   hue from [name].
 * @param contentDescription null when the name is already written next to the mark; set it when
 *   the mark stands alone.
 */
@Composable
fun BrandMark(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    logo: Painter? = null,
    color: Color? = null,
    contentDescription: String? = null,
) {
    val brand = color ?: CategoryColors.hueForName(name).color()
    // The wash keeps the brand tint; the letters take a readable version of the same hue. A
    // mid-tone brand (Spotify green) at 15% has a light background, so its own green letter would
    // be ~2:1: the ink walks toward black (light) or white (dark) until it clears 4.5:1.
    val ink = readableBrandInk(
        brand = brand,
        surface = MaterialTheme.colorScheme.surface,
        dark = MaterialTheme.colorScheme.isDarkScheme(),
    )
    val label = monogram(name)

    if (logo != null) {
        Surface(
            modifier = modifier.size(size),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Image(
                painter = logo,
                contentDescription = contentDescription,
                modifier = Modifier.padding(size * 0.18f),
                contentScale = ContentScale.Fit,
            )
        }
        return
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brand.copy(alpha = 0.15f))
            .then(
                if (contentDescription == null) {
                    Modifier.clearAndSetSemantics { }
                } else {
                    Modifier.semantics { this.contentDescription = contentDescription }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // A blank name has no initials: show the neutral category glyph, never "?" or "…".
        if (label.isEmpty()) {
            CategoryIcon(
                categoryKey = "other",
                tint = ink,
                contentDescription = null,
                size = size * 0.5f,
            )
        } else {
            Text(
                text = label,
                style = if (label.length > 1) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                color = ink,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The wash a brand mark paints behind its monogram: 15% of the brand over the screen surface.
 * Computed here so the ink contrast check sees the colour that actually ends up on screen.
 */
internal fun brandWash(brand: Color, surface: Color): Color = blend(surface, brand, 0.15f)

/**
 * A brand hue pushed just far enough from its own wash to read: black brands (DAZN) stay black on
 * light paper, bright brands (Spotify green) darken toward ink, and dim brands lighten on dark
 * paper. The hue survives; the contrast is not left to the brand's marketing team.
 */
internal fun readableBrandInk(brand: Color, surface: Color, dark: Boolean): Color {
    val wash = brandWash(brand, surface)
    if (contrastRatio(brand, wash) >= BRAND_MONOGRAM_MIN_CONTRAST) return brand
    val pole = if (dark) Color.White else Color.Black
    // Smallest move toward the pole that clears the bar: hue is kept as much as possible.
    var tooLittle = 0f
    var enough = 1f
    repeat(12) {
        val amount = (tooLittle + enough) / 2f
        if (contrastRatio(blend(brand, pole, amount), wash) >= BRAND_MONOGRAM_MIN_CONTRAST) {
            enough = amount
        } else {
            tooLittle = amount
        }
    }
    return blend(brand, pole, enough)
}

/** WCAG relative-luminance contrast between two opaque colours. */
internal fun contrastRatio(first: Color, second: Color): Float {
    val a = first.luminance() + 0.05f
    val b = second.luminance() + 0.05f
    return if (a > b) a / b else b / a
}

private const val BRAND_MONOGRAM_MIN_CONTRAST = 4.5f

private fun blend(from: Color, to: Color, amount: Float): Color = Color(
    red = from.red + (to.red - from.red) * amount,
    green = from.green + (to.green - from.green) * amount,
    blue = from.blue + (to.blue - from.blue) * amount,
    alpha = from.alpha,
)

/**
 * First letters of the first two words: "Prime Video" becomes "PV", "Netflix" becomes "N".
 * Blank names yield an empty string so [BrandMark] can fall back to an icon instead of a glyph.
 */
internal fun monogram(name: String): String {
    val words = name.trim().split(' ', '\u00A0').filter { it.isNotBlank() }.take(2)
    return words.mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
}

@Preview(name = "Brand marks · light", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 360)
@Composable
private fun BrandMarkLightPreview() {
    PreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandMark("Netflix")
            BrandMark("Prime Video")
            BrandMark("Spotify")
            BrandMark("Enel Energia")
        }
    }
}

@Preview(name = "Brand marks · dark", showBackground = true, backgroundColor = 0xFF141513, widthDp = 360)
@Composable
private fun BrandMarkDarkPreview() {
    PreviewSurface(dark = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandMark("Netflix")
            BrandMark("Prime Video")
            BrandMark("Spotify", contentDescription = "Spotify")
        }
    }
}
