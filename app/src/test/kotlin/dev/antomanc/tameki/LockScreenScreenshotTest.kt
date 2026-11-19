package dev.antomanc.tameki

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

/** The lock cover is a real surface: it must stay readable in dark mode and at 200% font. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class)
class LockScreenScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test fun light() = capture("lock_light", false, 1f, available = true)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test fun dark() = capture("lock_dark", true, 1f, available = true)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test fun darkUnavailable() = capture("lock_dark_unavailable", true, 1f, available = false)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test fun font200() = capture("lock_font200", false, 2f, available = true)

    private fun capture(name: String, dark: Boolean, fontScale: Float, available: Boolean) {
        compose.setContent {
            TamekiTheme(darkTheme = dark, dynamicColor = false, motionEnabled = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        LockScreen(lockAvailable = available, onUnlock = {})
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }
}
