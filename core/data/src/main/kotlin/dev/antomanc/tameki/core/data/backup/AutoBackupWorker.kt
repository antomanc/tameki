package dev.antomanc.tameki.core.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.antomanc.tameki.core.data.R
import dev.antomanc.tameki.core.model.repository.BackupService
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Periodic auto-backup into the user-chosen SAF folder URI stored in settings.
 * Writes an encrypted `.tameki` file, keeps the last [KEEP_COUNT] files this worker created,
 * and records success/failure timestamps for the UI.
 */
class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {
    private val backupService: BackupService by inject()
    private val clock: Clock by inject()

    override suspend fun doWork(): Result {
        val impl = backupService as? BackupServiceImpl ?: return Result.success()
        val settings = impl.settingsRepository()
        val folderUri = impl.autoBackupFolderUri()
        if (folderUri == null) {
            recordFailure(settings, applicationContext.getString(R.string.data_auto_backup_no_folder))
            return Result.failure()
        }
        val password = impl.passwordStore().load()
        if (password == null) {
            recordFailure(settings, applicationContext.getString(R.string.data_auto_backup_no_password))
            return Result.failure()
        }
        return try {
            val uri = Uri.parse(folderUri)
            val persisted = applicationContext.contentResolver.persistedUriPermissions
                .any { it.uri == uri && it.isWritePermission }
            val isFileScheme = uri.scheme == "file"
            if (!persisted && !isFileScheme) {
                recordFailure(settings, applicationContext.getString(R.string.data_auto_backup_permission_lost))
                return Result.failure()
            }
            val today = LocalDate.now(clock).format(DATE_FMT)
            val fileName = "tameki-autobackup-$today.tameki"
            val temp = File(applicationContext.cacheDir, "auto_backup_tmp.tameki")
            try {
                impl.export(temp, password)
                if (isFileScheme) {
                    val dir = File(
                        uri.path ?: return fail(
                            settings,
                            applicationContext.getString(R.string.data_auto_backup_invalid_folder),
                        ),
                    )
                    if (!dir.isDirectory || !dir.canWrite()) {
                        return fail(
                            settings,
                            applicationContext.getString(R.string.data_auto_backup_permission_lost),
                        )
                    }
                    val out = File(dir, fileName)
                    temp.copyTo(out, overwrite = true)
                    pruneFileBackups(dir)
                } else {
                    val tree = DocumentFile.fromTreeUri(applicationContext, uri)
                        ?: return fail(
                            settings,
                            applicationContext.getString(R.string.data_auto_backup_permission_lost),
                        )
                    if (!tree.canWrite()) {
                        return fail(
                            settings,
                            applicationContext.getString(R.string.data_auto_backup_permission_lost),
                        )
                    }
                    val existing = tree.findFile(fileName)
                    existing?.delete()
                    val target = tree.createFile("application/octet-stream", fileName)
                        ?: return fail(
                            settings,
                            applicationContext.getString(R.string.data_auto_backup_create_file),
                        )
                    applicationContext.contentResolver.openOutputStream(target.uri)?.use { out ->
                        temp.inputStream().use { it.copyTo(out) }
                    } ?: return fail(
                        settings,
                        applicationContext.getString(R.string.data_auto_backup_write_file),
                    )
                    pruneDocumentBackups(tree)
                }
                settings.setAutoBackupLastSuccessAt(Instant.now(clock))
                settings.setAutoBackupLastFailureAt(null)
                settings.setAutoBackupLastFailureReason(null)
                Result.success()
            } finally {
                temp.delete()
                password.fill('\u0000')
            }
        } catch (t: Exception) {
            recordFailure(settings, applicationContext.getString(R.string.data_auto_backup_failed))
            Result.failure()
        }
    }

    private suspend fun fail(settings: dev.antomanc.tameki.core.data.settings.SettingsRepositoryImpl, reason: String): Result {
        recordFailure(settings, reason)
        return Result.failure()
    }

    private suspend fun recordFailure(
        settings: dev.antomanc.tameki.core.data.settings.SettingsRepositoryImpl,
        reason: String,
    ) {
        settings.setAutoBackupLastFailureAt(Instant.now(clock))
        settings.setAutoBackupLastFailureReason(reason)
    }

    private fun pruneFileBackups(dir: File) {
        val files = dir.listFiles { f -> f.isFile && f.name.startsWith(PREFIX) && f.name.endsWith(SUFFIX) }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
        files.drop(KEEP_COUNT).forEach { it.delete() }
    }

    private fun pruneDocumentBackups(tree: DocumentFile) {
        val files = tree.listFiles()
            .filter { it.isFile && (it.name ?: "").startsWith(PREFIX) && (it.name ?: "").endsWith(SUFFIX) }
            .sortedByDescending { it.lastModified() }
        files.drop(KEEP_COUNT).forEach { it.delete() }
    }

    companion object {
        const val UNIQUE_NAME = "tameki_auto_backup"
        const val TAG = "auto_backup"
        const val KEEP_COUNT = 5
        private const val PREFIX = "tameki-autobackup-"
        private const val SUFFIX = ".tameki"
        private val DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE
    }
}
