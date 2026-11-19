package dev.antomanc.tameki.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AutomationUndoReceiver : BroadcastReceiver(), KoinComponent {
    private val transactions: TransactionRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AutomationNotifier.ACTION_UNDO) return
        val id = intent.getLongExtra(AutomationNotifier.EXTRA_TX_ID, -1L)
        if (id < 0) return
        // The Undo action is done the moment it is tapped: the notification goes away with it.
        NotificationManagerCompat.from(context).cancel(AutomationNotifier.NOTIFICATION_TAG, id.toInt())
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                transactions.delete(id)
            } finally {
                pending.finish()
            }
        }
    }
}
