package dev.antomanc.tameki.scan.mlkit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import dev.antomanc.tameki.scan.api.DocumentScanLauncher
import dev.antomanc.tameki.scan.api.DocumentScanOutcome
import java.io.File
import java.util.UUID

/**
 * ML Kit Document Scanner (Play services, unbundled). No CAMERA permission — the scanner UI
 * runs in Google Play services with edge detection, perspective crop, and gallery import.
 */
class MlKitDocumentScanLauncher(
    private val appContext: Context,
) : DocumentScanLauncher {

    private val scanner by lazy {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options)
    }

    override fun prepare(
        activity: Any,
        onReady: (intentSender: Any) -> Unit,
        onUnavailable: (cause: Throwable?) -> Unit,
    ) {
        val act = activity as? Activity
        if (act == null) {
            onUnavailable(IllegalArgumentException("Expected Activity"))
            return
        }
        scanner.getStartScanIntent(act)
            .addOnSuccessListener { sender: IntentSender -> onReady(sender) }
            .addOnFailureListener { error -> onUnavailable(error) }
    }

    override fun interpret(resultCode: Int, data: Any?, cacheDirPath: String): DocumentScanOutcome {
        if (resultCode != Activity.RESULT_OK) return DocumentScanOutcome.Cancelled
        val intent = data as? Intent
        val result = GmsDocumentScanningResult.fromActivityResultIntent(intent)
            ?: return DocumentScanOutcome.Cancelled
        val pageUri = result.pages?.firstOrNull()?.imageUri
            ?: return DocumentScanOutcome.Cancelled
        return copyToCache(pageUri, cacheDirPath)
            ?.let { DocumentScanOutcome.Success(it) }
            ?: DocumentScanOutcome.Unavailable
    }

    private fun copyToCache(uri: Uri, cacheDirPath: String): String? {
        val dir = File(cacheDirPath).apply { mkdirs() }
        val dest = File(dir, "scan_${UUID.randomUUID()}.jpg")
        return try {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            dest.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}
