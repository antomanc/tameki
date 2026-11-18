package dev.antomanc.tameki.capture

import dev.antomanc.tameki.core.model.capture.PaymentNotificationParser
import dev.antomanc.tameki.core.model.entity.InboxItem
import dev.antomanc.tameki.core.model.entity.InboxStatus
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.repository.InboxRepository
import java.time.Clock
import java.time.Instant

/**
 * Pure-ish pipeline: allowlist → parse → dedupe → persist PENDING inbox item.
 * Never logs notification content.
 */
class PaymentNotificationProcessor(
    private val inbox: InboxRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val deduper: NotificationDeduper = NotificationDeduper(),
    private val onInboxChanged: suspend () -> Unit = {},
) {
    data class Incoming(
        val packageName: String,
        val title: String,
        val text: String,
        val notificationKey: String?,
        val postedAt: Instant,
        val captureEnabled: Boolean,
        val allowlist: Set<String>,
    )

    /**
     * @return upserted inbox id, or null when ignored.
     */
    suspend fun handle(incoming: Incoming): Long? {
        if (!incoming.captureEnabled) return null
        val allowed = CaptureAllowlist.effective(incoming.allowlist)
        if (incoming.packageName !in allowed) return null

        val blob = listOf(incoming.title, incoming.text)
            .filter { it.isNotBlank() }
            .joinToString("\n")
        if (blob.isBlank()) return null

        val now = Instant.now(clock)
        if (deduper.shouldSkip(incoming.packageName, blob, incoming.notificationKey, now)) {
            return null
        }

        val draft = PaymentNotificationParser.parse(
            packageName = incoming.packageName,
            title = incoming.title,
            text = incoming.text,
            postedAt = incoming.postedAt,
        ) ?: return null

        val id = inbox.upsert(
            InboxItem(
                draft = draft,
                origin = TransactionSource.NOTIFICATION,
                rawText = blob,
                receivedAt = now,
                status = InboxStatus.PENDING,
            ),
        )
        onInboxChanged()
        return id
    }
}
