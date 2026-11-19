package dev.antomanc.tameki.core.model.lock

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.lock.AppLockState.Phase
import java.time.Instant
import org.junit.Test

class AppLockTest {
    private val t0 = Instant.parse("2026-01-01T10:00:00Z")

    private fun run(vararg events: AppLockEvent, from: AppLockState = AppLockState()) =
        events.fold(from) { s, e -> AppLock.reduce(s, e) }

    @Test fun startsLoadingAndCoversNothingUntilTheSettingIsRead() {
        val s = AppLockState()
        assertThat(s.loading).isTrue()
        assertThat(s.cover).isFalse()
        assertThat(s.shouldPrompt).isFalse()
    }

    @Test fun disabledLockOpensImmediately() {
        val s = run(AppLockEvent.SettingLoaded(false))
        assertThat(s.phase).isEqualTo(Phase.Unlocked)
        assertThat(s.cover).isFalse()
    }

    @Test fun coldStartWithLockOnCoversAndAsksForAPrompt() {
        val s = run(AppLockEvent.SettingLoaded(true))
        assertThat(s.cover).isTrue()
        assertThat(s.shouldPrompt).isTrue()
        val prompting = AppLock.reduce(s, AppLockEvent.PromptShown)
        assertThat(prompting.phase).isEqualTo(Phase.Prompting)
        assertThat(prompting.shouldPrompt).isFalse()
        assertThat(prompting.cover).isTrue()
    }

    @Test fun authenticationUnlocksAndFailureStaysCoveredWithoutReprompting() {
        val prompting = run(AppLockEvent.SettingLoaded(true), AppLockEvent.PromptShown)
        assertThat(AppLock.reduce(prompting, AppLockEvent.Authenticated).cover).isFalse()
        val failed = AppLock.reduce(prompting, AppLockEvent.AuthFailed)
        assertThat(failed.phase).isEqualTo(Phase.Covered)
        assertThat(failed.cover).isTrue()
        assertThat(failed.shouldPrompt).isFalse()
        // Resuming after a cancel does not loop back into the prompt; only a tap on Unlock does.
        assertThat(AppLock.reduce(failed, AppLockEvent.Resumed(t0)).shouldPrompt).isFalse()
        assertThat(AppLock.reduce(failed, AppLockEvent.UnlockRequested).shouldPrompt).isTrue()
    }

    @Test fun shortBackgroundKeepsTheAppOpen() {
        val s = run(
            AppLockEvent.SettingLoaded(true), AppLockEvent.PromptShown, AppLockEvent.Authenticated,
            AppLockEvent.Paused(t0), AppLockEvent.Resumed(t0.plusSeconds(30)),
        )
        assertThat(s.phase).isEqualTo(Phase.Unlocked)
        assertThat(s.backgroundedAt).isNull()
    }

    @Test fun backgroundPastTheGracePeriodLocks() {
        val s = run(
            AppLockEvent.SettingLoaded(true), AppLockEvent.PromptShown, AppLockEvent.Authenticated,
            AppLockEvent.Paused(t0), AppLockEvent.Resumed(t0.plusSeconds(61)),
        )
        assertThat(s.phase).isEqualTo(Phase.NeedsPrompt)
        assertThat(s.shouldPrompt).isTrue()
    }

    @Test fun pauseDuringThePromptDoesNotStartTheGraceClock() {
        val s = run(AppLockEvent.SettingLoaded(true), AppLockEvent.PromptShown, AppLockEvent.Paused(t0))
        assertThat(s.backgroundedAt).isNull()
    }

    @Test fun turningTheLockOnWhileInsideDoesNotLockAtOnce() {
        val s = run(AppLockEvent.SettingLoaded(false), AppLockEvent.SettingLoaded(true))
        assertThat(s.cover).isFalse()
    }

    @Test fun turningTheLockOffWhileCoveredOpens() {
        val s = run(AppLockEvent.SettingLoaded(true), AppLockEvent.SettingLoaded(false))
        assertThat(s.cover).isFalse()
    }
}
