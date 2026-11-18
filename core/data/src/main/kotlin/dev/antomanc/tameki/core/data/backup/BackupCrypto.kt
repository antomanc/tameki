package dev.antomanc.tameki.core.data.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM with PBKDF2-HMAC-SHA256 (310k iterations).
 * Payload layout: magic(6) + version(1) + salt(16) + iv(12) + ciphertext+tag.
 */
object BackupCrypto {
    private const val MAGIC = "TAMEKI"
    private const val VERSION: Byte = 1
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val KEY_LEN_BITS = 256
    private const val ITERATIONS = 310_000
    private const val GCM_TAG_BITS = 128

    class WrongPasswordException : Exception("Wrong backup password")
    class InvalidBackupException(message: String) : Exception(message)

    fun encrypt(plaintext: ByteArray, password: CharArray): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LEN).also(random::nextBytes)
        val iv = ByteArray(IV_LEN).also(random::nextBytes)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        val header = MAGIC.toByteArray(Charsets.US_ASCII) + byteArrayOf(VERSION) + salt + iv
        return header + ciphertext
    }

    fun decrypt(payload: ByteArray, password: CharArray): ByteArray {
        // Validate magic/header/length before any decrypt so truncated files never look like a wrong password.
        if (payload.size < MAGIC.length + 1 + SALT_LEN + IV_LEN + 16) {
            throw InvalidBackupException("corrupted file")
        }
        val magic = payload.copyOfRange(0, MAGIC.length).toString(Charsets.US_ASCII)
        if (magic != MAGIC) throw InvalidBackupException("corrupted file")
        val version = payload[MAGIC.length]
        if (version != VERSION) throw InvalidBackupException("corrupted file")
        var offset = MAGIC.length + 1
        val salt = payload.copyOfRange(offset, offset + SALT_LEN)
        offset += SALT_LEN
        val iv = payload.copyOfRange(offset, offset + IV_LEN)
        offset += IV_LEN
        val ciphertext = payload.copyOfRange(offset, payload.size)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return try {
            cipher.doFinal(ciphertext)
        } catch (_: Exception) {
            throw WrongPasswordException()
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LEN_BITS)
        val encoded = factory.generateSecret(spec).encoded
        return SecretKeySpec(encoded, "AES")
    }

    /** Expose version byte for tests. */
    fun headerVersion(payload: ByteArray): Int {
        if (payload.size <= MAGIC.length) return -1
        return payload[MAGIC.length].toInt() and 0xff
    }
}
