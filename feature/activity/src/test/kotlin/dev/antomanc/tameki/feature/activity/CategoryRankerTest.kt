package dev.antomanc.tameki.feature.activity

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import java.time.LocalDateTime
import org.junit.Test

class CategoryRankerTest {
    private val groceries = Category(1, "Alimentari", CategoryKind.EXPENSE, "groceries", 1)
    private val coffee = Category(2, "Caffè", CategoryKind.EXPENSE, "coffee", 2)
    private val transport = Category(3, "Trasporti", CategoryKind.EXPENSE, "transport", 3)
    private val now = LocalDateTime.of(2026, 11, 18, 8, 30)

    @Test
    fun merchantMemoryWins() {
        val ranked = CategoryRanker.rank(
            categories = listOf(groceries, coffee, transport),
            kind = CategoryKind.EXPENSE,
            merchant = "Esselunga",
            hint = null,
            merchantMemory = mapOf("Esselunga" to 1L),
            recent = emptyList(),
            now = now,
        )
        assertThat(ranked.first().id).isEqualTo(1)
    }

    @Test
    fun frequencyAndHourBoostCoffeeInTheMorning() {
        val recent = listOf(
            tx(coffee.id, hour = 8),
            tx(coffee.id, hour = 8),
            tx(coffee.id, hour = 8),
            tx(transport.id, hour = 18),
        )
        val ranked = CategoryRanker.rank(
            categories = listOf(groceries, coffee, transport),
            kind = CategoryKind.EXPENSE,
            merchant = null,
            hint = null,
            merchantMemory = emptyMap(),
            recent = recent,
            now = now,
        )
        assertThat(ranked.first().id).isEqualTo(coffee.id)
    }

    @Test
    fun synonymHintRanksGroceries() {
        val ranked = CategoryRanker.rank(
            categories = listOf(groceries, coffee, transport),
            kind = CategoryKind.EXPENSE,
            merchant = null,
            hint = "supermercato",
            merchantMemory = emptyMap(),
            recent = emptyList(),
            now = now,
        )
        assertThat(ranked.first().id).isEqualTo(groceries.id)
    }

    private fun tx(categoryId: Long, hour: Int) = Transaction(
        id = categoryId * 10 + hour,
        kind = TransactionKind.EXPENSE,
        amount = Money(100, CurrencyCode("EUR")),
        accountId = 1,
        categoryId = categoryId,
        occurredAt = LocalDateTime.of(2026, 11, 17, hour, 0),
    )
}
