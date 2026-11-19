package dev.antomanc.tameki.core.data.reminder

import androidx.room.withTransaction
import dev.antomanc.tameki.core.data.db.TamekiDatabase
import dev.antomanc.tameki.core.data.db.entity.TransactionEntity
import dev.antomanc.tameki.core.data.db.entity.toDomain
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.recurring.RecurrenceCalculator
import dev.antomanc.tameki.core.model.recurring.currentNext
import dev.antomanc.tameki.core.model.recurring.isRunning
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Persists auto-logged recurring charges and advances nextDate atomically with compare-and-set.
 * Catches up every missed occurrence up to [maxCatchUp] while nextDate <= today.
 */
class AutoLogProcessor(
    private val db: TamekiDatabase,
) {
    suspend fun processDue(today: LocalDate, maxCatchUp: Int = MAX_CATCH_UP): Int {
        val entities = db.recurringDao().getAll()
        var logged = 0
        for (entity in entities) {
            val prices = db.recurringDao().pricePointsFor(entity.id)
            var recurring = entity.toDomain(prices)
            if (!recurring.autoLog || recurring.accountId == null) {
                // Nothing to log, but the renewal date still has to follow the calendar: a plan
                // nobody logs must not show "12 days late" forever, nor stop reminding.
                if (recurring.isRunning && recurring.nextDate.isBefore(today)) {
                    db.recurringDao().updateNextDateCas(
                        id = recurring.id,
                        expectedDate = recurring.nextDate,
                        newDate = RecurrenceCalculator.currentNext(recurring, today),
                    )
                }
                continue
            }
            if (recurring.status == RecurringStatus.CANCELLED || recurring.status == RecurringStatus.PAUSED) continue
            val accountId = recurring.accountId ?: continue
            var steps = 0
            while (!recurring.nextDate.isAfter(today) && steps < maxCatchUp) {
                val chargeDate = recurring.nextDate
                val nextDate = RecurrenceCalculator.nextAfter(recurring, chargeDate)
                val kind = when (recurring.type) {
                    RecurringType.INCOME -> TransactionKind.INCOME
                    else -> TransactionKind.EXPENSE
                }
                val occurredAt = LocalDateTime.of(chargeDate, LocalTime.NOON)
                val step = db.withTransaction {
                    val rowId = db.transactionDao().insertIgnore(
                        TransactionEntity(
                            kind = kind.name,
                            amountMinor = recurring.amount.minor,
                            currency = recurring.amount.currency.value,
                            accountId = accountId,
                            categoryId = recurring.categoryId,
                            merchant = recurring.name,
                            occurredAt = occurredAt,
                            recurringId = recurring.id,
                            source = TransactionSource.RECURRING.name,
                        ),
                    )
                    val updated = db.recurringDao().updateNextDateCas(
                        id = recurring.id,
                        expectedDate = chargeDate,
                        newDate = nextDate,
                    )
                    if (updated == 0) {
                        // Lost the race; stop this series for this pass.
                        return@withTransaction null
                    }
                    rowId > 0
                }
                if (step == null) break
                if (step) logged++
                recurring = recurring.copy(nextDate = nextDate)
                steps++
            }
        }
        return logged
    }

    companion object {
        const val MAX_CATCH_UP = 500
    }
}
