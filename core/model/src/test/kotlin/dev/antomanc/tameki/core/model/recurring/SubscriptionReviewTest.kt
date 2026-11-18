package dev.antomanc.tameki.core.model.recurring

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Worth
import java.time.LocalDate
import org.junit.Test

class SubscriptionReviewTest {
    private val today = LocalDate.of(2026, 11, 18)
    private val eur = CurrencyCode("EUR")

    private fun sub(
        worth: Worth? = null,
        ratedOn: LocalDate? = null,
        type: RecurringType = RecurringType.SUBSCRIPTION,
        status: RecurringStatus = RecurringStatus.ACTIVE,
    ) = Recurring(
        id = 1,
        type = type,
        name = "Netflix",
        amount = Money(1_399, eur),
        cycle = Cycle(1, CycleUnit.MONTH),
        startDate = LocalDate.of(2025, 1, 1),
        nextDate = today.plusDays(3),
        status = status,
        worth = worth,
        worthRatedOn = ratedOn,
    )

    @Test
    fun unratedSubscriptionIsDue() {
        assertThat(SubscriptionReview.isDue(sub(), today)).isTrue()
    }

    @Test
    fun freshRatingIsNotDue() {
        assertThat(SubscriptionReview.isDue(sub(Worth.LOVE, today.minusDays(30)), today)).isFalse()
    }

    @Test
    fun ratingFromTodayIsNotDue() {
        assertThat(SubscriptionReview.isDue(sub(Worth.OK, today), today)).isFalse()
    }

    @Test
    fun ninetyDayOldRatingIsStillFresh() {
        assertThat(SubscriptionReview.isDue(sub(Worth.LOVE, today.minusDays(90)), today)).isFalse()
    }

    @Test
    fun ninetyOneDayOldRatingIsDue() {
        assertThat(SubscriptionReview.isDue(sub(Worth.LOVE, today.minusDays(91)), today)).isTrue()
    }

    @Test
    fun ratingDatedInTheFutureIsNotDue() {
        assertThat(SubscriptionReview.isDue(sub(Worth.LOVE, today.plusDays(5)), today)).isFalse()
    }

    @Test
    fun ratingWithoutADateIsLeftAlone() {
        assertThat(SubscriptionReview.isDue(sub(Worth.RARELY), today)).isFalse()
    }

    @Test
    fun trialIsReviewedWhenUnrated() {
        assertThat(SubscriptionReview.isDue(sub(status = RecurringStatus.TRIAL), today)).isTrue()
    }

    @Test
    fun pausedAndCancelledAreNotReviewed() {
        assertThat(SubscriptionReview.isDue(sub(status = RecurringStatus.PAUSED), today)).isFalse()
        assertThat(SubscriptionReview.isDue(sub(status = RecurringStatus.CANCELLED), today)).isFalse()
    }

    @Test
    fun billsAndIncomeAreNotReviewed() {
        assertThat(SubscriptionReview.isDue(sub(type = RecurringType.BILL), today)).isFalse()
        assertThat(SubscriptionReview.isDue(sub(type = RecurringType.INCOME), today)).isFalse()
    }
}
