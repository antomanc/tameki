package dev.antomanc.tameki.capture

import java.time.Duration
import java.time.Instant

/**
 * Dedupes payment notifications: same package + text within [window], and repeat posts of the
 * same notification key. Pure / testable — no Android types.
 */
class NotificationDeduper(
    private val window: Duration = DEFAULT_WINDOW,
) {
    private data class TextKey(val packageName: String, val text: String)

    private val recentByText = LinkedHashMap<TextKey, Instant>()
    private val seenKeys = LinkedHashSet<String>()

    // The listener handles posts concurrently; the two collections are not thread-safe.
    @Synchronized
    fun shouldSkip(
        packageName: String,
        text: String,
        notificationKey: String?,
        now: Instant,
    ): Boolean {
        prune(now)
        if (!notificationKey.isNullOrBlank() && !seenKeys.add(notificationKey)) {
            return true
        }
        val key = TextKey(packageName, text)
        val previous = recentByText[key]
        if (previous != null && Duration.between(previous, now) < window) {
            return true
        }
        recentByText[key] = now
        return false
    }

    @Synchronized
    fun clear() {
        recentByText.clear()
        seenKeys.clear()
    }

    private fun prune(now: Instant) {
        val cutoff = now.minus(window)
        val stale = recentByText.entries.filter { it.value.isBefore(cutoff) }.map { it.key }
        stale.forEach { recentByText.remove(it) }
        // Cap key memory; keys are unique per post lifecycle on the device.
        while (seenKeys.size > MAX_KEYS) {
            val first = seenKeys.firstOrNull() ?: break
            seenKeys.remove(first)
        }
    }

    companion object {
        val DEFAULT_WINDOW: Duration = Duration.ofMinutes(2)
        private const val MAX_KEYS: Int = 500
    }
}
