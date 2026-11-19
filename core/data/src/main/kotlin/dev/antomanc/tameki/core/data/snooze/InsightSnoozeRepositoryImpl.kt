package dev.antomanc.tameki.core.data.snooze

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.antomanc.tameki.core.model.coroutines.runCatchingCancellable
import dev.antomanc.tameki.core.model.repository.InsightSnoozeRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.snoozeDataStore: DataStore<Preferences> by preferencesDataStore(name = "tameki_insight_snooze")

/**
 * Stores snoozed insight ids as `id|yyyy-MM-dd` in a tiny DataStore of its own. Keeping the date
 * next to the id means an expired snooze simply stops matching, so no cleanup worker is needed.
 */
class InsightSnoozeRepositoryImpl(
    context: Context,
    private val dataStore: DataStore<Preferences> = context.applicationContext.snoozeDataStore,
) : InsightSnoozeRepository {
    private object Keys {
        val SNOOZED = stringSetPreferencesKey("snoozed_insights")
    }

    override fun observeSnoozed(on: LocalDate): Flow<Set<String>> =
        dataStore.data.map { prefs ->
            (prefs[Keys.SNOOZED] ?: emptySet()).mapNotNull { entry ->
                val parts = entry.split(SEPARATOR, limit = 2)
                if (parts.size != 2) return@mapNotNull null
                val until = runCatchingCancellable { LocalDate.parse(parts[1]) }.getOrNull() ?: return@mapNotNull null
                parts[0].takeIf { !until.isBefore(on) }
            }.toSet()
        }

    override suspend fun snooze(insightId: String, until: LocalDate) {
        dataStore.edit { prefs ->
            val kept = (prefs[Keys.SNOOZED] ?: emptySet()).filterNot {
                it.substringBefore(SEPARATOR) == insightId
            }
            prefs[Keys.SNOOZED] = kept.toSet() + "$insightId$SEPARATOR$until"
        }
    }

    override suspend fun clear(insightId: String) {
        dataStore.edit { prefs ->
            val kept = (prefs[Keys.SNOOZED] ?: emptySet()).filterNot {
                it.substringBefore(SEPARATOR) == insightId
            }
            prefs[Keys.SNOOZED] = kept.toSet()
        }
    }

    private companion object {
        const val SEPARATOR = "|"
    }
}
