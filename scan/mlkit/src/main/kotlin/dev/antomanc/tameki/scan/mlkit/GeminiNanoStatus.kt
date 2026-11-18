package dev.antomanc.tameki.scan.mlkit

import com.google.mlkit.genai.common.FeatureStatus

/** App-facing mirror of ML Kit [FeatureStatus] for Gemini Nano / GenAI Prompt. */
enum class GeminiNanoStatus {
    UNAVAILABLE,
    DOWNLOADABLE,
    DOWNLOADING,
    AVAILABLE,
    ;

    companion object {
        fun fromFeatureStatus(@FeatureStatus status: Int): GeminiNanoStatus = when (status) {
            FeatureStatus.AVAILABLE -> AVAILABLE
            FeatureStatus.DOWNLOADABLE -> DOWNLOADABLE
            FeatureStatus.DOWNLOADING -> DOWNLOADING
            else -> UNAVAILABLE
        }
    }
}
