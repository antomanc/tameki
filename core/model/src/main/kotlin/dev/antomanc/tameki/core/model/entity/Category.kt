package dev.antomanc.tameki.core.model.entity


enum class CategoryKind {
    EXPENSE,
    INCOME,
}

/**
 * User-facing bucket for spending and income.
 * [icon] is a key into the core:ui icon set; [color] is ARGB for chips without theme coupling.
 */
data class Category(
    val id: Long = 0L,
    val name: String,
    val kind: CategoryKind,
    val icon: String,
    val color: Long,
    val parentId: Long? = null,
    val archived: Boolean = false,
)
