package dev.antomanc.tameki.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {
    private val eur = CurrencyCode("EUR")
    private val usd = CurrencyCode("USD")

    @Test
    fun addsMinorUnits() {
        assertThat(Money(125, eur) + Money(75, eur)).isEqualTo(Money(200, eur))
    }

    @Test
    fun subtractsMinorUnits() {
        assertThat(Money(200, eur) - Money(125, eur)).isEqualTo(Money(75, eur))
    }

    @Test
    fun multipliesByWholeFactor() {
        assertThat(Money(250, eur) * 4).isEqualTo(Money(1000, eur))
    }

    @Test
    fun negates() {
        assertThat(Money(125, eur).negate()).isEqualTo(Money(-125, eur))
    }

    @Test
    fun zeroAndIsZero() {
        assertThat(Money.zero(eur).isZero).isTrue()
        assertThat(Money(1, eur).isZero).isFalse()
    }

    @Test
    fun refusesToCombineDifferentCurrencies() {
        assertThrows(IllegalArgumentException::class.java) { Money(100, eur) + Money(100, usd) }
    }
}
