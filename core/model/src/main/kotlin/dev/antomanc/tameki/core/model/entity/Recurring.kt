package dev.antomanc.tameki.core.model.entity

import dev.antomanc.tameki.core.model.Money
import java.time.LocalDate

enum class CycleUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR,
}

/** Every [every] [unit], e.g. every 3 MONTH or every 2 WEEK. */
data class Cycle(
    val every: Int,
    val unit: CycleUnit,
) {
    init {
        require(every > 0) { "Cycle.every must be positive" }
    }
}

enum class RecurringType {
    SUBSCRIPTION,
    BILL,
    INCOME,
}

enum class RecurringStatus {
    ACTIVE,
    TRIAL,
    PAUSED,
    CANCELLED,
}

/** User "is it worth it?" rating; null means not rated yet. */
enum class Worth {
    LOVE,
    OK,
    RARELY,
    UNUSED,
}


data class PricePoint(
    val amount: Money,
    val since: LocalDate,
)

/**
 * Subscription, bill or recurring income.
 * [sharedWith] > 1 splits the normalized cost so roommate/family plans don't inflate insights.
 * [annualAlternative] is the known yearly plan price — insights never invent one.
 * [worthRatedOn] is the day the current [worth] was given; see `SubscriptionReview` for the age
 * after which the rating is considered stale. Null when the rating predates the date column.
 */
data class Recurring(
    val id: Long = 0L,
    val type: RecurringType,
    val name: String,
    val amount: Money,
    val cycle: Cycle,
    val startDate: LocalDate,
    val nextDate: LocalDate,
    val trialEndsOn: LocalDate? = null,
    val status: RecurringStatus = RecurringStatus.ACTIVE,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val cancelUrl: String? = null,
    val note: String? = null,
    val worth: Worth? = null,
    val worthRatedOn: LocalDate? = null,
    val remindDaysBefore: Int = 2,
    val autoLog: Boolean = false,
    val priceHistory: List<PricePoint> = emptyList(),
    val annualAlternative: Money? = null,
    val brandKey: String? = null,
    val sharedWith: Int = 1,
) {
    init {
        require(sharedWith >= 1) { "sharedWith must be >= 1" }
    }
}
