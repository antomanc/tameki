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
    /** The locale's currency symbol or ISO code, e.g. "€", "USD". */
    val symbol: String,
    /** Whole units, grouped: "1.284". */
    val integer: String,
    /** Minor units without the separator: "50", or null when the currency has no minor units. */
    val fraction: String?,
    /** The locale's decimal separator: "," in Italian, "." in English. */
    val decimalSeparator: String,
    /** True when the symbol follows the number. */
    val symbolAfter: Boolean,
    /**
     * When true, a narrow no-break space separates an ISO code from the number
     * ("USD 12,50") even in PREFIX mode.
     */
    val spaceAfterSymbol: Boolean = false,
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
                if (spaceAfterSymbol && symbol.isNotEmpty()) append('\u202F')
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
 * Locale-aware money formatting for every screen.
 *
 * Base-currency amounts keep the usual symbol layout. Foreign amounts use the locale's symbol
 * when it uniquely identifies the currency; otherwise the ISO code plus a narrow no-break space
 * ("USD 12,50"), never "USD12,50".
 */
class MoneyFormatter(
    val locale: Locale = Locale.getDefault(),
    val placement: CurrencyPlacement = CurrencyPlacement.PREFIX,
    val baseCurrency: CurrencyCode? = null,
) {
    private val formats = mutableMapOf<Int, DecimalFormat>()

    /** The locale's decimal separator: the numpad's decimal key must match it. */
    val decimalSeparator: Char
        get() = DecimalFormatSymbols.getInstance(locale).decimalSeparator

    /** How many minor-unit digits [currency] uses (2 for EUR, 0 for JPY). */
    fun fractionDigits(currency: CurrencyCode): Int =
        currencyOrNull(currency)?.defaultFractionDigits?.coerceAtLeast(0) ?: 2

    fun format(
        money: Money,
        sign: MoneySign = MoneySign.AUTO,
        omitZeroFraction: Boolean = false,
    ): String {
        val parts = parts(money, sign)
        return if (omitZeroFraction && parts.hasZeroFraction) parts.copy(fraction = null).text else parts.text
    }

    fun formatWhole(money: Money, sign: MoneySign = MoneySign.AUTO): String {
        val currency = currencyOrNull(money.currency)
        val digits = currency?.defaultFractionDigits?.coerceAtLeast(0) ?: 2
        val label = currencyLabel(money.currency)
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
            "$signText$grouped\u00A0${label.symbol}"
        } else if (label.spaceAfterSymbol) {
            "$signText${label.symbol}\u202F$grouped"
        } else {
            "$signText${label.symbol}$grouped"
        }
    }

    fun parts(money: Money, sign: MoneySign = MoneySign.AUTO): FormattedMoney {
        val currency = currencyOrNull(money.currency)
        val digits = currency?.defaultFractionDigits?.coerceAtLeast(0) ?: 2
        val label = currencyLabel(money.currency)
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
            symbol = label.symbol,
            integer = integer,
            fraction = fraction,
            decimalSeparator = separator,
            symbolAfter = placement == CurrencyPlacement.SUFFIX,
            spaceAfterSymbol = label.spaceAfterSymbol && placement != CurrencyPlacement.SUFFIX,
        )
    }

    private data class CurrencyLabel(val symbol: String, val spaceAfterSymbol: Boolean)

    private fun currencyLabel(code: CurrencyCode): CurrencyLabel {
        val isBase = baseCurrency == null || code == baseCurrency
        val currency = currencyOrNull(code)
        val raw = (currency?.getSymbol(locale) ?: code.value)
            .replace('\u00A0', ' ')
            .trim()
            .ifEmpty { code.value }
        // Base currency keeps the locale symbol (e.g. €). Foreign amounts use a distinctive
        // glyph when unambiguous, otherwise the ISO code + narrow no-break space.
        if (isBase) {
            val needsSpace = raw.equals(code.value, ignoreCase = true)
            return CurrencyLabel(raw, spaceAfterSymbol = needsSpace)
        }
        return if (isUnambiguousSymbol(raw, code.value)) {
            CurrencyLabel(raw, spaceAfterSymbol = false)
        } else {
            CurrencyLabel(code.value, spaceAfterSymbol = true)
        }
    }

    /**
     * A symbol is unambiguous when it is not the bare ISO code and not a shared "$"/"¤" mark.
     * "US$", "€", "£", "¥" count; "USD" and "$" do not.
     */
    private fun isUnambiguousSymbol(symbol: String, code: String): Boolean {
        if (symbol.isEmpty()) return false
        if (symbol.equals(code, ignoreCase = true)) return false
        if (symbol == "$" || symbol == "¤") return false
        // Distinctive glyph or country-prefixed mark (US$, CA$).
        if (symbol.any { !it.isLetter() }) return true
        return false
    }

    private fun currencyOrNull(code: CurrencyCode): Currency? =
        runCatching { Currency.getInstance(code.value) }.getOrNull()

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
fun rememberMoneyFormatter(baseCurrency: CurrencyCode? = null): MoneyFormatter {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    return remember(locale, baseCurrency) { MoneyFormatter(locale, baseCurrency = baseCurrency) }
}
