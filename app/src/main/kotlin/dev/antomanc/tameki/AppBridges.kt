package dev.antomanc.tameki

import android.content.Context
import android.content.Intent
import dev.antomanc.tameki.capture.CaptureAccess
import dev.antomanc.tameki.feature.settings.BankingAppUi
import dev.antomanc.tameki.feature.settings.CaptureSettingsBridge
import dev.antomanc.tameki.feature.settings.GeminiNanoBridge
import dev.antomanc.tameki.feature.settings.NanoStatusUi
import dev.antomanc.tameki.scan.mlkit.GeminiNanoReceiptReader
import dev.antomanc.tameki.scan.mlkit.GeminiNanoStatus

class CaptureSettingsBridgeImpl(
    private val context: Context,
) : CaptureSettingsBridge {
    override fun isGranted(): Boolean = CaptureAccess.isGranted(context)

    override fun openGrantSettings() {
        context.startActivity(
            CaptureAccess.intentToGrant().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override fun installedBankingApps(): List<BankingAppUi> =
        CaptureAccess.installedKnownBankingApps(context).map {
            BankingAppUi(it.label, it.packageName)
        }

    override fun resolveAppLabel(packageName: String): String? =
        CaptureAccess.resolveAppLabel(context, packageName)
}

class GeminiNanoBridgeImpl(
    private val reader: GeminiNanoReceiptReader,
) : GeminiNanoBridge {
    override suspend fun status(): NanoStatusUi = when (reader.checkStatus()) {
        GeminiNanoStatus.AVAILABLE -> NanoStatusUi.AVAILABLE
        GeminiNanoStatus.DOWNLOADABLE -> NanoStatusUi.DOWNLOADABLE
        GeminiNanoStatus.DOWNLOADING -> NanoStatusUi.DOWNLOADING
        GeminiNanoStatus.UNAVAILABLE -> NanoStatusUi.UNSUPPORTED
    }

    override suspend fun download() {
        reader.downloadFeature()
    }
}
