package dev.antomanc.tameki.core.ui.gallery

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import com.github.takahirom.roborazzi.captureRoboImage
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The whole gallery, top to bottom, so component collisions and rhythm problems are visible. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GalleryScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    @Test
    fun light() = shoot(prefix = "gallery_light", dark = false)

    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    @Test
    fun dark() = shoot(prefix = "gallery_dark", dark = true)

    private fun shoot(prefix: String, dark: Boolean) {
        compose.setContent {
            TamekiTheme(darkTheme = dark, dynamicColor = false, motionEnabled = false) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ComponentGallery()
                }
            }
        }
        val scroll = compose.onNode(hasScrollAction())
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/${prefix}_1.png")
        scroll.performScrollToNode(hasText("Charts"))
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/${prefix}_2.png")
        scroll.performScrollToNode(hasText("Empty states"))
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/${prefix}_3.png")
    }
}
