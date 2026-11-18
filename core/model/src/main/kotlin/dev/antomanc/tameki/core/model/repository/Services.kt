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

    /** Schedule periodic auto-backup into a user-chosen SAF folder tree URI. */
    suspend fun scheduleAutoBackup(folderTreeUri: String)
    suspend fun cancelAutoBackup()
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
}

data class CsvProblem(
    val lineNumber: Int,
    val message: String,
)

data class CsvParsedRow(
    val lineNumber: Int,
    val date: LocalDate?,
    val amount: Money?,
    val currency: CurrencyCode?,
    val merchant: String?,
    val category: String?,
    val note: String?,
    val account: String?,
    val problems: List<String> = emptyList(),
)

data class CsvImportPreview(
    val rows: List<CsvParsedRow>,
    val problems: List<CsvProblem>,
)

/**
 * CSV import with column mapping and CSV export for migration.
 * [columnMapping] maps [CsvColumns] keys to header names in the file.
 */
interface CsvService {
    suspend fun exportTransactions(destination: File)
    suspend fun previewImport(source: File, columnMapping: Map<String, String>): CsvImportPreview
    suspend fun importTransactions(source: File, columnMapping: Map<String, String>): Int
}
