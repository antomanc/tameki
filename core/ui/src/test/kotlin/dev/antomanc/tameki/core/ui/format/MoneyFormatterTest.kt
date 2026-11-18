package dev.antomanc.tameki.core.ui.format

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import org.junit.Test
import java.util.Currency
import java.util.Locale

class MoneyFormatterTest {
    private val eur = CurrencyCode("EUR")
    private val jpy = CurrencyCode("JPY")
    private val usd = CurrencyCode("USD")

    private fun money(minor: Long, currency: CurrencyCode = eur) = Money(minor, currency)

    @Test
    fun `English groups with commas and a dot`() {
        val formatter = MoneyFormatter(Locale.US)
        assertThat(formatter.format(money(128450))).isEqualTo("€1,284.50")
    }

    @Test
    fun `Italian groups with dots and a comma`() {
        val formatter = MoneyFormatter(Locale.ITALY)
        assertThat(formatter.format(money(128450))).isEqualTo("€1.284,50")
    }

    @Test
    fun `negative amounts carry the locale minus before the symbol`() {
        val formatter = MoneyFormatter(Locale.ITALY)
        assertThat(formatter.format(money(-1230))).isEqualTo("-€12,30")
    }

    @Test
    fun `explicit sign marks incoming money`() {
        val formatter = MoneyFormatter(Locale.ITALY)
        assertThat(formatter.format(money(1230), MoneySign.EXPLICIT)).isEqualTo("+€12,30")
        assertThat(formatter.format(money(-1230), MoneySign.EXPLICIT)).isEqualTo("-€12,30")
    }

    @Test
    fun `no sign formats a magnitude even when the amount is negative`() {
        val formatter = MoneyFormatter(Locale.US)
        assertThat(formatter.format(money(-1230), MoneySign.NONE)).isEqualTo("€12.30")
    }

    @Test
    fun `zero is not signed`() {
        val formatter = MoneyFormatter(Locale.US)
        assertThat(formatter.format(money(0), MoneySign.EXPLICIT)).isEqualTo("€0.00")
    }

    @Test
    fun `currencies without minor units have no separator`() {
        val formatter = MoneyFormatter(Locale.JAPAN)
        val parts = formatter.parts(money(1284, jpy))
        assertThat(parts.fraction).isNull()
        assertThat(parts.text).doesNotContain(".")
        assertThat(formatter.fractionDigits(jpy)).isEqualTo(0)
    }

    @Test
    fun `the symbol follows the amount in suffix mode`() {
        val formatter = MoneyFormatter(Locale.ITALY, CurrencyPlacement.SUFFIX)
        assertThat(formatter.format(money(128450))).isEqualTo("1.284,50\u00A0€")
    }

    @Test
    fun `parts reassemble into the formatted string`() {
        val formatter = MoneyFormatter(Locale.ITALY)
        val parts = formatter.parts(money(128450))
        assertThat(parts.sign).isEmpty()
        assertThat(parts.symbol).isEqualTo("€")
        assertThat(parts.integer).isEqualTo("1.284")
        assertThat(parts.fraction).isEqualTo("50")
        assertThat(parts.decimalSeparator).isEqualTo(",")
        assertThat(parts.text).isEqualTo(formatter.format(money(128450)))
    }

    @Test
    fun `zero minor units are reported so hero figures can omit them honestly`() {
        val formatter = MoneyFormatter(Locale.US)
        assertThat(formatter.parts(money(1200)).hasZeroFraction).isTrue()
        assertThat(formatter.parts(money(1250)).hasZeroFraction).isFalse()
    }

    @Test
    fun `unknown currency codes fall back to the code as the symbol`() {
        val formatter = MoneyFormatter(Locale.US)
        val parts = formatter.parts(money(1200, CurrencyCode("ZZZ")))
        assertThat(parts.symbol).isEqualTo("ZZZ")
        assertThat(parts.integer).isEqualTo("12")
    }

    @Test
    fun `the decimal separator matches the locale for the numpad`() {
        assertThat(MoneyFormatter(Locale.ITALY).decimalSeparator).isEqualTo(',')
        assertThat(MoneyFormatter(Locale.US).decimalSeparator).isEqualTo('.')
    }

    @Test
    fun `grouping survives four and seven digit amounts`() {
        val formatter = MoneyFormatter(Locale.ITALY)
        assertThat(formatter.format(money(100000))).isEqualTo("€1.000,00")
        assertThat(formatter.format(money(123456789))).isEqualTo("€1.234.567,89")
    }

    @Test
    fun `the dollar symbol is the one the locale knows`() {
        val italian = MoneyFormatter(Locale.ITALY)
        val expected = Currency.getInstance("USD").getSymbol(Locale.ITALY)
        assertThat(italian.format(money(500, usd))).startsWith(expected)
    }
}
