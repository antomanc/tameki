package dev.antomanc.tameki.core.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PaceTest {
    @Test
    fun `spending below the expected pace is on track`() {
        assertThat(Pace.status(spent = 420, expected = 560, budget = 920)).isEqualTo(PaceStatus.ON_TRACK)
    }

    @Test
    fun `spending ahead of pace but inside the budget is at risk`() {
        assertThat(Pace.status(spent = 640, expected = 560, budget = 920)).isEqualTo(PaceStatus.AT_RISK)
    }

    @Test
    fun `over only once the budget is exceeded`() {
        assertThat(Pace.status(spent = 920, expected = 560, budget = 920)).isEqualTo(PaceStatus.AT_RISK)
        assertThat(Pace.status(spent = 921, expected = 560, budget = 920)).isEqualTo(PaceStatus.OVER)
    }

    @Test
    fun `without a budget nothing is ever over`() {
        assertThat(Pace.status(spent = 9999, expected = 560, budget = null)).isEqualTo(PaceStatus.AT_RISK)
    }

    @Test
    fun `the bar scales to the larger of budget and spend so overspending stays visible`() {
        val normal = Pace.fill(spent = 460, expected = 560, budget = 920)
        val over = Pace.fill(spent = 1010, expected = 560, budget = 920)
        val wayOver = Pace.fill(spent = 2000, expected = 560, budget = 920)
        assertThat(normal).isWithin(0.001f).of(0.5f)
        assertThat(over).isEqualTo(1f)
        assertThat(wayOver).isEqualTo(1f)
        assertThat(Pace.marker(1010, 560, 920)).isWithin(0.001f).of(0.554f)
        assertThat(Pace.marker(2000, 560, 920)).isWithin(0.001f).of(0.28f)
    }

    @Test
    fun `zero values never divide by zero`() {
        assertThat(Pace.fill(0, 0, null)).isEqualTo(0f)
        assertThat(Pace.marker(0, 0, null)).isEqualTo(0f)
        assertThat(Pace.fill(100, 0, 0)).isEqualTo(1f)
    }
}
