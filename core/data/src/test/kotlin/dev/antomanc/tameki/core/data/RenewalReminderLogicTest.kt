package dev.antomanc.tameki.core.data

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.data.reminder.ReminderKind
import dev.antomanc.tameki.core.data.reminder.RenewalReminderLogic
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import java.time.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RenewalReminderLogicTest {
    private val eur = CurrencyCode("EUR")
    private val today = LocalDate.of(2026, 11, 18)

    private fun sample(
        id: Long = 1L,
        next: LocalDate = LocalDate.of(2026, 11, 20),
        remind: Int = 2,
        autoLog: Boolean = false,
        status: RecurringStatus = RecurringStatus.ACTIVE,
        trialEndsOn: LocalDate? = null,
    ) = Recurring(
        id = id,
        type = RecurringType.SUBSCRIPTION,
        name = "Netflix",
        amount = Money(1_399L, eur),
        cycle = Cycle(1, CycleUnit.MONTH),
        startDate = LocalDate.of(2026, 1, 20),
        nextDate = next,
        trialEndsOn = trialEndsOn,
        status = status,
        accountId = 1L,
        remindDaysBefore = remind,
        autoLog = autoLog,
    )

    @Test
    fun renewalReminderWithinWindow() {
        val due = RenewalReminderLogic.remindersDue(listOf(sample()), today)
        assertThat(due).hasSize(1)
        assertThat(due.single().kind).isEqualTo(ReminderKind.RENEWAL)
    }

    @Test
    fun reminderOffWhenNegativeDays() {
        val due = RenewalReminderLogic.remindersDue(listOf(sample(remind = -1)), today)
        assertThat(due).isEmpty()
    }

    @Test
    fun trialEndingNotification() {
        val item = sample(
            status = RecurringStatus.TRIAL,
            trialEndsOn = LocalDate.of(2026, 11, 20),
            remind = -1,
        )
        val due = RenewalReminderLogic.remindersDue(listOf(item), today)
        assertThat(due.map { it.kind }).containsExactly(ReminderKind.TRIAL_ENDING)
    }

    @Test
    fun autoLogWhenNextDateReached() {
        val item = sample(next = today, autoLog = true)
        val actions = RenewalReminderLogic.autoLogsDue(listOf(item), today)
        assertThat(actions).hasSize(1)
        assertThat(actions.single().chargeDate).isEqualTo(today)
    }

    @Test
    fun autoLogSkippedWhenPaused() {
        val item = sample(next = today, autoLog = true, status = RecurringStatus.PAUSED)
        assertThat(RenewalReminderLogic.autoLogsDue(listOf(item), today)).isEmpty()
    }
}
