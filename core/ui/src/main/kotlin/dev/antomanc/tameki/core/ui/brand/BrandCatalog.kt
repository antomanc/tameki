package dev.antomanc.tameki.core.ui.brand

import android.content.Context
import androidx.compose.ui.graphics.Color
import dev.antomanc.tameki.core.model.entity.RecurringType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A known service, bundled so the add screen can recognise a name and fill in the rest: a stable
 * monogram colour, the typical category, the type (subscription or bill) and, when it is public,
 * the page where the subscription is cancelled.
 *
 * There are no logos here on purpose: trademarks stay with their owners, and a coloured monogram
 * with the real name is honest and unambiguous.
 */
@Serializable
data class Brand(
    val key: String,
    val name: String,
    /** `#RRGGBB`, used for the monogram wash and letter. */
    val color: String,
    /** A core:ui category key: streaming, music, utilities, phone… */
    val category: String,
    val type: String = "SUBSCRIPTION",
    val aliases: List<String> = emptyList(),
    val cancelUrl: String? = null,
) {
    /** The recurring type this brand usually is. */
    val recurringType: RecurringType
        get() = when (type) {
            "BILL" -> RecurringType.BILL
            "INCOME" -> RecurringType.INCOME
            else -> RecurringType.SUBSCRIPTION
        }

    val accent: Color
        get() = parseBrandColor(color)
}

/** Loaded once from `assets/brands.json`; suggestions stay a pure function of the query. */
class BrandCatalog(val brands: List<Brand>) {
    private val byKey: Map<String, Brand> = brands.associateBy { it.key }

    fun find(key: String?): Brand? = key?.let { byKey[it] }

    /**
     * The services a half-typed name most likely means, best match first. Fewer than two
     * characters returns nothing: a suggestion on "n" is noise.
     */
    fun suggest(query: String, limit: Int = 5): List<Brand> {
        val needle = query.trim().lowercase()
        if (needle.length < 2) return emptyList()
        return brands
            .mapNotNull { brand ->
                val score = score(brand, needle) ?: return@mapNotNull null
                score to brand
            }
            .sortedWith(compareBy({ it.first }, { it.second.name.length }))
            .map { it.second }
            .take(limit)
    }

    private fun score(brand: Brand, needle: String): Int? {
        val name = brand.name.lowercase()
        if (name.startsWith(needle)) return 0
        if (name.contains(needle)) return 1
        for (alias in brand.aliases) {
            val text = alias.lowercase()
            if (text.startsWith(needle)) return 2
            if (text.contains(needle)) return 3
        }
        return null
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(raw: String): BrandCatalog =
            BrandCatalog(json.decodeFromString<BrandsFile>(raw).brands)

        fun load(context: Context): BrandCatalog =
            context.assets.open("brands.json").bufferedReader().use { reader ->
                fromJson(reader.readText())
            }
    }

    @Serializable
    private data class BrandsFile(val brands: List<Brand>)
}

/** `#RRGGBB`, with a graphite fallback so a typo in the catalog can never crash a row. */
internal fun parseBrandColor(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    val value = cleaned.toLongOrNull(16) ?: return Color(0xFF55534C)
    return when (cleaned.length) {
        6 -> Color(0xFF000000 or value)
        8 -> Color(value)
        else -> Color(0xFF55534C)
    }
}
