package dev.antomanc.tameki.feature.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The filter contract at the UI level: the badge speaks the sheet facet count, a chip removes
 * exactly its own facet, and a no-match state always offers the action that reveals rows again.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ActivitySemanticsTest {
    @get:Rule val compose = createComposeRule()

    private fun setScreen(
        filters: ActivityFilters,
        items: List<ActivityListItem> = sampleActivityItems(),
        inbox: Boolean = true,
        onApplyFilters: (ActivityFilters) -> Unit = {},
        onAddMovement: () -> Unit = {},
    ) {
        val state = sampleActivityState(filters = filters)
            .let { if (inbox) it else it.copy(inbox = emptyList()) }
        compose.setContent {
            TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    ActivityScreen(
                        state = state,
                        paging = null,
                        staticItems = items,
                        onQueryChange = {},
                        onOpenFilters = {},
                        onCloseFilters = {},
                        onApplyFilters = onApplyFilters,
                        onAddMovement = onAddMovement,
                        onDelete = {},
                        onUndoDelete = {},
                        onAcknowledgeDelete = {},
                        onEdit = {},
                        onAcceptInbox = {},
                        onDismissInbox = {},
                    )
                }
            }
        }
    }

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun `the badge counts the sheet facets and not the period`() {
        setScreen(
            filters = ActivityFilters(
                period = ActivityPeriod.THIS_MONTH,
                categoryId = 1L,
                accountId = 1L,
                source = TransactionSource.RECEIPT,
            ),
        )

        // The period is excluded on purpose: its chips under search already show it, and the sheet
        // cannot change it.
        compose.onNodeWithContentDescription("Filtri attivi: 3").assertExists()
    }

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun `a period alone leaves the filter icon uncounted`() {
        setScreen(filters = ActivityFilters(period = ActivityPeriod.THIS_MONTH))

        compose.onNodeWithContentDescription("Filtri").assertExists()
        compose.onNodeWithContentDescription("Filtri attivi", substring = true).assertDoesNotExist()
    }

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun `a preset stays selected and removable after the day it was chosen`() {
        setScreen(filters = ActivityFilters(period = ActivityPeriod.THIS_MONTH))

        // The chip is selected by the preset, never by a pair of dates: nothing here knows what
        // today is, which is exactly why the selection survives the midnight rollover.
        compose.onNodeWithText("Questo mese").assertIsSelected()
    }

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun `removing a facet chip keeps the other facets and the period`() {
        var applied: ActivityFilters? = null
        setScreen(
            filters = ActivityFilters(
                period = ActivityPeriod.THIS_MONTH,
                categoryId = 1L,
                accountId = 1L,
            ),
            onApplyFilters = { applied = it },
        )

        compose.onNodeWithText("Alimentari").performClick()

        val next = checkNotNull(applied)
        assertThat(next.categoryId).isNull()
        assertThat(next.accountId).isEqualTo(1L)
        assertThat(next.period).isEqualTo(ActivityPeriod.THIS_MONTH)
    }

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun `a filtered-out list explains itself under the inbox and offers one way out`() {
        var applied: ActivityFilters? = null
        setScreen(
            filters = ActivityFilters(categoryId = 1L),
            items = emptyList(),
            onApplyFilters = { applied = it },
        )

        // The sample state keeps one pending Inbox item: it must sit above the explanation, never
        // replace it. Exactly one Clear filters is on screen: the empty state names the cause, so
        // the header's copy steps aside instead of offering the same tap twice.
        compose.onNodeWithText("Nessun movimento corrisponde a questi filtri.").assertExists()
        compose.onAllNodesWithText("Azzera filtri").assertCountEquals(1)
        compose.onNodeWithText("Azzera filtri").performClick()
        assertThat(checkNotNull(applied).categoryId).isNull()
    }

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun `a period-only no-match offers clearing the period`() {
        var applied: ActivityFilters? = null
        setScreen(
            filters = ActivityFilters(period = ActivityPeriod.THIS_MONTH),
            items = emptyList(),
            onApplyFilters = { applied = it },
        )

        compose.onNodeWithText("Nessun movimento in questo periodo.").assertExists()
        compose.onNodeWithText("Azzera periodo").performClick()
        assertThat(checkNotNull(applied).period).isEqualTo(ActivityPeriod.ANY)
    }

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun `an empty journal offers the action that fills it`() {
        var added = 0
        setScreen(
            filters = ActivityFilters(),
            items = emptyList(),
            inbox = false,
            onAddMovement = { added++ },
        )

        // No dead avatar and no "tap +" instruction: the empty state carries the action itself.
        compose.onNodeWithText("Nessun movimento").assertExists()
        compose.onNodeWithText("Aggiungi movimento").performClick()
        assertThat(added).isEqualTo(1)
    }
}
