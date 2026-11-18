package dev.antomanc.tameki.core.model.entity

import dev.antomanc.tameki.core.model.CurrencyCode

/** How the balance is held; drives icons and CSV column hints, not accounting rules. */
enum class AccountType {
    CASH,
    BANK,
    CARD,
    SAVINGS,
    OTHER,
}

/**
 * A place money sits (wallet, card, bank). id 0 means not yet persisted.
 * Currency is fixed per account so transfers always cross an exchange path explicitly.
 *
 * Seeded Cash/Card rows carry [seedKey] (`cash` / `card`). The UI localises unless [customName]
 * is set.
 */
data class Account(
    val id: Long = 0L,
    val name: String,
    val currency: CurrencyCode,
    val type: AccountType,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
    val seedKey: String? = null,
    val customName: String? = null,
) {
    fun resolvedName(): String = customName?.takeIf { it.isNotBlank() } ?: name
}
