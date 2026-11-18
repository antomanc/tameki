package dev.antomanc.tameki.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Goal
import java.time.LocalDate

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val targetMinor: Long,
    val targetCurrency: String,
    val savedMinor: Long,
    val savedCurrency: String,
    val deadline: LocalDate? = null,
    val icon: String,
    val archived: Boolean = false,
) {
    fun toDomain(): Goal = Goal(
        id = id,
        name = name,
        target = Money(targetMinor, CurrencyCode(targetCurrency)),
        saved = Money(savedMinor, CurrencyCode(savedCurrency)),
        deadline = deadline,
        icon = icon,
        archived = archived,
    )

    companion object {
        fun from(goal: Goal): GoalEntity = GoalEntity(
            id = goal.id,
            name = goal.name,
            targetMinor = goal.target.minor,
            targetCurrency = goal.target.currency.value,
            savedMinor = goal.saved.minor,
            savedCurrency = goal.saved.currency.value,
            deadline = goal.deadline,
            icon = goal.icon,
            archived = goal.archived,
        )
    }
}
