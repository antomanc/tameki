package dev.antomanc.tameki.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import dev.antomanc.tameki.core.ui.components.categoryIcon
import org.junit.Test

/** The category palette is a tested set, not a collection of pleasant hex values. */
class CategoryPaletteTest {
    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return if (la > lb) la / lb else lb / la
    }

    @Test
    fun `every hue clears 3 to 1 on its own paper`() {
        CategoryColors.all.forEachIndexed { index, hue ->
            val light = contrast(hue.light, PaperLightScheme.surface)
            val dark = contrast(hue.dark, PaperDarkScheme.surface)
            assertWithMessage("hue $index light contrast $light").that(light).isAtLeast(3f)
            assertWithMessage("hue $index dark contrast $dark").that(dark).isAtLeast(3f)
        }
    }

    @Test
    fun `data colours clear 3 to 1 so a mark is never invisible`() {
        listOf(PaperLightColors to PaperLightScheme, PaperDarkColors to PaperDarkScheme).forEach { (colors, scheme) ->
            listOf(colors.leaf, colors.amber, colors.over).forEach { color ->
                assertThat(contrast(color, scheme.surface)).isAtLeast(3f)
            }
        }
    }

    @Test
    fun `small-text data inks clear 4_5 to 1 on paper`() {
        listOf(PaperLightColors to PaperLightScheme, PaperDarkColors to PaperDarkScheme).forEach { (colors, scheme) ->
            listOf(colors.leafInk, colors.amberInk).forEach { color ->
                assertThat(contrast(color, scheme.surface)).isAtLeast(4.5f)
            }
        }
    }

    @Test
    fun `there are sixteen hues and thirty category keys`() {
        assertThat(CategoryColors.all).hasSize(16)
        assertThat(CategoryColors.all.toSet()).hasSize(16)
        assertThat(CategoryColors.keys).hasSize(30)
    }

    @Test
    fun `every category key has its own glyph`() {
        CategoryColors.keys.forEach { key ->
            assertWithMessage("glyph for $key").that(categoryIcon(key).name).isEqualTo(key)
        }
    }

    @Test
    fun `unknown keys fall back to the other glyph and graphite`() {
        assertThat(categoryIcon("not-a-category").name).isEqualTo("other")
        assertThat(CategoryColors.hueFor("not-a-category")).isEqualTo(CategoryColors.graphite)
    }

    @Test
    fun `a brand monogram hue is stable and known`() {
        val first = CategoryColors.hueForName("Netflix")
        assertThat(CategoryColors.hueForName("Netflix")).isEqualTo(first)
        assertThat(CategoryColors.all).contains(first)
    }
}
