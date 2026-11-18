package dev.antomanc.tameki

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The FAB is a Surface with [combinedClickable], not Material's FloatingActionButton — which
 * swallowed long-press. Click and long-press must both fire.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AddFabTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun clickFiresOnClick() {
        var clicks = 0
        var longs = 0
        compose.setContent {
            TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
                AddFab(onClick = { clicks++ }, onLongClick = { longs++ }, label = "New expense")
            }
        }
        compose.onNodeWithContentDescription("New expense").performClick()
        assertThat(clicks).isEqualTo(1)
        assertThat(longs).isEqualTo(0)
    }

    @Test
    fun longPressFiresOnLongClick() {
        var clicks = 0
        var longs = 0
        compose.setContent {
            TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
                AddFab(onClick = { clicks++ }, onLongClick = { longs++ }, label = "New expense")
            }
        }
        compose.onNodeWithContentDescription("New expense").performTouchInput { longClick() }
        assertThat(longs).isEqualTo(1)
        assertThat(clicks).isEqualTo(0)
    }
}
