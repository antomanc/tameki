package dev.antomanc.tameki.feature.activity

import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.CategoryMatcher
import dev.antomanc.tameki.core.model.entity.DefaultCategories
import dev.antomanc.tameki.core.model.entity.Transaction
import java.time.LocalDateTime

/**
 * Ranks category chips for the quick-add sheet.
 *
 * Order signals (highest first):
 * 1. [CategoryMatcher] hit from merchant memory or free-text hint
 * 2. Merchant → category memory (exact)
 * 3. Frequency in recent transactions
 * 4. Same hour-of-day affinity in recent history
 * 5. Fixed commonness order when there is no history
 * 6. Stable name order as a last tie-break
 */
object CategoryRanker {
    /**
     * [categories] in display order and the one category, if any, that is backed by a real
     * signal: a merchant/hint match or a remembered merchant. A null [Suggestion.suggestedId]
     * means the ranking is only a layout order, never a preselection.
     */
    data class Suggestion(
        val categories: List<Category>,
        val suggestedId: Long?,
    )

    fun rank(
        categories: List<Category>,
        kind: CategoryKind,
        merchant: String?,
        hint: String?,
        merchantMemory: Map<String, Long>,
        recent: List<Transaction>,
        now: LocalDateTime,
    ): List<Category> = rankWithSuggestion(
        categories = categories,
        kind = kind,
        merchant = merchant,
        hint = hint,
        merchantMemory = merchantMemory,
        recent = recent,
        now = now,
    ).categories

    fun rankWithSuggestion(
        categories: List<Category>,
        kind: CategoryKind,
        merchant: String?,
        hint: String?,
        merchantMemory: Map<String, Long>,
        recent: List<Transaction>,
        now: LocalDateTime,
    ): Suggestion {
        val candidates = categories.filter { !it.archived && it.kind == kind }
        if (candidates.isEmpty()) return Suggestion(emptyList(), null)

        val matchedId = CategoryMatcher.match(
            hint = hint,
            categories = candidates,
            merchantHistory = merchantMemory,
            merchant = merchant,
        )
        val memoryId = merchant
            ?.takeIf { it.isNotBlank() }
            ?.let { m ->
                merchantMemory.entries.firstOrNull { it.key.equals(m, ignoreCase = true) }?.value
            }

        val frequency = LinkedHashMap<Long, Int>()
        val hourHits = LinkedHashMap<Long, Int>()
        val hour = now.hour
        for (tx in recent) {
            val id = tx.categoryId ?: continue
            frequency[id] = (frequency[id] ?: 0) + 1
            if (tx.occurredAt.hour == hour) {
                hourHits[id] = (hourHits[id] ?: 0) + 1
            }
        }
        val hasHistory = frequency.isNotEmpty() || matchedId != null || memoryId != null
        val commonness = DefaultCategories.COMMONNESS_ORDER.withIndex()
            .associate { (index, key) -> key to (DefaultCategories.COMMONNESS_ORDER.size - index) }

        val ranked = candidates.sortedWith(
            compareByDescending<Category> { cat ->
                when {
                    matchedId != null && cat.id == matchedId -> 1_000_000
                    memoryId != null && cat.id == memoryId -> 900_000
                    else -> 0
                } + (frequency[cat.id] ?: 0) * 100 + (hourHits[cat.id] ?: 0) * 10 +
                    if (!hasHistory) {
                        (cat.seedKey?.let { commonness[it] } ?: 0)
                    } else {
                        0
                    }
            }.thenBy { it.resolvedName().lowercase() },
        )
        val suggested = (matchedId ?: memoryId)
            ?.takeIf { id -> ranked.any { it.id == id } }
        return Suggestion(ranked, suggested)
    }
}
