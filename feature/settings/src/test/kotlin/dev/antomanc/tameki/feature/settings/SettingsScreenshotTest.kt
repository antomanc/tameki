package dev.antomanc.tameki.feature.settings

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
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.repository.CsvImportPreview
import dev.antomanc.tameki.core.model.repository.CsvParsedRow
import dev.antomanc.tameki.core.model.repository.CsvProblem
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test fun rootLight() = capture(SettingsSection.ROOT, "settings_root_light", false, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test fun rootDark() = capture(SettingsSection.ROOT, "settings_root_dark", true, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test fun rootFont200() = capture(SettingsSection.ROOT, "settings_root_font200", false, 2f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test fun backupLight() = capture(SettingsSection.BACKUP, "settings_backup_light", false, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test fun backupDark() = capture(SettingsSection.BACKUP, "settings_backup_dark", true, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test fun backupFont200() = capture(SettingsSection.BACKUP, "settings_backup_font200", false, 2f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test fun csvLight() = capture(SettingsSection.CSV_IMPORT, "settings_csv_light", false, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test fun csvDark() = capture(SettingsSection.CSV_IMPORT, "settings_csv_dark", true, 1f)

    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    @Test fun csvFont200() = capture(SettingsSection.CSV_IMPORT, "settings_csv_font200", false, 2f)

    private fun capture(section: SettingsSection, name: String, dark: Boolean, fontScale: Float) {
        val state = sampleSettingsState().copy(
            section = section,
            csvHeaders = listOf("date", "amount", "note"),
            csvMapping = mapOf("date" to "date", "amount" to "amount"),
            csvPreview = CsvImportPreview(
                rows = listOf(
                    CsvParsedRow(2, LocalDate.of(2026, 11, 1), null, null, "X", null, null, null),
                ),
                problems = listOf(CsvProblem(2, "Missing amount")),
            ),
            versionName = "0.1.0",
            baseCurrency = CurrencyCode("EUR"),
        )
        compose.setContent {
            TamekiTheme(darkTheme = dark, dynamicColor = false, motionEnabled = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        SettingsScreen(
                            state = state,
                            onOpen = {},
                            onBackRoot = {},
                            onSetBaseCurrency = {},
                            onSetTheme = {},
                            onSetDynamicColor = {},
                            onSetLock = {},
                            onSetReminders = {},
                            onSetDefaultRemindDays = {},
                            onSetCaptureEnabled = {},
                            onOpenCaptureGrant = {},
                            onRefreshCapture = {},
                            onSetAllowlistNone = {},
                            onSetAllowlistDefault = {},
                            onToggleBanking = { _, _ -> },
                            onDownloadNano = {},
                            onExportBackup = { _, _ -> },
                            onRequestRestore = {},
                            onDismissRestore = {},
                            onRestore = { _, _ -> },
                            onScheduleAutoBackup = { _, _, _ -> },
                            onCancelAutoBackup = {},
                            onExportCsv = {},
                            onExportJson = {},
                            onLoadCsv = { _, _ -> },
                            onSetCsvMapping = { _, _ -> },
                            onPreviewCsv = {},
                            onImportCsv = {},
                            onSetManualRate = { _, _, _ -> },
                            onSetSilent = {},
                            onOpenAutomationDocs = {},
                            onOpenDebugGallery = {},
                            onClearSnackbar = {},
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }
}
