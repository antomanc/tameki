package dev.antomanc.tameki.core.model.entity

import dev.antomanc.tameki.core.model.Money

enum class BudgetPeriod {
    WEEK,
    MONTH,
}

/**
 * Spending cap for a set of categories (empty [categoryIds] = all expenses).
 * [rollover] means unused limit carries into the next period when evaluating pace.
 */
data class Budget(
    val id: Long = 0L,
    val name: String,
    val categoryIds: Set<Long> = emptySet(),
    val limit: Money,
    val period: BudgetPeriod,
    val rollover: Boolean = false,
)
