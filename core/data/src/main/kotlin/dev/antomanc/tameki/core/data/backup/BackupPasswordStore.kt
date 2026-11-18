package dev.antomanc.tameki.core.data.backup

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dev.antomanc.tameki.core.model.repository.BackupPasswordStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** In-memory store for unit tests. */
class InMemoryBackupPasswordStore : BackupPasswordStore {
    private var password: CharArray? = null
    private val mutex = Mutex()

    override suspend fun save(password: CharArray) = mutex.withLock {
        this.password = password.copyOf()
    }

    override suspend fun load(): CharArray? = mutex.withLock {
        password?.copyOf()
    }

    override suspend fun clear() = mutex.withLock {
        password?.fill('\u0000')
        password = null
    }
}

/**
 * Encrypts the auto-backup password with an AES-GCM key in the Android Keystore.
 * Ciphertext (IV + payload) is kept in private SharedPreferences.
 */
class KeystoreBackupPasswordStore(
    context: Context,
) : BackupPasswordStore {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override suspend fun save(password: CharArray) = withContext(Dispatchers.IO) {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(password.concatToString().toByteArray(Charsets.UTF_8))
        val packed = Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
        prefs.edit().putString(KEY_CIPHERTEXT, packed).apply()
        password.fill('\u0000')
    }

    override suspend fun load(): CharArray? = withContext(Dispatchers.IO) {
        val packed = prefs.getString(KEY_CIPHERTEXT, null) ?: return@withContext null
        val bytes = Base64.decode(packed, Base64.NO_WRAP)
        if (bytes.size <= IV_LEN) return@withContext null
        val iv = bytes.copyOfRange(0, IV_LEN)
        val ciphertext = bytes.copyOfRange(IV_LEN, bytes.size)
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.doFinal(ciphertext).toString(Charsets.UTF_8).toCharArray()
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().remove(KEY_CIPHERTEXT).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "tameki_auto_backup_password"
        private const val PREFS = "tameki_backup_password"
        private const val KEY_CIPHERTEXT = "ciphertext"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LEN = 12
        private const val GCM_TAG_BITS = 128
    }
}
