package dev.antomanc.tameki.core.model.repository

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Remembers which savings insights the user put away, and until when. Snoozing is a promise not
 * to nag: an insight disappears for the requested days and comes back by itself afterwards.
 *
 * Ids are the stable [dev.antomanc.tameki.core.model.insight.Insight.id] values; storage keeps the
 * date so no background job is needed to revive them.
 */
interface InsightSnoozeRepository {
    /** The insights still snoozed on [on]: their snooze date has not passed yet. */
    fun observeSnoozed(on: LocalDate): Flow<Set<String>>

    suspend fun snooze(insightId: String, until: LocalDate)

    /** Shows a snoozed insight again immediately. */
    suspend fun clear(insightId: String)
}
