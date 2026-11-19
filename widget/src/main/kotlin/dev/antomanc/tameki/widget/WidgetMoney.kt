package dev.antomanc.tameki.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.coroutines.runCatchingCancellable
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Warm paper surfaces matching DESIGN.md / core:ui static palette. */
internal object WidgetPaper {
    private val surfaceLight = Color(0xFFF2F0EA)
    private val surfaceDark = Color(0xFF141513)
    private val inkLight = Color(0xFF1C1D1A)
    private val inkDark = Color(0xFFECEAE3)
    private val mutedLight = Color(0xFF5B5A52)
    private val mutedDark = Color(0xFFB9B7AC)
    private val amberLight = Color(0xFFB7791F)
    private val amberDark = Color(0xFFF2B544)
    private val amberInkLight = Color(0xFF8A5514)
    private val amberInkDark = Color(0xFFF2B544)
    private val containerLight = Color(0xFFD6E7D9)
    private val containerDark = Color(0xFF24352B)
    private val onContainerLight = Color(0xFF16301F)
    private val onContainerDark = Color(0xFFA9CDB3)

    fun surface(): ColorProvider = ColorProvider(day = surfaceLight, night = surfaceDark)
    fun ink(): ColorProvider = ColorProvider(day = inkLight, night = inkDark)
    fun muted(): ColorProvider = ColorProvider(day = mutedLight, night = mutedDark)
    fun amber(): ColorProvider = ColorProvider(day = amberLight, night = amberDark)
    /** Small amber labels clear 4.5:1 on paper; decorative amber keeps [amber]. */
    fun amberInk(): ColorProvider = ColorProvider(day = amberInkLight, night = amberInkDark)
    fun primaryContainer(): ColorProvider = ColorProvider(day = containerLight, night = containerDark)
    fun onPrimaryContainer(): ColorProvider = ColorProvider(day = onContainerLight, night = onContainerDark)
}

/** Locale-aware amount string with tabular-friendly grouping (AmountText-like). */
internal fun formatWidgetMoney(money: Money, locale: Locale = Locale.getDefault()): String {
    val currency = runCatchingCancellable { Currency.getInstance(money.currency.value) }.getOrNull()
    val digits = currency?.defaultFractionDigits ?: 2
    val format = NumberFormat.getCurrencyInstance(locale).apply {
        if (currency != null) this.currency = currency
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }
    val major = BigDecimal.valueOf(money.minor).movePointLeft(digits).setScale(digits, RoundingMode.UNNECESSARY)
    return format.format(major)
}
