package dev.antomanc.tameki.core.model.repository

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import java.io.File
import java.time.LocalDate

/**
 * Schedules local reminders for renewals and trial endings.
 * WorkManager (or AlarmManager) implements this in app/core:data — domain only needs the contract.
 */
interface ReminderScheduler {
    /** Ensures the daily renewal/trial worker is registered. */
    suspend fun ensureDailyWorker()
    suspend fun scheduleRecurringReminder(recurringId: Long, on: LocalDate, daysBefore: Int)
    suspend fun cancelRecurringReminder(recurringId: Long)
    suspend fun scheduleTrialEnding(recurringId: Long, endsOn: LocalDate)
    suspend fun cancelTrialEnding(recurringId: Long)
}

/**
 * Encrypted backup of JSON + receipt files.
 * AES-GCM with a key derived via PBKDF2-HMAC-SHA256 (310k iterations) from the user password.
 */
interface BackupService {
    suspend fun export(destination: File, password: CharArray)
    suspend fun import(source: File, password: CharArray)

    /** Schedule periodic auto-backup into a user-chosen SAF folder; stores [password] for the worker. */
    suspend fun scheduleAutoBackup(folderTreeUri: String, password: CharArray)
    suspend fun cancelAutoBackup()
}

/**
 * Stores the auto-backup password encrypted at rest (Android Keystore AES-GCM in production).
 */
interface BackupPasswordStore {
    suspend fun save(password: CharArray)
    suspend fun load(): CharArray?
    suspend fun clear()
}

/** Logical CSV columns the importer understands. */
object CsvColumns {
    const val DATE = "date"
    const val AMOUNT = "amount"
    const val CURRENCY = "currency"
    const val MERCHANT = "merchant"
    const val CATEGORY = "category"
    const val NOTE = "note"
    const val ACCOUNT = "account"
    const val KIND = "kind"
    const val TAGS = "tags"
}

/** How signed amounts map to expense/income when [CsvColumns.KIND] is absent. */
enum class CsvSignConvention {
    /** Negative amounts are expenses (bank-statement style). Default. */
    NegativeIsExpense,
    /** Negative amounts are income. */
    NegativeIsIncome,
}

/** Explicit date pattern for ambiguous numeric dates like 04/05/2026. */
enum class CsvDateFormat {
    ISO,
    /** day/month/year (EU). */
    DMY,
    /** month/day/year (US). */
    MDY,
}

data class CsvImportOptions(
    val signConvention: CsvSignConvention = CsvSignConvention.NegativeIsExpense,
    /** When null, auto-detected from the date column (day>12 decides; else device locale). */
    val dateFormat: CsvDateFormat? = null,
)

data class CsvProblem(
    val lineNumber: Int,
    val kind: CsvProblemKind,
    /** The offending cell or column key, when naming it makes the message concrete. */
    val detail: String? = null,
)

/** Every CSV import problem the user can be shown; localised in the settings feature. */
enum class CsvProblemKind {
    EMPTY_FILE,
    MISSING_MAPPING,
    UNPARSEABLE_DATE,
    MISSING_DATE,
    UNPARSEABLE_CURRENCY,
    UNPARSEABLE_AMOUNT,
    MISSING_AMOUNT,
    UNKNOWN_ACCOUNT,
    UNKNOWN_KIND,
}

data class CsvParsedRow(
    val lineNumber: Int,
    val date: LocalDate?,
    val amount: Money?,
    val currency: CurrencyCode?,
    val merchant: String?,
    val category: String?,
    val note: String?,
    val account: String?,
    val kind: String? = null,
    val tags: String? = null,
    val problems: List<CsvProblem> = emptyList(),
)

data class CsvImportPreview(
    val rows: List<CsvParsedRow>,
    val problems: List<CsvProblem>,
    /** Date format that will be used (resolved from [CsvImportOptions.dateFormat] or auto-detect). */
    val resolvedDateFormat: CsvDateFormat = CsvDateFormat.ISO,
)

data class CsvImportResult(
    val imported: Int,
    val skippedDuplicates: Int,
)

/**
 * CSV import with column mapping and CSV export for migration.
 * [columnMapping] maps [CsvColumns] keys to header names in the file.
 */
interface CsvService {
    suspend fun exportTransactions(destination: File)
    suspend fun previewImport(
        source: File,
        columnMapping: Map<String, String>,
        options: CsvImportOptions = CsvImportOptions(),
    ): CsvImportPreview
    suspend fun importTransactions(
        source: File,
        columnMapping: Map<String, String>,
        options: CsvImportOptions = CsvImportOptions(),
    ): CsvImportResult
}
