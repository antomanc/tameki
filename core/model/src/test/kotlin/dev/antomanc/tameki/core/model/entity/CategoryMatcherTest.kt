package dev.antomanc.tameki.core.model.entity

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategoryMatcherTest {
    private val categories = DefaultCategories.materialize("en")

    @Test
    fun matchesEnglishAndItalianSynonyms() {
        val groceriesId = categories.first { it.name == "Groceries" }.id
        assertThat(CategoryMatcher.match("spesa", categories)).isEqualTo(groceriesId)
        assertThat(CategoryMatcher.match("supermarket", categories)).isEqualTo(groceriesId)
        assertThat(CategoryMatcher.match("Groceries", categories)).isEqualTo(groceriesId)
    }

    @Test
    fun merchantHistoryWins() {
        val clothesId = categories.first { it.name == "Clothes" }.id
        val id = CategoryMatcher.match(
            hint = "spesa",
            categories = categories,
            merchantHistory = mapOf("Zara" to clothesId),
            merchant = "Zara",
        )
        assertThat(id).isEqualTo(clothesId)
    }

    @Test
    fun defaultCategoriesSeedCounts() {
        val expenses = DefaultCategories.all.count { it.kind == CategoryKind.EXPENSE }
        val income = DefaultCategories.all.count { it.kind == CategoryKind.INCOME }
        assertThat(expenses).isAtLeast(16)
        assertThat(income).isAtLeast(5)
        assertThat(DefaultCategories.all.map { it.colorIndex }).containsAtLeast(0, 15)
    }
}
