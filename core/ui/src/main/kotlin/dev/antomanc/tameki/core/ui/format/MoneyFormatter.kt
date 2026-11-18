package dev.antomanc.tameki.core.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** How the sign of an amount is rendered. */
enum class MoneySign {
    /** The locale's minus for negative amounts, nothing for positive ones. */
    AUTO,

    /** A plus in front of positive amounts: "the money came in". */
    EXPLICIT,

    /** No sign at all: the value is a magnitude (a total, a budget). */
    NONE,
}

/** Where the currency symbol sits relative to the number. */
enum class CurrencyPlacement {
    /** "€1.284,50". Tameki's default, and what DESIGN.md shows. */
    PREFIX,

    /** "1.284,50 €", for markets where that reads as normal. */
    SUFFIX,
}

/**
 * An amount split into the pieces [AmountText] styles independently: the symbol is smaller and
 * lighter, the minor units can be dimmed, and the integer keeps the locale's grouping.
 */
data class FormattedMoney(
    /** "", "-" or "+". */
    val sign: String,
    /** The locale's currency symbol, e.g. "€", "US$", "CHF". */
    val symbol: String,
    /** Whole units, grouped: "1.284". */
    val integer: String,
    /** Minor units without the separator: "50", or null when the currency has no minor units. */
    val fraction: String?,
    /** The locale's decimal separator: "," in Italian, "." in English. */
    val decimalSeparator: String,
    /** True when the symbol follows the number. */
    val symbolAfter: Boolean,
) {
    /** The full amount as a string, identical to what TalkBack reads. */
    val text: String
        get() = buildString {
            if (symbolAfter) {
                append(sign)
                append(integer)
                appendFraction()
                if (symbol.isNotEmpty()) {
                    append('\u00A0')
                    append(symbol)
                }
            } else {
                append(sign)
                append(symbol)
                append(integer)
                appendFraction()
            }
        }

    private fun StringBuilder.appendFraction() {
        if (fraction != null) {
            append(decimalSeparator)
            append(fraction)
        }
    }

    /** True when the minor units are only zeros, so a hero figure may omit them honestly. */
    val hasZeroFraction: Boolean
        get() = fraction == null || fraction.all { it == '0' || it == '٠' }
}

/**
 * Locale-aware money formatting for every screen. Amounts are never built by hand: grouping,
 * decimal separator, currency symbol and sign all come from the locale, and money never passes
 * through a floating point type.
 *
 * ```kotlin
 * val f = MoneyFormatter(Locale.ITALY)
 * f.format(Money(128450, CurrencyCode("EUR"))) // "€1.284,50"
 * ```
 */
class MoneyFormatter(
    val locale: Locale = Locale.getDefault(),
    val placement: CurrencyPlacement = CurrencyPlacement.PREFIX,
) {
    private val formats = mutableMapOf<Int, DecimalFormat>()

    /** The locale's decimal separator: the numpad's decimal key must match it. */
    val decimalSeparator: Char
        get() = DecimalFormatSymbols.getInstance(locale).decimalSeparator

    /** How many minor-unit digits [currency] uses (2 for EUR, 0 for JPY). */
    fun fractionDigits(currency: CurrencyCode): Int =
        currencyOrNull(currency)?.defaultFractionDigits?.coerceAtLeast(0) ?: 2

    /**
     * The formatted amount as one string. With [omitZeroFraction], a whole amount drops its ",00"
     * so a chart label stays short; real minor units are never hidden.
     */
    fun format(
        money: Money,
        sign: MoneySign = MoneySign.AUTO,
        omitZeroFraction: Boolean = false,
    ): String {
        val parts = parts(money, sign)
        return if (omitZeroFraction && parts.hasZeroFraction) parts.copy(fraction = null).text else parts.text
    }

    /**
     * The amount rounded to whole currency units and formatted as one string: **€1.285**, never
     * €1.284,50. Chart labels and dense tables read as one row of numbers only when every label
     * has the same shape, so these figures never show cents. Rounding is half-even, like money
     * everywhere else; real cents reappear wherever the exact amount matters.
     */
    fun formatWhole(money: Money, sign: MoneySign = MoneySign.AUTO): String {
        val currency = currencyOrNull(money.currency)
        val digits = currency?.defaultFractionDigits?.coerceAtLeast(0) ?: 2
        val symbol = formattedSymbol(money.currency)
        val units = BigDecimal.valueOf(money.minor, digits)
            .abs()
            .setScale(0, RoundingMode.HALF_EVEN)
        val grouped = formatFor(0).format(units)
        val signText = when {
            money.minor < 0 && sign != MoneySign.NONE -> "-"
            money.minor > 0 && sign == MoneySign.EXPLICIT -> "+"
            else -> ""
        }
        return if (placement == CurrencyPlacement.SUFFIX) {
            "$signText$grouped\u00A0$symbol"
        } else {
            "$signText$symbol$grouped"
        }
    }

    /**
     * The amount split for [AmountText]. Every call is pure with respect to [Money]: the same
     * amount always formats the same way in the same locale.
     */
    fun parts(money: Money, sign: MoneySign = MoneySign.AUTO): FormattedMoney {
        val currency = currencyOrNull(money.currency)
        val digits = currency?.defaultFractionDigits?.coerceAtLeast(0) ?: 2
        val symbol = formattedSymbol(money.currency)
        val format = formatFor(digits)
        val absolute = BigDecimal.valueOf(money.minor, digits).abs()
        val formatted = format.format(absolute)
        val separator = format.decimalFormatSymbols.decimalSeparator.toString()

        val (integer, fraction) = if (digits == 0) {
            formatted to null
        } else {
            val index = formatted.lastIndexOf(separator)
            if (index >= 0) {
                formatted.substring(0, index) to formatted.substring(index + 1)
            } else {
                formatted to "0".repeat(digits)
            }
        }

        val signText = when {
            money.minor < 0 && sign != MoneySign.NONE -> "-"
            money.minor > 0 && sign == MoneySign.EXPLICIT -> "+"
            else -> ""
        }

        return FormattedMoney(
            sign = signText,
            symbol = symbol,
            integer = integer,
            fraction = fraction,
            decimalSeparator = separator,
            symbolAfter = placement == CurrencyPlacement.SUFFIX,
        )
    }

    private fun currencyOrNull(code: CurrencyCode): Currency? =
        runCatching { Currency.getInstance(code.value) }.getOrNull()

    private fun formattedSymbol(code: CurrencyCode): String =
        (currencyOrNull(code)?.getSymbol(locale) ?: code.value)
            .replace('\u00A0', ' ')
            .trim()

    private fun formatFor(digits: Int): DecimalFormat = synchronized(formats) {
        formats.getOrPut(digits) {
            (NumberFormat.getNumberInstance(locale) as DecimalFormat).apply {
                isGroupingUsed = true
                minimumIntegerDigits = 1
                minimumFractionDigits = digits
                maximumFractionDigits = digits
            }
        }
    }
}

/** A [MoneyFormatter] for the current configuration's locale. */
@Composable
fun rememberMoneyFormatter(): MoneyFormatter {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    return remember(locale) { MoneyFormatter(locale) }
}
