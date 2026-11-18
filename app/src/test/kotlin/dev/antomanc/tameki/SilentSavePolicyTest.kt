package dev.antomanc.tameki

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.automation.SilentSavePolicy
import org.junit.Test

class SilentSavePolicyTest {
    @Test
    fun requiresBothSettingAndFlag() {
        assertThat(SilentSavePolicy.shouldSilentSave(false, true)).isFalse()
        assertThat(SilentSavePolicy.shouldSilentSave(true, false)).isFalse()
        assertThat(SilentSavePolicy.shouldSilentSave(true, true)).isTrue()
        assertThat(SilentSavePolicy.shouldSilentSave(false, false)).isFalse()
    }
}
