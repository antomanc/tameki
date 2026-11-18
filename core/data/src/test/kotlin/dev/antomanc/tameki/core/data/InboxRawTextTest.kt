package dev.antomanc.tameki.core.data

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.entity.InboxItem
import dev.antomanc.tameki.core.model.entity.InboxStatus
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.core.model.entity.TransactionSource
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** The source notification text is kept only while a draft waits for confirmation. */
class InboxRawTextTest : DbTestRule() {
    private suspend fun pending(): Long = inbox.upsert(
        InboxItem(
            draft = TransactionDraft(merchant = "Esselunga"),
            origin = TransactionSource.NOTIFICATION,
            rawText = "Pagamento di 12,50 EUR presso Esselunga",
            receivedAt = Instant.parse("2026-11-18T09:00:00Z"),
        ),
    )

    @Test
    fun `pending items keep the notification text`() = runTest {
        val id = pending()
        assertThat(inbox.get(id)!!.rawText).isNotEmpty()
    }

    @Test
    fun `accepting erases the notification text`() = runTest {
        val id = pending()
        inbox.setStatus(id, InboxStatus.ACCEPTED)
        assertThat(inbox.get(id)!!.rawText).isEmpty()
    }

    @Test
    fun `dismissing erases the notification text`() = runTest {
        val id = pending()
        inbox.setStatus(id, InboxStatus.DISMISSED)
        assertThat(inbox.get(id)!!.rawText).isEmpty()
        assertThat(inbox.get(id)!!.draft.merchant).isEqualTo("Esselunga")
    }
}
