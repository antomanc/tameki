package dev.antomanc.tameki.core.model.recurring

import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.Recurring
import java.time.LocalDate
import kotlin.math.min

/**
 * Calendar math for renewals.
 * Month-end anchors (31st) clamp into shorter months then snap back when the month allows it,
 * so a rent due on the 31st does not drift to the 28th forever after February.
 */
object RecurrenceCalculator {
    fun next(date: LocalDate, cycle: Cycle, anchorDayOfMonth: Int = date.dayOfMonth): LocalDate =
        advance(date, cycle, anchorDayOfMonth)

    fun occurrencesBetween(recurring: Recurring, from: LocalDate, to: LocalDate): List<LocalDate> {
        require(!to.isBefore(from)) { "to must be on or after from" }
        val anchor = recurring.startDate.dayOfMonth
        val out = mutableListOf<LocalDate>()
        var cursor = recurring.startDate
        // Walk forward from start until we pass [to], collecting dates in [from, to].
        var guard = 0
        while (cursor.isBefore(from) && guard < 100_000) {
            cursor = advance(cursor, recurring.cycle, anchor)
            guard++
        }
        while (!cursor.isAfter(to) && guard < 100_000) {
            if (!cursor.isBefore(from)) {
                out += cursor
            }
            cursor = advance(cursor, recurring.cycle, anchor)
            guard++
        }
        return out
    }

    private fun advance(date: LocalDate, cycle: Cycle, anchorDayOfMonth: Int): LocalDate =
        when (cycle.unit) {
            CycleUnit.DAY -> date.plusDays(cycle.every.toLong())
            CycleUnit.WEEK -> date.plusWeeks(cycle.every.toLong())
            CycleUnit.MONTH -> addMonthsAnchored(date, cycle.every, anchorDayOfMonth)
            CycleUnit.YEAR -> addMonthsAnchored(date, cycle.every * 12, anchorDayOfMonth)
        }

    private fun addMonthsAnchored(date: LocalDate, months: Int, anchorDayOfMonth: Int): LocalDate {
        val shifted = date.plusMonths(months.toLong())
        val day = min(anchorDayOfMonth, shifted.lengthOfMonth())
        return shifted.withDayOfMonth(day)
    }
}
