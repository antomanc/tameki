package dev.antomanc.tameki.core.model.entity

/**
 * Seed categories for a fresh install (~16 expense, ~5 income).
 * [icon] keys match the core:ui icon set; [colorIndex] 0–15 maps to the palette there.
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

    /** ARGB colours for indices 0–15 (stable across installs). */
    fun colorArgb(index: Int): Long {
        val palette = longArrayOf(
            0xFF2A9D8FFF, // teal
            0xFFE76F51FF, // coral
            0xFF264653FF, // ink
            0xFFE9C46AFF, // sand
            0xFF457B9DFF, // steel
            0xFFF4A261FF, // apricot
            0xFF1D3557FF, // navy
            0xFFA8DADCFF, // mist
            0xFF6D597AFF, // mauve
            0xFFB56576FF, // rose
            0xFF355070FF, // slate
            0xFFEAAC8BFF, // peach
            0xFF6A994EFF, // leaf
            0xFFBC4749FF, // brick
            0xFF386641FF, // pine
            0xFF9A8C98FF, // stone
        )
        return palette[index.mod(palette.size)]
    }

    fun materialize(localeLanguage: String = "en"): List<Category> =
        all.mapIndexed { index, seed ->
            Category(
                id = (index + 1).toLong(),
                name = if (localeLanguage == "it") seed.nameIt else seed.nameEn,
                kind = seed.kind,
                icon = seed.icon,
                color = colorArgb(seed.colorIndex),
            )
        }
}
