package dev.antomanc.tameki.core.data

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.data.backup.BackupCrypto
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BackupCryptoTest {
    @Test
    fun encryptDecryptRoundTrip() {
        val plain = "hello tameki".toByteArray()
        val password = "secret".toCharArray()
        val encrypted = BackupCrypto.encrypt(plain, password)
        assertThat(BackupCrypto.headerVersion(encrypted)).isEqualTo(1)
        assertThat(BackupCrypto.decrypt(encrypted, password).toString(Charsets.UTF_8))
            .isEqualTo("hello tameki")
    }

    @Test(expected = BackupCrypto.WrongPasswordException::class)
    fun wrongPasswordThrows() {
        val encrypted = BackupCrypto.encrypt("x".toByteArray(), "good".toCharArray())
        BackupCrypto.decrypt(encrypted, "bad".toCharArray())
    }
}
