package dev.antomanc.tameki.automation

import android.content.Intent
import android.net.Uri
import dev.antomanc.tameki.core.model.TamekiIntents

/**
 * Pure classification of every entry intent (deep links, ADD_TRANSACTION, SEND share, shortcuts,
 * QS tile, inbox notification). [MainActivity] executes the resulting [IntentAction].
 */
object IntentRouter {
    fun route(intent: Intent): IntentAction {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val type = intent.type.orEmpty()
                return when {
                    type.startsWith("text/") -> IntentAction.ShareText(
                        intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty(),
                    )
                    type.startsWith("image/") -> {
                        @Suppress("DEPRECATION") // The type-safe overload starts at API 33; minSdk is 31
                        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                            ?: intent.clipData?.getItemAt(0)?.uri
                        IntentAction.ShareImage(uri)
                    }
                    else -> IntentAction.None
                }
            }
            AutomationIntents.ACTION_ADD_TRANSACTION -> return IntentAction.AddTransaction
        }

        val open = intent.getStringExtra(AutomationIntents.EXTRA_OPEN)
            ?: intent.getStringExtra(TamekiIntents.EXTRA_OPEN)
        when (open) {
            AutomationIntents.OPEN_QUICK_ADD, "quick_add" -> return IntentAction.OpenQuickAdd
            AutomationIntents.OPEN_PASTE_AI, AutomationIntents.OPEN_IMPORT, "paste_ai", "import" ->
                return IntentAction.OpenPasteAi
            AutomationIntents.OPEN_SCAN, "scan" -> return IntentAction.OpenScan
            TamekiIntents.OPEN_INBOX, "inbox" -> return IntentAction.OpenInbox
        }

        val data = intent.data
        if (data != null && data.scheme.equals("tameki", ignoreCase = true)) {
            when (data.host?.lowercase()) {
                "add" -> return IntentAction.AddTransaction
                "import" -> return IntentAction.OpenPasteAi
            }
        }

        if (intent.hasExtra(AutomationIntents.EXTRA_AMOUNT)) {
            return IntentAction.AddTransaction
        }

        return IntentAction.None
    }
}

sealed interface IntentAction {
    data object None : IntentAction
    data object OpenQuickAdd : IntentAction
    data object OpenPasteAi : IntentAction
    data object OpenScan : IntentAction
    data object OpenInbox : IntentAction
    data object AddTransaction : IntentAction
    data class ShareText(val text: String) : IntentAction
    data class ShareImage(val uri: Uri?) : IntentAction
}
