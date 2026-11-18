package dev.antomanc.tameki.core.model.recurring

import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Decides when a subscription goes back into the review queue. Three months is long enough for a
 * habit to change and short enough to catch a plan the user stopped using before the next renewal
 * makes it obvious.
 */
object SubscriptionReview {
    /** A rating older than this many days is stale. */
    const val STALE_AFTER_DAYS = 90L

    /**
     * True when the user should rate the subscription again: an active subscription or trial with
     * no rating, or with a rating given more than [STALE_AFTER_DAYS] days ago. Paused, cancelled,
     * bills and income are never asked about.
     *
     * A rating stored without a date (data from before the date was kept) is left alone: nothing
     * proves it is stale, and the app does not nag about ratings it cannot judge.
     */
    fun isDue(recurring: Recurring, today: LocalDate): Boolean {
        if (recurring.type != RecurringType.SUBSCRIPTION) return false
        if (recurring.status != RecurringStatus.ACTIVE && recurring.status != RecurringStatus.TRIAL) {
            return false
        }
        if (recurring.worth == null) return true
        val ratedOn = recurring.worthRatedOn ?: return false
        return ChronoUnit.DAYS.between(ratedOn, today) > STALE_AFTER_DAYS
    }
}
