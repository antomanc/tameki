package dev.antomanc.tameki.core.model.recurring

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import java.time.LocalDate
import org.junit.Test

class RecurringLifecycleTest {
    private val today = LocalDate.of(2026, 11, 19)
    private val plan = Recurring(
        id = 1,
        type = RecurringType.SUBSCRIPTION,
        name = "Music",
        amount = Money(999, CurrencyCode("EUR")),
        cycle = Cycle(1, CycleUnit.MONTH),
        startDate = LocalDate.of(2026, 1, 5),
        nextDate = LocalDate.of(2026, 9, 5),
        remindDaysBefore = 3,
    )

    @Test fun currentNextRollsAStaleDateToTheFirstRenewalFromToday() {
        assertThat(RecurrenceCalculator.currentNext(plan, today)).isEqualTo(LocalDate.of(2026, 12, 5))
    }

    @Test fun currentNextKeepsARenewalDueToday() {
        val dueToday = plan.copy(startDate = LocalDate.of(2026, 1, 19), nextDate = LocalDate.of(2026, 10, 19))
        assertThat(RecurrenceCalculator.currentNext(dueToday, today)).isEqualTo(today)
    }

    @Test fun currentNextLeavesAFutureDateAlone() {
        val future = plan.copy(nextDate = LocalDate.of(2026, 12, 5))
        assertThat(RecurrenceCalculator.currentNext(future, today)).isEqualTo(LocalDate.of(2026, 12, 5))
    }

    @Test fun pauseAndResumeRoundTripAndResumeKeepsARunningTrial() {
        val trial = plan.copy(status = RecurringStatus.TRIAL, trialEndsOn = today.plusDays(5))
        val paused = RecurringLifecycle.pause(trial)
        assertThat(paused.status).isEqualTo(RecurringStatus.PAUSED)
        assertThat(RecurringLifecycle.resume(paused, today).status).isEqualTo(RecurringStatus.TRIAL)
        assertThat(RecurringLifecycle.resume(paused.copy(trialEndsOn = null), today).status)
            .isEqualTo(RecurringStatus.ACTIVE)
    }

    @Test fun pausedAndCancelledPlansHaveNoReminders() {
        val paused = RecurringLifecycle.pause(plan)
        assertThat(RecurringLifecycle.reminders(paused, today)).isEqualTo(ReminderPlan(null, null))
        val cancelled = RecurringLifecycle.cancel(plan)
        assertThat(RecurringLifecycle.reminders(cancelled, today)).isEqualTo(ReminderPlan(null, null))
    }

    @Test fun remindersUseThePlansLeadTimeAndTheCurrentNextDate() {
        val plan = RecurringLifecycle.reminders(plan, today)
        assertThat(plan.renewal).isEqualTo(LocalDate.of(2026, 12, 5) to 3)
        assertThat(plan.trialEnding).isNull()
    }

    @Test fun settleMarksARunningTrialAndEndsAnExpiredOne() {
        val newTrial = RecurringLifecycle.settle(plan.copy(trialEndsOn = today.plusDays(10)), today)
        assertThat(newTrial.status).isEqualTo(RecurringStatus.TRIAL)
        val ended = RecurringLifecycle.settle(newTrial.copy(trialEndsOn = today.minusDays(1)), today)
        assertThat(ended.status).isEqualTo(RecurringStatus.ACTIVE)
    }

    @Test fun reactivateBringsACancelledPlanBackRunning() {
        val back = RecurringLifecycle.reactivate(RecurringLifecycle.cancel(plan), today)
        assertThat(back.isRunning).isTrue()
        assertThat(back.nextDate).isEqualTo(LocalDate.of(2026, 12, 5))
    }

    @Test fun upcomingFollowsTheStoredNextDateEvenWhenItIsOffTheStartDay() {
        val moved = plan.copy(nextDate = LocalDate.of(2026, 11, 25))
        assertThat(RecurrenceCalculator.upcoming(moved, today, today.plusDays(40)))
            .containsExactly(LocalDate.of(2026, 11, 25), LocalDate.of(2026, 12, 25)).inOrder()
    }

    @Test fun aMonthEndPlanSnapsBackAfterAShortMonth() {
        val rent = plan.copy(startDate = LocalDate.of(2026, 1, 31), nextDate = LocalDate.of(2026, 2, 28))
        assertThat(RecurrenceCalculator.nextRenewals(rent, LocalDate.of(2026, 2, 1), 2))
            .containsExactly(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31)).inOrder()
    }
}
