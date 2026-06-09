package com.example.weatherapp.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Low-level OkHttp wrapper for the OpenWeatherMap current-weather endpoint.
 *
 * All methods are synchronous — they must be called from a background thread.
 * The repository dispatches them on [kotlinx.coroutines.Dispatchers.IO].
 *
 * @param client OkHttp client. Injected so tests can swap in a mock/fake client.
 * @param apiKey OpenWeatherMap API key.
 *
 * TODO: ideally both params would be provided via a DI framework like Hilt, but keeping it simple for now.
 */
class WeatherApiService(
    private val client: OkHttpClient,
    private val apiKey: String
) {

    /**
     * Fetches current weather for a city by name.
     *
     * @param city City name as a plain string (e.g. "London", "New York").
     *             OkHttp's URL builder percent-encodes it automatically.
     * @return Raw JSON string from the API. Parsing is left to the caller.
     * @throws WeatherApiException on non-2xx HTTP responses.
     * @throws java.io.IOException on network-level failures (no connectivity, timeout, etc.).
     */
    fun fetchByCity(city: String): String {
        val url = BASE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("q", city)
            .addQueryParameter("appid", apiKey)
            .addQueryParameter("units", UNITS)
            .build()

        return executeRequest(url.toString())
    }

    /**
     * Fetches current weather for a geographic coordinate pair.
     * Called when the user grants location permission and we have a GPS fix.
     *
     * @param lat Latitude in decimal degrees.
     * @param lon Longitude in decimal degrees.
     * @return Raw JSON string from the API.
     * @throws WeatherApiException on non-2xx HTTP responses.
     * @throws java.io.IOException on network-level failures.
     */
    fun fetchByCoordinates(lat: Double, lon: Double): String {
        val url = BASE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("lat", lat.toString())
            .addQueryParameter("lon", lon.toString())
            .addQueryParameter("appid", apiKey)
            .addQueryParameter("units", UNITS)
            .build()

        return executeRequest(url.toString())
    }

    /**
     * Executes a GET request and returns the body as a string.
     *
     * Uses [okhttp3.Response.use] to guarantee the body is closed even if an
     * exception is thrown, preventing connection pool leaks.
     */
    private fun executeRequest(url: String): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = client.newCall(request).execute()
        response.use {
            if (!response.isSuccessful) {
                throw WeatherApiException.fromHttpCode(response.code)
            }
            // body is non-null for successful responses, but we guard defensively
            return response.body?.string()
                ?: throw WeatherApiException("Empty response body from weather API.")
        }
    }

    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/weather"

        /** Request metric units: temperatures in °C, wind speed in m/s. */
        private const val UNITS = "metric"

        /**
         * Convenience factory that builds a [WeatherApiService] with sensible timeouts.
         *
         * TODO: ideally the OkHttpClient would be a singleton provided by DI so it is
         * shared across the app and connection pool reuse is maximised — keeping it simple for now.
         */
        fun create(apiKey: String): WeatherApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
            return WeatherApiService(client, apiKey)
        }
    }
}
