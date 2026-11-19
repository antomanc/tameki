package dev.antomanc.tameki

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dev.antomanc.tameki.core.model.lock.AppLock
import dev.antomanc.tameki.core.model.lock.AppLockEvent
import dev.antomanc.tameki.core.model.lock.AppLockState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The process-wide holder of [AppLockState]: rotation and a configuration change must not unlock,
 * while a process death starts locked again. Decisions live in [AppLock]; this object only stores
 * the state and talks to the platform prompt.
 */
object BiometricLock {
    private val _state = MutableStateFlow(AppLockState())
    val state: StateFlow<AppLockState> = _state.asStateFlow()

    fun dispatch(event: AppLockEvent) {
        _state.update { AppLock.reduce(it, event) }
    }

    private const val AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /** True only when this phone can actually ask for a biometric or the screen-lock PIN. */
    fun canAuthenticate(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    /** Shows the system prompt when the state asks for one. Safe to call repeatedly. */
    fun promptIfNeeded(activity: FragmentActivity) {
        if (!state.value.shouldPrompt) return
        if (!canAuthenticate(activity)) {
            // No enrolled biometric or device credential: the lock cannot protect anything. Keep
            // the journal covered and let the lock screen say so instead of pretending.
            dispatch(AppLockEvent.AuthFailed)
            return
        }
        dispatch(AppLockEvent.PromptShown)
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    dispatch(AppLockEvent.Authenticated)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    dispatch(AppLockEvent.AuthFailed)
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.app_unlock_title))
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build(),
        )
    }
}
