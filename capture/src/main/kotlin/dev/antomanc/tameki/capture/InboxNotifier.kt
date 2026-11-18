package dev.antomanc.tameki.capture

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.antomanc.tameki.core.model.TamekiIntents
import dev.antomanc.tameki.core.model.entity.InboxItem
import dev.antomanc.tameki.core.model.entity.InboxStatus
import dev.antomanc.tameki.core.model.repository.InboxRepository
import kotlinx.coroutines.flow.first

/**
 * User-visible "Inbox" notification summarizing PENDING items with Add / Dismiss actions.
 */
class InboxNotifier(
    private val context: Context,
    private val inbox: InboxRepository,
) {
    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.capture_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.capture_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    suspend fun refresh() {
        ensureChannel()
        val pending = inbox.observe(InboxStatus.PENDING).first()
        if (pending.isEmpty()) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            return
        }
        val head = pending.first()
        val summary = if (pending.size == 1) {
            context.getString(R.string.capture_inbox_summary_one)
        } else {
            context.getString(R.string.capture_inbox_summary_many, pending.size)
        }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent().setClassName(context.packageName, MAIN_ACTIVITY).apply {
                putExtra(TamekiIntents.EXTRA_OPEN, TamekiIntents.OPEN_INBOX)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val add = actionPending(InboxActionReceiver.ACTION_ADD, head, requestCode = 1)
        val dismiss = actionPending(InboxActionReceiver.ACTION_DISMISS, head, requestCode = 2)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(dev.antomanc.tameki.core.data.R.drawable.ic_stat_tameki)
            .setContentTitle(context.getString(R.string.capture_inbox_title))
            .setContentText(summary)
            .setContentIntent(openApp)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setNumber(pending.size)
            .addAction(0, context.getString(R.string.capture_action_add), add)
            .addAction(0, context.getString(R.string.capture_action_dismiss), dismiss)
            .build()

        // Without POST_NOTIFICATIONS the item still waits in the in-app list; only the alert is skipped.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun actionPending(action: String, item: InboxItem, requestCode: Int): PendingIntent {
        val intent = Intent(context, InboxActionReceiver::class.java).apply {
            this.action = action
            putExtra(InboxActionReceiver.EXTRA_ITEM_ID, item.id)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID: String = "detected_payments"
        const val NOTIFICATION_ID: Int = 41001
        private const val MAIN_ACTIVITY: String = "dev.antomanc.tameki.MainActivity"
    }
}
