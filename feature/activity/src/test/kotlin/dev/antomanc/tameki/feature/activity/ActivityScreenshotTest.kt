package dev.antomanc.tameki.feature.activity

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
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ActivityScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun activityLight() = captureActivity("activity_light", dark = false, fontScale = 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun activityDark() = captureActivity("activity_dark", dark = true, fontScale = 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun activityFont200() = captureActivity("activity_font200", dark = false, fontScale = 2f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun activityFiltersLight() = captureActivity(
        name = "activity_filters_light",
        dark = false,
        fontScale = 1f,
        state = sampleActivityState(
            filters = ActivityFilters(
                categoryId = 1L,
                accountId = 1L,
                source = TransactionSource.RECEIPT,
            ),
        ),
    )

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun quickAddEmptyLight() = captureQuickAdd("quickadd_empty_light", sampleQuickAddEmpty(), false, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun quickAddEmptyDark() = captureQuickAdd("quickadd_empty_dark", sampleQuickAddEmpty(), true, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun quickAddEmptyFont200() = captureQuickAdd("quickadd_empty_font200", sampleQuickAddEmpty(), false, 2f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun quickAddFilledLight() = captureQuickAdd("quickadd_filled_light", sampleQuickAddFilled(), false, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun quickAddFilledDark() = captureQuickAdd("quickadd_filled_dark", sampleQuickAddFilled(), true, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun quickAddFilledFont200() = captureQuickAdd("quickadd_filled_font200", sampleQuickAddFilled(), false, 2f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun quickAddReceiptLight() = captureQuickAdd("quickadd_receipt_light", sampleQuickAddWithReceipt(), false, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun quickAddReceiptDark() = captureQuickAdd("quickadd_receipt_dark", sampleQuickAddWithReceipt(), true, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun quickAddReceiptFont200() = captureQuickAdd("quickadd_receipt_font200", sampleQuickAddWithReceipt(), false, 2f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun quickAddTransferLight() = captureQuickAdd("quickadd_transfer_light", sampleQuickAddTransfer(), false, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun quickAddTransferDark() = captureQuickAdd("quickadd_transfer_dark", sampleQuickAddTransfer(), true, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test
    fun quickAddTransferFont200() = captureQuickAdd("quickadd_transfer_font200", sampleQuickAddTransfer(), false, 2f)

    private fun captureActivity(
        name: String,
        dark: Boolean,
        fontScale: Float,
        state: ActivityUiState = sampleActivityState(),
    ) {
        compose.setContent {
            TamekiTheme(darkTheme = dark, dynamicColor = false, motionEnabled = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        ActivityScreen(
                            state = state,
                            paging = null,
                            staticItems = sampleActivityItems(),
                            onQueryChange = {},
                            onOpenFilters = {},
                            onCloseFilters = {},
                            onApplyFilters = {},
                            onDelete = {},
                            onUndoDelete = {},
                            onAcknowledgeDelete = {},
                            onEdit = {},
                            onAcceptInbox = {},
                            onDismissInbox = {},
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    private fun captureQuickAdd(name: String, state: QuickAddUiState, dark: Boolean, fontScale: Float) {
        compose.setContent {
            TamekiTheme(darkTheme = dark, dynamicColor = false, motionEnabled = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        QuickAddContent(
                            state = state,
                            onSelectKind = {},
                            onDigit = {},
                            onDecimal = {},
                            onBackspace = {},
                            onSelectCategory = {},
                            onSelectAccount = {},
                            onSelectToAccount = {},
                            onSetDate = {},
                            onSetNote = {},
                            onToggleNote = {},
                            onScanReceipt = {},
                            onClearReceipt = {},
                            onSave = {},
                            onClose = {},
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }
}
