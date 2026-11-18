package dev.antomanc.tameki.feature.subscriptions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Worth
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** The locale the screen is drawing in; every date and number follows it. */
@Composable
internal fun rememberLocale(): Locale {
    val configuration = LocalConfiguration.current
    return remember(configuration) { configuration.locales[0] }
}

/** Dates as the locale writes them ("18 nov 2026"), never ISO. */
@Composable
internal fun rememberDateFormatter(): DateTimeFormatter {
    val locale = rememberLocale()
    return remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
}

/**
 * "today", "tomorrow", "in 3 days", "2 days late" — facts, short, localised. The screen owns this
 * because it is the only layer that has string resources.
 */
@Composable
internal fun relativeDateLabel(date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days < 0L -> {
            val late = (-days).toInt()
            pluralStringResource(R.plurals.subs_relative_days_late, late, late)
        }
        days == 0L -> stringResource(R.string.subs_relative_today)
        days == 1L -> stringResource(R.string.subs_relative_tomorrow)
        else -> {
            val ahead = days.toInt()
            pluralStringResource(R.plurals.subs_relative_in_days, ahead, ahead)
        }
    }
}

/** "every month", "every 3 months", "every year" — the billing cycle in words. */
@Composable
internal fun cycleLabel(cycle: Cycle): String = when (cycle.unit) {
    CycleUnit.MONTH -> pluralStringResource(R.plurals.subs_cycle_every_months, cycle.every, cycle.every)
    CycleUnit.YEAR -> pluralStringResource(R.plurals.subs_cycle_every_years, cycle.every, cycle.every)
    CycleUnit.WEEK -> pluralStringResource(R.plurals.subs_cycle_every_weeks, cycle.every, cycle.every)
    CycleUnit.DAY -> pluralStringResource(R.plurals.subs_cycle_every_days, cycle.every, cycle.every)
}

@Composable
internal fun typeLabel(type: RecurringType): String = stringResource(
    when (type) {
        RecurringType.SUBSCRIPTION -> R.string.subs_type_subscription
        RecurringType.BILL -> R.string.subs_type_bill
        RecurringType.INCOME -> R.string.subs_type_income
    },
)

@Composable
internal fun statusLabel(status: RecurringStatus): String = stringResource(
    when (status) {
        RecurringStatus.CANCELLED -> R.string.subs_status_cancelled
        RecurringStatus.PAUSED -> R.string.subs_status_paused
        RecurringStatus.TRIAL -> R.string.subs_status_trial
        RecurringStatus.ACTIVE -> R.string.subs_status_active
    },
)

@Composable
internal fun worthLabel(worth: Worth?): String = stringResource(
    when (worth) {
        Worth.LOVE -> R.string.subs_worth_love
        Worth.OK -> R.string.subs_worth_ok
        Worth.RARELY -> R.string.subs_worth_rarely
        Worth.UNUSED -> R.string.subs_worth_unused
        null -> R.string.subs_worth_unrated
    },
)
