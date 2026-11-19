package dev.antomanc.tameki.core.model.capture

/**
 * Default allowlist of banking/wallet apps whose notifications may be parsed.
 * The user allowlist in settings overrides this; packages here are suggestions, not permissions.
 */
object KnownBankingPackages {
    data class Entry(val label: String, val packageName: String)

    val all: List<Entry> = listOf(
        // Intesa Sanpaolo Mobile (official Play Store ID) & Isybank
        Entry("Intesa Sanpaolo", "com.latuabancaperandroid"),
        Entry("Isybank", "com.isybank.app"),
        // UniCredit (Italian & international mobile banking variants)
        Entry("UniCredit", "com.unicredit"),
        Entry("UniCredit Mobile", "it.unicredit.mobile"),
        Entry("buddybank", "com.buddybank.app"),
        Entry("Fineco", "it.finecobank.mobile"),
        Entry("BBVA", "com.bbva.bbvacontigo"),
        Entry("Revolut", "com.revolut.revolut"),
        Entry("N26", "de.number26.android"),
        Entry("PayPal", "com.paypal.android.p2pmobile"),
        Entry("Satispay", "com.satispay.customer"),
        Entry("Hype", "it.sella.hype"),
        Entry("Poste Italiane", "posteitaliane.posteapp.appposte"),
        Entry("BancoPosta", "posteitaliane.posteapp.appbancoposta"),
        Entry("Postepay", "posteitaliane.posteapp.apppostepay"),
        Entry("ING", "com.ing.mobile"),
        Entry("ING (NL)", "com.ing.mobile.nl"),
        Entry("Mediolanum", "it.mediolanum.app"),
        Entry("BPER", "it.bper.smartbanking"),
        Entry("YouApp (Banco BPM)", "it.bpm.youapp"),
        Entry("illimity", "com.illimity.app"),
        Entry("Wise", "com.transferwise.android"),
        Entry("Curve", "com.imaginecurve.curve.prd"),
        Entry("Trade Republic", "com.traderepublic.app"),
        Entry("Klarna", "com.myklarnamobile"),
        Entry("SumUp Pay", "com.sumup.pay"),
        Entry("Google Wallet", "com.google.android.apps.walletnfcrel"),
    )

    val packageNames: Set<String> = all.map { it.packageName }.toSet()

    private val labelByPackage: Map<String, String> = all.associate { it.packageName to it.label }

    fun labelFor(packageName: String): String? = labelByPackage[packageName]
}
