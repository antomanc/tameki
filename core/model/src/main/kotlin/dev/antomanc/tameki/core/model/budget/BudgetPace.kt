package dev.antomanc.tameki.core.model.budget

import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Budget
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class PaceStatus {
    ON_TRACK,
    AT_RISK,
    OVER,
}

data class Pace(
    val spent: Money,
    val limit: Money,
    val projected: Money,
    val status: PaceStatus,
)

/**
 * Linear spend projection for the current budget period.
 * Rollover is applied by adding [previousPeriodUnspent] when [Budget.rollover] is true —
 * callers compute the previous remainder so this stays a pure calendar calculation.
 */
object BudgetPace {
    fun evaluate(
        budget: Budget,
        spentSoFar: Money,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        today: LocalDate,
        previousPeriodUnspent: Money = Money.zero(budget.limit.currency),
    ): Pace {
        require(spentSoFar.currency == budget.limit.currency) {
            "spent and limit currencies must match"
        }
        require(!periodEnd.isBefore(periodStart)) { "periodEnd before periodStart" }

        val effectiveLimit = if (budget.rollover) {
            require(previousPeriodUnspent.currency == budget.limit.currency) {
                "rollover currency mismatch"
            }
            budget.limit + previousPeriodUnspent
        } else {
            budget.limit
        }

        val clampedToday = when {
            today.isBefore(periodStart) -> periodStart
            today.isAfter(periodEnd) -> periodEnd
            else -> today
        }
        val totalDays = ChronoUnit.DAYS.between(periodStart, periodEnd).toInt() + 1
        val elapsedDays = ChronoUnit.DAYS.between(periodStart, clampedToday).toInt() + 1
        val projectedMinor = if (elapsedDays <= 0) {
            spentSoFar.minor
        } else {
            BigDecimal.valueOf(spentSoFar.minor)
                .multiply(BigDecimal.valueOf(totalDays.toLong()))
                .divide(BigDecimal.valueOf(elapsedDays.toLong()), 0, RoundingMode.HALF_EVEN)
                .longValueExact()
        }
        val projected = Money(projectedMinor, spentSoFar.currency)
        val status = when {
            spentSoFar.minor > effectiveLimit.minor -> PaceStatus.OVER
            projected.minor > effectiveLimit.minor -> PaceStatus.AT_RISK
            else -> PaceStatus.ON_TRACK
        }
        return Pace(
            spent = spentSoFar,
            limit = effectiveLimit,
            projected = projected,
            status = status,
        )
    }
}
