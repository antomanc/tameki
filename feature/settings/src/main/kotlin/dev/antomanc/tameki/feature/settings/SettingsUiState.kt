package dev.antomanc.tameki.feature.settings

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.repository.CsvImportPreview
import java.time.Instant

enum class SettingsSection {
    ROOT, GENERAL, SECURITY, NOTIFICATIONS, RECEIPTS, BACKUP, DATA, CSV_IMPORT,
    CATEGORIES, AUTOMATION, ABOUT, EXCHANGE,
}

enum class SettingsSnackbarMessage {
    BACKUP_EXPORTED,
    BACKUP_RESTORED,
    BACKUP_FAILED,
    AUTO_BACKUP_ON,
    CSV_EXPORTED,
    JSON_EXPORTED,
    CSV_IMPORTED,
    RATE_SAVED,
}

data class SettingsUiState(
    val section: SettingsSection = SettingsSection.ROOT,
    val baseCurrency: CurrencyCode = CurrencyCode("EUR"),
    val theme: String = "system",
    val dynamicColor: Boolean = true,
    val lockEnabled: Boolean = false,
    val remindersEnabled: Boolean = true,
    val defaultRemindDays: Int = 2,
    val captureEnabled: Boolean = false,
    val captureGranted: Boolean = false,
    val bankingApps: List<BankingAppUi> = emptyList(),
    val allowlist: Set<String> = emptySet(),
    val allowlistNone: Boolean = false,
    val nanoStatus: NanoStatusUi = NanoStatusUi.UNSUPPORTED,
    val silentAutomation: Boolean = false,
    val autoBackupUri: String? = null,
    val autoBackupFreqDays: Int = 7,
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
