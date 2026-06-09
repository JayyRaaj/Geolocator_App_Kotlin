package com.example.weatherapp.data.model

import org.json.JSONObject

/**
 * Represents a single weather condition entry from the API's "weather" array.
 *
 * The API can return multiple conditions, but in practice always returns one.
 * Callers should read index 0.
 */
data class WeatherCondition(
    /** Numeric OWM condition code (e.g. 800 = clear sky, 500 = light rain). */
    val id: Int,
    /** Short group label (e.g. "Clear", "Rain", "Clouds"). Used for grouping logic. */
    val main: String,
    /** Human-readable description (e.g. "light intensity drizzle"). */
    val description: String,
    /** Icon code from OWM, used to build the CDN URL (e.g. "01d", "09n"). */
    val iconCode: String
) {
    companion object {
        /**
         * Builds the full icon URL for a given icon code.
         * Using @2x variant so it looks sharp on high-density screens.
         */
        fun iconUrl(iconCode: String): String =
            "https://openweathermap.org/img/wn/${iconCode}@2x.png"

        /** Parses a [WeatherCondition] from a single element of the "weather" JSON array. */
        fun fromJson(json: JSONObject): WeatherCondition = WeatherCondition(
            id = json.getInt("id"),
            main = json.getString("main"),
            description = json.getString("description"),
            iconCode = json.getString("icon")
        )
    }
}
