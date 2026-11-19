package dev.antomanc.tameki.core.model.account

import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind

/** Current computed balance for one account in its native currency. */
data class AccountBalance(
    val account: Account,
    val balance: Money,
    val movementCount: Int,
)

/**
 * Pure per-account balance ledger.
 *
 * Opening balances are stored as [TransactionKind.INCOME] with `TransactionSource.OPENING_BALANCE`,
 * so they naturally initialize the account balance without special-casing. Transfers subtract from
 * `accountId` and credit `toAccountId` when both share the currency.
 */
object AccountBalanceCalculator {
    fun compute(
        accounts: List<Account>,
        transactions: List<Transaction>,
    ): List<AccountBalance> {
        val active = accounts.filterNot { it.archived }.sortedBy { it.sortOrder }
        if (active.isEmpty()) return emptyList()

        val minorTotals = active.associate { it.id to 0L }.toMutableMap()
        val counts = active.associate { it.id to 0 }.toMutableMap()
        val currencyById = active.associate { it.id to it.currency }

        for (tx in transactions) {
            val fromCurrency = currencyById[tx.accountId]
            if (fromCurrency != null && tx.amount.currency == fromCurrency) {
                val delta = when (tx.kind) {
                    TransactionKind.INCOME -> tx.amount.minor
                    TransactionKind.EXPENSE, TransactionKind.TRANSFER -> -tx.amount.minor
                }
                minorTotals[tx.accountId] = (minorTotals[tx.accountId] ?: 0L) + delta
                counts[tx.accountId] = (counts[tx.accountId] ?: 0) + 1
            }

            if (tx.kind == TransactionKind.TRANSFER) {
                val toId = tx.toAccountId
                val toCurrency = toId?.let { currencyById[it] }
                if (toId != null && toCurrency != null && tx.amount.currency == toCurrency) {
                    minorTotals[toId] = (minorTotals[toId] ?: 0L) + tx.amount.minor
                    counts[toId] = (counts[toId] ?: 0) + 1
                }
            }
        }

        return active.map { account ->
            AccountBalance(
                account = account,
                balance = Money(minorTotals[account.id] ?: 0L, account.currency),
                movementCount = counts[account.id] ?: 0,
            )
        }
    }
}
