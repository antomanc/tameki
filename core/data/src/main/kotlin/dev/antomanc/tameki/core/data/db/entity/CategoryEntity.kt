package dev.antomanc.tameki.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind

@Entity(
    tableName = "categories",
    indices = [Index("parentId"), Index("kind"), Index(value = ["seedKey"], unique = false)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val kind: String,
    val icon: String,
    val color: Long,
    val parentId: Long? = null,
    val archived: Boolean = false,
    val seedKey: String? = null,
    val customName: String? = null,
) {
    fun toDomain(): Category = Category(
        id = id,
        name = name,
        kind = CategoryKind.valueOf(kind),
        icon = icon,
        color = color,
        parentId = parentId,
        archived = archived,
        seedKey = seedKey,
        customName = customName,
    )

    companion object {
        fun from(category: Category): CategoryEntity = CategoryEntity(
            id = category.id,
            name = category.name,
            kind = category.kind.name,
            icon = category.icon,
            color = category.color,
            parentId = category.parentId,
            archived = category.archived,
            seedKey = category.seedKey,
            customName = category.customName,
        )
    }
}

@Entity(tableName = "merchant_category_memory")
data class MerchantCategoryEntity(
    @PrimaryKey val merchant: String,
    val categoryId: Long,
)
