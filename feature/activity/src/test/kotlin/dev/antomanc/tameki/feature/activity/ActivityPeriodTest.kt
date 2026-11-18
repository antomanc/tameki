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
    fun `a period is selected by the preset it stores, not by its resolved bounds`() {
        val filters = ActivityFilters(
            period = ActivityPeriod.THIS_MONTH,
            categoryId = 3L,
        )
        assertThat(ActivityPeriod.THIS_MONTH.isSelected(filters)).isTrue()
        assertThat(ActivityPeriod.LAST_30_DAYS.isSelected(filters)).isFalse()
        assertThat(ActivityPeriod.THIS_YEAR.isSelected(filters)).isFalse()
        assertThat(ActivityPeriod.ANY.isSelected(filters)).isFalse()
        assertThat(ActivityPeriod.ANY.isSelected(ActivityFilters())).isTrue()
    }

    @Test
    fun `a preset chosen yesterday is still selected and resolves against the new today`() {
        val filters = ActivityFilters(period = ActivityPeriod.THIS_MONTH)
        val tomorrow = today.plusDays(1)

        // The chip cannot go stale: it reads the preset, and the preset keeps pointing at whatever
        // "this month" is on the day the query runs. Only the bounds move.
        assertThat(ActivityPeriod.THIS_MONTH.isSelected(filters)).isTrue()
        assertThat(ActivityPeriod.THIS_MONTH.bounds(tomorrow).second).isEqualTo(tomorrow)
    }
}
