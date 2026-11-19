package dev.antomanc.tameki.opening

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.entity.countsAsIncome
import java.time.LocalDateTime
import org.junit.Test

class OpeningBalanceTest {
    @Test
    fun `an opening balance is money in but never counted as income`() {
        val tx = openingBalanceTransaction(
            amount = Money(300_000, CurrencyCode("EUR")),
            accountId = 7,
            occurredAt = LocalDateTime.of(2026, 11, 18, 9, 0),
            note = "Opening balance",
        )

        assertThat(tx.kind).isEqualTo(TransactionKind.INCOME)
        assertThat(tx.source).isEqualTo(TransactionSource.OPENING_BALANCE)
        assertThat(tx.accountId).isEqualTo(7)
        assertThat(tx.countsAsIncome()).isFalse()
    }
}
