package dev.antomanc.tameki.core.data.repository

import dev.antomanc.tameki.core.model.repository.TransactionFilter
import java.time.LocalDateTime

internal data class BoundFilter(
    val fromIso: LocalDateTime?,
    val toIso: LocalDateTime?,
    val accountId: Long?,
    val categoryId: Long?,
    val source: String?,
    val tag: String?,
    val ftsQuery: String?,
)

internal fun TransactionFilter.bind(): BoundFilter {
    val fromIso = from?.atStartOfDay()
    val toIso = to?.plusDays(1)?.atStartOfDay()
    val fts = text?.trim()?.takeIf { it.isNotEmpty() }?.let { sanitizeFts(it) }
    return BoundFilter(
        fromIso = fromIso,
        toIso = toIso,
        accountId = accountId,
        categoryId = categoryId,
        source = source?.name,
        tag = tag?.trim()?.takeIf { it.isNotEmpty() },
        ftsQuery = fts,
    )
}

/**
 * Turns free text into a safe FTS4 MATCH prefix query.
 * Each token is quoted (`"token"*`) so OR/AND/NOT/`"`/`-` never break MATCH; tokens without
 * letters or digits are dropped.
 */
internal fun sanitizeFts(raw: String): String {
    val tokens = raw.split(Regex("\\s+"))
        .map { it.replace("\"", "") }
        .map { it.replace(Regex("[^\\p{L}\\p{N}_-]"), "") }
        .filter { token -> token.any { it.isLetterOrDigit() } }
        .map { token ->
            val escaped = token.replace("\"", "")
            "\"$escaped\"*"
        }
    return tokens.joinToString(" ")
}
