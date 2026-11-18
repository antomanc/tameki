package dev.antomanc.tameki

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

/**
 * After [LOCK_AFTER] of background time, require biometric/device credential on resume when enabled.
 */
object BiometricLock {
    private val backgroundedAt = AtomicReference<Instant?>(null)
    private const val LOCK_AFTER_SECONDS = 60L

    fun onPause() {
        backgroundedAt.set(Instant.now())
    }

    fun shouldLock(now: Instant = Instant.now()): Boolean {
        val at = backgroundedAt.getAndSet(null) ?: return false
        return now.isAfter(at.plusSeconds(LOCK_AFTER_SECONDS))
    }

    fun prompt(activity: FragmentActivity, onUnlocked: () -> Unit, onFailed: () -> Unit) {
        val can = BiometricManager.from(activity)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            onUnlocked()
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailed()
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.app_unlock_title))
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
                .build(),
        )
    }
}
