package dev.antomanc.tameki.core.model.capture

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.TransactionKind
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Test

class PaymentNotificationParserTest {
    private val posted = Instant.parse("2026-11-18T12:00:00Z")
    private val zone = ZoneOffset.UTC

    @Test
    fun parsesItalianPresso() {
        val draft = PaymentNotificationParser.parse(
            "com.intesasanpaolo.mobile",
            "Pagamento",
            "Pagamento di 12,50 EUR presso Esselunga",
            posted,
            zone,
        )
        assertThat(draft?.amount).isEqualTo(Money(1250, CurrencyCode("EUR")))
        assertThat(draft?.merchant).isEqualTo("Esselunga")
        assertThat(draft?.kind).isEqualTo(TransactionKind.EXPENSE)
    }

    @Test
    fun parsesHaiPagato() {
        val draft = PaymentNotificationParser.parse(
            "com.revolut.revolut",
            "",
            "Hai pagato 12,50 € a Uber",
            posted,
            zone,
        )
        assertThat(draft?.merchant).isEqualTo("Uber")
        assertThat(draft?.amount?.minor).isEqualTo(1250L)
    }

    @Test
    fun parsesYouPaid() {
        val draft = PaymentNotificationParser.parse(
            "com.paypal.android.p2pmobile",
            "PayPal",
            "You paid €12.50 to Steam",
            posted,
            zone,
        )
        assertThat(draft?.merchant).isEqualTo("Steam")
    }

    @Test
    fun parsesPaymentOfDollar() {
        val draft = PaymentNotificationParser.parse(
            "com.paypal.android.p2pmobile",
            "",
            "Payment of $3.20 at Amazon",
            posted,
            zone,
        )
        assertThat(draft?.amount).isEqualTo(Money(320, CurrencyCode("USD")))
        assertThat(draft?.merchant).isEqualTo("Amazon")
    }

    @Test
    fun parsesGoogleWalletStyle() {
        val draft = PaymentNotificationParser.parse(
            KnownBankingPackages.packageNames.first { it.contains("wallet") },
            "Google Wallet",
            "€4.50 at Metro",
            posted,
            zone,
        )
        assertThat(draft?.amount?.minor).isEqualTo(450L)
        assertThat(draft?.merchant).isEqualTo("Metro")
    }

    @Test
    fun ignoresOtp() {
        assertThat(
            PaymentNotificationParser.parse(
                "com.unicredit",
                "OTP",
                "Il tuo codice di sicurezza è 123456",
                posted,
                zone,
            ),
        ).isNull()
    }

    @Test
    fun incomingCreditIsIncome() {
        val draft = PaymentNotificationParser.parse(
            "com.intesasanpaolo.mobile",
            "Accredito",
            "Accredito di 100,00 EUR",
            posted,
            zone,
        )
        assertThat(draft?.kind).isEqualTo(TransactionKind.INCOME)
        assertThat(draft?.amount).isEqualTo(Money(10_000, CurrencyCode("EUR")))
    }

    @Test
    fun parsesRealIntesaSanpaoloNotification() {
        val draft = PaymentNotificationParser.parse(
            "com.latuabancaperandroid",
            "Intesa Sanpaolo Mobile",
            "💸 Hai pagato 105,99 € con la carta *1234 il 19.11 alle ore 09:25 da WWW.AMAZON.IT.",
            posted,
            zone,
        )
        assertThat(draft?.amount).isEqualTo(Money(10_599, CurrencyCode("EUR")))
        assertThat(draft?.merchant).isEqualTo("WWW.AMAZON.IT")
        assertThat(draft?.kind).isEqualTo(TransactionKind.EXPENSE)
    }

    @Test
    fun parsesItalianBankCardPaymentWithPresso() {
        val draft = PaymentNotificationParser.parse(
            "com.isybank.app",
            "Pagamento carta",
            "Hai effettuato un pagamento di 14,90 € con carta *5678 presso ESSELUNGA MILANO.",
            posted,
            zone,
        )
        assertThat(draft?.amount).isEqualTo(Money(1_490, CurrencyCode("EUR")))
        assertThat(draft?.merchant).isEqualTo("ESSELUNGA MILANO")
        assertThat(draft?.kind).isEqualTo(TransactionKind.EXPENSE)
    }

    @Test
    fun parsesBonificoRicevutoWithMerchant() {
        val draft = PaymentNotificationParser.parse(
            "com.latuabancaperandroid",
            "Bonifico",
            "Bonifico ricevuto di 1.250,00 € da ACME SRL.",
            posted,
            zone,
        )
        assertThat(draft?.amount).isEqualTo(Money(125_000, CurrencyCode("EUR")))
        assertThat(draft?.merchant).isEqualTo("ACME SRL")
        assertThat(draft?.kind).isEqualTo(TransactionKind.INCOME)
    }

    @Test
    fun knownPackagesIncludeMajorBanks() {
        val labels = KnownBankingPackages.all.map { it.label }.toSet()
        assertThat(labels).containsAtLeast(
            "Intesa Sanpaolo", "Isybank", "UniCredit", "Fineco", "BBVA", "Revolut", "N26",
            "PayPal", "Satispay", "Hype", "Poste Italiane", "BancoPosta", "Postepay",
            "ING", "Mediolanum", "Google Wallet",
        )
        assertThat(KnownBankingPackages.packageNames).contains("com.latuabancaperandroid")
    }
}
