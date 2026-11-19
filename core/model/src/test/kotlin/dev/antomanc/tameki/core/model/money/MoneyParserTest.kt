package dev.antomanc.tameki.core.model.money

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import org.junit.Test

class MoneyParserTest {
    private val eur = CurrencyCode("EUR")
    private val jpy = CurrencyCode("JPY")

    @Test
    fun parsesCommaDecimal() {
        assertThat(MoneyParser.parse("12,50", eur)).isEqualTo(Money(1250, eur))
    }

    @Test
    fun parsesDotDecimal() {
        assertThat(MoneyParser.parse("12.50", eur)).isEqualTo(Money(1250, eur))
    }

    @Test
    fun parsesItalianThousands() {
        assertThat(MoneyParser.parse("1.234,56", eur)).isEqualTo(Money(123456, eur))
    }

    @Test
    fun parsesEnglishThousands() {
        assertThat(MoneyParser.parse("1,234.56", eur)).isEqualTo(Money(123456, eur))
    }

    @Test
    fun oneGroupSeparatorWithoutDecimalsIsThousands() {
        // OCR and pasted text write 1,000 or 1.000; treating the tail as cents lost a thousand.
        assertThat(MoneyParser.parse("1,000", eur)).isEqualTo(Money(100_000, eur))
        assertThat(MoneyParser.parse("1.000", eur)).isEqualTo(Money(100_000, eur))
        assertThat(MoneyParser.parse("12.500", eur)).isEqualTo(Money(1_250_000, eur))
    }

    @Test
    fun repeatedGroupSeparatorsAreThousands() {
        assertThat(MoneyParser.parse("1,234,567", eur)).isEqualTo(Money(123_456_700, eur))
        assertThat(MoneyParser.parse("1.234.567", eur)).isEqualTo(Money(123_456_700, eur))
    }

    @Test
    fun threeFractionDigitsBelongToCurrenciesThatUseThem() {
        val bhd = CurrencyCode("BHD")
        assertThat(MoneyParser.parse("1.234", bhd)).isEqualTo(Money(1234, bhd))
        assertThat(MoneyParser.parse("1,234", bhd)).isEqualTo(Money(1234, bhd))
    }

    @Test
    fun parsesEuroSymbol() {
        assertThat(MoneyParser.parse("€ 12", eur)).isEqualTo(Money(1200, eur))
    }

    @Test
    fun parsesNegative() {
        assertThat(MoneyParser.parse("-3", eur)).isEqualTo(Money(-300, eur))
    }

    @Test
    fun parsesZero() {
        assertThat(MoneyParser.parse("0", eur)).isEqualTo(Money(0, eur))
        assertThat(MoneyParser.parse("0,00", eur)).isEqualTo(Money(0, eur))
    }

    @Test
    fun jpyHasNoDecimals() {
        assertThat(MoneyParser.parse("1234", jpy)).isEqualTo(Money(1234, jpy))
        // A three-digit tail is a group in every locale that writes 1.234 or 1,234.
        assertThat(MoneyParser.parse("1.234", jpy)).isEqualTo(Money(1234, jpy))
        assertThat(MoneyParser.parse("12,50", jpy)).isEqualTo(Money(12, jpy))
    }

    @Test
    fun rejectsGarbage() {
        assertThat(MoneyParser.parse("abc", eur)).isNull()
        assertThat(MoneyParser.parse("", eur)).isNull()
    }

    @Test
    fun fractionDigitsFromJavaCurrency() {
        assertThat(Currencies.fractionDigits(eur)).isEqualTo(2)
        assertThat(Currencies.fractionDigits(jpy)).isEqualTo(0)
    }
}
