package dev.antomanc.tameki.core.data.reminder

import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import java.time.LocalDate

data class ReminderNotification(
    val recurringId: Long,
    val name: String,
    val kind: ReminderKind,
    val on: LocalDate,
)

enum class ReminderKind {
    RENEWAL,
    TRIAL_ENDING,
}

data class AutoLogAction(
    val recurring: Recurring,
    val chargeDate: LocalDate,
)

/**
 * Pure decision logic for the daily renewal worker — unit-tested without WorkManager.
 */
object RenewalReminderLogic {
    fun remindersDue(recurring: List<Recurring>, today: LocalDate): List<ReminderNotification> {
        val out = mutableListOf<ReminderNotification>()
        for (item in recurring) {
            if (item.status == RecurringStatus.CANCELLED || item.status == RecurringStatus.PAUSED) continue
            if (item.remindDaysBefore >= 0) {
                val remindOn = item.nextDate.minusDays(item.remindDaysBefore.toLong())
                if (!remindOn.isAfter(today) && !item.nextDate.isBefore(today)) {
                    out += ReminderNotification(item.id, item.name, ReminderKind.RENEWAL, item.nextDate)
                }
            }
            val trial = item.trialEndsOn
            if (trial != null && item.status == RecurringStatus.TRIAL) {
                // Notify from 3 days before through the end date.
                val windowStart = trial.minusDays(3)
                if (!today.isBefore(windowStart) && !today.isAfter(trial)) {
                    out += ReminderNotification(item.id, item.name, ReminderKind.TRIAL_ENDING, trial)
                }
            }
        }
        return out
    }

    fun autoLogsDue(recurring: List<Recurring>, today: LocalDate): List<AutoLogAction> =
        recurring.filter { item ->
            item.autoLog &&
                item.status != RecurringStatus.CANCELLED &&
                item.status != RecurringStatus.PAUSED &&
                !item.nextDate.isAfter(today)
        }.map { AutoLogAction(it, it.nextDate) }
}
