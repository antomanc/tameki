package dev.antomanc.tameki.core.model.entity

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DefaultCategoriesTest {
    @Test
    fun paletteIs32BitArgb() {
        // A 40-bit literal (0xFF2A9D8FFF) is not an ARGB colour: Compose reads it as a colour
        // space and the chip silently changes hue.
        repeat(16) { index ->
            assertThat(DefaultCategories.colorArgb(index)).isAtMost(0xFFFFFFFFL)
        }
    }

    @Test
    fun legacyFortyBitColorFoldsBackToArgb() {
        val category = Category(
            name = "Legacy",
            kind = CategoryKind.EXPENSE,
            icon = "other",
            color = 0xFF2A9D8FFF,
        )
        assertThat(category.argbColor).isEqualTo(0xFF2A9D8F)
        val alreadyArgb = category.copy(color = 0xFF2A9D8F)
        assertThat(alreadyArgb.argbColor).isEqualTo(0xFF2A9D8F)
    }
}
