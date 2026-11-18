package dev.antomanc.tameki

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.automation.AutomationIntents
import dev.antomanc.tameki.core.model.CurrencyCode
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class AutomationIntentsTest {
    private val eur = CurrencyCode("EUR")

    @Test
    fun parsesDeepLink() {
        val intent = Intent(Intent.ACTION_VIEW).setData(
            android.net.Uri.parse("tameki://add?amount=12.50&merchant=Esselunga&save=1"),
        )
        val result = AutomationIntents.parseAddIntent(intent, eur).getOrThrow()
        assertThat(result.save).isTrue()
        assertThat(result.draft.merchant).isEqualTo("Esselunga")
        assertThat(result.draft.amount?.minor).isEqualTo(1250)
    }

    @Test
    fun parsesExplicitAction() {
        val intent = Intent(AutomationIntents.ACTION_ADD_TRANSACTION)
            .putExtra(AutomationIntents.EXTRA_AMOUNT, "3,50")
            .putExtra(AutomationIntents.EXTRA_MERCHANT, "Bar")
            .putExtra(AutomationIntents.EXTRA_SAVE, "0")
        val result = AutomationIntents.parseAddIntent(intent, eur).getOrThrow()
        assertThat(result.save).isFalse()
        assertThat(result.draft.amount?.minor).isEqualTo(350)
    }

    @Test
    fun detectsImportHost() {
        assertThat(
            AutomationIntents.isImportDeepLink(android.net.Uri.parse("tameki://import")),
        ).isTrue()
    }
}
