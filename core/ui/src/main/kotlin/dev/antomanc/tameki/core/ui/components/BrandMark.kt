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
    val hue = if (color == null) {
        CategoryColors.hueForName(name).color()
    } else {
        legible(color, MaterialTheme.colorScheme.isDarkScheme())
    }
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
            .background(hue.copy(alpha = 0.15f))
            .then(
                if (contentDescription == null) {
                    Modifier.clearAndSetSemantics { }
                } else {
                    Modifier.semantics { this.contentDescription = contentDescription }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = if (label.length > 1) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.titleLarge
            },
            color = hue,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A brand hue pushed just far enough from the surface to stay legible: black brands (DAZN) lighten
 * on dark paper, bright brands (McFit) darken on light paper. The hue survives; the contrast is not
 * left to the brand's marketing team.
 */
private fun legible(color: Color, dark: Boolean): Color {
    val lightness = color.luminance()
    return when {
        dark && lightness < 0.32f -> blend(color, Color.White, 0.62f)
        !dark && lightness > 0.62f -> blend(color, Color.Black, 0.42f)
        else -> color
    }
}

private fun blend(from: Color, to: Color, amount: Float): Color = Color(
    red = from.red + (to.red - from.red) * amount,
    green = from.green + (to.green - from.green) * amount,
    blue = from.blue + (to.blue - from.blue) * amount,
    alpha = from.alpha,
)

/** First letters of the first two words: "Prime Video" becomes "PV", "Netflix" becomes "N". */
internal fun monogram(name: String): String {
    val words = name.trim().split(' ', '\u00A0').filter { it.isNotBlank() }.take(2)
    val initials = words.mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    return initials.ifEmpty { "?" }
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
