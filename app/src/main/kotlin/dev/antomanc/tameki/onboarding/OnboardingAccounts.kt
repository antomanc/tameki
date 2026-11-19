package dev.antomanc.tameki.onboarding

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.DefaultAccounts
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import dev.antomanc.tameki.opening.openingBalanceTransaction
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first

internal suspend fun applyOnboardingAccounts(
    accounts: AccountRepository,
    transactions: TransactionRepository,
    drafts: List<OnboardingAccountDraft>,
    currency: CurrencyCode,
    clock: Clock,
    openingBalanceNote: String,
) {
    val existing = accounts.observeAll(includeArchived = true).first()
    val touched = mutableSetOf<Long>()
    drafts.forEachIndexed { index, draft ->
        val name = draft.name.trim().ifBlank { return@forEachIndexed }
        val seedKey = when (index) {
            0 -> DefaultAccounts.CASH
            1 -> DefaultAccounts.CARD
            else -> null
        }
        val match = when {
            seedKey != null -> existing.firstOrNull { it.seedKey == seedKey }
                ?: existing.getOrNull(index)
            else -> null
        }
        val type = when (seedKey) {
            DefaultAccounts.CASH -> AccountType.CASH
            DefaultAccounts.CARD -> AccountType.CARD
            else -> AccountType.BANK
        }
        val isDefaultLabel = seedKey != null && (
            name.equals("Cash", true) || name.equals("Contanti", true) ||
                name.equals("Card", true) || name.equals("Carta", true)
            )
        val id = accounts.upsert(
            Account(
                id = match?.id ?: 0L,
                name = name,
                currency = currency,
                type = match?.type ?: type,
                sortOrder = match?.sortOrder ?: index,
                seedKey = seedKey ?: match?.seedKey,
                customName = if (isDefaultLabel) null else name,
            ),
        )
        touched += id
        val balance = draft.balance
        if (balance != null && balance.minor > 0L) {
            transactions.upsert(
                openingBalanceTransaction(
                    amount = balance.copy(currency = currency),
                    accountId = id,
                    occurredAt = LocalDateTime.now(clock),
                    note = openingBalanceNote,
                ),
            )
        }
    }
    existing.filter { it.seedKey != null && it.id !in touched }.forEach { account ->
        if (account.currency != currency) {
            accounts.upsert(account.copy(currency = currency))
        }
    }
}
