package il.hiya.shabbatwatch.mode

import android.app.NotificationManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.shabbatDataStore: DataStore<Preferences> by preferencesDataStore(name = "shabbat_state")

/** Persisted Shabbat-mode state. Survives process death and reboot. */
data class ShabbatState(
    val active: Boolean = false,
    val endAtMillis: Long = 0L,
    /** Original system-setting values, keyed by "NAMESPACE:key". Restored on deactivation. */
    val savedSettings: Map<String, String> = emptyMap(),
    val previousDndFilter: Int = NotificationManager.INTERRUPTION_FILTER_ALL,
)

class ShabbatStateStore(context: Context) {
    private val dataStore = context.applicationContext.shabbatDataStore

    val state: Flow<ShabbatState> = dataStore.data.map { prefs ->
        ShabbatState(
            active = prefs[KEY_ACTIVE] ?: false,
            endAtMillis = prefs[KEY_END_AT] ?: 0L,
            savedSettings = decodeSettings(prefs[KEY_SAVED_SETTINGS].orEmpty()),
            previousDndFilter = prefs[KEY_PREV_DND] ?: NotificationManager.INTERRUPTION_FILTER_ALL,
        )
    }

    suspend fun read(): ShabbatState = state.first()

    suspend fun markActive(endAtMillis: Long, savedSettings: Map<String, String>, previousDndFilter: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_ACTIVE] = true
            prefs[KEY_END_AT] = endAtMillis
            prefs[KEY_SAVED_SETTINGS] = encodeSettings(savedSettings)
            prefs[KEY_PREV_DND] = previousDndFilter
        }
    }

    suspend fun markInactive() {
        dataStore.edit { prefs ->
            prefs[KEY_ACTIVE] = false
            prefs.remove(KEY_END_AT)
            prefs.remove(KEY_SAVED_SETTINGS)
            prefs.remove(KEY_PREV_DND)
        }
    }

    companion object {
        private val KEY_ACTIVE = booleanPreferencesKey("active")
        private val KEY_END_AT = longPreferencesKey("end_at_millis")
        private val KEY_SAVED_SETTINGS = stringPreferencesKey("saved_settings")
        private val KEY_PREV_DND = intPreferencesKey("previous_dnd_filter")

        private const val ENTRY_SEPARATOR = "\n"
        private const val KEY_VALUE_SEPARATOR = "="

        /** Setting keys never contain '=' or newlines, so a line-per-entry encoding is safe. */
        fun encodeSettings(settings: Map<String, String>): String =
            settings.entries.joinToString(ENTRY_SEPARATOR) { "${it.key}$KEY_VALUE_SEPARATOR${it.value}" }

        fun decodeSettings(encoded: String): Map<String, String> =
            encoded.split(ENTRY_SEPARATOR)
                .filter { it.contains(KEY_VALUE_SEPARATOR) }
                .associate { line ->
                    val idx = line.indexOf(KEY_VALUE_SEPARATOR)
                    line.substring(0, idx) to line.substring(idx + 1)
                }
    }
}
