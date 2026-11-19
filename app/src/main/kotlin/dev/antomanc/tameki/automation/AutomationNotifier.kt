package dev.antomanc.tameki.automation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.antomanc.tameki.MainActivity
import dev.antomanc.tameki.R
import dev.antomanc.tameki.core.model.coroutines.runCatchingCancellable

object AutomationNotifier {
    private const val CHANNEL_ID = "automation"
    const val ACTION_UNDO = "dev.antomanc.tameki.action.UNDO_AUTOMATION"
    const val EXTRA_TX_ID = "tx_id"
    /** Tag + transaction id identify one "Added" notification, so Undo can take down exactly it. */
    const val NOTIFICATION_TAG = "automation_added"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.app_automation_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    fun showAdded(context: Context, transactionId: Long) {
        ensureChannel(context)
        val undo = PendingIntent.getBroadcast(
            context,
            transactionId.toInt(),
            Intent(ACTION_UNDO).setPackage(context.packageName).putExtra(EXTRA_TX_ID, transactionId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(dev.antomanc.tameki.core.data.R.drawable.ic_stat_tameki)
            .setContentTitle(context.getString(R.string.app_automation_added))
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.app_automation_undo), undo)
            .setAutoCancel(true)
            .build()
        val manager = NotificationManagerCompat.from(context)
        if (manager.areNotificationsEnabled()) {
            runCatchingCancellable {
                manager.notify(NOTIFICATION_TAG, transactionId.toInt(), notification)
            }
        }
    }
}
