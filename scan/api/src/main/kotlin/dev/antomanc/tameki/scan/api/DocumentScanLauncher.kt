package dev.antomanc.tameki.scan.api

/**
 * Result of a document/receipt capture attempt.
 * [Success.imagePath] is a local JPEG the host can copy into private receipt storage.
 */
sealed class DocumentScanOutcome {
    data class Success(val imagePath: String) : DocumentScanOutcome()
    data object Cancelled : DocumentScanOutcome()
    data object Unavailable : DocumentScanOutcome()
}

/**
 * Document scanner UI (ML Kit on Android). Typed with [Any] for Activity / IntentSender /
 * Intent so this module stays pure JVM; the Android impl casts.
 *
 * Hosts register an Activity Result launcher for IntentSender, call [prepare], then
 * [interpret] the activity result. Tests swap in a fake.
 */
interface DocumentScanLauncher {
    /**
     * Resolve a start IntentSender for [activity] (an `android.app.Activity`).
     * [onReady] receives an `android.content.IntentSender`; [onUnavailable] when Play
     * services or the scanner cannot start (host should fall back to a photo picker).
     */
    fun prepare(
        activity: Any,
        onReady: (intentSender: Any) -> Unit,
        onUnavailable: (cause: Throwable?) -> Unit,
    )

    /**
     * Map `Activity.RESULT_*` + result Intent into an outcome, copying the scanned page
     * into a JPEG under [cacheDirPath] on success.
     */
    fun interpret(resultCode: Int, data: Any?, cacheDirPath: String): DocumentScanOutcome
}
