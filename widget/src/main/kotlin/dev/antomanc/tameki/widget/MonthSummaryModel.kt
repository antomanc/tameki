package dev.antomanc.tameki.widget

import dev.antomanc.tameki.core.model.recurring.RecurringLifecycle
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.budget.AvailableToSpendCalculator
import dev.antomanc.tameki.core.model.budget.HeroMode
import dev.antomanc.tameki.core.model.entity.Budget
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Transaction
import java.time.LocalDate

data class MonthSummaryWidgetData(
    val available: Money,
    val mode: HeroMode,
    val spent: Money,
    val pool: Money?,
    val nextRenewalName: String?,
    val nextRenewalDate: LocalDate?,
    /** The injected clock's day the figures were computed for; the pace bar reads it too. */
    val today: LocalDate,
)

/**
 * The widget renders the same figure as Today, from the same pure calculator. It used to do its
 * own arithmetic: no upcoming bills, and a fake negative when there was neither budget nor income.
 */
object MonthSummaryModel {
    fun compute(
        budgets: List<Budget>,
        transactionsThisMonth: List<Transaction>,
        recurrings: List<Recurring>,
        today: LocalDate,
        base: CurrencyCode,
    ): MonthSummaryWidgetData {
        val hero = AvailableToSpendCalculator.compute(
            budgets = budgets,
            transactionsThisMonth = transactionsThisMonth,
            recurrings = recurrings,
            today = today,
            baseCurrency = base,
        )
        val next = recurrings
            .filter { it.status == RecurringStatus.ACTIVE || it.status == RecurringStatus.TRIAL }
            .filter { it.type != RecurringType.INCOME }
            .map { RecurringLifecycle.settle(it, today) }
            .minByOrNull { it.nextDate }
        return MonthSummaryWidgetData(
            available = hero.amount,
            mode = hero.mode,
            spent = hero.spentThisMonth,
            pool = hero.budgetLimit ?: hero.incomeThisMonth.takeUnless { it.isZero },
            nextRenewalName = next?.name,
            nextRenewalDate = next?.nextDate,
            today = today,
        )
    }

    fun paceFraction(spent: Money, pool: Money?, today: LocalDate): Float {
        if (pool == null || pool.minor <= 0) return 0f
        val expected = pool.minor * today.dayOfMonth / today.lengthOfMonth().coerceAtLeast(1)
        if (expected <= 0L) return 0f
        return (spent.minor.toFloat() / expected.toFloat()).coerceIn(0f, 2f)
    }
}
