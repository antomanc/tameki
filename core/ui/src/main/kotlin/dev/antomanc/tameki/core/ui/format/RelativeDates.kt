package dev.antomanc.tameki.core.ui.format

import android.icu.text.RelativeDateTimeFormatter
import android.icu.text.RelativeDateTimeFormatter.AbsoluteUnit
import android.icu.text.RelativeDateTimeFormatter.Direction
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit
import android.icu.util.ULocale
import java.util.Locale

/**
 * Day words come from the platform's ICU data, never from hard-coded English: Italian gets
 * "oggi" / "ieri" / "tra 3 giorni" for free. ICU returns these phrases lowercase because they are
 * normally glued into a sentence; the first letter is title-cased here because every caller uses
 * them as a label (day header, renewal row).
 */
object RelativeDates {
    fun today(locale: Locale = Locale.getDefault()): String =
        titlecase(formatter(locale).format(Direction.THIS, AbsoluteUnit.DAY), locale)

    fun yesterday(locale: Locale = Locale.getDefault()): String =
        titlecase(formatter(locale).format(Direction.LAST, AbsoluteUnit.DAY), locale)

    fun tomorrow(locale: Locale = Locale.getDefault()): String =
        titlecase(formatter(locale).format(Direction.NEXT, AbsoluteUnit.DAY), locale)

    fun inDays(days: Int, locale: Locale = Locale.getDefault()): String = when {
        days <= 0 -> today(locale)
        days == 1 -> tomorrow(locale)
        else -> titlecase(
            formatter(locale).format(days.toDouble(), Direction.NEXT, RelativeUnit.DAYS),
            locale,
        )
    }

    private fun formatter(locale: Locale): RelativeDateTimeFormatter =
        RelativeDateTimeFormatter.getInstance(ULocale.forLocale(locale))

    private fun titlecase(text: String, locale: Locale): String =
        text.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(locale) else char.toString() }
}
