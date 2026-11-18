package dev.antomanc.tameki.core.model.repository

import java.io.File
import java.time.LocalDate

/**
 * Schedules local reminders for renewals and trial endings.
 * WorkManager (or AlarmManager) implements this in app/core:data — domain only needs the contract.
 */
interface ReminderScheduler {
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
}

/** CSV import with column mapping and CSV/JSON export for migration. */
interface CsvService {
    suspend fun exportTransactions(destination: File)
    suspend fun importTransactions(source: File, columnMapping: Map<String, String>): Int
}
