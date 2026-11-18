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

class RecurrenceCalculatorTest {
    private val eur = CurrencyCode("EUR")

    @Test
    fun monthEndClampsThenReanchors() {
        val jan31 = LocalDate.of(2026, 1, 31)
        val feb = RecurrenceCalculator.next(jan31, Cycle(1, CycleUnit.MONTH), anchorDayOfMonth = 31)
        assertThat(feb).isEqualTo(LocalDate.of(2026, 2, 28))
        val mar = RecurrenceCalculator.next(feb, Cycle(1, CycleUnit.MONTH), anchorDayOfMonth = 31)
        assertThat(mar).isEqualTo(LocalDate.of(2026, 3, 31))
    }

    @Test
    fun leapYearFebruary() {
        val jan31 = LocalDate.of(2024, 1, 31)
        val feb = RecurrenceCalculator.next(jan31, Cycle(1, CycleUnit.MONTH), anchorDayOfMonth = 31)
        assertThat(feb).isEqualTo(LocalDate.of(2024, 2, 29))
    }

    @Test
    fun everyTwoWeeks() {
        val start = LocalDate.of(2026, 1, 1)
        assertThat(RecurrenceCalculator.next(start, Cycle(2, CycleUnit.WEEK)))
            .isEqualTo(LocalDate.of(2026, 1, 15))
    }

    @Test
    fun occurrencesBetweenUsesStartAnchor() {
        val r = Recurring(
            id = 1,
            type = RecurringType.BILL,
            name = "Rent",
            amount = Money(100_000, eur),
            cycle = Cycle(1, CycleUnit.MONTH),
            startDate = LocalDate.of(2026, 1, 31),
            nextDate = LocalDate.of(2026, 1, 31),
        )
        val dates = RecurrenceCalculator.occurrencesBetween(
            r,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 4, 30),
        )
        assertThat(dates).containsExactly(
            LocalDate.of(2026, 1, 31),
            LocalDate.of(2026, 2, 28),
            LocalDate.of(2026, 3, 31),
            LocalDate.of(2026, 4, 30),
        ).inOrder()
    }
}
