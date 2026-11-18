package dev.antomanc.tameki.feature.insights

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.BudgetPeriod
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** The first and last day of the budget period that contains [today] (weeks start on Monday). */
internal fun periodBounds(period: BudgetPeriod, today: LocalDate): Pair<LocalDate, LocalDate> = when (period) {
    BudgetPeriod.WEEK -> {
        val start = today.with(DayOfWeek.MONDAY)
        start to start.plusDays(6)
    }
    BudgetPeriod.MONTH -> {
        val start = today.withDayOfMonth(1)
        start to today.withDayOfMonth(today.lengthOfMonth())
    }
}

/**
 * Linear "where the budget should be today": the part of the limit that covers the elapsed days.
 * Half-even, like every money calculation in the app.
 */
internal fun expectedByToday(limit: Money, periodStart: LocalDate, periodEnd: LocalDate, today: LocalDate): Money {
    val totalDays = ChronoUnit.DAYS.between(periodStart, periodEnd).toInt() + 1
    val elapsed = ChronoUnit.DAYS.between(periodStart, today.coerceIn(periodStart, periodEnd)).toInt() + 1
    if (totalDays <= 0) return Money.zero(limit.currency)
    val minor = BigDecimal.valueOf(limit.minor)
        .multiply(BigDecimal.valueOf(elapsed.toLong()))
        .divide(BigDecimal.valueOf(totalDays.toLong()), 0, RoundingMode.HALF_EVEN)
        .longValueExact()
    return Money(minor, limit.currency)
}

/**
 * Expense total in [currency] between two days. A split transaction counts each split against its
 * own category, exactly like [dev.antomanc.tameki.core.model.insight.SavingsInsights] does. An
 * empty [categoryIds] means "all expenses".
 */
internal fun sumExpenses(
    transactions: List<Transaction>,
    categoryIds: Set<Long>,
    from: LocalDate,
    to: LocalDate,
    currency: CurrencyCode,
): Money {
    var total = 0L
    for (transaction in transactions) {
        if (transaction.kind != TransactionKind.EXPENSE) continue
        if (transaction.amount.currency != currency) continue
        val date = transaction.occurredAt.toLocalDate()
        if (date.isBefore(from) || date.isAfter(to)) continue
        if (transaction.splits.isNotEmpty()) {
            transaction.splits.forEach { split ->
                if (split.amount.currency != currency) return@forEach
                if (categoryIds.isEmpty() || split.categoryId in categoryIds) {
                    total += split.amount.minor
                }
            }
        } else {
            val categoryId = transaction.categoryId
            if (categoryIds.isEmpty() || (categoryId != null && categoryId in categoryIds)) {
                total += transaction.amount.minor
            }
        }
    }
    return Money(total, currency)
}

/** Spending per calendar month, oldest first, for the months ending with [today]'s month. */
internal fun monthTotals(
    transactions: List<Transaction>,
    today: LocalDate,
    months: Int,
    currency: CurrencyCode,
): List<MonthSpend> {
    val current = YearMonth.from(today)
    return (months - 1 downTo 0).map { back ->
        val month = current.minusMonths(back.toLong())
        val amount = transactions
            .filter {
                it.kind == TransactionKind.EXPENSE &&
                    YearMonth.from(it.occurredAt).equals(month)
            }
            .sumOf { it.amount.minor }
        MonthSpend(month, Money(amount, currency))
    }
}

/**
 * This month's spending by category, largest first. Splits count towards their own category;
 * transactions without a category are grouped under the null id.
 */
internal fun categoryTotals(
    transactions: List<Transaction>,
    categories: Map<Long, Category>,
    limit: Int,
    currency: CurrencyCode,
): List<CategorySpend> {
    val totals = mutableMapOf<Long?, Long>()
    for (transaction in transactions) {
        if (transaction.kind != TransactionKind.EXPENSE) continue
        if (transaction.splits.isNotEmpty()) {
            transaction.splits.forEach { split ->
                totals[split.categoryId] = (totals[split.categoryId] ?: 0L) + split.amount.minor
            }
        } else {
            totals[transaction.categoryId] = (totals[transaction.categoryId] ?: 0L) + transaction.amount.minor
        }
    }
    return totals.entries
        .sortedByDescending { it.value }
        .take(limit)
        .map { (categoryId, minor) ->
            val category = categoryId?.let(categories::get)
            CategorySpend(
                categoryId = categoryId,
                categoryKey = category?.icon ?: "other",
                label = category?.name,
                amount = Money(minor, currency),
            )
        }
}

/** Days in a row, ending today, with no expense at all. Capped so the streak never gets silly. */
internal fun noSpendStreak(transactions: List<Transaction>, today: LocalDate, max: Int = 30): Int {
    val daysWithSpending = transactions
        .filter { it.kind == TransactionKind.EXPENSE }
        .map { it.occurredAt.toLocalDate() }
        .toSet()
    var days = 0
    var cursor = today
    while (days < max && cursor !in daysWithSpending) {
        days++
        cursor = cursor.minusDays(1)
    }
    return days
}

/** Percentage growth of [part] over [base], rounded half-even; 0 when there is no base. */
internal fun percentOf(part: Long, base: Long): Int {
    if (base == 0L) return 0
    return BigDecimal.valueOf(part)
        .multiply(BigDecimal.valueOf(100L))
        .divide(BigDecimal.valueOf(base), 0, RoundingMode.HALF_EVEN)
        .toInt()
}
