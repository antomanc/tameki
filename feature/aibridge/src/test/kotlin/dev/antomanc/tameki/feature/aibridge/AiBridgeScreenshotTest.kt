package dev.antomanc.tameki.feature.aibridge

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
class AiBridgeScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun promptsLight() = capturePrompts("ai_prompts_light", false, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun promptsDark() = capturePrompts("ai_prompts_dark", true, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun promptsFont200() = capturePrompts("ai_prompts_font200", false, 2f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun pasteProblemLight() = capturePaste("ai_paste_problem_light", false, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun pasteProblemDark() = capturePaste("ai_paste_problem_dark", true, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun pasteProblemFont200() = capturePaste("ai_paste_problem_font200", false, 2f)

    private fun capturePrompts(name: String, dark: Boolean, fontScale: Float) {
        compose.setContent {
            TamekiTheme(darkTheme = dark, dynamicColor = false, motionEnabled = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        AibridgeScreen(
                            state = sampleAiState(),
                            onCopyPrompt = {},
                            onClearSnackbar = {},
                            onOpenChatbot = {},
                            onOpenPaste = {},
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    private fun capturePaste(name: String, dark: Boolean, fontScale: Float) {
        compose.setContent {
            TamekiTheme(darkTheme = dark, dynamicColor = false, motionEnabled = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        PasteResultScreen(
                            state = samplePasteState(),
                            onPasteTextChange = {},
                            onPasteClipboard = {},
                            onParse = {},
                            onSetCategory = { _, _ -> },
                            onSetAccount = { _, _ -> },
                            onImport = {},
                            onClearSnackbar = {},
                            onBack = {},
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }
}
