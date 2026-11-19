package dev.antomanc.tameki

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.onboarding.OnboardingScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class)
class OnboardingScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test fun light() = capture("onboarding_light", false, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test fun dark() = capture("onboarding_dark", true, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test fun font200() = capture("onboarding_font200", false, 2f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test fun currencyStep() = capture("onboarding_currency_light", false, 1f, stepsForward = 1)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test fun accountsStep() = capture("onboarding_accounts_light", false, 1f, stepsForward = 2)

    private fun capture(name: String, dark: Boolean, fontScale: Float, stepsForward: Int = 0) {
        compose.setContent {
            TamekiTheme(darkTheme = dark, dynamicColor = false, motionEnabled = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        OnboardingScreen(initialCurrency = "EUR", onFinish = { _, _, _ -> }, onSkip = { _ -> })
                    }
                }
            }
        }
        compose.waitForIdle()
        if (stepsForward >= 1) {
            compose.onNodeWithText("Inizia").performClick()
            compose.waitForIdle()
        }
        if (stepsForward >= 2) {
            compose.onNodeWithText("Avanti").performClick()
            compose.waitForIdle()
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }
}
