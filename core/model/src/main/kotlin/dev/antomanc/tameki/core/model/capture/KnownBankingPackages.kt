package dev.antomanc.tameki.core.model.capture

/**
 * Default allowlist of banking/wallet apps whose notifications may be parsed.
 * The user allowlist in settings overrides this; packages here are suggestions, not permissions.
 *
 * Package names verified against public Play Store listings where possible; entries marked
 * TODO should be confirmed on-device before shipping capture defaults.
 */
object KnownBankingPackages {
    data class Entry(val label: String, val packageName: String)

    val all: List<Entry> = listOf(
        Entry("Intesa Sanpaolo", "com.intesasanpaolo.mobile"),
        // TODO: confirm UniCredit package on Play Store (variants exist per country)
        Entry("UniCredit", "com.unicredit"),
        Entry("Fineco", "it.finecobank.mobile"),
        Entry("BBVA", "com.bbva.bbvacontigo"),
        Entry("Revolut", "com.revolut.revolut"),
        Entry("N26", "de.number26.android"),
        Entry("PayPal", "com.paypal.android.p2pmobile"),
        Entry("Satispay", "com.satispay.customer"),
        // TODO: confirm Hype (Banca Sella) package — historically it.sella.hype / com.hyepay.app
        Entry("Hype", "it.sella.hype"),
        Entry("Poste Italiane", "posteitaliane.posteapp.appposte"),
        Entry("BancoPosta", "posteitaliane.posteapp.appbancoposta"),
        Entry("Postepay", "posteitaliane.posteapp.apppostepay"),
        // TODO: confirm ING Italy package (may differ from NL com.ing.mobile)
        Entry("ING", "com.ing.mobile.nl"),
        // TODO: confirm Mediolanum package
        Entry("Mediolanum", "it.mediolanum.app"),
        Entry("Google Wallet", "com.google.android.apps.walletnfcrel"),
    )

    val packageNames: Set<String> = all.map { it.packageName }.toSet()
}
