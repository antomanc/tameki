package dev.antomanc.tameki.feature.aibridge

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.ai.MonthSummary
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.insight.Insight
import dev.antomanc.tameki.core.model.recurring.CostNormalizer
import java.time.YearMonth

/** Builds a [MonthSummary] for [AiPrompts.monthReview] from already-converted base-currency data. */
object MonthSummaryBuilder {
    fun build(
        month: YearMonth,
        base: CurrencyCode,
        transactions: List<Transaction>,
        categories: List<Category>,
        recurrings: List<Recurring>,
        insights: List<Insight> = emptyList(),
    ): MonthSummary {
        val inMonth = transactions.filter {
            val d = it.occurredAt.toLocalDate()
            YearMonth.from(d) == month && it.amount.currency == base
        }
        var expenses = Money.zero(base)
        var income = Money.zero(base)
        val byCat = linkedMapOf<Long, Money>()
        for (tx in inMonth) {
            when (tx.kind) {
                TransactionKind.EXPENSE -> {
                    expenses += tx.amount
                    val id = tx.categoryId ?: continue
                    byCat[id] = (byCat[id] ?: Money.zero(base)) + tx.amount
                }
                TransactionKind.INCOME -> income += tx.amount
                TransactionKind.TRANSFER -> Unit
            }
        }
        val catNames = categories.associate { it.id to it.name }
        val byCategory = byCat.entries
            .sortedByDescending { it.value.minor }
            .map { (id, amount) -> (catNames[id] ?: "#$id") to amount }

        val subscriptions = recurrings
            .filter { it.status == RecurringStatus.ACTIVE || it.status == RecurringStatus.TRIAL }
            .filter { it.amount.currency == base }
            .map { it.name to CostNormalizer.monthly(it) }
            .sortedByDescending { it.second.minor }

        return MonthSummary(
            month = month,
            totalExpenses = expenses,
            totalIncome = income,
            byCategory = byCategory,
            subscriptions = subscriptions,
            insights = insights,
        )
    }
}
