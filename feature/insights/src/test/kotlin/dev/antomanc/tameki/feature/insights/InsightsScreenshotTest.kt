package dev.antomanc.tameki.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Save tab in Italian, in light, dark and at 200% font scale. The sample carries four
 * opportunities, two budgets, two goals and six months of bars, so the whole page is judged at
 * once and at the length a real November has.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class InsightsScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun light() = shoot(prefix = "insights_light", dark = false, fontScale = 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun dark() = shoot(prefix = "insights_dark", dark = true, fontScale = 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun fontScale200() = shoot(prefix = "insights_font200", dark = false, fontScale = 2f)

    private fun shoot(prefix: String, dark: Boolean, fontScale: Float) {
        compose.setContent {
            TamekiTheme(darkTheme = dark, dynamicColor = false, motionEnabled = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        InsightsScreen(
                            state = sampleInsightsState(),
                            onSnooze = {},
                            onOpenSubscription = {},
                            onReviewSubscriptions = {},
                            onOpenCategory = {},
                            onOpenActivity = {},
                            onSaveBudget = { _, _, _, _, _, _ -> },
                            onDeleteBudget = {},
                            onSaveGoal = { _, _, _, _ -> },
                            onAddToGoal = { _, _ -> },
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/${prefix}_save_1.png")
        val scroll = compose.onNode(hasScrollAction())
        scroll.performScrollToNode(hasText("Budget"))
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/${prefix}_save_2.png")
        scroll.performTouchInput { swipeUp() }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/${prefix}_save_3.png")
    }
}
