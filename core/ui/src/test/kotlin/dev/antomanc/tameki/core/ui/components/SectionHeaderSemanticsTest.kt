package dev.antomanc.tameki.core.ui.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Section titles must be headings, or TalkBack cannot jump from section to section. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SectionHeaderSemanticsTest {
    @get:Rule val compose = createComposeRule()

    @Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
    @Test
    fun `the section title is marked as a heading`() {
        compose.setContent {
            TamekiTheme(dynamicColor = false, motionEnabled = false) {
                SectionHeader(title = "Upcoming")
            }
        }

        compose.onNodeWithText("Upcoming")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }
}
