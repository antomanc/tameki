package dev.antomanc.tameki.core.model.recurring

import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import java.time.LocalDate

/** Running plans renew and remind; paused and cancelled ones do neither. */
val Recurring.isRunning: Boolean
    get() = status == RecurringStatus.ACTIVE || status == RecurringStatus.TRIAL

/**
 * The day of month the schedule is anchored on. The stored next date is the source of truth (the
 * user can move it independently of the start date); the start day only wins when the next date
 * is that day clamped into a short month, so a plan due on the 31st snaps back after February.
 */
fun RecurrenceCalculator.anchorDay(recurring: Recurring): Int {
    val startDay = recurring.startDate.dayOfMonth
    val clamped = minOf(startDay, recurring.nextDate.lengthOfMonth())
    return if (recurring.nextDate.dayOfMonth == clamped) startDay else recurring.nextDate.dayOfMonth
}

/**
 * The next renewal as of [today]. A stored [Recurring.nextDate] only moves forward when the daily
 * worker runs (or a charge is auto-logged), so a plan nobody logs could otherwise show a date in
 * the past forever. A renewal due today is still today's.
 */
fun RecurrenceCalculator.currentNext(recurring: Recurring, today: LocalDate): LocalDate {
    var cursor = recurring.nextDate
    val anchor = anchorDay(recurring)
    var guard = 0
    while (cursor.isBefore(today) && guard < 100_000) {
        cursor = next(cursor, recurring.cycle, anchor)
        guard++
    }
    return cursor
}

/** Every renewal of [recurring] in [from]..[to], walking forward from its current next date. */
fun RecurrenceCalculator.upcoming(recurring: Recurring, from: LocalDate, to: LocalDate): List<LocalDate> {
    val out = mutableListOf<LocalDate>()
    var cursor = currentNext(recurring, from)
    val anchor = anchorDay(recurring)
    while (!cursor.isAfter(to) && out.size < 1_000) {
        out += cursor
        cursor = next(cursor, recurring.cycle, anchor)
    }
    return out
}

/** The next [count] renewals of [recurring] from [today]. */
fun RecurrenceCalculator.nextRenewals(recurring: Recurring, today: LocalDate, count: Int): List<LocalDate> {
    val out = mutableListOf<LocalDate>()
    var cursor = currentNext(recurring, today)
    val anchor = anchorDay(recurring)
    repeat(count) {
        out += cursor
        cursor = next(cursor, recurring.cycle, anchor)
    }
    return out
}

/** The reminders a plan should have right now; null fields mean "none". */
data class ReminderPlan(
    /** Renewal day and how many days before it to remind, or null for no renewal reminder. */
    val renewal: Pair<LocalDate, Int>?,
    /** The trial's last day, or null for no trial reminder. */
    val trialEnding: LocalDate?,
)

/**
 * The status transitions of a recurring payment, with their effect on reminders. Pure: the view
 * models apply the result and schedule exactly what [reminders] says, so detail, review and edit
 * can never disagree about what pausing or resuming means.
 *
 * Transitions:
 * - ACTIVE/TRIAL → PAUSED ([pause]) → back to ACTIVE, or TRIAL while the trial runs ([resume]);
 * - ACTIVE/TRIAL/PAUSED → CANCELLED ([cancel]) → ACTIVE/TRIAL ([reactivate]);
 * - a new or active plan whose trial has not ended is TRIAL; a trial that ended is ACTIVE ([settle]).
 */
object RecurringLifecycle {
    fun pause(r: Recurring): Recurring = if (r.isRunning) r.copy(status = RecurringStatus.PAUSED) else r

    fun resume(r: Recurring, today: LocalDate): Recurring =
        if (r.status == RecurringStatus.PAUSED) settle(r.copy(status = RecurringStatus.ACTIVE), today) else r

    fun cancel(r: Recurring): Recurring = r.copy(status = RecurringStatus.CANCELLED)

    fun reactivate(r: Recurring, today: LocalDate): Recurring =
        if (r.status == RecurringStatus.CANCELLED) settle(r.copy(status = RecurringStatus.ACTIVE), today) else r

    /**
     * Brings a running plan up to [today]: its next date never lies in the past, and its status
     * says TRIAL exactly while a trial end date is still ahead (income has no trial).
     */
    fun settle(r: Recurring, today: LocalDate): Recurring {
        if (!r.isRunning) return r
        val next = RecurrenceCalculator.currentNext(r, today)
        val trialRunning = r.type != RecurringType.INCOME && r.trialEndsOn?.let { !it.isBefore(today) } == true
        val status = if (trialRunning) RecurringStatus.TRIAL else RecurringStatus.ACTIVE
        return r.copy(nextDate = next, status = status)
    }

    /** What reminders [r] should have as of [today]. Only a running plan reminds. */
    fun reminders(r: Recurring, today: LocalDate): ReminderPlan {
        if (!r.isRunning) return ReminderPlan(renewal = null, trialEnding = null)
        val renewal = if (r.remindDaysBefore >= 0) {
            RecurrenceCalculator.currentNext(r, today) to r.remindDaysBefore
        } else {
            null
        }
        val trial = r.trialEndsOn?.takeIf { r.status == RecurringStatus.TRIAL && !it.isBefore(today) }
        return ReminderPlan(renewal = renewal, trialEnding = trial)
    }
}
