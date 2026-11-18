package dev.antomanc.tameki.core.ui.format

import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Category
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * The one line a transaction row can always show. A merchant is the best title; without one the
 * category, the account, the note and finally the time of day still say what the row is. A bare
 * dash is never a title: it makes the row look broken rather than anonymous.
 */
object TransactionTitle {
    fun of(
        merchant: String?,
        category: Category?,
        account: Account?,
        note: String?,
        occurredAt: LocalDateTime,
        locale: Locale = Locale.getDefault(),
    ): String {
        merchant?.takeIf { it.isNotBlank() }?.let { return it }
        category?.resolvedName()?.takeIf { it.isNotBlank() }?.let { return it }
        account?.resolvedName()?.takeIf { it.isNotBlank() }?.let { return it }
        note?.takeIf { it.isNotBlank() }?.let { return it }
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(locale)
            .format(occurredAt)
    }
}
