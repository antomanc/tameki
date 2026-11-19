package dev.antomanc.tameki.feature.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackupPasswordRulesTest {
    @Test
    fun `export needs a non-empty password confirmed twice`() {
        assertThat(backupPasswordReady("", "")).isFalse()
        assertThat(backupPasswordReady("secret", "")).isFalse()
        assertThat(backupPasswordReady("secret", "secre")).isFalse()
        assertThat(backupPasswordReady("secret", "secret")).isTrue()
    }
}
