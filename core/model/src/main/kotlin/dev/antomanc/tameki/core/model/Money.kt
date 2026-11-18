package dev.antomanc.tameki.core.model

/** ISO 4217 currency code, upper case (EUR, USD, JPY). */
@JvmInline
value class CurrencyCode(val value: String) {
    init {
        require(value.length == 3 && value.all { it in 'A'..'Z' }) { "Not an ISO 4217 code: $value" }
    }
}

/**
 * An amount in minor units of [currency] (cents for EUR, whole units for JPY).
 * Never a binary floating point type: money math has to be exact.
 */
data class Money(val minor: Long, val currency: CurrencyCode) {
    val isZero: Boolean get() = minor == 0L

    operator fun plus(other: Money): Money = Money(Math.addExact(minor, other.minorIn(currency)), currency)

    operator fun minus(other: Money): Money = Money(Math.subtractExact(minor, other.minorIn(currency)), currency)

    operator fun times(factor: Int): Money = Money(Math.multiplyExact(minor, factor.toLong()), currency)

    fun negate(): Money = Money(Math.negateExact(minor), currency)

    private fun minorIn(target: CurrencyCode): Long {
        require(target == currency) { "Cannot combine $currency and $target" }
        return minor
    }

    companion object {
        fun zero(currency: CurrencyCode): Money = Money(0L, currency)
    }
}
