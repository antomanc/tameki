package dev.antomanc.tameki.core.model.money

import dev.antomanc.tameki.core.model.coroutines.runCatchingCancellable
import java.util.Currency
import java.util.Locale

/**
 * Circulating ISO 4217 currencies that have a real display name for the locale.
 * Popular codes come first; the rest are alphabetical by localised name.
 */
object CurrencyCatalog {
    val POPULAR: List<String> = listOf(
        "EUR", "USD", "GBP", "CHF", "JPY", "CNY", "CAD", "AUD",
        "SEK", "NOK", "DKK", "PLN", "CZK", "HUF", "RON", "TRY",
        "BRL", "INR", "MXN",
    )

    /** Obsolete / non-circulating codes that Java still exposes. */
    private val OBSOLETE: Set<String> = setOf(
        "ALK", "AOK", "AON", "AOR", "ARL", "ARM", "ARP", "ARY",
        "BAD", "BAN", "BGL", "BGO", "BGM", "BEC", "BEL", "BYB", "BYR",
        "CSD", "CSK", "DDM", "DEM", "ECS", "ECV", "EEK", "ESA", "ESB",
        "FIM", "FRF", "GHC", "GRD", "GWP", "HRD", "IEP", "ILP", "ILR",
        "ISJ", "ITL", "LAJ", "LTL", "LTT", "LUC", "LUF", "LUL", "LVL", "LVR",
        "MGF", "MKN", "MLF", "MZE", "MZM", "NIC", "NLG", "PEH", "PEI",
        "PLZ", "PTE", "RHD", "ROK", "ROL", "RUR", "SDD", "SDP", "SIT",
        "SKK", "SRG", "SUR", "TJR", "TMM", "TPE", "TRL", "UAK", "UGS",
        "UYN", "UYP", "VEB", "VEF", "VNN", "XEU", "YDD", "YER", "YUD",
        "YUM", "YUN", "ZAL", "ZMK", "ZRN", "ZRZ", "ZWD", "ZWL", "ZWN", "ZWR",
        "ADP", "ATS", "BEF", "CYP", "ESP", "MTL", "SML", "VAL", "XFO", "XFU",
        "CLE", "CNX", "ISJ", "KRH", "KRO", "MCF", "MVP", "SLE",
    )

    data class Option(
        val code: String,
        val name: String,
        val symbol: String,
    )

    /**
     * Currently circulating currencies with a capitalised display name for [locale].
     * [preferred] is pinned first when present; then [POPULAR] (in that order); then A–Z by name.
     */
    fun options(locale: Locale, preferred: String? = null): List<Option> {
        val available = Currency.getAvailableCurrencies()
            .asSequence()
            .filter { it.currencyCode !in OBSOLETE }
            .filter { it.defaultFractionDigits >= 0 }
            .mapNotNull { currency ->
                val code = currency.currencyCode
                val rawName = runCatchingCancellable { currency.getDisplayName(locale) }.getOrNull()
                    ?.trim()
                    .orEmpty()
                if (rawName.isEmpty() || rawName.equals(code, ignoreCase = true)) return@mapNotNull null
                val name = rawName.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(locale) else ch.toString()
                }
                val symbol = runCatchingCancellable { currency.getSymbol(locale) }.getOrNull()
                    ?.replace('\u00A0', ' ')
                    ?.trim()
                    .orEmpty()
                    .ifEmpty { code }
                Option(code = code, name = name, symbol = symbol)
            }
            .associateBy { it.code }

        val preferredCode = preferred?.uppercase(Locale.ROOT)
        val popularOrdered = buildList {
            if (preferredCode != null && preferredCode in available) add(preferredCode)
            POPULAR.forEach { code ->
                if (code != preferredCode && code in available) add(code)
            }
        }
        val rest = available.keys
            .filter { it !in popularOrdered.toSet() }
            .sortedBy { available.getValue(it).name.lowercase(locale) }

        return (popularOrdered + rest).map { available.getValue(it) }
    }

    fun matchesQuery(option: Option, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim()
        return option.code.contains(q, ignoreCase = true) ||
            option.name.contains(q, ignoreCase = true) ||
            option.symbol.contains(q, ignoreCase = true)
    }
}
