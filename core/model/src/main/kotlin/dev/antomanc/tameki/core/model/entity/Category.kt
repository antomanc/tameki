package dev.antomanc.tameki.core.model.entity


enum class CategoryKind {
    EXPENSE,
    INCOME,
}

/**
 * User-facing bucket for spending and income.
 * [icon] is a key into the core:ui icon set; [color] is ARGB for chips without theme coupling.
 *
 * Seeded rows carry a stable [seedKey]. The UI shows the localised string for that key unless
 * the user renamed the category ([customName]). [name] is a durable fallback (user-created name,
 * or the last known label) for backups and non-UI code.
 */
data class Category(
    val id: Long = 0L,
    val name: String,
    val kind: CategoryKind,
    val icon: String,
    val color: Long,
    val parentId: Long? = null,
    val archived: Boolean = false,
    val seedKey: String? = null,
    val customName: String? = null,
) {
    /** Label to show when string resources are unavailable (tests, logs). */
    fun resolvedName(): String = customName?.takeIf { it.isNotBlank() } ?: name
}
