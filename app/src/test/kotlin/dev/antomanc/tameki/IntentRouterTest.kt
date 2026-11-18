package dev.antomanc.tameki

import android.content.Intent
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.automation.AutomationIntents
import dev.antomanc.tameki.automation.IntentAction
import dev.antomanc.tameki.automation.IntentRouter
import dev.antomanc.tameki.core.model.TamekiIntents
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntentRouterTest {
    @Before
    fun setUp() {
        runCatching { stopKoin() }
    }

    @After
    fun tearDown() {
        runCatching { stopKoin() }
    }

    @Test
    fun deepLinkAdd() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tameki://add?amount=12.50"))
        assertThat(IntentRouter.route(intent)).isEqualTo(IntentAction.AddTransaction)
    }

    @Test
    fun shortcutQuickAdd() {
        val intent = Intent().putExtra(AutomationIntents.EXTRA_OPEN, AutomationIntents.OPEN_QUICK_ADD)
        assertThat(IntentRouter.route(intent)).isEqualTo(IntentAction.OpenQuickAdd)
    }

    @Test
    fun inboxNotification() {
        val intent = Intent().putExtra(TamekiIntents.EXTRA_OPEN, TamekiIntents.OPEN_INBOX)
        assertThat(IntentRouter.route(intent)).isEqualTo(IntentAction.OpenInbox)
    }

    @Test
    fun shareText() {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "hello")
        assertThat(IntentRouter.route(intent)).isEqualTo(IntentAction.ShareText("hello"))
    }

    @Test
    fun addTransactionAction() {
        val intent = Intent(AutomationIntents.ACTION_ADD_TRANSACTION)
            .putExtra(AutomationIntents.EXTRA_AMOUNT, "10")
        assertThat(IntentRouter.route(intent)).isEqualTo(IntentAction.AddTransaction)
    }
}
