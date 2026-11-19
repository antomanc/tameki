package dev.antomanc.tameki.core.model.lock

import java.time.Duration
import java.time.Instant

/**
 * The app lock as an explicit state machine. The activity feeds it lifecycle and prompt events and
 * renders [AppLockState.cover]; every decision (when to lock, when to prompt) lives here.
 *
 * Phases and transitions:
 * - [Phase.NeedsPrompt]: covered, and the system prompt should open (cold start, grace expired,
 *   the user tapped Unlock). → [Phase.Prompting] on [AppLockEvent.PromptShown].
 * - [Phase.Prompting]: the prompt is up over the cover. → [Phase.Unlocked] or [Phase.Covered].
 * - [Phase.Covered]: covered and waiting for the user (the prompt was cancelled or failed, or the
 *   phone cannot authenticate). Never re-prompts by itself, so a cancel cannot loop.
 *   → [Phase.NeedsPrompt] on [AppLockEvent.UnlockRequested].
 * - [Phase.Unlocked]: the journal is visible. → [Phase.NeedsPrompt] when resumed after more than
 *   [AppLock.GracePeriod] in the background.
 *
 * A process starts in [Phase.NeedsPrompt] with the setting unknown, so nothing draws until the
 * setting is read; when it turns out to be off the machine opens without a prompt.
 */
data class AppLockState(
    /** The lock setting, null until it has been read once. */
    val enabled: Boolean? = null,
    val phase: Phase = Phase.NeedsPrompt,
    /** When the app last went to the background while unlocked; cleared on resume. */
    val backgroundedAt: Instant? = null,
) {
    enum class Phase { NeedsPrompt, Prompting, Covered, Unlocked }

    /** Nothing may draw yet: the setting has not been read. */
    val loading: Boolean get() = enabled == null

    /** The journal must be hidden behind the lock screen. */
    val cover: Boolean get() = enabled == true && phase != Phase.Unlocked

    /** The activity should show the system prompt now, then send [AppLockEvent.PromptShown]. */
    val shouldPrompt: Boolean get() = enabled == true && phase == Phase.NeedsPrompt
}

sealed interface AppLockEvent {
    data class SettingLoaded(val enabled: Boolean) : AppLockEvent
    data class Paused(val at: Instant) : AppLockEvent
    data class Resumed(val at: Instant) : AppLockEvent
    data object PromptShown : AppLockEvent
    data object Authenticated : AppLockEvent
    /** Cancelled, failed, or the phone cannot authenticate at all. */
    data object AuthFailed : AppLockEvent
    /** The user tapped Unlock on the cover. */
    data object UnlockRequested : AppLockEvent
}

object AppLock {
    /** How long the app may sit in the background before it locks again. */
    val GracePeriod: Duration = Duration.ofSeconds(60)

    fun reduce(state: AppLockState, event: AppLockEvent): AppLockState = when (event) {
        is AppLockEvent.SettingLoaded -> when {
            // Turning the lock off opens the app at once.
            !event.enabled -> state.copy(enabled = false, phase = AppLockState.Phase.Unlocked, backgroundedAt = null)
            // First read in this process: keep the phase the process started in (NeedsPrompt).
            state.enabled == null -> state.copy(enabled = true)
            // Turned on while using the app: the user is already here, keep it open.
            state.enabled == false -> state.copy(enabled = true, phase = AppLockState.Phase.Unlocked)
            else -> state
        }
        is AppLockEvent.Paused ->
            // A prompt can pause the activity on some phones: only an unlocked app starts the grace clock.
            if (state.phase == AppLockState.Phase.Unlocked) state.copy(backgroundedAt = event.at) else state
        is AppLockEvent.Resumed -> {
            val since = state.backgroundedAt
            val expired = since != null && Duration.between(since, event.at) > GracePeriod
            if (state.enabled != false && state.phase == AppLockState.Phase.Unlocked && expired) {
                state.copy(phase = AppLockState.Phase.NeedsPrompt, backgroundedAt = null)
            } else {
                state.copy(backgroundedAt = null)
            }
        }
        AppLockEvent.PromptShown ->
            if (state.phase == AppLockState.Phase.NeedsPrompt) state.copy(phase = AppLockState.Phase.Prompting) else state
        AppLockEvent.Authenticated -> state.copy(phase = AppLockState.Phase.Unlocked, backgroundedAt = null)
        AppLockEvent.AuthFailed ->
            if (state.phase == AppLockState.Phase.Unlocked) state else state.copy(phase = AppLockState.Phase.Covered)
        AppLockEvent.UnlockRequested ->
            if (state.phase == AppLockState.Phase.Covered) state.copy(phase = AppLockState.Phase.NeedsPrompt) else state
    }
}
