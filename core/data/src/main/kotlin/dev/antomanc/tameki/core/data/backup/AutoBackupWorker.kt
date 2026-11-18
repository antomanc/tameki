package dev.antomanc.tameki.core.data.backup

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.antomanc.tameki.core.model.repository.BackupService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Periodic auto-backup into the user-chosen SAF folder URI stored in settings.
 * Full encrypted write is wired when the settings UI stores a backup password; until then
 * the worker verifies the folder URI is still grantable and succeeds.
 */
class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {
    private val backupService: BackupService by inject()

    override suspend fun doWork(): Result {
        val impl = backupService as? BackupServiceImpl ?: return Result.success()
        val folderUri = impl.autoBackupFolderUri() ?: return Result.success()
        return try {
            val uri = Uri.parse(folderUri)
            val persisted = applicationContext.contentResolver.persistedUriPermissions
                .any { it.uri == uri && it.isWritePermission }
            if (!persisted && uri.scheme != "file") {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "tameki_auto_backup"
        const val TAG = "auto_backup"
    }
}
