package dev.antomanc.tameki.core.model.budget

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Budget
import dev.antomanc.tameki.core.model.entity.BudgetPeriod
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.countsAsIncome
import dev.antomanc.tameki.core.model.recurring.RecurrenceCalculator
import java.time.LocalDate

/** What the Today hero shows, and how it was derived. */
enum class HeroMode {
    /** Budget or income − spent − upcoming bills. */
    AVAILABLE,

    /** No budget and no income yet: show spent with a prompt to set a budget. */
    SPENT_ONLY,
}

data class AvailableToSpend(
    val amount: Money,
    val mode: HeroMode,
    val spentThisMonth: Money,
    val upcomingRecurring: Money,
    val budgetLimit: Money?,
    val incomeThisMonth: Money,
)

/**
 * Pure calculator for the Today hero figure.
 *
 * available = (unscoped budget total if any, else income this month)
 *           − spent this month
 *           − upcoming recurring charges until month end.
 *
 * Only a budget with no [Budget.categoryIds] is a global spending pool. Category budgets are
 * compared inside their own category (and against their own spend); summing them into one pot
 * would subtract all spending from a groceries-only limit and print a meaningless negative hero.
 *
 * A weekly global budget is included instead of being silently dropped: its limit is converted to
 * the month-equivalent the hero speaks in (weekly × days in this month ÷ 7). A €100/week limit
 * therefore shows as roughly €428 in a 30-day month and keeps the hero comparable with monthly
 * limits, spending and expected pace.
 *
 * When neither a global budget nor income exists, [HeroMode.SPENT_ONLY] returns spent alone.
 * All amounts must already be in [baseCurrency].
 */
object AvailableToSpendCalculator {
    fun compute(
        budgets: List<Budget>,
        transactionsThisMonth: List<Transaction>,
        recurrings: List<Recurring>,
        today: LocalDate,
        baseCurrency: CurrencyCode,
    ): AvailableToSpend {
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
        val spent = sumByKind(transactionsThisMonth, TransactionKind.EXPENSE, baseCurrency)
        val income = sumByKind(
            transactionsThisMonth.filter { it.countsAsIncome() },
            TransactionKind.INCOME,
            baseCurrency,
        )
        val globalBudgets = budgets.filter {
            it.categoryIds.isEmpty() && it.limit.currency == baseCurrency
        }
        val budgetTotal = if (globalBudgets.isEmpty()) {
            null
        } else {
            globalBudgets.fold(Money.zero(baseCurrency)) { acc, budget ->
                acc + when (budget.period) {
                    BudgetPeriod.MONTH -> budget.limit
                    BudgetPeriod.WEEK -> weeklyToMonthEquivalent(budget.limit, today)
                }
            }
        }
        val upcoming = upcomingRecurringCharges(
            recurrings = recurrings,
            fromExclusive = today,
            toInclusive = monthEnd,
            baseCurrency = baseCurrency,
        )
        if (budgetTotal == null && income.isZero) {
            return AvailableToSpend(
                amount = spent,
                mode = HeroMode.SPENT_ONLY,
                spentThisMonth = spent,
                upcomingRecurring = upcoming,
                budgetLimit = null,
                incomeThisMonth = income,
            )
        }
        val pool = budgetTotal ?: income
        val available = pool - spent - upcoming
        return AvailableToSpend(
            amount = available,
            mode = HeroMode.AVAILABLE,
            spentThisMonth = spent,
            upcomingRecurring = upcoming,
            budgetLimit = budgetTotal,
            incomeThisMonth = income,
        )
    }

    /**
     * A weekly limit as the equivalent limit for this calendar month. 7 days make a week, so a
     * 30-day month holds 30/7 of it; rounding is half-even to the minor unit, never a float.
     */
    internal fun weeklyToMonthEquivalent(weekly: Money, today: LocalDate): Money {
        val totalDays = today.lengthOfMonth().toLong()
        val minor = java.math.BigDecimal.valueOf(weekly.minor)
            .multiply(java.math.BigDecimal.valueOf(totalDays))
            .divide(java.math.BigDecimal.valueOf(7L), 0, java.math.RoundingMode.HALF_EVEN)
            .longValueExact()
        return Money(minor, weekly.currency)
    }

    /** Expected spend by [today] for a linear monthly budget (or income pool). */
    fun expectedByToday(limit: Money, today: LocalDate): Money {
        val totalDays = today.lengthOfMonth()
        val elapsed = today.dayOfMonth
        val minor = if (totalDays <= 0) {
            0L
        } else {
            (limit.minor.toBigDecimal() * elapsed.toBigDecimal())
                .divide(totalDays.toBigDecimal(), 0, java.math.RoundingMode.HALF_EVEN)
                .longValueExact()
        }
        return Money(minor, limit.currency)
    }

    private fun upcomingRecurringCharges(
        recurrings: List<Recurring>,
        fromExclusive: LocalDate,
        toInclusive: LocalDate,
        baseCurrency: CurrencyCode,
    ): Money {
        if (toInclusive.isBefore(fromExclusive.plusDays(1))) return Money.zero(baseCurrency)
        var total = Money.zero(baseCurrency)
        for (r in recurrings) {
            if (r.status != RecurringStatus.ACTIVE && r.status != RecurringStatus.TRIAL) continue
            if (r.type == RecurringType.INCOME) continue
            if (r.amount.currency != baseCurrency) continue
            val from = fromExclusive.plusDays(1)
            if (from.isAfter(toInclusive)) continue
            val dates = RecurrenceCalculator.occurrencesBetween(r, from, toInclusive)
            if (dates.isNotEmpty()) {
                total += r.amount * dates.size
            }
        }
        return total
    }

    private fun sumByKind(
        transactions: List<Transaction>,
        kind: TransactionKind,
        currency: CurrencyCode,
    ): Money {
        var total = Money.zero(currency)
        for (tx in transactions) {
            if (tx.kind != kind) continue
            if (tx.amount.currency != currency) continue
            total += tx.amount
        }
        return total
    }
}
