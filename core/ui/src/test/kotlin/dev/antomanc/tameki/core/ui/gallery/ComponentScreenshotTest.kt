package dev.antomanc.tameki.core.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.ui.components.AmountText
import dev.antomanc.tameki.core.ui.components.BrandMark
import dev.antomanc.tameki.core.ui.components.CategoryAvatar
import dev.antomanc.tameki.core.ui.components.CategoryBar
import dev.antomanc.tameki.core.ui.components.CategoryBars
import dev.antomanc.tameki.core.ui.components.CategoryChip
import dev.antomanc.tameki.core.ui.components.DayHeader
import dev.antomanc.tameki.core.ui.components.EmptyState
import dev.antomanc.tameki.core.ui.components.InsightCard
import dev.antomanc.tameki.core.ui.components.MoneyField
import dev.antomanc.tameki.core.ui.components.MonthBar
import dev.antomanc.tameki.core.ui.components.MonthlyBars
import dev.antomanc.tameki.core.ui.components.Numpad
import dev.antomanc.tameki.core.ui.components.PaceBar
import dev.antomanc.tameki.core.ui.components.RecurringFlag
import dev.antomanc.tameki.core.ui.components.RecurringRow
import dev.antomanc.tameki.core.ui.components.SectionHeader
import dev.antomanc.tameki.core.ui.components.SegmentedKind
import dev.antomanc.tameki.core.ui.components.TransactionKind
import dev.antomanc.tameki.core.ui.components.TransactionRow
import dev.antomanc.tameki.core.ui.format.MoneySign
import dev.antomanc.tameki.core.ui.theme.CategoryColors
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.TamekiType
import dev.antomanc.tameki.core.ui.theme.tamekiColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roborazzi screenshots for every component in the library, in light, dark and at 200% font scale.
 * Each capture is one component on the real paper background, so a regression in a row cannot hide
 * behind a busy screen.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComponentScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val eur = CurrencyCode("EUR")

    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    @Test
    fun light() = captureAll(name = "light", dark = false, fontScale = 1f)

    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    @Test
    fun dark() = captureAll(name = "dark", dark = true, fontScale = 1f)

    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    @Test
    fun fontScale200() = captureAll(name = "font200", dark = false, fontScale = 2f)

    private fun captureAll(name: String, dark: Boolean, fontScale: Float) {
        val cases = cases()
        val current = mutableIntStateOf(0)
        compose.setContent {
            TamekiTheme(darkTheme = dark, dynamicColor = false, motionEnabled = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        Column(Modifier.padding(16.dp).fillMaxSize()) {
                            cases[current.intValue].second()
                        }
                    }
                }
            }
        }
        cases.forEachIndexed { index, case ->
            compose.runOnIdle { current.intValue = index }
            compose.waitForIdle()
            compose.onRoot().captureRoboImage("build/outputs/roborazzi/${name}_${case.first}.png")
        }
    }

    private fun cases(): List<Pair<String, @Composable () -> Unit>> = listOf(
        "amounts" to {
            Column {
                AmountText(
                    amount = Money(128450, eur),
                    style = TamekiType.hero,
                    dimMinorUnits = true,
                    omitZeroMinorUnits = true,
                )
                Spacer(Modifier.height(8.dp))
                AmountText(Money(4200, eur), style = TamekiType.amount)
                Spacer(Modifier.height(8.dp))
                AmountText(
                    amount = Money(215000, eur),
                    style = TamekiType.amount,
                    sign = MoneySign.EXPLICIT,
                    color = MaterialTheme.tamekiColors.leafInk,
                )
                Spacer(Modifier.height(8.dp))
                AmountText(Money(-1530, eur), style = TamekiType.amountSmall)
            }
        },
        "pace" to {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                PaceBar(spent = Money(42000, eur), expected = Money(56000, eur), budget = Money(92000, eur))
                PaceBar(spent = Money(64000, eur), expected = Money(56000, eur), budget = Money(92000, eur))
                PaceBar(spent = Money(101000, eur), expected = Money(56000, eur), budget = Money(92000, eur))
            }
        },
        "chips" to {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("groceries", "coffee", "restaurant", "transport", "streaming", "salary").forEachIndexed { index, key ->
                    CategoryChip(
                        name = key.replaceFirstChar { it.uppercaseChar() },
                        categoryKey = key,
                        selected = index == 0,
                        suggested = index == 1,
                        onClick = {},
                    )
                }
            }
        },
        "avatars" to {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategoryColors.keys.forEach { key -> CategoryAvatar(categoryKey = key) }
            }
        },
        "brand_marks" to {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("Netflix", "Prime Video", "Spotify", "Enel Energia").forEach { BrandMark(it) }
            }
        },
        "insight" to {
            InsightCard(
                title = "Switch Spotify to the yearly plan",
                body = "Yearly plan is 17% cheaper.",
                yearlySaving = Money(3480, eur),
                actionLabel = "See the plan",
                onAction = {},
                onDismiss = {},
                categoryKey = "music",
            )
        },
        "transactions" to {
            Column {
                DayHeader(label = "Today", total = Money(4820, eur))
                TransactionRow(
                    title = "Esselunga",
                    amount = Money(4320, eur),
                    categoryKey = "groceries",
                    meta = "Conto corrente",
                    note = "18:42",
                    onClick = {},
                )
                TransactionRow(
                    title = "Stipendio",
                    amount = Money(215000, eur),
                    categoryKey = "salary",
                    meta = "Conto corrente",
                    income = true,
                    onClick = {},
                )
                TransactionRow(
                    title = "Spritz con Marta",
                    amount = Money(1400, eur),
                    categoryKey = "restaurant",
                    note = "Aperitivo",
                    pending = true,
                )
            }
        },
        "recurring" to {
            Column {
                SectionHeader(title = "Upcoming")
                RecurringRow(
                    name = "Netflix",
                    amount = Money(1349, eur),
                    relativeDate = "in 3 days",
                    flags = setOf(RecurringFlag.PRICE_UP),
                    detail = "€161,88 a year",
                    onClick = {},
                )
                RecurringRow(
                    name = "Prime Video",
                    amount = Money(499, eur),
                    relativeDate = "tomorrow",
                    flags = setOf(RecurringFlag.TRIAL_ENDING),
                    onClick = {},
                )
                RecurringRow(
                    name = "Palestra",
                    amount = Money(3900, eur),
                    relativeDate = "in 12 days",
                    flags = setOf(RecurringFlag.RARELY_USED),
                    onClick = {},
                )
            }
        },
        "empty_state" to {
            EmptyState(
                title = "No subscriptions yet",
                body = "Add Netflix, the gym or the phone bill.",
                categoryKey = "streaming",
                actionLabel = "Add recurring",
                onAction = {},
            )
        },
        "money_field" to {
            MoneyField(
                value = Money(1349, eur),
                onValueChange = {},
                currency = eur,
                label = "Amount",
            )
        },
        "numpad" to {
            Box(Modifier.width(320.dp)) {
                Numpad(onDigit = {}, onDecimal = {}, onBackspace = {})
            }
        },
        "segmented_kind" to {
            SegmentedKind(selected = TransactionKind.EXPENSE, onSelect = {})
        },
        "monthly_bars" to {
            MonthlyBars(
                bars = listOf(
                    MonthBar("Jun", Money(91200, eur)),
                    MonthBar("Jul", Money(104500, eur)),
                    MonthBar("Aug", Money(98700, eur)),
                    MonthBar("Sep", Money(128450, eur)),
                    MonthBar("Oct", Money(110200, eur)),
                    MonthBar("Nov", Money(74200, eur)),
                ),
            )
        },
        "category_bars" to {
            CategoryBars(
                rows = listOf(
                    CategoryBar("Groceries", Money(31240, eur), "groceries"),
                    CategoryBar("Home", Money(21500, eur), "home"),
                    CategoryBar("Restaurants", Money(14900, eur), "restaurant"),
                    CategoryBar("Transport", Money(8600, eur), "transport"),
                    CategoryBar("Streaming", Money(3297, eur), "streaming"),
                ),
            )
        },
        "section_header" to {
            SectionHeader(
                title = "Recent",
                supporting = "Last 30 days",
                actionLabel = "See all",
                onAction = {},
            )
        },
    )
}
