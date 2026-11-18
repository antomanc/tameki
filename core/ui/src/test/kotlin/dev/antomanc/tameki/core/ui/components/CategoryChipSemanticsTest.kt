package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * TalkBack must hear "Suggested" on the chip the ranker picked, including when that pick is
 * already selected: that is the moment of choice the suggestion exists for.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CategoryChipSemanticsTest {
    @get:Rule val compose = createComposeRule()

    @Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
    @Test
    fun `the suggested chip keeps its state while selected`() {
        compose.setContent {
            TamekiTheme(dynamicColor = false, motionEnabled = false) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryChip(
                        name = "Groceries",
                        categoryKey = "groceries",
                        selected = true,
                        onClick = {},
                        suggested = true,
                    )
                    CategoryChip(
                        name = "Coffee",
                        categoryKey = "coffee",
                        selected = false,
                        onClick = {},
                        suggested = true,
                    )
                    CategoryChip(
                        name = "Transport",
                        categoryKey = "transport",
                        selected = false,
                        onClick = {},
                    )
                }
            }
        }

        compose.onNodeWithText("Groceries")
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Suggested"))
        compose.onNodeWithText("Coffee")
            .assertIsNotSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Suggested"))
        compose.onNodeWithText("Transport")
            .assertIsNotSelected()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }
}
