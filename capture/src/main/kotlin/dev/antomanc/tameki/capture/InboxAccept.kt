package dev.antomanc.tameki.capture

import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.CategoryMatcher
import dev.antomanc.tameki.core.model.entity.InboxStatus
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.InboxRepository
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first

/**
 * Accepts a PENDING inbox item: creates a transaction with [CategoryMatcher] suggestion
 * and merchant memory, then marks the item ACCEPTED.
 */
class InboxAccept(
    private val inbox: InboxRepository,
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val accounts: AccountRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    suspend fun accept(itemId: Long): Boolean {
        val item = inbox.get(itemId) ?: return false
        if (item.status != InboxStatus.PENDING) return false
        val amount = item.draft.amount ?: return false
        val accountId = resolveAccount(item.draft) ?: return false
        val categoryList = categories.observeAll().first()
        val memory = categories.observeMerchantCategoryMemory().first()
        val categoryId = CategoryMatcher.match(
            hint = item.draft.categoryHint,
            categories = categoryList,
            merchantHistory = memory,
            merchant = item.draft.merchant,
        )
        val merchant = item.draft.merchant
        transactions.upsert(
            Transaction(
                kind = item.draft.kind,
                amount = amount,
                accountId = accountId,
                categoryId = categoryId,
                merchant = merchant,
                note = item.draft.note,
                occurredAt = item.draft.occurredAt
                    ?: LocalDateTime.ofInstant(item.receivedAt, clock.zone),
                source = item.origin.takeIf { it != TransactionSource.MANUAL }
                    ?: TransactionSource.NOTIFICATION,
            ),
        )
        if (!merchant.isNullOrBlank() && categoryId != null) {
            categories.rememberMerchantCategory(merchant, categoryId)
        }
        inbox.setStatus(itemId, InboxStatus.ACCEPTED)
        return true
    }

    suspend fun dismiss(itemId: Long) {
        inbox.setStatus(itemId, InboxStatus.DISMISSED)
    }

    private suspend fun resolveAccount(draft: TransactionDraft): Long? {
        val all = accounts.observeAll().first().filter { !it.archived }
        if (all.isEmpty()) return null
        val hint = draft.accountHint?.trim()
        if (!hint.isNullOrBlank()) {
            all.firstOrNull { it.name.equals(hint, ignoreCase = true) }?.id?.let { return it }
        }
        return preferredAccount(all)?.id
    }

    companion object {
        fun preferredAccount(accounts: List<Account>): Account? =
            accounts.firstOrNull { it.type == AccountType.CARD }
                ?: accounts.firstOrNull { it.type == AccountType.BANK }
                ?: accounts.firstOrNull()
    }
}
