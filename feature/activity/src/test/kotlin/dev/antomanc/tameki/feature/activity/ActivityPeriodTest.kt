package dev.antomanc.tameki.feature.activity

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class ActivityPeriodTest {
    private val today = LocalDate.of(2026, 11, 18)

    @Test
    fun `any period clears both bounds`() {
        val (from, to) = ActivityPeriod.ANY.bounds(today)
        assertThat(from).isNull()
        assertThat(to).isNull()
    }

    @Test
    fun `this month starts on the first and ends today`() {
        val (from, to) = ActivityPeriod.THIS_MONTH.bounds(today)
        assertThat(from).isEqualTo(LocalDate.of(2026, 11, 1))
        assertThat(to).isEqualTo(today)
    }

    @Test
    fun `last 30 days includes today and the 29 days before`() {
        val (from, to) = ActivityPeriod.LAST_30_DAYS.bounds(today)
        assertThat(from).isEqualTo(LocalDate.of(2026, 10, 20))
        assertThat(to).isEqualTo(today)
        assertThat(java.time.temporal.ChronoUnit.DAYS.between(from, today)).isEqualTo(29)
    }

    @Test
    fun `this year starts on 1 January`() {
        val (from, to) = ActivityPeriod.THIS_YEAR.bounds(today)
        assertThat(from).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(to).isEqualTo(today)
    }

    @Test
    fun `a period is selected only while its own bounds are applied`() {
        val filters = ActivityFilters(
            from = LocalDate.of(2026, 11, 1),
            to = today,
            categoryId = 3L,
        )
        assertThat(ActivityPeriod.THIS_MONTH.isSelected(filters, today)).isTrue()
        assertThat(ActivityPeriod.LAST_30_DAYS.isSelected(filters, today)).isFalse()
        assertThat(ActivityPeriod.THIS_YEAR.isSelected(filters, today)).isFalse()
        assertThat(ActivityPeriod.ANY.isSelected(filters, today)).isFalse()
        assertThat(ActivityPeriod.ANY.isSelected(ActivityFilters(), today)).isTrue()
    }
}
