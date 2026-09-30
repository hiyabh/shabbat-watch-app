package il.hiya.shabbatwatch.mode

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import il.hiya.shabbatwatch.Constants
import il.hiya.shabbatwatch.ShabbatLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.locationDataStore: DataStore<Preferences> by preferencesDataStore(name = "shabbat_prefs")

/** Persisted city and candle-lighting custom. Falls back to [Constants.LOCATION] until chosen. */
class LocationStore(context: Context) {
    private val dataStore = context.applicationContext.locationDataStore

    val location: Flow<ShabbatLocation> = dataStore.data.map { prefs ->
        resolve(prefs[KEY_CITY], prefs[KEY_CANDLE_MINUTES])
    }

    /** Picking a city also resets the candle-lighting minutes to that city's default. */
    suspend fun setCity(city: ShabbatLocation) {
        dataStore.edit { prefs ->
            prefs[KEY_CITY] = city.name
            prefs[KEY_CANDLE_MINUTES] = city.candleLightingMinutes
        }
    }

    suspend fun setCandleMinutes(minutes: Int) {
        dataStore.edit { prefs -> prefs[KEY_CANDLE_MINUTES] = minutes }
    }

    companion object {
        private val KEY_CITY = stringPreferencesKey("city")
        private val KEY_CANDLE_MINUTES = intPreferencesKey("candle_minutes")

        /** Unknown or missing values fall back to the default city / that city's default minutes. */
        fun resolve(cityName: String?, candleMinutes: Int?): ShabbatLocation {
            val city = Constants.CITIES.firstOrNull { it.name == cityName } ?: Constants.LOCATION
            val minutes = candleMinutes?.takeIf { it in Constants.CANDLE_MINUTES_OPTIONS }
                ?: city.candleLightingMinutes
            return city.copy(candleLightingMinutes = minutes)
        }
    }
}
