package dev.antomanc.tameki.core.ui.format

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * The clock time of a transaction, in the user's locale. Rows carry it in their quiet meta line
 * ("Carta · 18:42") so two entries on the same day stay distinguishable at a glance.
 */
object TimeOfDay {
    fun short(at: LocalDateTime, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(locale)
            .format(at)
}
