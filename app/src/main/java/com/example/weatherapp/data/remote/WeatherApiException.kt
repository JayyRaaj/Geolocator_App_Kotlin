package com.example.weatherapp.data.remote

/**
 * Thrown by [WeatherApiService] when the server returns a non-2xx HTTP response.
 *
 * Carries a user-displayable message so the repository can forward it to the UI
 * without ever inspecting raw HTTP status codes itself.
 */
class WeatherApiException(message: String) : Exception(message) {

    companion object {
        /**
         * Maps well-known OpenWeatherMap HTTP codes to readable error strings.
         * Falls back to a generic message for anything unexpected.
         */
        fun fromHttpCode(code: Int): WeatherApiException = when (code) {
            401 -> WeatherApiException("Invalid API key. Check your OpenWeatherMap credentials.")
            404 -> WeatherApiException("City not found. Try a different city name.")
            429 -> WeatherApiException("Too many requests. Please wait a moment and try again.")
            in 500..599 -> WeatherApiException("Weather service is unavailable. Try again later.")
            else -> WeatherApiException("Unexpected error from weather service (HTTP $code).")
        }
    }
}
