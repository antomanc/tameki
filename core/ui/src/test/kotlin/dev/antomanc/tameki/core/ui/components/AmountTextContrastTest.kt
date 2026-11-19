package dev.antomanc.tameki.core.ui.components

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.ui.theme.PaperLightColors
import dev.antomanc.tameki.core.ui.theme.PaperLightScheme
import org.junit.Test

/**
 * The quiet symbol/minor tone is allowed to be quiet only while it stays readable. These are the
 * two reported cases: an income amount using leafInk on list paper, and a hero using leaf, where
 * the old unconditional 72% fade landed at ~3.1:1 and ~2.8:1.
 */
class AmountTextContrastTest {
    private val paper = PaperLightScheme.surface

    @Test
    fun `an income symbol stays at 45 to 1 on paper`() {
        val symbol = quietInk(PaperLightColors.leafInk, paper, minContrast = 4.5f, alpha = 0.72f)

        assertThat(contrastRatio(symbol, paper)).isAtLeast(4.5f)
    }

    @Test
    fun `a hero leaf symbol and minor units stay at 3 to 1 on paper`() {
        val symbol = quietInk(PaperLightColors.leaf, paper, minContrast = 3f, alpha = 0.72f)
        val minor = quietInk(PaperLightColors.leaf, paper, minContrast = 3f, alpha = 0.76f)

        assertThat(contrastRatio(symbol, paper)).isAtLeast(3f)
        assertThat(contrastRatio(minor, paper)).isAtLeast(3f)
    }

    @Test
    fun `an ink with headroom still gets a quieter tone`() {
        val dimmed = quietInk(PaperLightScheme.onSurface, paper, minContrast = 4.5f, alpha = 0.72f)

        assertThat(dimmed).isNotEqualTo(PaperLightScheme.onSurface)
        assertThat(dimmed.alpha).isEqualTo(1f)
        assertThat(contrastRatio(dimmed, paper)).isAtLeast(4.5f)
    }
}
