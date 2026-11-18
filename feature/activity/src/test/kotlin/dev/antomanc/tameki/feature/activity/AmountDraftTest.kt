package dev.antomanc.tameki.feature.activity

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import org.junit.Test

class AmountDraftTest {
    private val eur = CurrencyCode("EUR")

    @Test
    fun typesTwelveFifty() {
        var draft = AmountDraft()
        "1250".forEach { draft = draft.appendDigit(it) }
        // Without decimal: 1250 euros → 125000 cents? No — integer "1250" with 2 fraction digits = 125000
        // Users type 12,50: digits 1,2 decimal 5,0
        draft = AmountDraft()
            .appendDigit('1')
            .appendDigit('2')
            .appendDecimal()
            .appendDigit('5')
            .appendDigit('0')
        assertThat(draft.toMoney(eur).minor).isEqualTo(1250)
    }

    @Test
    fun backspaceRemovesDecimalDigits() {
        val draft = AmountDraft()
            .appendDigit('3')
            .appendDecimal()
            .appendDigit('5')
            .backspace()
        assertThat(draft.hasDecimal).isTrue()
        assertThat(draft.fraction).isEmpty()
    }

    @Test
    fun fromMoneyRoundTrips() {
        val money = AmountDraft.fromMoney(
            dev.antomanc.tameki.core.model.Money(1999, eur),
        ).toMoney(eur)
        assertThat(money.minor).isEqualTo(1999)
    }
}
