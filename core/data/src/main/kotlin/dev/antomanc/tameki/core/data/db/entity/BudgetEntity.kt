package dev.antomanc.tameki.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Budget
import dev.antomanc.tameki.core.model.entity.BudgetPeriod

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val limitMinor: Long,
    val currency: String,
    val period: String,
    val rollover: Boolean = false,
)

@Entity(
    tableName = "budget_categories",
    primaryKeys = ["budgetId", "categoryId"],
    indices = [Index("budgetId"), Index("categoryId")],
)
data class BudgetCategoryEntity(
    val budgetId: Long,
    val categoryId: Long,
)

fun BudgetEntity.toDomain(categoryIds: Set<Long>): Budget = Budget(
    id = id,
    name = name,
    categoryIds = categoryIds,
    limit = Money(limitMinor, CurrencyCode(currency)),
    period = BudgetPeriod.valueOf(period),
    rollover = rollover,
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    name = name,
    limitMinor = limit.minor,
    currency = limit.currency.value,
    period = period.name,
    rollover = rollover,
)
