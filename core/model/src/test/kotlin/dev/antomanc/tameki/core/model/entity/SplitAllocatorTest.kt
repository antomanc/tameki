package dev.antomanc.tameki.core.model.entity

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import org.junit.Test

class SplitAllocatorTest {
    private val eur = CurrencyCode("EUR")

    private fun split(categoryId: Long, minor: Long, currency: CurrencyCode = eur) =
        Split(categoryId = categoryId, amount = Money(minor, currency))

    @Test
    fun `proportions survive an amount change and the result still sums to the total`() {
        val splits = listOf(split(1, 600), split(2, 400))

        val scaled = SplitAllocator.scaleTo(splits, Money(10_500, eur))

        assertThat(scaled.map { it.amount.minor }).containsExactly(6_300L, 4_200L).inOrder()
        assertThat(scaled.sumOf { it.amount.minor }).isEqualTo(10_500L)
        assertThat(scaled.map { it.categoryId }).containsExactly(1L, 2L).inOrder()
    }

    @Test
    fun `rounding cents go to the largest remainders so the sum is exact`() {
        val splits = listOf(split(1, 100), split(2, 100), split(3, 100))

        val scaled = SplitAllocator.scaleTo(splits, Money(100, eur))

        assertThat(scaled.sumOf { it.amount.minor }).isEqualTo(100L)
        assertThat(scaled.map { it.amount.minor }).containsExactly(34L, 33L, 33L).inOrder()
    }

    @Test
    fun `an unchanged total is returned untouched`() {
        val splits = listOf(split(1, 600), split(2, 400))

        val scaled = SplitAllocator.scaleTo(splits, Money(1_000, eur))

        assertThat(scaled).isSameInstanceAs(splits)
    }

    @Test
    fun `a currency change keeps the proportions in the new currency`() {
        val usd = CurrencyCode("USD")
        val splits = listOf(split(1, 600), split(2, 400))

        val scaled = SplitAllocator.scaleTo(splits, Money(2_000, usd))

        assertThat(scaled.map { it.amount.currency }).containsExactly(usd, usd)
        assertThat(scaled.sumOf { it.amount.minor }).isEqualTo(2_000L)
        assertThat(scaled.map { it.amount.minor }).containsExactly(1_200L, 800L).inOrder()
    }

    @Test
    fun `a zero or negative total never invents a breakdown`() {
        val splits = listOf(split(1, 600), split(2, 400))

        assertThat(SplitAllocator.scaleTo(splits, Money(0, eur))).isSameInstanceAs(splits)
        assertThat(SplitAllocator.scaleTo(splits, Money(-100, eur))).isSameInstanceAs(splits)
        assertThat(SplitAllocator.scaleTo(emptyList(), Money(1_000, eur))).isEmpty()
    }
}
