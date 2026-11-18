package dev.antomanc.tameki.core.data

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.data.db.entity.ExchangeRateEntity
import dev.antomanc.tameki.core.data.exchange.FakeExchangeRates
import dev.antomanc.tameki.core.data.exchange.FrankfurterExchangeRates
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test

class ExchangeRatesTest : DbTestRule() {
    private val eur = CurrencyCode("EUR")
    private val usd = CurrencyCode("USD")

    @Test
    fun fakeConvertsWithManualRate() = runTest {
        val rates = FakeExchangeRates()
        rates.setManualRate(eur, usd, "1.10")
        val converted = rates.convert(Money(1_000L, eur), usd, LocalDate.of(2026, 11, 18))
        assertThat(converted).isNotNull()
        assertThat(converted!!.minor).isEqualTo(1_100L)
        assertThat(converted.currency).isEqualTo(usd)
    }

    @Test
    fun roomCacheUsedByFrankfurterImpl() = runTest {
        db.exchangeRateDao().upsert(
            ExchangeRateEntity(
                baseCurrency = "EUR",
                quoteCurrency = "USD",
                rateDate = LocalDate.of(2026, 11, 17),
                rate = "1.05",
                source = ExchangeRateEntity.SOURCE_ECB,
            ),
        )
        val engine = MockEngine {
            respond(
                content = """{"amount":1.0,"base":"EUR","date":"2026-11-18","rates":{"USD":1.08}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val rates = FrankfurterExchangeRates(db.exchangeRateDao(), client)
        // Offline convert from cache before refresh.
        val cached = rates.convert(Money(1_000L, eur), usd, LocalDate.of(2026, 11, 17))
        assertThat(cached!!.minor).isEqualTo(1_050L)

        assertThat(rates.refreshRemoteRates(eur)).isTrue()
        val refreshed = rates.convert(Money(1_000L, eur), usd, LocalDate.of(2026, 11, 18))
        assertThat(refreshed!!.minor).isEqualTo(1_080L)
    }

    @Test
    fun manualRatePreferredOverEcb() = runTest {
        db.exchangeRateDao().upsert(
            ExchangeRateEntity("EUR", "USD", LocalDate.of(2026, 11, 18), "1.08", ExchangeRateEntity.SOURCE_ECB),
        )
        val rates = FrankfurterExchangeRates(
            db.exchangeRateDao(),
            HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }),
        )
        rates.setManualRate(eur, usd, "2.00")
        val converted = rates.convert(Money(1_000L, eur), usd, LocalDate.of(2026, 11, 18))
        assertThat(converted!!.minor).isEqualTo(2_000L)
    }
}
