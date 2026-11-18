package dev.antomanc.tameki.feature.activity

import androidx.paging.LoadState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The list area's message: a load that is still running or that failed must never borrow the words
 * of an empty journal. "No movements" is a fact about the data, not about the read.
 */
class ActivityEmptyKindTest {
    private val idle = LoadState.NotLoading(endOfPaginationReached = true)
    private val error = LoadState.Error(IllegalStateException("boom"))

    @Test
    fun `a refresh in progress says nothing yet`() {
        assertThat(activityEmptyKind(hasRows = false, refresh = LoadState.Loading, noMatch = null))
            .isNull()
        assertThat(
            activityEmptyKind(
                hasRows = false,
                refresh = LoadState.Loading,
                noMatch = ActivityNoMatch.FILTERS,
            ),
        ).isNull()
    }

    @Test
    fun `a failed refresh reads as an error even when filters are on`() {
        assertThat(activityEmptyKind(hasRows = false, refresh = error, noMatch = null))
            .isEqualTo(ActivityEmptyKind.ERROR)
        // The filters are not what hid the rows: the read failed.
        assertThat(
            activityEmptyKind(
                hasRows = false,
                refresh = error,
                noMatch = ActivityNoMatch.BOTH,
            ),
        ).isEqualTo(ActivityEmptyKind.ERROR)
    }

    @Test
    fun `rows on screen are never replaced by a message`() {
        assertThat(activityEmptyKind(hasRows = true, refresh = idle, noMatch = ActivityNoMatch.PERIOD))
            .isNull()
    }

    @Test
    fun `a loaded empty list explains the filters or the journal`() {
        assertThat(activityEmptyKind(hasRows = false, refresh = idle, noMatch = ActivityNoMatch.PERIOD))
            .isEqualTo(ActivityEmptyKind.NO_MATCH)
        assertThat(activityEmptyKind(hasRows = false, refresh = idle, noMatch = null))
            .isEqualTo(ActivityEmptyKind.BASE)
    }
}
