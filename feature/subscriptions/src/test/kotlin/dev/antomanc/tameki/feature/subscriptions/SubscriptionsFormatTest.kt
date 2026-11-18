package dev.antomanc.tameki.feature.subscriptions

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SubscriptionsFormatTest {
    @Test
    fun `zero counts are omitted from the summary`() {
        assertThat(joinCountLabels(listOf("4 subscriptions", "2 bills")))
            .isEqualTo("4 subscriptions · 2 bills")
        assertThat(joinCountLabels(listOf("4 abbonamenti", "2 bollette")))
            .isEqualTo("4 abbonamenti · 2 bollette")
        assertThat(joinCountLabels(listOf("1 subscription")))
            .isEqualTo("1 subscription")
        assertThat(joinCountLabels(listOf("2 bills", "1 income")))
            .isEqualTo("2 bills · 1 income")
        assertThat(joinCountLabels(emptyList())).isNull()
        assertThat(joinCountLabels(listOf("", "  "))).isNull()
    }
}
