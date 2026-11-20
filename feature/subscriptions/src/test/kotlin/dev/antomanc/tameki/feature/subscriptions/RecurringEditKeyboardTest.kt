package dev.antomanc.tameki.feature.subscriptions

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The add/edit form inside the app shell's Scaffold (`contentWindowInsets = safeDrawing`, content
 * padded by the Scaffold's padding, as in MainActivity) while the keyboard is open.
 *
 * The form's own `imePadding()` keeps the field being edited above the keyboard, but only if the
 * shell has consumed the IME inset it already padded for. Otherwise the keyboard is padded twice
 * and a keyboard-high dead band opens between the form and the keyboard.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecurringEditKeyboardTest {
    @get:Rule val compose = createComposeRule()

    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    @Test
    fun formEndsWhereTheKeyboardStartsWhenTheShellConsumesItsPadding() {
        val (formBottom, rootHeight) = formBottomWithKeyboard(shellConsumesPadding = true)
        assertThat(formBottom).isWithin(1f).of(rootHeight - KEYBOARD_PX)
    }

    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    @Test
    fun keyboardIsPaddedTwiceWhenTheShellDoesNotConsumeItsPadding() {
        val (formBottom, rootHeight) = formBottomWithKeyboard(shellConsumesPadding = false)
        assertThat(formBottom).isWithin(1f).of(rootHeight - 2 * KEYBOARD_PX)
    }

    /** Bottom edge of the form and height of the window, both in px, with the keyboard open. */
    private fun formBottomWithKeyboard(shellConsumesPadding: Boolean): Pair<Float, Float> {
        var view: View? = null
        compose.setContent {
            view = LocalView.current
            TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
                Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .then(if (shellConsumesPadding) Modifier.consumeWindowInsets(padding) else Modifier),
                    ) {
                        RecurringEditScreen(
                            state = sampleEditState(),
                            onBack = {}, onName = {}, onSelectBrand = {}, onType = {}, onAmount = {},
                            onCycleEvery = {}, onCycleUnit = {}, onStartDate = {}, onNextDate = {},
                            onTrialEnd = {}, onAccount = {}, onCategory = {}, onReminderDays = {},
                            onAutoLog = {}, onAnnualAlternative = {}, onSharedWith = {}, onCancelUrl = {},
                            onNote = {}, onSave = {},
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        val root = checkNotNull(view)
        compose.runOnUiThread {
            val keyboard = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, KEYBOARD_PX))
                .setVisible(WindowInsetsCompat.Type.ime(), true)
                .build()
            ViewCompat.dispatchApplyWindowInsets(root, keyboard)
        }
        compose.waitForIdle()
        val form = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
        val density = root.resources.displayMetrics.density
        return form.getUnclippedBoundsInRoot().bottom.value * density to root.height.toFloat()
    }

    private companion object {
        const val KEYBOARD_PX = 600
    }
}
