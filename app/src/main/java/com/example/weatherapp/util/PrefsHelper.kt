package com.example.weatherapp.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Thin wrapper around [SharedPreferences] for persisting the last successfully
 * searched city name across app sessions.
 *
 * Kept intentionally narrow — only stores what the app actually needs right now.
 * Adding more keys later is straightforward: add a KEY_ constant and a pair of
 * save/load methods following the same pattern.
 *
 * Uses [SharedPreferences.Editor.apply] (async write) rather than [commit]
 * (synchronous) so saving never blocks the calling thread.
 *
 * @param context Used to open the preferences file. Pass Application context to
 *                avoid holding a reference to a short-lived Activity.
 *
 * TODO: ideally this would be injected via a DI framework like Hilt, but keeping it simple for now.
 */
class PrefsHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Persists [city] as the last successfully searched city.
     * Overwrites any previously saved value.
     *
     * @param city Trimmed city name as returned by the API (e.g. "London").
     *             The API-returned name is used rather than the raw user input so
     *             the saved value is consistently formatted.
     */
    fun saveLastCity(city: String) {
        prefs.edit().putString(KEY_LAST_CITY, city).apply()
    }

    /**
     * Returns the last saved city name, or `null` if no city has been saved yet
     * (e.g. on first app launch, or after [clearLastCity] has been called).
     */
    fun loadLastCity(): String? =
        prefs.getString(KEY_LAST_CITY, null)

    /**
     * Removes the saved city. Can be used if the user explicitly clears app data
     * or if a future settings screen offers a "clear history" option.
     */
    fun clearLastCity() {
        prefs.edit().remove(KEY_LAST_CITY).apply()
    }

    companion object {
        /** Preferences file name. Must be stable — renaming it loses saved data. */
        private const val PREFS_NAME = "weather_app_prefs"
        private const val KEY_LAST_CITY = "last_city"
    }
}
