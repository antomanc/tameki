package dev.antomanc.tameki.core.ui.components

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertWithMessage
import dev.antomanc.tameki.core.ui.theme.PaperDarkScheme
import dev.antomanc.tameki.core.ui.theme.PaperLightScheme
import org.junit.Test

/**
 * Brand monograms are drawn on a 15% wash of their own colour, so the ink has to clear 4.5:1
 * against that wash, not against the plain surface. Mid-tone brands (Spotify green) were the
 * failing case: their own letters sat at ~2:1 on the pale wash.
 */
class BrandMarkContrastTest {
    private val realBrands = listOf(
        Color(0xFF1DB954), // Spotify green: the reported failure
        Color(0xFFE50914), // Netflix red
        Color(0xFF00A8E1), // Prime Video blue
        Color(0xFFFF0000), // YouTube red
        Color(0xFF4285F4), // Google blue
        Color(0xFFF2B544), // amber brand
        Color(0xFF000000), // DAZN / Apple TV+
        Color(0xFFFFFFFF), // white-on-white edge case
    )

    @Test
    fun `known brand colours clear 45 on their own wash in both themes`() {
        assertInkIsReadable(realBrands, PaperLightScheme.surface, dark = false)
        assertInkIsReadable(realBrands, PaperDarkScheme.surface, dark = true)
    }

    @Test
    fun `every category hue stays readable as a monogram in both themes`() {
        val hues = dev.antomanc.tameki.core.ui.theme.CategoryColors.all
        assertInkIsReadable(hues.map { it.light }, PaperLightScheme.surface, dark = false)
        assertInkIsReadable(hues.map { it.dark }, PaperDarkScheme.surface, dark = true)
    }

    @Test
    fun `a brand that already reads is left untouched`() {
        val brand = Color(0xFF000000)
        val ink = readableBrandInk(brand, PaperLightScheme.surface, dark = false)
        assertWithMessage("black needs no adjustment").that(ink).isEqualTo(brand)
    }

    private fun assertInkIsReadable(brands: List<Color>, surface: Color, dark: Boolean) {
        brands.forEach { brand ->
            val wash = brandWash(brand, surface)
            val ink = readableBrandInk(brand, surface, dark)
            val contrast = contrastRatio(ink, wash)
            assertWithMessage("brand $brand on $surface washed to $wash")
                .that(contrast)
                .isAtLeast(4.5f)
        }
    }
}
