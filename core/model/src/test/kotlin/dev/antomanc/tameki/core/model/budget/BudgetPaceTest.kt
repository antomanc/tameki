package dev.antomanc.tameki.core.model.budget

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Budget
import dev.antomanc.tameki.core.model.entity.BudgetPeriod
import java.time.LocalDate
import org.junit.Test

class BudgetPaceTest {
    private val eur = CurrencyCode("EUR")

    private val budget = Budget(
        id = 1,
        name = "Food",
        limit = Money(30_000, eur),
        period = BudgetPeriod.MONTH,
    )

    @Test
    fun onTrackWhenProjectionUnderLimit() {
        val pace = BudgetPace.evaluate(
            budget = budget,
            spentSoFar = Money(5_000, eur),
            periodStart = LocalDate.of(2026, 11, 1),
            periodEnd = LocalDate.of(2026, 11, 30),
            today = LocalDate.of(2026, 11, 10),
        )
        assertThat(pace.status).isEqualTo(PaceStatus.ON_TRACK)
        assertThat(pace.projected.minor).isEqualTo(15_000L)
    }

    @Test
    fun atRiskWhenProjectionExceedsLimit() {
        val pace = BudgetPace.evaluate(
            budget = budget,
            spentSoFar = Money(20_000, eur),
            periodStart = LocalDate.of(2026, 11, 1),
            periodEnd = LocalDate.of(2026, 11, 30),
            today = LocalDate.of(2026, 11, 10),
        )
        assertThat(pace.status).isEqualTo(PaceStatus.AT_RISK)
    }

    @Test
    fun overWhenAlreadyPastLimit() {
        val pace = BudgetPace.evaluate(
            budget = budget,
            spentSoFar = Money(31_000, eur),
            periodStart = LocalDate.of(2026, 11, 1),
            periodEnd = LocalDate.of(2026, 11, 30),
            today = LocalDate.of(2026, 11, 10),
        )
        assertThat(pace.status).isEqualTo(PaceStatus.OVER)
    }

    @Test
    fun rolloverAddsPreviousUnspent() {
        val rolling = budget.copy(rollover = true)
        val pace = BudgetPace.evaluate(
            budget = rolling,
            spentSoFar = Money(5_000, eur),
            periodStart = LocalDate.of(2026, 11, 1),
            periodEnd = LocalDate.of(2026, 11, 30),
            today = LocalDate.of(2026, 11, 10),
            previousPeriodUnspent = Money(10_000, eur),
        )
        assertThat(pace.limit).isEqualTo(Money(40_000, eur))
        assertThat(pace.status).isEqualTo(PaceStatus.ON_TRACK)
    }
}
