package dev.antomanc.tameki.store

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.feature.activity.ActivityScreen
import dev.antomanc.tameki.feature.activity.QuickAddContent
import dev.antomanc.tameki.feature.home.HomeScreen
import dev.antomanc.tameki.feature.insights.InsightsScreen
import dev.antomanc.tameki.feature.subscriptions.SubscriptionsScreen
import java.io.File
import java.io.FileOutputStream
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Generates Play Store listing graphics into `fastlane/metadata/android/<locale>/images/`.
 * Gated: only runs when `-PstoreAssets` is set (see `:app:recordStoreAssets`).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, sdk = [35])
class StoreAssetsTest {
    @get:Rule val compose = createComposeRule()

    private lateinit var outRoot: File

    @Before
    fun requireGateAndOutDir() {
        val enabled = System.getProperty("store.assets.enabled") == "true"
        assertThat(enabled).isTrue()
        val path = System.getProperty("store.assets.out")
            ?: error("store.assets.out system property missing")
        outRoot = File(path)
        assertThat(outRoot.isDirectory || outRoot.mkdirs()).isTrue()
    }

    @Config(qualifiers = "w512dp-h512dp-mdpi")
    @Test
    fun icon() {
        compose.setContent {
            Box(Modifier.size(512.dp)) { StoreIcon() }
        }
        compose.waitForIdle()
        val raw = File(outRoot, "_raw_icon.png")
        compose.onRoot().captureRoboImage(raw.absolutePath)
        for (locale in listOf("it-IT", "en-US")) {
            val dest = File(outRoot, "$locale/images/icon.png")
            dest.parentFile?.mkdirs()
            writeSized(raw, dest, 512, 512, opaque = false)
        }
        raw.delete()
    }

    @Config(qualifiers = "it-rIT-w1024dp-h500dp-mdpi")
    @Test
    fun featureGraphicIt() = featureGraphic(StoreLocale.IT, "it-IT")

    @Config(qualifiers = "en-rUS-w1024dp-h500dp-mdpi")
    @Test
    fun featureGraphicEn() = featureGraphic(StoreLocale.EN, "en-US")

    @Config(qualifiers = "it-rIT-w360dp-h800dp-xxhdpi")
    @Test
    fun screenshotsIt() = screenshots(StoreLocale.IT, "it-IT")

    @Config(qualifiers = "en-rUS-w360dp-h800dp-xxhdpi")
    @Test
    fun screenshotsEn() = screenshots(StoreLocale.EN, "en-US")

    private fun featureGraphic(locale: StoreLocale, folder: String) {
        compose.setContent {
            Box(Modifier.size(1024.dp, 500.dp)) {
                StoreFeatureGraphic(storeFeatureTagline(locale))
            }
        }
        compose.waitForIdle()
        val raw = File(outRoot, "_raw_feature_$folder.png")
        compose.onRoot().captureRoboImage(raw.absolutePath)
        val dest = File(outRoot, "$folder/images/featureGraphic.png")
        dest.parentFile?.mkdirs()
        writeSized(raw, dest, 1024, 500, opaque = true)
        raw.delete()
    }

    private fun screenshots(locale: StoreLocale, folder: String) {
        val dir = File(outRoot, "$folder/images/phoneScreenshots")
        dir.mkdirs()
        val catalog = storeCatalog()
        val shots = listOf<Pair<String, @Composable () -> Unit>>(
            "1_today" to {
                PhoneFrame {
                    HomeScreen(
                        state = storeHomeState(locale),
                        onOpenSettings = {},
                        onOpenActivity = {},
                        onOpenInbox = {},
                        onSeeAllRenewals = {},
                    )
                }
            },
            "2_activity" to {
                PhoneFrame {
                    ActivityScreen(
                        state = storeActivityState(locale),
                        paging = null,
                        staticItems = storeActivityItems(locale),
                        onQueryChange = {},
                        onOpenFilters = {},
                        onCloseFilters = {},
                        onApplyFilters = {},
                        onAddMovement = {},
                        onDelete = {},
                        onUndo = {},
                        onAcknowledgeNotice = {},
                        onEdit = {},
                        onAcceptInbox = {},
                        onDismissInbox = {},
                    )
                }
            },
            "3_recurring" to {
                PhoneFrame {
                    SubscriptionsScreen(
                        state = storeSubscriptionsState(locale),
                        catalog = catalog,
                        onTogglePeriod = {},
                        onSelectDay = {},
                        onTypeFilter = {},
                        onStatusFilter = {},
                        onClearFilters = {},
                        onOpenSubscription = {},
                        onAddRecurring = {},
                        onReview = {},
                    )
                }
            },
            "4_save" to {
                PhoneFrame {
                    InsightsScreen(
                        state = storeInsightsState(locale),
                        onSnooze = {},
                        onOpenSubscription = {},
                        onReviewSubscriptions = {},
                        onOpenCategory = {},
                        onOpenActivity = {},
                        onSaveBudget = { _, _, _, _, _, _ -> },
                        onDeleteBudget = {},
                        onSaveGoal = { _, _, _, _ -> },
                        onDeleteGoal = {},
                        onAddToGoal = { _, _ -> },
                    )
                }
            },
            "5_quickadd" to {
                PhoneFrame {
                    QuickAddContent(
                        state = storeQuickAddState(locale),
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
            },
            "6_today_dark" to {
                PhoneFrame(dark = true) {
                    HomeScreen(
                        state = storeHomeState(locale),
                        onOpenSettings = {},
                        onOpenActivity = {},
                        onOpenInbox = {},
                        onSeeAllRenewals = {},
                    )
                }
            },
        )
        val current = mutableIntStateOf(0)
        compose.setContent {
            shots[current.intValue].second()
        }
        shots.forEachIndexed { index, (name, _) ->
            compose.runOnIdle { current.intValue = index }
            compose.waitForIdle()
            val raw = File(dir, "_raw_$name.png")
            compose.onRoot().captureRoboImage(raw.absolutePath)
            writeSized(raw, File(dir, "$name.png"), 1080, 2400, opaque = false)
            raw.delete()
        }
    }

    @Composable
    private fun PhoneFrame(dark: Boolean = false, content: @Composable () -> Unit) {
        TamekiTheme(darkTheme = dark, dynamicColor = false, motionEnabled = false) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                content()
            }
        }
    }

    /**
     * Scale to exact Play dimensions. [opaque] flattens onto paper so the later Pillow pass can
     * emit a true RGB PNG without an alpha channel (required for featureGraphic).
     */
    private fun writeSized(src: File, dest: File, width: Int, height: Int, opaque: Boolean) {
        val decoded = BitmapFactory.decodeFile(src.absolutePath)
            ?: error("Failed to decode ${src.absolutePath}")
        val scaled = Bitmap.createScaledBitmap(decoded, width, height, true)
        val outBitmap = if (opaque) {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { flat ->
                val canvas = Canvas(flat)
                canvas.drawColor(AndroidColor.rgb(0xF2, 0xF0, 0xEA))
                canvas.drawBitmap(scaled, 0f, 0f, null)
            }
        } else {
            scaled
        }
        FileOutputStream(dest).use { stream ->
            outBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        if (outBitmap !== scaled) scaled.recycle()
        decoded.recycle()
    }
}
