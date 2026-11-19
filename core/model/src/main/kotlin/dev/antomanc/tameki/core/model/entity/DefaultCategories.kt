package dev.antomanc.tameki.core.model.entity

/**
 * Seed categories for a fresh install (~16 expense, ~5 income).
 * [icon] keys match the core:ui icon set; [colorIndex] 0–15 maps to the palette there.
 * Localised labels live in string resources keyed by [Seed.key]; the DB stores [seedKey] only.
 */
object DefaultCategories {
    data class Seed(
        val key: String,
        val nameEn: String,
        val nameIt: String,
        val kind: CategoryKind,
        val icon: String,
        val colorIndex: Int,
    )

    /** Fixed commonness order for cold-start quick-add ranking (expenses). */
    val COMMONNESS_ORDER: List<String> = listOf(
        "groceries", "restaurant", "coffee", "transport", "fuel", "home", "utilities",
        "health", "pharmacy", "phone", "internet", "clothes", "entertainment", "streaming",
        "sport", "other_expense",
    )

    val all: List<Seed> = listOf(
        // Expenses (~16)
        Seed("groceries", "Groceries", "Alimentari", CategoryKind.EXPENSE, "groceries", 0),
        Seed("restaurant", "Restaurant", "Ristorante", CategoryKind.EXPENSE, "restaurant", 1),
        Seed("coffee", "Coffee", "Caffè", CategoryKind.EXPENSE, "coffee", 2),
        Seed("transport", "Transport", "Trasporti", CategoryKind.EXPENSE, "transport", 3),
        Seed("fuel", "Fuel", "Carburante", CategoryKind.EXPENSE, "fuel", 4),
        Seed("home", "Home", "Casa", CategoryKind.EXPENSE, "home", 5),
        Seed("utilities", "Utilities", "Utenze", CategoryKind.EXPENSE, "utilities", 6),
        Seed("phone", "Phone", "Telefono", CategoryKind.EXPENSE, "phone", 7),
        Seed("internet", "Internet", "Internet", CategoryKind.EXPENSE, "internet", 8),
        Seed("health", "Health", "Salute", CategoryKind.EXPENSE, "health", 9),
        Seed("pharmacy", "Pharmacy", "Farmacia", CategoryKind.EXPENSE, "pharmacy", 10),
        Seed("sport", "Sport", "Sport", CategoryKind.EXPENSE, "sport", 11),
        Seed("clothes", "Clothes", "Abbigliamento", CategoryKind.EXPENSE, "clothes", 12),
        Seed("entertainment", "Entertainment", "Svago", CategoryKind.EXPENSE, "entertainment", 13),
        Seed("streaming", "Streaming", "Streaming", CategoryKind.EXPENSE, "streaming", 14),
        Seed("other_expense", "Other", "Altro", CategoryKind.EXPENSE, "other", 15),
        // Income (~5)
        Seed("salary", "Salary", "Stipendio", CategoryKind.INCOME, "salary", 0),
        Seed("refund", "Refund", "Rimborso", CategoryKind.INCOME, "refund", 1),
        Seed("investment", "Investment", "Investimenti", CategoryKind.INCOME, "investment", 2),
        Seed("gifts_income", "Gifts", "Regali", CategoryKind.INCOME, "gifts", 3),
        Seed("other_income", "Other", "Altro", CategoryKind.INCOME, "other", 4),
    )

    fun byKey(key: String): Seed? = all.firstOrNull { it.key == key }

    /** ARGB colours for indices 0–15 (stable across installs). */
    fun colorArgb(index: Int): Long {
        val palette = longArrayOf(
            0xFF2A9D8F, // teal
            0xFFE76F51, // coral
            0xFF264653, // ink
            0xFFE9C46A, // sand
            0xFF457B9D, // steel
            0xFFF4A261, // apricot
            0xFF1D3557, // navy
            0xFFA8DADC, // mist
            0xFF6D597A, // mauve
            0xFFB56576, // rose
            0xFF355070, // slate
            0xFFEAAC8B, // peach
            0xFF6A994E, // leaf
            0xFFBC4749, // brick
            0xFF386641, // pine
            0xFF9A8C98, // stone
        )
        return palette[index.mod(palette.size)]
    }

    /**
     * Materialises seed rows. [name] is English fallback for non-UI use; [seedKey] drives UI
     * localisation. [customName] stays null so language changes rename automatically.
     */
    fun materialize(localeLanguage: String = "en"): List<Category> =
        all.mapIndexed { index, seed ->
            val label = if (localeLanguage == "it") seed.nameIt else seed.nameEn
            Category(
                id = (index + 1).toLong(),
                name = label,
                kind = seed.kind,
                icon = seed.icon,
                color = colorArgb(seed.colorIndex),
                seedKey = seed.key,
                customName = null,
            )
        }

    /** Infer a seed key from a frozen display name (migration / legacy rows). */
    fun seedKeyForName(name: String): String? {
        val trimmed = name.trim()
        return all.firstOrNull {
            it.key.equals(trimmed, ignoreCase = true) ||
                it.nameEn.equals(trimmed, ignoreCase = true) ||
                it.nameIt.equals(trimmed, ignoreCase = true)
        }?.key
    }
}

/** Stable seed keys for the default Cash and Card accounts. */
object DefaultAccounts {
    const val CASH = "cash"
    const val CARD = "card"

    fun seedKeyForName(name: String): String? = when (name.trim().lowercase()) {
        "cash", "contanti" -> CASH
        "card", "carta" -> CARD
        else -> null
    }
}
