package dev.antomanc.tameki.core.model.money

import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Test

class CurrencyCatalogTest {
    @Test
    fun popularComeFirstThenAlphabetical() {
        val options = CurrencyCatalog.options(Locale.ITALY, preferred = "EUR")
        assertThat(options.first().code).isEqualTo("EUR")
        assertThat(options.map { it.code }.take(3)).containsAtLeast("EUR", "USD", "GBP")
        assertThat(options.none { it.code == "ALK" }).isTrue()
        assertThat(options.none { it.code == "ARL" }).isTrue()
        assertThat(options.none { it.code == "ARM" }).isTrue()
    }

    @Test
    fun displayNamesAreCapitalised() {
        val euro = CurrencyCatalog.options(Locale.ITALY).first { it.code == "EUR" }
        assertThat(euro.name.first().isUpperCase()).isTrue()
        assertThat(euro.name.lowercase(Locale.ITALY)).isEqualTo("euro")
    }

    @Test
    fun searchMatchesCodeNameAndSymbol() {
        val usd = CurrencyCatalog.options(Locale.US).first { it.code == "USD" }
        assertThat(CurrencyCatalog.matchesQuery(usd, "usd")).isTrue()
        assertThat(CurrencyCatalog.matchesQuery(usd, "dollar")).isTrue()
        assertThat(CurrencyCatalog.matchesQuery(usd, usd.symbol)).isTrue()
        assertThat(CurrencyCatalog.matchesQuery(usd, "zzz")).isFalse()
    }
}
