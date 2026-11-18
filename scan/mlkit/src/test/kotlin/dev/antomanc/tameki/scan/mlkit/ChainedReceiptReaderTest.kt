package dev.antomanc.tameki.scan.mlkit

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.scan.api.ReceiptEngine
import dev.antomanc.tameki.scan.api.ReceiptReadResult
import dev.antomanc.tameki.scan.api.ReceiptReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ChainedReceiptReaderTest {
    private val geminiResult = ReceiptReadResult(
        draft = TransactionDraft(merchant = "Gemini"),
        rawText = """{"tameki":1}""",
        engine = ReceiptEngine.GEMINI_NANO,
    )
    private val ocrResult = ReceiptReadResult(
        draft = TransactionDraft(merchant = "OCR"),
        rawText = "TOTALE 10,00",
        engine = ReceiptEngine.ML_KIT_OCR,
    )

    @Test
    fun usesPrimaryWhenAvailable() = runTest {
        val result = ChainedReceiptReader.chainRead(
            tryPrimary = { geminiResult },
            fallback = { ocrResult },
        )
        assertThat(result.engine).isEqualTo(ReceiptEngine.GEMINI_NANO)
        assertThat(result.draft.merchant).isEqualTo("Gemini")
    }

    @Test
    fun fallsBackWhenPrimaryUnavailable() = runTest {
        val result = ChainedReceiptReader.chainRead(
            tryPrimary = { null },
            fallback = { ocrResult },
        )
        assertThat(result.engine).isEqualTo(ReceiptEngine.ML_KIT_OCR)
    }

    @Test
    fun fallsBackOnPrimaryException() = runTest {
        val result = ChainedReceiptReader.chainRead(
            tryPrimary = { error("parse failure") },
            fallback = { ocrResult },
        )
        assertThat(result.engine).isEqualTo(ReceiptEngine.ML_KIT_OCR)
    }

    @Test
    fun fallsBackOnTimeout() = runTest {
        val result = ChainedReceiptReader.chainRead(
            tryPrimary = {
                delay(50)
                geminiResult
            },
            fallback = { ocrResult },
            timeoutMs = 1,
        )
        assertThat(result.engine).isEqualTo(ReceiptEngine.ML_KIT_OCR)
    }

    @Test
    fun chainedReaderDelegatesToInjectedFakes() = runTest {
        val reader = ChainedReceiptReader(
            tryGemini = { path ->
                assertThat(path).isEqualTo("/tmp/r.jpg")
                geminiResult
            },
            ocr = object : ReceiptReader {
                override suspend fun read(imagePath: String) = ocrResult
            },
        )
        val result = reader.read("/tmp/r.jpg")
        assertThat(result.rawText).isEqualTo("""{"tameki":1}""")
    }
}
