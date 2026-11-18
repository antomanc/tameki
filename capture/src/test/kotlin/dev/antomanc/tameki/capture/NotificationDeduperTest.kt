package dev.antomanc.tameki.capture

import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import org.junit.Test

class NotificationDeduperTest {
    private val deduper = NotificationDeduper(window = Duration.ofMinutes(2))
    private val t0 = Instant.parse("2026-11-18T12:00:00Z")

    @Test
    fun skipsSamePackageAndTextWithinWindow() {
        assertThat(deduper.shouldSkip("com.bank", "paid 10", "key-1", t0)).isFalse()
        assertThat(
            deduper.shouldSkip("com.bank", "paid 10", "key-2", t0.plusSeconds(30)),
        ).isTrue()
        assertThat(
            deduper.shouldSkip("com.bank", "paid 10", "key-3", t0.plus(Duration.ofMinutes(3))),
        ).isFalse()
    }

    @Test
    fun skipsRepeatOfSameNotificationKey() {
        assertThat(deduper.shouldSkip("com.bank", "a", "same-key", t0)).isFalse()
        assertThat(deduper.shouldSkip("com.bank", "different text", "same-key", t0.plusSeconds(1)))
            .isTrue()
    }

    @Test
    fun differentPackagesAreIndependent() {
        assertThat(deduper.shouldSkip("com.a", "paid 10", "k1", t0)).isFalse()
        assertThat(deduper.shouldSkip("com.b", "paid 10", "k2", t0.plusSeconds(1))).isFalse()
    }
}
