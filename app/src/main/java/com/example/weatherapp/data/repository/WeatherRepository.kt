package com.example.weatherapp.data.repository

import com.example.weatherapp.data.model.NetworkResult
import com.example.weatherapp.data.model.WeatherResponse

/**
 * Contract for fetching weather data.
 *
 * Keeping this as an interface lets the ViewModel be tested against a fake
 * without any real network calls — the ViewModel never depends on the concrete class.
 */
interface WeatherRepository {

    /**
     * Fetches current weather for the given city name.
     *
     * @param city Plain city name (e.g. "London", "Tokyo").
     * @return [NetworkResult.Success] with parsed data, or [NetworkResult.Error] with
     *         a user-displayable message if anything goes wrong.
     */
    suspend fun getWeatherByCity(city: String): NetworkResult<WeatherResponse>

    /**
     * Fetches current weather for a geographic coordinate pair.
     * Used when the user has granted location permission.
     *
     * @param lat Latitude in decimal degrees.
     * @param lon Longitude in decimal degrees.
     */
    suspend fun getWeatherByCoordinates(lat: Double, lon: Double): NetworkResult<WeatherResponse>
}
