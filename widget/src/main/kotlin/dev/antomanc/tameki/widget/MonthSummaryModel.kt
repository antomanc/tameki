package dev.antomanc.tameki.widget

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Budget
import dev.antomanc.tameki.core.model.entity.BudgetPeriod
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import java.time.LocalDate

data class MonthSummaryWidgetData(
    val available: Money,
    val spent: Money,
    val pool: Money?,
    val nextRenewalName: String?,
    val nextRenewalDate: LocalDate?,
)

/** Pure widget calculator (mirrors Home available-to-spend without depending on feature:home). */
object MonthSummaryModel {
    fun compute(
        budgets: List<Budget>,
        transactionsThisMonth: List<Transaction>,
        recurrings: List<Recurring>,
        today: LocalDate,
        base: CurrencyCode,
    ): MonthSummaryWidgetData {
        val spent = sum(transactionsThisMonth, TransactionKind.EXPENSE, base)
        val income = sum(transactionsThisMonth, TransactionKind.INCOME, base)
        val budgetTotal = budgets
            .filter { it.period == BudgetPeriod.MONTH && it.limit.currency == base }
            .fold(Money.zero(base)) { acc, b -> acc + b.limit }
            .takeUnless { it.isZero }
        val pool = budgetTotal ?: income.takeUnless { it.isZero }
        val available = (pool ?: Money.zero(base)) - spent
        val next = recurrings
            .filter { it.status == RecurringStatus.ACTIVE || it.status == RecurringStatus.TRIAL }
            .filter { it.type != RecurringType.INCOME }
            .filter { !it.nextDate.isBefore(today) }
            .minByOrNull { it.nextDate }
        return MonthSummaryWidgetData(
            available = available,
            spent = spent,
            pool = pool,
            nextRenewalName = next?.name,
            nextRenewalDate = next?.nextDate,
        )
    }

    fun paceFraction(spent: Money, pool: Money?, today: LocalDate): Float {
        if (pool == null || pool.minor <= 0) return 0f
        val expected = pool.minor * today.dayOfMonth / today.lengthOfMonth().coerceAtLeast(1)
        if (expected <= 0L) return 0f
        return (spent.minor.toFloat() / expected.toFloat()).coerceIn(0f, 2f)
    }

    private fun sum(txs: List<Transaction>, kind: TransactionKind, currency: CurrencyCode): Money {
        var total = Money.zero(currency)
        for (tx in txs) {
            if (tx.kind == kind && tx.amount.currency == currency) total += tx.amount
        }
        return total
    }
}
