package dev.antomanc.tameki.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Handles Add / Dismiss actions on the inbox notification. Not exported.
 */
class InboxActionReceiver : BroadcastReceiver(), KoinComponent {
    private val accept: InboxAccept by inject()
    private val notifier: InboxNotifier by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        if (itemId < 0L) return
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_ADD -> accept.accept(itemId)
                    ACTION_DISMISS -> accept.dismiss(itemId)
                }
                notifier.refresh()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_ADD: String = "dev.antomanc.tameki.capture.ACTION_ADD"
        const val ACTION_DISMISS: String = "dev.antomanc.tameki.capture.ACTION_DISMISS"
        const val EXTRA_ITEM_ID: String = "item_id"
    }
}
