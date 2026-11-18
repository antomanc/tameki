package dev.antomanc.tameki.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.antomanc.tameki.core.data.seed.DatabaseSeeder
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.insight.InsightRules
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "tameki_settings")

class SettingsRepositoryImpl(
    context: Context,
    private val dataStore: DataStore<Preferences> = context.applicationContext.settingsDataStore,
    private val defaultCurrency: CurrencyCode = DatabaseSeeder.baseCurrencyFor(Locale.getDefault()),
) : SettingsRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object Keys {
        val BASE_CURRENCY = stringPreferencesKey("base_currency")
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LOCK = booleanPreferencesKey("lock_enabled")
        val NOTIF_CAPTURE = booleanPreferencesKey("notification_capture_enabled")
        val NOTIF_ALLOWLIST = stringSetPreferencesKey("notification_allowlist")
        val SILENT_AUTOMATION = booleanPreferencesKey("silent_automation_saves")
        val INSIGHT_RULES = stringPreferencesKey("insight_rules")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val AUTO_BACKUP_URI = stringPreferencesKey("auto_backup_folder_uri")
    }

    override fun observeBaseCurrency(): Flow<CurrencyCode> =
        dataStore.data.map { prefs ->
            prefs[Keys.BASE_CURRENCY]?.let { CurrencyCode.parse(it) } ?: defaultCurrency
        }

    override suspend fun setBaseCurrency(currency: CurrencyCode) {
        dataStore.edit { it[Keys.BASE_CURRENCY] = currency.value }
    }

    override fun observeTheme(): Flow<String> =
        dataStore.data.map { it[Keys.THEME] ?: "system" }

    override suspend fun setTheme(theme: String) {
        dataStore.edit { it[Keys.THEME] = theme }
    }

    override fun observeDynamicColor(): Flow<Boolean> =
        dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: true }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    override fun observeLockEnabled(): Flow<Boolean> =
        dataStore.data.map { it[Keys.LOCK] ?: false }

    override suspend fun setLockEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.LOCK] = enabled }
    }

    override fun observeNotificationCaptureEnabled(): Flow<Boolean> =
        dataStore.data.map { it[Keys.NOTIF_CAPTURE] ?: false }

    override suspend fun setNotificationCaptureEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIF_CAPTURE] = enabled }
    }

    override fun observeNotificationAllowlist(): Flow<Set<String>> =
        dataStore.data.map { it[Keys.NOTIF_ALLOWLIST] ?: emptySet() }

    override suspend fun setNotificationAllowlist(packages: Set<String>) {
        dataStore.edit { it[Keys.NOTIF_ALLOWLIST] = packages }
    }

    override fun observeSilentAutomationSaves(): Flow<Boolean> =
        dataStore.data.map { it[Keys.SILENT_AUTOMATION] ?: false }

    override suspend fun setSilentAutomationSaves(enabled: Boolean) {
        dataStore.edit { it[Keys.SILENT_AUTOMATION] = enabled }
    }

    override fun observeInsightRules(): Flow<InsightRules> =
        dataStore.data.map { prefs ->
            prefs[Keys.INSIGHT_RULES]?.let { raw ->
                runCatching { json.decodeFromString<InsightRules>(raw) }.getOrNull()
            } ?: InsightRules()
        }

    override suspend fun setInsightRules(rules: InsightRules) {
        dataStore.edit { it[Keys.INSIGHT_RULES] = json.encodeToString(rules) }
    }

    override fun observeOnboardingDone(): Flow<Boolean> =
        dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    override suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    fun observeAutoBackupFolderUri(): Flow<String?> =
        dataStore.data.map { it[Keys.AUTO_BACKUP_URI] }

    suspend fun setAutoBackupFolderUri(uri: String?) {
        dataStore.edit { prefs ->
            if (uri == null) prefs.remove(Keys.AUTO_BACKUP_URI) else prefs[Keys.AUTO_BACKUP_URI] = uri
        }
    }
}
