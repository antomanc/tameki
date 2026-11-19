package dev.antomanc.tameki.capture

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Opt-in notification capture. Only runs when capture is enabled and the package is allowlisted.
 * Hands work off the main thread immediately; never logs notification content.
 */
class PaymentNotificationListener : NotificationListenerService(), KoinComponent {
    private val settings: SettingsRepository by inject()
    private val processor: PaymentNotificationProcessor by inject()
    private val notifier: InboxNotifier by inject()

    private var scope: CoroutineScope? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    override fun onListenerDisconnected() {
        scope?.cancel()
        scope = null
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val packageName = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return
        val title = extras.charSeq(Notification.EXTRA_TITLE)
        val text = extras.charSeq(Notification.EXTRA_TEXT)
        val bigText = extras.charSeq(Notification.EXTRA_BIG_TEXT)
        val body = bigText.ifBlank { text }
        // Banking apps often reuse one notification id for every payment, so the key alone would
        // skip every payment after the first. The app-set timestamp tells payments apart while
        // staying the same across re-posts (updates) of one notification.
        val key = "${sbn.key}@${sbn.notification?.`when` ?: sbn.postTime}"
        val postedAt = Instant.ofEpochMilli(sbn.postTime)
        // Hand off immediately — no parsing / IO on the main thread.
        val jobScope = scope ?: return
        jobScope.launch {
            val enabled = settings.observeNotificationCaptureEnabled().first()
            val allowlist = settings.observeNotificationAllowlist().first()
            processor.handle(
                PaymentNotificationProcessor.Incoming(
                    packageName = packageName,
                    title = title,
                    text = body,
                    notificationKey = key,
                    postedAt = postedAt,
                    captureEnabled = enabled,
                    allowlist = allowlist,
                ),
            )
            notifier.refresh()
        }
    }

    private fun Bundle.charSeq(key: String): String =
        getCharSequence(key)?.toString()?.trim().orEmpty()
}
