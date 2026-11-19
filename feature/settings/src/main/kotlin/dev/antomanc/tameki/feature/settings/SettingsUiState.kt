package dev.antomanc.tameki.feature.settings

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.repository.CsvImportPreview
import java.time.Instant

enum class SettingsSection {
    ROOT, GENERAL, SECURITY, NOTIFICATIONS, RECEIPTS, BACKUP, DATA, CSV_IMPORT,
    CATEGORIES, AUTOMATION, ABOUT, EXCHANGE,
    ;

    /** The page Back returns to: CSV import and exchange rates live under Data. */
    val parent: SettingsSection?
        get() = when (this) {
            ROOT -> null
            CSV_IMPORT, EXCHANGE -> DATA
            else -> ROOT
        }

    /** How deep the page is, for the direction of the transition. */
    val depth: Int get() = generateSequence(parent) { it.parent }.count()
}

enum class SettingsSnackbarMessage {
    BACKUP_EXPORTED,
    BACKUP_RESTORED,
    BACKUP_FAILED,
    AUTO_BACKUP_ON,
    CSV_EXPORTED,
    JSON_EXPORTED,
    EXPORT_FAILED,
    CSV_IMPORTED,
    CSV_FAILED,
    RATE_SAVED,
    RATE_INVALID,
    CAPTURE_APP_ADDED,
    CAPTURE_APP_INVALID,
}

data class SettingsUiState(
    /** The instant this screen was built; relative labels read it instead of the wall clock. */
    val now: Instant,
    val section: SettingsSection = SettingsSection.ROOT,
    val baseCurrency: CurrencyCode = CurrencyCode("EUR"),
    val theme: String = "system",
    val dynamicColor: Boolean = false,
    val lockEnabled: Boolean = false,
    val remindersEnabled: Boolean = true,
    val defaultRemindDays: Int = 2,
    val captureEnabled: Boolean = false,
    val captureGranted: Boolean = false,
    val bankingApps: List<BankingAppUi> = emptyList(),
    val customCaptureApps: List<BankingAppUi> = emptyList(),
    val recentNotificationApps: List<BankingAppUi> = emptyList(),
    val allowlist: Set<String> = emptySet(),
    val allowlistNone: Boolean = false,
    val nanoStatus: NanoStatusUi = NanoStatusUi.UNSUPPORTED,
    val silentAutomation: Boolean = false,
    val autoBackupUri: String? = null,
    val autoBackupFreqDays: Int = 7,
    /** Whether a password is already in the encrypted store, even if this screen never saw it. */
    val autoBackupPasswordStored: Boolean = false,
    val autoBackupLastSuccess: Instant? = null,
    val autoBackupLastFailure: Instant? = null,
    val autoBackupLastFailureReason: String? = null,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val csvPreview: CsvImportPreview? = null,
    val csvHeaders: List<String> = emptyList(),
    val csvMapping: Map<String, String> = emptyMap(),
    val snackbar: SettingsSnackbarMessage? = null,
    val confirmRestore: Boolean = false,
    val versionName: String = "",
    val isDebug: Boolean = false,
)
