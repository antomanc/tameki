package dev.antomanc.tameki.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun light() = capture("home_light", dark = false, fontScale = 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun dark() = capture("home_dark", dark = true, fontScale = 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun fontScale200() = capture("home_font200", dark = false, fontScale = 2f)

    private fun capture(name: String, dark: Boolean, fontScale: Float) {
        compose.setContent {
            TamekiTheme(darkTheme = dark, dynamicColor = false, motionEnabled = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        HomeScreen(
                            state = sampleHomeState(),
                            onOpenSettings = {},
                            onOpenActivity = {},
                            onOpenInbox = {},
                            onSeeAllRenewals = {},
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }
}
