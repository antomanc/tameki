package dev.antomanc.tameki.feature.settings

/** App-bound bridge so settings never depends on :capture. */
interface CaptureSettingsBridge {
    fun isGranted(): Boolean
    fun openGrantSettings()
    fun installedBankingApps(): List<BankingAppUi>
    fun resolveAppLabel(packageName: String): String? = null
}

data class BankingAppUi(val label: String, val packageName: String)

enum class NanoStatusUi { AVAILABLE, DOWNLOADABLE, DOWNLOADING, UNSUPPORTED }

/** App-bound bridge so settings never depends on :scan:mlkit. */
interface GeminiNanoBridge {
    suspend fun status(): NanoStatusUi
    suspend fun download()
}
