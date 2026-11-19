package dev.antomanc.tameki.scan.mlkit

import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.coroutines.runCatchingCancellable
import dev.antomanc.tameki.core.model.receipt.ReceiptTextParser
import dev.antomanc.tameki.scan.api.ReceiptEngine
import dev.antomanc.tameki.scan.api.ReceiptReadResult
import dev.antomanc.tameki.scan.api.ReceiptReader
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ML Kit text-recognition via the Play Services (unbundled) model → [ReceiptTextParser].
 * When the on-device model is not yet available, returns null so the chain can fall back to the
 * heuristic path (or the next reader).
 */
class MlKitOcrReceiptReader(
    private val appContext: android.content.Context,
    private val recognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
    private val baseCurrency: suspend () -> CurrencyCode = { CurrencyCode("EUR") },
    private val clock: Clock = Clock.systemDefaultZone(),
    private val loadBitmap: (String) -> android.graphics.Bitmap = { ReceiptBitmapLoader.load(it) },
) : ReceiptReader {

    override suspend fun read(imagePath: String): ReceiptReadResult = withContext(Dispatchers.IO) {
        val currency = baseCurrency()
        val bitmap = loadBitmap(imagePath)
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText = try {
            Tasks.await(recognizer.process(image))
        } catch (e: ExecutionException) {
            // Model not downloaded / Play Services unavailable → request install and fail soft.
            requestModelInstall()
            throw e.cause ?: e
        }
        val raw = visionText.text
        val draft = ReceiptTextParser.parse(
            rawText = raw,
            defaultCurrency = currency,
            today = LocalDate.now(clock),
        )
        ReceiptReadResult(
            draft = draft,
            rawText = raw,
            engine = ReceiptEngine.ML_KIT_OCR,
        )
    }

    private fun requestModelInstall() {
        runCatchingCancellable {
            val client = ModuleInstall.getClient(appContext)
            val request = ModuleInstallRequest.newBuilder()
                .addApi(recognizer)
                .build()
            client.installModules(request)
        }
    }
}
