package dev.antomanc.tameki.feature.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
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

/**
 * The Recurring tab in Italian, in light, dark and at 200% font scale: the list, one detail, the
 * add/edit form, the review step and its summary. The data is shaped like a real November
 * (Netflix, DAZN, a two-monthly Enel bill) so the layouts are judged on plausible lengths.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SubscriptionsScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun light() = captureAll(prefix = "subs_light", dark = false, fontScale = 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun dark() = captureAll(prefix = "subs_dark", dark = true, fontScale = 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun fontScale200() = captureAll(prefix = "subs_font200", dark = false, fontScale = 2f)

    private fun captureAll(prefix: String, dark: Boolean, fontScale: Float) {
        val screens = listOf<Pair<String, @Composable () -> Unit>>(
            "list" to { SubscriptionsScreenSample() },
            "list_status" to { SubscriptionsScreenSample(sampleSubscriptionsStatePaused()) },
            "detail" to { DetailSample() },
            "edit" to { EditSample() },
            "review" to { ReviewSample() },
            "review_summary" to { ReviewSummarySample() },
        )
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
                        screens[current.intValue].second()
                    }
                }
            }
        }
        screens.forEachIndexed { index, (name, _) ->
            compose.runOnIdle { current.intValue = index }
            compose.waitForIdle()
            compose.onRoot().captureRoboImage("build/outputs/roborazzi/${prefix}_$name.png")
        }
    }

    @Composable
    private fun SubscriptionsScreenSample(
        state: SubscriptionsUiState = sampleSubscriptionsState(),
    ) {
        SubscriptionsScreen(
            state = state,
            catalog = sampleCatalog,
            onTogglePeriod = {},
            onSelectDay = {},
            onTypeFilter = {},
            onStatusFilter = {},
            onOpenSubscription = {},
            onAddRecurring = {},
            onReview = {},
        )
    }

    @Composable
    private fun DetailSample() {
        RecurringDetailScreen(
            state = sampleDetailState(),
            catalog = sampleCatalog,
            onBack = {},
            onEdit = {},
            onRate = {},
            onPause = {},
            onResume = {},
            onCancel = {},
        )
    }

    @Composable
    private fun EditSample() {
        RecurringEditScreen(
            state = sampleEditState(),
            onBack = {},
            onName = {},
            onSelectBrand = {},
            onType = {},
            onAmount = {},
            onCycleEvery = {},
            onCycleUnit = {},
            onStartDate = {},
            onNextDate = {},
            onTrialEnd = {},
            onAccount = {},
            onCategory = {},
            onReminderDays = {},
            onAutoLog = {},
            onAnnualAlternative = {},
            onSharedWith = {},
            onCancelUrl = {},
            onNote = {},
            onSave = {},
        )
    }

    @Composable
    private fun ReviewSample() {
        SubscriptionsReviewScreen(
            state = sampleReviewState(),
            catalog = sampleCatalog,
            onBack = {},
            onChoose = {},
            onDone = {},
        )
    }

    @Composable
    private fun ReviewSummarySample() {
        SubscriptionsReviewScreen(
            state = sampleReviewSummary(),
            catalog = sampleCatalog,
            onBack = {},
            onChoose = {},
            onDone = {},
        )
    }
}
