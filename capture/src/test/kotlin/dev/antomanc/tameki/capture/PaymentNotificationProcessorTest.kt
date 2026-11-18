package dev.antomanc.tameki.capture

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.capture.KnownBankingPackages
import dev.antomanc.tameki.core.model.entity.InboxItem
import dev.antomanc.tameki.core.model.entity.InboxStatus
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.repository.InboxRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PaymentNotificationProcessorTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC)
    private val wallet = KnownBankingPackages.packageNames.first { it.contains("wallet") }

    @Test
    fun createsPendingInboxItemFromParsableNotification() = runTest {
        val repo = FakeInboxRepository()
        val processor = PaymentNotificationProcessor(inbox = repo, clock = clock)
        val id = processor.handle(
            PaymentNotificationProcessor.Incoming(
                packageName = wallet,
                title = "Google Wallet",
                text = "€4.50 at Esselunga",
                notificationKey = "n1",
                postedAt = Instant.parse("2026-11-18T11:59:00Z"),
                captureEnabled = true,
                allowlist = emptySet(), // defaults to KnownBankingPackages
            ),
        )
        assertThat(id).isNotNull()
        val item = repo.items.single()
        assertThat(item.status).isEqualTo(InboxStatus.PENDING)
        assertThat(item.origin).isEqualTo(TransactionSource.NOTIFICATION)
        assertThat(item.draft.amount).isNotNull()
        assertThat(item.draft.merchant).contains("Esselunga")
    }

    @Test
    fun ignoresWhenCaptureDisabledOrPackageNotAllowlisted() = runTest {
        val repo = FakeInboxRepository()
        val processor = PaymentNotificationProcessor(inbox = repo, clock = clock)
        assertThat(
            processor.handle(
                baseIncoming(captureEnabled = false),
            ),
        ).isNull()
        assertThat(
            processor.handle(
                baseIncoming(allowlist = setOf("com.other.app")),
            ),
        ).isNull()
        assertThat(repo.items).isEmpty()
    }

    @Test
    fun dedupesSecondIdenticalPost() = runTest {
        val repo = FakeInboxRepository()
        val processor = PaymentNotificationProcessor(inbox = repo, clock = clock)
        val first = processor.handle(baseIncoming(key = "k1"))
        val second = processor.handle(baseIncoming(key = "k2"))
        assertThat(first).isNotNull()
        assertThat(second).isNull()
        assertThat(repo.items).hasSize(1)
    }

    private fun baseIncoming(
        captureEnabled: Boolean = true,
        allowlist: Set<String> = emptySet(),
        key: String = "n1",
    ) = PaymentNotificationProcessor.Incoming(
        packageName = wallet,
        title = "Wallet",
        text = "€4.50 at Esselunga",
        notificationKey = key,
        postedAt = Instant.parse("2026-11-18T11:59:00Z"),
        captureEnabled = captureEnabled,
        allowlist = allowlist,
    )

    private class FakeInboxRepository : InboxRepository {
        val items = mutableListOf<InboxItem>()
        private var nextId = 1L
        private val flow = MutableStateFlow<List<InboxItem>>(emptyList())

        override fun observe(status: InboxStatus?): Flow<List<InboxItem>> =
            flow.map { list -> status?.let { s -> list.filter { it.status == s } } ?: list }

        override suspend fun get(id: Long): InboxItem? = items.firstOrNull { it.id == id }

        override suspend fun upsert(item: InboxItem): Long {
            val id = if (item.id == 0L) nextId++ else item.id
            val stored = item.copy(id = id)
            items.removeAll { it.id == id }
            items += stored
            flow.value = items.toList()
            return id
        }

        override suspend fun setStatus(id: Long, status: InboxStatus) {
            val idx = items.indexOfFirst { it.id == id }
            if (idx >= 0) {
                items[idx] = items[idx].copy(status = status)
                flow.value = items.toList()
            }
        }
    }
}
