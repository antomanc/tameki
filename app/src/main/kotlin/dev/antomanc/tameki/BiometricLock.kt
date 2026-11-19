package dev.antomanc.tameki

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * The app lock is a process-wide fact, not a composable's remembered boolean: rotation and a
 * configuration change must not unlock it, while a process death starts locked again.
 *
 * [markUnlocked] lasts for the process until [shouldLock] sees more than [LOCK_AFTER_SECONDS] in
 * the background. A first launch after process death has never been unlocked, so the caller locks
 * before drawing anything when the setting is on.
 */
object BiometricLock {
    private val backgroundedAt = AtomicReference<Instant?>(null)
    private val locked = AtomicBoolean(false)
    private val unlockedForProcess = AtomicBoolean(false)
    private const val LOCK_AFTER_SECONDS = 60L

    fun onPause() {
        backgroundedAt.set(Instant.now())
    }

    /** True while the UI must stay covered, including when a prompt was cancelled. */
    fun isLocked(): Boolean = locked.get()

    fun isUnlockedForProcess(): Boolean = unlockedForProcess.get()

    fun markLocked() {
        locked.set(true)
        unlockedForProcess.set(false)
    }

    fun markUnlocked() {
        locked.set(false)
        unlockedForProcess.set(true)
        // The grace period starts from the next pause, not from the one that already locked us.
        backgroundedAt.set(null)
    }

    fun shouldLock(now: Instant = Instant.now()): Boolean {
        val at = backgroundedAt.getAndSet(null)
        if (locked.get()) return true
        if (at == null) return false
        if (!now.isAfter(at.plusSeconds(LOCK_AFTER_SECONDS))) return false
        markLocked()
        return true
    }

    /** True only when this phone can actually ask for a biometric or the screen-lock PIN. */
    fun canAuthenticate(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        ) == BiometricManager.BIOMETRIC_SUCCESS

    fun prompt(activity: FragmentActivity, onUnlocked: () -> Unit, onFailed: () -> Unit) {
        if (!canAuthenticate(activity)) {
            // No enrolled biometric or device credential: the lock cannot protect anything. Keep
            // the journal covered and tell the lock screen instead of pretending it unlocked.
            markLocked()
            onFailed()
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    markUnlocked()
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Cancelled or failed: stay covered, and let the locked screen offer a retry.
                    markLocked()
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
