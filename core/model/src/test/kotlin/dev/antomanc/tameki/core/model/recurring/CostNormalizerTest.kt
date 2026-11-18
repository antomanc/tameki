package dev.antomanc.tameki.core.model.recurring

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringType
import java.time.LocalDate
import org.junit.Test

class CostNormalizerTest {
    private val eur = CurrencyCode("EUR")

    private fun sub(
        minor: Long,
        cycle: Cycle,
        sharedWith: Int = 1,
    ) = Recurring(
        id = 1,
        type = RecurringType.SUBSCRIPTION,
        name = "X",
        amount = Money(minor, eur),
        cycle = cycle,
        startDate = LocalDate.of(2026, 1, 1),
        nextDate = LocalDate.of(2026, 2, 1),
        sharedWith = sharedWith,
    )

    @Test
    fun monthlySubscriptionIsItself() {
        assertThat(CostNormalizer.monthly(sub(1_399, Cycle(1, CycleUnit.MONTH))))
            .isEqualTo(Money(1_399, eur))
    }

    @Test
    fun yearlyPlanDividedByTwelve() {
        // 120.00 / 12 = 10.00
        assertThat(CostNormalizer.monthly(sub(12_000, Cycle(1, CycleUnit.YEAR))))
            .isEqualTo(Money(1_000, eur))
    }

    @Test
    fun sharedSubscriptionSplitsCost() {
        val r = sub(1_200, Cycle(1, CycleUnit.MONTH), sharedWith = 2)
        assertThat(CostNormalizer.monthly(r)).isEqualTo(Money(600, eur))
        assertThat(CostNormalizer.yearly(r)).isEqualTo(Money(7_200, eur))
    }

    @Test
    fun halfEvenRoundingOnOddSplit() {
        // 100 cents / 3 = 33.333... → 33 half-even? 100/3 = 33.333 → 33
        val r = sub(100, Cycle(1, CycleUnit.MONTH), sharedWith = 3)
        assertThat(CostNormalizer.monthly(r).minor).isEqualTo(33L)
    }

    @Test
    fun zeroAmount() {
        assertThat(CostNormalizer.yearly(sub(0, Cycle(1, CycleUnit.MONTH))).isZero).isTrue()
    }
}
