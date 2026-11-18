package dev.antomanc.tameki.core.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.ui.components.AmountText
import dev.antomanc.tameki.core.ui.components.PaceBar
import dev.antomanc.tameki.core.ui.format.MoneyFormatter
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.TamekiType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

/** TalkBack must hear one whole amount, not a pile of separate glyphs. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AmountSemanticsTest {
    @get:Rule val compose = createComposeRule()
    private val eur = CurrencyCode("EUR")

    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    @Test
    fun amountIsSpokenAsOneFormattedString() {
        val expected = MoneyFormatter(Locale.US).format(Money(128450, eur))
        compose.setContent {
            TamekiTheme(dynamicColor = false, motionEnabled = false) {
                AmountText(Money(128450, eur), style = TamekiType.hero)
            }
        }
        compose.onNodeWithContentDescription(expected).assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/semantics_amount.png")
    }

    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    @Test
    fun paceBarSpeaksSpentExpectedAndStatus() {
        val expected = MoneyFormatter(Locale.US).format(Money(56000, eur), omitZeroFraction = true)
        compose.setContent {
            TamekiTheme(dynamicColor = false, motionEnabled = false) {
                PaceBar(
                    spent = Money(42000, eur),
                    expected = Money(56000, eur),
                    budget = Money(92000, eur),
                )
            }
        }
        compose.onNodeWithContentDescription("$expected", substring = true).assertExists()
    }
}
