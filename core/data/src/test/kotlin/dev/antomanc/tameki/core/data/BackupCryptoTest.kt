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

    @Test
    fun badMagicIsCorruptedFileNotWrongPassword() {
        try {
            BackupCrypto.decrypt("NOTAMEKI".toByteArray() + ByteArray(40), "x".toCharArray())
            throw AssertionError("expected InvalidBackupException")
        } catch (e: BackupCrypto.InvalidBackupException) {
            assertThat(e.message).isEqualTo("corrupted file")
        }
    }

    @Test
    fun streamingEncryptionDecryptsWithTheSameFormat() {
        val plain = ByteArray(300_000) { (it % 251).toByte() }
        val out = java.io.ByteArrayOutputStream()
        BackupCrypto.encrypt(plain.inputStream(), out, "secret".toCharArray())
        val decrypted = BackupCrypto.decrypt(out.toByteArray(), "secret".toCharArray())
        com.google.common.truth.Truth.assertThat(decrypted).isEqualTo(plain)
    }
}
