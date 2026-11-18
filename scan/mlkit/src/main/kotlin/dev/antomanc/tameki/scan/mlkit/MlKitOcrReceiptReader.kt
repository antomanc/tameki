package dev.antomanc.tameki.scan.mlkit

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.receipt.ReceiptTextParser
import dev.antomanc.tameki.scan.api.ReceiptEngine
import dev.antomanc.tameki.scan.api.ReceiptReadResult
import dev.antomanc.tameki.scan.api.ReceiptReader
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ML Kit text-recognition (bundled Latin) → [ReceiptTextParser].
 * Bitmap is loaded downsampled and EXIF-rotated via [ReceiptBitmapLoader].
 */
class MlKitOcrReceiptReader(
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
        val visionText = Tasks.await(recognizer.process(image))
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
}
