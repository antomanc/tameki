package dev.antomanc.tameki.feature.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import java.time.LocalDate
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

    private val today = LocalDate.of(2026, 11, 18)
    private val monthStart = today.withDayOfMonth(1)

    private fun setScreen(
        filters: ActivityFilters,
        items: List<ActivityListItem> = sampleActivityItems(),
        onApplyFilters: (ActivityFilters) -> Unit = {},
    ) {
        compose.setContent {
            TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    ActivityScreen(
                        state = sampleActivityState(filters = filters),
                        paging = null,
                        staticItems = items,
                        onQueryChange = {},
                        onOpenFilters = {},
                        onCloseFilters = {},
                        onApplyFilters = onApplyFilters,
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
                from = monthStart,
                to = today,
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
        setScreen(filters = ActivityFilters(from = monthStart, to = today))

        compose.onNodeWithContentDescription("Filtri").assertExists()
        compose.onNodeWithContentDescription("Filtri attivi", substring = true).assertDoesNotExist()
    }

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun `removing a facet chip keeps the other facets and the period`() {
        var applied: ActivityFilters? = null
        setScreen(
            filters = ActivityFilters(
                from = monthStart,
                to = today,
                categoryId = 1L,
                accountId = 1L,
            ),
            onApplyFilters = { applied = it },
        )

        compose.onNodeWithText("Alimentari").performClick()

        val next = checkNotNull(applied)
        assertThat(next.categoryId).isNull()
        assertThat(next.accountId).isEqualTo(1L)
        assertThat(next.from).isEqualTo(monthStart)
    }

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun `a filtered-out list explains itself under the inbox and offers a way out`() {
        var applied: ActivityFilters? = null
        setScreen(
            filters = ActivityFilters(categoryId = 1L),
            items = emptyList(),
            onApplyFilters = { applied = it },
        )

        // The sample state keeps one pending Inbox item: it must sit above the explanation, never
        // replace it. Two "Azzera filtri" exist (header and empty state); the empty state's is last
        // and clears exactly the same facets.
        compose.onNodeWithText("Nessun movimento corrisponde a questi filtri.").assertExists()
        compose.onAllNodesWithText("Azzera filtri").onLast().performClick()
        assertThat(checkNotNull(applied).categoryId).isNull()
    }

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun `a period-only no-match offers clearing the period`() {
        var applied: ActivityFilters? = null
        setScreen(
            filters = ActivityFilters(from = monthStart, to = today),
            items = emptyList(),
            onApplyFilters = { applied = it },
        )

        compose.onNodeWithText("Nessun movimento in questo periodo.").assertExists()
        compose.onNodeWithText("Azzera periodo").performClick()
        assertThat(checkNotNull(applied).from).isNull()
    }
}
