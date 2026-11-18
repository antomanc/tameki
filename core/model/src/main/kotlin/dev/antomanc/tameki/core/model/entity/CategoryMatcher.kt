package dev.antomanc.tameki.core.model.entity

/**
 * Maps free-text hints (AI, OCR, notifications) onto category ids.
 * Synonym table covers IT/EN grocery-store language; merchant history wins when present.
 * Matching prefers [Category.seedKey] so renamed/localised labels stay findable.
 */
object CategoryMatcher {
    /** Synonym (lowercase) → canonical seed key used by [DefaultCategories]. */
    val synonyms: Map<String, String> = mapOf(
        // groceries
        "groceries" to "groceries",
        "grocery" to "groceries",
        "spesa" to "groceries",
        "supermarket" to "groceries",
        "supermercato" to "groceries",
        "alimentari" to "groceries",
        "food shopping" to "groceries",
        // restaurant
        "restaurant" to "restaurant",
        "ristorante" to "restaurant",
        "dining" to "restaurant",
        "cena" to "restaurant",
        "pranzo fuori" to "restaurant",
        // coffee
        "coffee" to "coffee",
        "caffè" to "coffee",
        "caffe" to "coffee",
        "bar" to "coffee",
        // transport
        "transport" to "transport",
        "trasporti" to "transport",
        "transit" to "transport",
        "metro" to "transport",
        "bus" to "transport",
        "taxi" to "transport",
        "uber" to "transport",
        // fuel
        "fuel" to "fuel",
        "benzina" to "fuel",
        "gas" to "fuel",
        "diesel" to "fuel",
        "carburante" to "fuel",
        // home
        "home" to "home",
        "casa" to "home",
        "rent" to "home",
        "affitto" to "home",
        // utilities
        "utilities" to "utilities",
        "utenze" to "utilities",
        "bollette" to "utilities",
        "electricity" to "utilities",
        "gas bill" to "utilities",
        // phone / internet
        "phone" to "phone",
        "telefono" to "phone",
        "mobile" to "phone",
        "internet" to "internet",
        "wifi" to "internet",
        "broadband" to "internet",
        // health / pharmacy
        "health" to "health",
        "salute" to "health",
        "doctor" to "health",
        "medico" to "health",
        "pharmacy" to "pharmacy",
        "farmacia" to "pharmacy",
        // sport / clothes / beauty
        "sport" to "sport",
        "gym" to "sport",
        "palestra" to "sport",
        "clothes" to "clothes",
        "clothing" to "clothes",
        "abbigliamento" to "clothes",
        "beauty" to "beauty",
        "cura persona" to "beauty",
        // gifts / travel / entertainment
        "gifts" to "gifts",
        "gifts_income" to "gifts_income",
        "regali" to "gifts",
        "travel" to "travel",
        "viaggi" to "travel",
        "holiday" to "travel",
        "entertainment" to "entertainment",
        "svago" to "entertainment",
        "streaming" to "streaming",
        "music" to "music",
        "musica" to "music",
        "games" to "games",
        "giochi" to "games",
        // education / kids / pets
        "education" to "education",
        "istruzione" to "education",
        "school" to "education",
        "kids" to "kids",
        "bambini" to "kids",
        "pets" to "pets",
        "animali" to "pets",
        // insurance / taxes / bank
        "insurance" to "insurance",
        "assicurazione" to "insurance",
        "taxes" to "taxes",
        "tasse" to "taxes",
        "bank" to "bank",
        "banca" to "bank",
        "fees" to "bank",
        // income
        "salary" to "salary",
        "stipendio" to "salary",
        "wage" to "salary",
        "refund" to "refund",
        "rimborso" to "refund",
        "investment" to "investment",
        "investimenti" to "investment",
        "other" to "other_expense",
        "other_expense" to "other_expense",
        "other_income" to "other_income",
        "altro" to "other_expense",
    )

    fun match(
        hint: String?,
        categories: List<Category>,
        merchantHistory: Map<String, Long> = emptyMap(),
        merchant: String? = null,
    ): Long? {
        if (!merchant.isNullOrBlank()) {
            val fromHistory = merchantHistory.entries.firstOrNull { (key, _) ->
                key.equals(merchant, ignoreCase = true)
            }?.value
            if (fromHistory != null) return fromHistory
        }
        val normalized = hint?.trim()?.lowercase() ?: return null
        if (normalized.isEmpty()) return null

        categories.firstOrNull {
            it.customName?.equals(hint.trim(), ignoreCase = true) == true ||
                it.name.equals(hint.trim(), ignoreCase = true)
        }?.id?.let { return it }

        val key = synonyms[normalized] ?: canonicalKey(normalized)
        if (key != null) {
            categories.firstOrNull { it.seedKey == key }?.id?.let { return it }
            // Legacy rows without seedKey: match EN/IT seed names or icon.
            val seed = DefaultCategories.byKey(key)
            if (seed != null) {
                categories.firstOrNull { cat ->
                    cat.seedKey == null && (
                        cat.icon == seed.icon ||
                            cat.name.equals(seed.nameEn, ignoreCase = true) ||
                            cat.name.equals(seed.nameIt, ignoreCase = true)
                        )
                }?.id?.let { return it }
            }
        }

        return categories.firstOrNull { cat ->
            val label = cat.resolvedName().lowercase()
            normalized.contains(label) || label.contains(normalized)
        }?.id
    }

    /** Resolves a synonym or name to the DefaultCategories key when possible. */
    fun canonicalKey(hint: String): String? {
        val normalized = hint.trim().lowercase()
        synonyms[normalized]?.let { return it }
        DefaultCategories.seedKeyForName(hint)?.let { return it }
        return synonyms.entries.firstOrNull { it.value == normalized }?.value
    }
}
