package dev.antomanc.tameki.capture

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.capture.KnownBankingPackages
import org.junit.Test

class CaptureAllowlistTest {
    @Test
    fun emptyUserAllowlistUsesKnownDefaults() {
        assertThat(CaptureAllowlist.effective(emptySet()))
            .isEqualTo(KnownBankingPackages.packageNames)
    }

    @Test
    fun nonEmptyUserAllowlistWins() {
        assertThat(CaptureAllowlist.effective(setOf("com.example.bank")))
            .containsExactly("com.example.bank")
    }
}
