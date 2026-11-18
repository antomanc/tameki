package dev.antomanc.tameki.scan.mlkit

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.ai.AiExchangeFormat
import dev.antomanc.tameki.scan.api.ReceiptEngine
import dev.antomanc.tameki.scan.api.ReceiptReadResult
import dev.antomanc.tameki.scan.api.ReceiptReader
import java.time.Clock
import kotlinx.coroutines.flow.first

/**
 * On-device Gemini Nano via ML Kit GenAI Prompt API (genai-prompt 1.0.0-beta5).
 *
 * Real calls (from AAR): [Generation.getClient], [GenerativeModel.checkStatus],
 * [GenerativeModel.download], [GenerativeModel.generateContent] with [ImagePart]/[TextPart].
 */
class GeminiNanoReceiptReader(
    modelProvider: () -> GenerativeModel = { Generation.getClient() },
    private val baseCurrency: suspend () -> CurrencyCode = { CurrencyCode("EUR") },
    private val clock: Clock = Clock.systemDefaultZone(),
    private val loadBitmap: (String) -> android.graphics.Bitmap = { ReceiptBitmapLoader.load(it) },
) : ReceiptReader {
    // Created on first use, never at DI time: on devices (or builds) where the GenAI client
    // cannot be built, getClient() throws, and that must only disable this reader, not the app.
    private val model: GenerativeModel by lazy(modelProvider)


    suspend fun checkStatus(): GeminiNanoStatus =
        runCatching { GeminiNanoStatus.fromFeatureStatus(model.checkStatus()) }
            .getOrDefault(GeminiNanoStatus.UNAVAILABLE)

    /**
     * Triggers model download when status is [GeminiNanoStatus.DOWNLOADABLE].
     * Collects the [GenerativeModel.download] flow until completed or failed.
     * Used by settings UI later.
     */
    suspend fun downloadFeature() {
        model.download().first { status ->
            when (status) {
                is DownloadStatus.DownloadCompleted -> true
                is DownloadStatus.DownloadFailed -> throw status.e
                else -> false
            }
        }
    }

    /**
     * Reads a receipt. Returns null when the feature is not [GeminiNanoStatus.AVAILABLE],
     * generation fails, or the model output is not parsable tameki JSON.
     */
    suspend fun tryRead(imagePath: String): ReceiptReadResult? {
        if (checkStatus() != GeminiNanoStatus.AVAILABLE) return null
        return runCatching { read(imagePath) }.getOrNull()
    }

    override suspend fun read(imagePath: String): ReceiptReadResult {
        val bitmap = loadBitmap(imagePath)
        val request = generateContentRequest(
            ImagePart(bitmap),
            TextPart(RECEIPT_PROMPT),
        ) { /* defaults */ }
        val response = model.generateContent(request)
        val raw = response.candidates.firstOrNull()?.text?.trim().orEmpty()
        val draft = GeminiNanoJson.parseTransaction(raw, baseCurrency(), clock)
            ?: error("Gemini Nano output is not parsable tameki AI JSON")
        return ReceiptReadResult(
            draft = draft,
            rawText = raw,
            engine = ReceiptEngine.GEMINI_NANO,
        )
    }

    companion object {
        /**
         * Strict instruction: ONLY the documented Tameki AI exchange JSON for one transaction
         * with line items (see docs/ai-format.md).
         */
        val RECEIPT_PROMPT: String =
            "Extract exactly one purchase from this receipt image.\n" +
                "Output ONLY a single JSON object matching the Tameki AI exchange format v1 — " +
                "no markdown fences, no prose:\n" +
                "{\"tameki\":1,\"transactions\":[{\"date\":\"YYYY-MM-DD\",\"time\":\"HH:MM\"," +
                "\"kind\":\"expense\",\"amount\":\"12.50\",\"currency\":\"EUR\",\"merchant\":\"Store\"," +
                "\"category\":\"Groceries\",\"note\":\"\",\"tags\":[]," +
                "\"items\":[{\"name\":\"Item\",\"amount\":\"1.00\",\"quantity\":1}]}]}\n" +
                "Include line items when visible. If a field is unclear, omit it rather than inventing values."
    }
}

/** Pure parse gate used by [GeminiNanoReceiptReader] and unit tests. */
object GeminiNanoJson {
    fun parseTransaction(
        raw: String,
        baseCurrency: CurrencyCode,
        clock: Clock,
    ): dev.antomanc.tameki.core.model.entity.TransactionDraft? {
        if (raw.isBlank()) return null
        val result = AiExchangeFormat.parse(raw, baseCurrency = baseCurrency, clock = clock)
        if (result.problems.any { it.path == "$" }) return null
        if (result.problems.any { it.path == "tameki" }) return null
        return result.transactions.firstOrNull()
    }
}
