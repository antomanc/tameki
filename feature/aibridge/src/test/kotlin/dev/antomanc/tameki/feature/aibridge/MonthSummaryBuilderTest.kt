package dev.antomanc.tameki.feature.aibridge

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import java.time.LocalDateTime
import java.time.YearMonth
import org.junit.Test

class MonthSummaryBuilderTest {
    @Test
    fun sumsExpensesByCategory() {
        val eur = CurrencyCode("EUR")
        val summary = MonthSummaryBuilder.build(
            month = YearMonth.of(2026, 11),
            base = eur,
            transactions = listOf(
                Transaction(
                    kind = TransactionKind.EXPENSE,
                    amount = Money(1000, eur),
                    accountId = 1,
                    categoryId = 1,
                    occurredAt = LocalDateTime.of(2026, 11, 2, 12, 0),
                ),
            ),
            categories = listOf(Category(1, "Spesa", CategoryKind.EXPENSE, "groceries", 1)),
            recurrings = emptyList(),
        )
        assertThat(summary.totalExpenses.minor).isEqualTo(1000)
        assertThat(summary.byCategory).containsExactly("Spesa" to Money(1000, eur))
    }
}
