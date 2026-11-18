package dev.antomanc.tameki.core.data.exchange

import dev.antomanc.tameki.core.data.db.dao.ExchangeRateDao
import dev.antomanc.tameki.core.data.db.entity.ExchangeRateEntity
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.money.Currencies
import dev.antomanc.tameki.core.model.money.ExchangeRates
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Manual user rates (preferred) plus optional ECB daily rates from api.frankfurter.dev.
 * Cache lives in Room; network failures leave the last good cache and never throw to UI callers of [convert].
 */
class FrankfurterExchangeRates(
    private val dao: ExchangeRateDao,
    private val httpClient: HttpClient = defaultClient(),
    private val baseUrl: String = "https://api.frankfurter.dev",
) : ExchangeRates {
    override suspend fun convert(money: Money, to: CurrencyCode, on: LocalDate): Money? {
        if (money.currency == to) return money
        val rate = findRate(money.currency, to, on) ?: return null
        val fromDigits = Currencies.fractionDigits(money.currency)
        val toDigits = Currencies.fractionDigits(to)
        val major = BigDecimal.valueOf(money.minor, fromDigits)
        val converted = major.multiply(rate).setScale(toDigits, RoundingMode.HALF_EVEN)
        val minor = converted.movePointRight(toDigits).longValueExact()
        return Money(minor, to)
    }

    override suspend fun setManualRate(from: CurrencyCode, to: CurrencyCode, rate: String) {
        val normalized = BigDecimal(rate.trim()).stripTrailingZeros()
        require(normalized > BigDecimal.ZERO) { "rate must be positive" }
        dao.upsert(
            ExchangeRateEntity(
                baseCurrency = from.value,
                quoteCurrency = to.value,
                rateDate = LocalDate.of(1970, 1, 1),
                rate = normalized.toPlainString(),
                source = ExchangeRateEntity.SOURCE_MANUAL,
            ),
        )
        // Inverse for convenience when converting the other way without a second entry.
        val inverse = BigDecimal.ONE.divide(normalized, 12, RoundingMode.HALF_EVEN).stripTrailingZeros()
        dao.upsert(
            ExchangeRateEntity(
                baseCurrency = to.value,
                quoteCurrency = from.value,
                rateDate = LocalDate.of(1970, 1, 1),
                rate = inverse.toPlainString(),
                source = ExchangeRateEntity.SOURCE_MANUAL,
            ),
        )
    }

    override suspend fun refreshRemoteRates(base: CurrencyCode): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val response: FrankfurterResponse = httpClient.get("$baseUrl/v1/latest") {
                url { parameters.append("base", base.value) }
            }.body()
            val date = LocalDate.parse(response.date)
            val entities = response.rates.map { (quote, rate) ->
                ExchangeRateEntity(
                    baseCurrency = response.base,
                    quoteCurrency = quote,
                    rateDate = date,
                    rate = rate.toString(),
                    source = ExchangeRateEntity.SOURCE_ECB,
                )
            }
            dao.upsertAll(entities)
            true
        }.getOrDefault(false)
    }

    private suspend fun findRate(from: CurrencyCode, to: CurrencyCode, on: LocalDate): BigDecimal? {
        dao.findRate(from.value, to.value, on)?.let { return BigDecimal(it.rate) }
        // Cross via EUR if both legs exist.
        if (from.value != "EUR" && to.value != "EUR") {
            val toEur = dao.findRate(from.value, "EUR", on)?.let { BigDecimal(it.rate) }
                ?: dao.findRate("EUR", from.value, on)?.let { BigDecimal.ONE.divide(BigDecimal(it.rate), 12, RoundingMode.HALF_EVEN) }
            val fromEur = dao.findRate("EUR", to.value, on)?.let { BigDecimal(it.rate) }
            if (toEur != null && fromEur != null) {
                return toEur.multiply(fromEur)
            }
        }
        return null
    }

    companion object {
        fun defaultClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}

@Serializable
internal data class FrankfurterResponse(
    val amount: Double = 1.0, // money-ok: FX API payload
    val base: String,
    val date: String,
    val rates: Map<String, Double>, // money-ok: FX API payload
)

/** Deterministic rates for unit tests — no network, no Room required if seeded. */
class FakeExchangeRates(
    private val rates: MutableMap<Pair<String, String>, String> = mutableMapOf(),
) : ExchangeRates {
    override suspend fun convert(money: Money, to: CurrencyCode, on: LocalDate): Money? {
        if (money.currency == to) return money
        val rate = rates[money.currency.value to to.value]?.let { BigDecimal(it) } ?: return null
        val fromDigits = Currencies.fractionDigits(money.currency)
        val toDigits = Currencies.fractionDigits(to)
        val major = BigDecimal.valueOf(money.minor, fromDigits)
        val converted = major.multiply(rate).setScale(toDigits, RoundingMode.HALF_EVEN)
        return Money(converted.movePointRight(toDigits).longValueExact(), to)
    }

    override suspend fun setManualRate(from: CurrencyCode, to: CurrencyCode, rate: String) {
        rates[from.value to to.value] = rate
        val inverse = BigDecimal.ONE.divide(BigDecimal(rate), 12, RoundingMode.HALF_EVEN).toPlainString()
        rates[to.value to from.value] = inverse
    }

    override suspend fun refreshRemoteRates(base: CurrencyCode): Boolean = true
}
