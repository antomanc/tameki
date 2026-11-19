package dev.antomanc.tameki.core.model.account

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.TransactionSource
import java.time.LocalDateTime
import org.junit.Test

class AccountBalanceCalculatorTest {
    private val eur = CurrencyCode("EUR")
    private val now = LocalDateTime.of(2026, 11, 19, 9, 30)

    @Test
    fun computesBalancesAcrossOpeningIncomeExpenseAndTransfers() {
        val cash = Account(id = 1, name = "Cash", currency = eur, type = AccountType.CASH, sortOrder = 0)
        val bank = Account(id = 2, name = "Bank", currency = eur, type = AccountType.BANK, sortOrder = 1)
        val archived = Account(id = 3, name = "Old", currency = eur, type = AccountType.CARD, archived = true, sortOrder = 2)

        val txs = listOf(
            Transaction(
                id = 1,
                kind = TransactionKind.INCOME,
                amount = Money(100_00, eur),
                accountId = 1,
                occurredAt = now,
                source = TransactionSource.OPENING_BALANCE,
            ),
            Transaction(
                id = 2,
                kind = TransactionKind.INCOME,
                amount = Money(500_00, eur),
                accountId = 2,
                occurredAt = now,
            ),
            Transaction(
                id = 3,
                kind = TransactionKind.EXPENSE,
                amount = Money(25_50, eur),
                accountId = 1,
                occurredAt = now,
            ),
            Transaction(
                id = 4,
                kind = TransactionKind.TRANSFER,
                amount = Money(150_00, eur),
                accountId = 2,
                toAccountId = 1,
                occurredAt = now,
            ),
        )

        val balances = AccountBalanceCalculator.compute(listOf(bank, archived, cash), txs)
        assertThat(balances.map { it.account.id }).containsExactly(1L, 2L).inOrder()
        // Cash: 100 - 25.50 + 150 = 224.50
        assertThat(balances[0].balance).isEqualTo(Money(224_50, eur))
        assertThat(balances[0].movementCount).isEqualTo(3)
        // Bank: 500 - 150 = 350.00
        assertThat(balances[1].balance).isEqualTo(Money(350_00, eur))
        assertThat(balances[1].movementCount).isEqualTo(2)
    }
}
