package dev.antomanc.tameki.core.model.entity

import dev.antomanc.tameki.core.model.Money
import java.time.LocalDate

/** Named savings target; progress is [saved] toward [target]. */
data class Goal(
    val id: Long = 0L,
    val name: String,
    val target: Money,
    val saved: Money,
    val deadline: LocalDate? = null,
    val icon: String,
    val archived: Boolean = false,
)
