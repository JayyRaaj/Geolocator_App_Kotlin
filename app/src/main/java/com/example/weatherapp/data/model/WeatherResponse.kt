package com.example.weatherapp.data.model

import org.json.JSONObject

/**
 * Top-level model for the OpenWeatherMap "current weather" endpoint.
 *
 * Endpoint: GET https://api.openweathermap.org/data/2.5/weather
 *
 * JSON is parsed manually using [org.json.JSONObject] — no serialisation library needed.
 * Parsing lives here rather than in the repository so it can be unit-tested in isolation.
 */
data class WeatherResponse(
    /** City name as returned by the API (e.g. "London"). */
    val cityName: String,
    /** ISO 3166-1 alpha-2 country code (e.g. "GB"). */
    val countryCode: String,
    /**
     * Primary weather condition. We read index 0 from the array.
     * The API rarely returns more than one condition.
     */
    val condition: WeatherCondition,
    val main: WeatherMain,
    val wind: Wind,
    /**
     * Visibility in metres (max 10 000 m).
     * Nullable — not always present in the API response.
     */
    val visibilityMetres: Int?,
    /** Unix UTC timestamp of the observation. */
    val timestamp: Long,
    /** Unix UTC sunrise time for the location. */
    val sunriseTimestamp: Long,
    /** Unix UTC sunset time for the location. */
    val sunsetTimestamp: Long
) {
    companion object {
        /**
         * Parses a [WeatherResponse] from the raw JSON string returned by the API.
         *
         * @throws org.json.JSONException if the response is malformed or missing required fields.
         * Callers (the repository) are responsible for catching this and mapping it to an error state.
         */
        fun fromJson(rawJson: String): WeatherResponse {
            val root = JSONObject(rawJson)
            val sys = root.getJSONObject("sys")
            val weatherArray = root.getJSONArray("weather")

            // Always use the first element — the API guarantees at least one entry.
            val condition = WeatherCondition.fromJson(weatherArray.getJSONObject(0))
            val main = WeatherMain.fromJson(root.getJSONObject("main"))
            val wind = Wind.fromJson(root.getJSONObject("wind"))

            return WeatherResponse(
                cityName = root.getString("name"),
                countryCode = sys.getString("country"),
                condition = condition,
                main = main,
                wind = wind,
                visibilityMetres = if (root.has("visibility")) root.getInt("visibility") else null,
                timestamp = root.getLong("dt"),
                sunriseTimestamp = sys.getLong("sunrise"),
                sunsetTimestamp = sys.getLong("sunset")
            )
        }
    }
}
