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

    @Test
    fun `clear filters keeps the typed query`() {
        val filters = ActivityFilters(
            from = day.minusDays(29),
            to = day,
            categoryId = 1L,
            accountId = 2L,
            source = TransactionSource.CSV_IMPORT,
            tag = "work",
            query = "esselunga",
        )

        val cleared = filters.clearedFacets()

        assertThat(cleared.query).isEqualTo("esselunga")
        assertThat(cleared.hasFacets).isFalse()
        assertThat(cleared.isActive).isTrue()
    }

    @Test
    fun `facet count counts the period once and the query never`() {
        assertThat(ActivityFilters().facetCount).isEqualTo(0)
        assertThat(ActivityFilters(query = "bar").facetCount).isEqualTo(0)
        assertThat(ActivityFilters(from = day).facetCount).isEqualTo(1)
        assertThat(
            ActivityFilters(
                from = day.minusDays(29),
                to = day,
                categoryId = 1L,
                accountId = 2L,
                source = TransactionSource.RECEIPT,
                tag = "work",
            ).facetCount,
        ).isEqualTo(5)
    }

    @Test
    fun `no match names search, filters, or both`() {
        assertThat(ActivityFilters().noMatch).isNull()
        assertThat(ActivityFilters(query = "bar").noMatch).isEqualTo(ActivityNoMatch.SEARCH)
        assertThat(ActivityFilters(source = TransactionSource.RECEIPT).noMatch)
            .isEqualTo(ActivityNoMatch.FILTERS)
        assertThat(ActivityFilters(query = "bar", categoryId = 1L).noMatch)
            .isEqualTo(ActivityNoMatch.BOTH)
    }
}
