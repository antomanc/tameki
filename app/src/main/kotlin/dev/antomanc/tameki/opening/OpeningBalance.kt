package dev.antomanc.tameki.opening

import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.TransactionSource
import java.time.LocalDateTime

/**
 * The balance a user already had, booked once at onboarding. It is money in for the journal, but
 * its source keeps it out of every earnings pool: a bank balance is not this month's income.
 */
internal fun openingBalanceTransaction(
    amount: Money,
    accountId: Long,
    occurredAt: LocalDateTime,
    note: String,
): Transaction = Transaction(
    kind = TransactionKind.INCOME,
    amount = amount,
    accountId = accountId,
    note = note,
    occurredAt = occurredAt,
    source = TransactionSource.OPENING_BALANCE,
)
