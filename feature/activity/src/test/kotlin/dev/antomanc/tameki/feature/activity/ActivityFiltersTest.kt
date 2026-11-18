package dev.antomanc.tameki.feature.activity

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.entity.TransactionSource
import java.time.LocalDate
import org.junit.Test

/**
 * The filter contract the Activity screen and the Tune sheet share: what the badge counts, what
 * "Clear filters" is allowed to touch, and which sentence the empty state must use.
 */
class ActivityFiltersTest {
    private val day = LocalDate.of(2026, 11, 18)
    private val month = ActivityFilters(period = ActivityPeriod.THIS_MONTH)

    @Test
    fun `clear filters keeps the typed query and the period`() {
        val filters = ActivityFilters(
            period = ActivityPeriod.THIS_MONTH,
            categoryId = 1L,
            accountId = 2L,
            source = TransactionSource.CSV_IMPORT,
            tag = "work",
            query = "esselunga",
        )

        val cleared = filters.clearedSheetFacets()

        assertThat(cleared.query).isEqualTo("esselunga")
        assertThat(cleared.hasSheetFacets).isFalse()
        // The period chips sit outside the sheet: a clear inside it must not move them.
        assertThat(cleared.period).isEqualTo(ActivityPeriod.THIS_MONTH)
        assertThat(cleared.hasFacets).isTrue()
    }

    @Test
    fun `facet count covers the sheet facets and never the period`() {
        assertThat(ActivityFilters().facetCount).isEqualTo(0)
        assertThat(ActivityFilters(query = "bar").facetCount).isEqualTo(0)
        // The period has its own chips: counting it would badge a filter the sheet cannot show.
        assertThat(month.facetCount).isEqualTo(0)
        assertThat(
            ActivityFilters(
                period = ActivityPeriod.THIS_MONTH,
                categoryId = 1L,
                accountId = 2L,
                source = TransactionSource.RECEIPT,
                tag = "work",
            ).facetCount,
        ).isEqualTo(4)
    }

    @Test
    fun `a period selection only is not a sheet facet but still narrows`() {
        assertThat(month.hasSheetFacets).isFalse()
        assertThat(month.hasPeriod).isTrue()
        assertThat(month.hasFacets).isTrue()
    }

    @Test
    fun `no match names search, sheet filters, the period, or a mix`() {
        assertThat(ActivityFilters().noMatch).isNull()
        assertThat(ActivityFilters(query = "bar").noMatch).isEqualTo(ActivityNoMatch.SEARCH)
        assertThat(ActivityFilters(source = TransactionSource.RECEIPT).noMatch)
            .isEqualTo(ActivityNoMatch.FILTERS)
        assertThat(month.noMatch).isEqualTo(ActivityNoMatch.PERIOD)
        assertThat(ActivityFilters(query = "bar", categoryId = 1L).noMatch)
            .isEqualTo(ActivityNoMatch.BOTH)
        assertThat(month.copy(query = "bar").noMatch).isEqualTo(ActivityNoMatch.BOTH)
    }

    @Test
    fun `clearing the period leaves the sheet facets alone`() {
        val cleared = month.copy(categoryId = 1L).clearedPeriod()

        assertThat(cleared.period).isEqualTo(ActivityPeriod.ANY)
        assertThat(cleared.hasPeriod).isFalse()
        assertThat(cleared.categoryId).isEqualTo(1L)
    }

    @Test
    fun `the period preset keeps its meaning on a later day`() {
        // The same filters resolve to a different window tomorrow: nothing about the preset is
        // frozen to the day it was chosen, which is what used to leave the query at yesterday.
        val tomorrow = day.plusDays(1)
        assertThat(ActivityPeriod.THIS_MONTH.bounds(day).second).isEqualTo(day)
        assertThat(ActivityPeriod.THIS_MONTH.bounds(tomorrow).second).isEqualTo(tomorrow)
        assertThat(ActivityPeriod.THIS_MONTH.isSelected(month)).isTrue()
    }
}
