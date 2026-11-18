package dev.antomanc.tameki.core.model.recurring

import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Turns arbitrary billing cycles into comparable monthly/yearly costs.
 * Divides by [Recurring.sharedWith] so family plans do not look like personal overspend.
 * Rounding is half-even so repeated normalize-and-sum does not systematically drift up.
 */
object CostNormalizer {
    fun monthly(recurring: Recurring): Money {
        val perPeriod = share(recurring.amount, recurring.sharedWith)
        return scaleToMonth(perPeriod, recurring.cycle)
    }

    fun yearly(recurring: Recurring): Money {
        val perPeriod = share(recurring.amount, recurring.sharedWith)
        return scaleToYear(perPeriod, recurring.cycle)
    }

    fun totalMonthly(
        items: List<Recurring>,
        type: RecurringType? = null,
        status: RecurringStatus? = RecurringStatus.ACTIVE,
    ): Money? {
        val filtered = items.filter { r ->
            (type == null || r.type == type) && (status == null || r.status == status)
        }
        if (filtered.isEmpty()) return null
        val currency = filtered.first().amount.currency
        require(filtered.all { it.amount.currency == currency }) {
            "Cannot total mixed currencies; convert first"
        }
        return filtered.fold(Money.zero(currency)) { acc, r -> acc + monthly(r) }
    }

    fun totalYearly(
        items: List<Recurring>,
        type: RecurringType? = null,
        status: RecurringStatus? = RecurringStatus.ACTIVE,
    ): Money? {
        val filtered = items.filter { r ->
            (type == null || r.type == type) && (status == null || r.status == status)
        }
        if (filtered.isEmpty()) return null
        val currency = filtered.first().amount.currency
        require(filtered.all { it.amount.currency == currency }) {
            "Cannot total mixed currencies; convert first"
        }
        return filtered.fold(Money.zero(currency)) { acc, r -> acc + yearly(r) }
    }

    private fun share(amount: Money, sharedWith: Int): Money {
        if (sharedWith == 1) return amount
        return Money(divideHalfEven(amount.minor, sharedWith.toLong()), amount.currency)
    }

    private fun scaleToMonth(amount: Money, cycle: Cycle): Money {
        // monthly = amount * (days-in-average-month factor via cycle)
        // DAY: * (365.25/12) / every ≈ * 30.4375 / every — use 36525/1200 rational via BigDecimal
        val factor = when (cycle.unit) {
            CycleUnit.DAY -> BigDecimal("30.4375").divide(BigDecimal(cycle.every), 16, RoundingMode.HALF_EVEN)
            CycleUnit.WEEK -> BigDecimal("4.34524").divide(BigDecimal(cycle.every), 16, RoundingMode.HALF_EVEN)
            CycleUnit.MONTH -> BigDecimal.ONE.divide(BigDecimal(cycle.every), 16, RoundingMode.HALF_EVEN)
            CycleUnit.YEAR -> BigDecimal.ONE.divide(BigDecimal(cycle.every * 12L), 16, RoundingMode.HALF_EVEN)
        }
        return Money(multiplyHalfEven(amount.minor, factor), amount.currency)
    }

    private fun scaleToYear(amount: Money, cycle: Cycle): Money {
        val factor = when (cycle.unit) {
            CycleUnit.DAY -> BigDecimal("365.25").divide(BigDecimal(cycle.every), 16, RoundingMode.HALF_EVEN)
            CycleUnit.WEEK -> BigDecimal("52.1785").divide(BigDecimal(cycle.every), 16, RoundingMode.HALF_EVEN)
            CycleUnit.MONTH -> BigDecimal(12).divide(BigDecimal(cycle.every), 16, RoundingMode.HALF_EVEN)
            CycleUnit.YEAR -> BigDecimal.ONE.divide(BigDecimal(cycle.every), 16, RoundingMode.HALF_EVEN)
        }
        return Money(multiplyHalfEven(amount.minor, factor), amount.currency)
    }

    private fun divideHalfEven(minor: Long, divisor: Long): Long =
        BigDecimal.valueOf(minor)
            .divide(BigDecimal.valueOf(divisor), 0, RoundingMode.HALF_EVEN)
            .longValueExact()

    private fun multiplyHalfEven(minor: Long, factor: BigDecimal): Long =
        BigDecimal.valueOf(minor)
            .multiply(factor)
            .setScale(0, RoundingMode.HALF_EVEN)
            .longValueExact()
}
