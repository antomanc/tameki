package dev.antomanc.tameki.widget

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Budget
import dev.antomanc.tameki.core.model.entity.BudgetPeriod
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Test

class MonthSummaryModelTest {
    @Test
    fun availableIsPoolMinusSpent() {
        val eur = CurrencyCode("EUR")
        val data = MonthSummaryModel.compute(
            budgets = listOf(Budget(1, "Mese", limit = Money(100_00, eur), period = BudgetPeriod.MONTH)),
            transactionsThisMonth = listOf(
                Transaction(
                    kind = TransactionKind.EXPENSE,
                    amount = Money(20_00, eur),
                    accountId = 1,
                    occurredAt = LocalDateTime.of(2026, 11, 5, 12, 0),
                ),
            ),
            recurrings = emptyList(),
            today = LocalDate.of(2026, 11, 18),
            base = eur,
        )
        assertThat(data.available.minor).isEqualTo(80_00)
    }
}
