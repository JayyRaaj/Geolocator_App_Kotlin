package com.example.weatherapp.data.model

import org.json.JSONObject

/**
 * Temperature and atmospheric readings from the "main" block of the API response.
 *
 * All temperatures are in Celsius when the request includes units=metric.
 */
data class WeatherMain(
    val tempCelsius: Double,
    val feelsLikeCelsius: Double,
    val tempMinCelsius: Double,
    val tempMaxCelsius: Double,
    /** Atmospheric pressure at sea level, in hPa. */
    val pressureHpa: Int,
    /** Relative humidity as a percentage (0–100). */
    val humidityPercent: Int
) {
    companion object {
        /** Parses the "main" block from the API response JSON. */
        fun fromJson(json: JSONObject): WeatherMain = WeatherMain(
            tempCelsius = json.getDouble("temp"),
            feelsLikeCelsius = json.getDouble("feels_like"),
            tempMinCelsius = json.getDouble("temp_min"),
            tempMaxCelsius = json.getDouble("temp_max"),
            pressureHpa = json.getInt("pressure"),
            humidityPercent = json.getInt("humidity")
        )
    }
}
