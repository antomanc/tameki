package dev.antomanc.tameki.core.model.money

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ConvertedTotalTest {
    private val eur = CurrencyCode("EUR")
    private val usd = CurrencyCode("USD")
    private val gbp = CurrencyCode("GBP")
    private val day = LocalDate.of(2026, 11, 18)

    @Test
    fun convertsWhenRateExistsAndKeepsUnconverted() = runTest {
        val rates = object : ExchangeRates {
            override suspend fun convert(money: Money, to: CurrencyCode, on: LocalDate): Money? {
                if (money.currency == usd && to == eur) {
                    return Money(money.minor, eur) // 1:1 for the test
                }
                return null
            }

            override suspend fun setManualRate(
                from: CurrencyCode,
                to: CurrencyCode,
                rate: String,
                since: LocalDate,
            ) = Unit

            override suspend fun refreshRemoteRates(base: CurrencyCode): Boolean = true
        }
        val total = sumConverting(
            amounts = listOf(Money(1000, eur), Money(1250, usd), Money(500, gbp)),
            base = eur,
            on = day,
            rates = rates,
        )
        assertThat(total.converted).isEqualTo(Money(2250, eur))
        assertThat(total.unconverted).containsExactly(Money(500, gbp))
    }
}
