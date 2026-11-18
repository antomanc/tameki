package dev.antomanc.tameki.core.data

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.data.reminder.AutoLogProcessor
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AutoLogProcessorTest : DbTestRule() {
    private val eur = CurrencyCode("EUR")

    @Test
    fun catchesUpMissedOccurrencesInOnePassAtomically() = runTest {
        val accountId = accounts.upsert(Account(name = "Card", currency = eur, type = AccountType.CARD))
        val id = recurring.upsert(
            Recurring(
                type = RecurringType.SUBSCRIPTION,
                name = "Netflix",
                amount = Money(1_399L, eur),
                cycle = Cycle(1, CycleUnit.MONTH),
                startDate = LocalDate.of(2026, 1, 15),
                nextDate = LocalDate.of(2026, 9, 15),
                status = RecurringStatus.ACTIVE,
                accountId = accountId,
                autoLog = true,
            ),
        )
        val processor = AutoLogProcessor(db)
        val today = LocalDate.of(2026, 11, 18)
        val logged = processor.processDue(today)
        assertThat(logged).isEqualTo(3) // Sep, Oct, Nov

        val txs = transactions.observe().first()
        assertThat(txs).hasSize(3)
        assertThat(txs.map { it.occurredAt.toLocalDate() }).containsExactly(
            LocalDate.of(2026, 9, 15),
            LocalDate.of(2026, 10, 15),
            LocalDate.of(2026, 11, 15),
        )
        val advanced = recurring.get(id)!!
        assertThat(advanced.nextDate).isEqualTo(LocalDate.of(2026, 12, 15))

        // Second pass is idempotent thanks to unique (recurringId, occurredAt) + IGNORE.
        assertThat(processor.processDue(today)).isEqualTo(0)
        assertThat(transactions.observe().first()).hasSize(3)
    }
}
