package dev.antomanc.tameki.core.model.insight

import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.budget.Pace
import dev.antomanc.tameki.core.model.entity.Budget
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.Transaction

/**
 * Everything [SavingsInsights] needs, already converted to the user's base currency.
 * Conversion stays outside so this module never suspends on [dev.antomanc.tameki.core.model.money.ExchangeRates].
 */
data class InsightInput(
    val recurrings: List<Recurring> = emptyList(),
    val transactionsThisMonth: List<Transaction> = emptyList(),
    /** Trailing three full months of expenses, used for category averages. */
    val transactionsTrailingThreeMonths: List<Transaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val budgetPaces: Map<Long, Pace> = emptyMap(),
    val noSpendStreakDays: Int = 0,
    val rules: InsightRules = InsightRules(),
    val baseCurrency: Money? = null,
)
