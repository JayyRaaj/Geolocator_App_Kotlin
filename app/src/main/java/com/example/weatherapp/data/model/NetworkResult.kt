package com.example.weatherapp.data.model

/**
 * Sealed wrapper for network operation outcomes.
 *
 * Used by [com.example.weatherapp.data.repository.WeatherRepository] as a return type
 * and observed in [com.example.weatherapp.ui.viewmodel.WeatherViewModel] to drive UI state.
 *
 * TODO: ideally this would live in a separate domain layer, but keeping it simple for now.
 */
sealed class NetworkResult<out T> {

    /** The operation succeeded. [data] holds the parsed response. */
    data class Success<T>(val data: T) : NetworkResult<T>()

    /**
     * The operation failed.
     *
     * @param message User-facing error message (already localised or safe to display).
     * @param cause   The underlying exception, if any. Useful for logging; not shown to the user.
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : NetworkResult<Nothing>()

    /** The operation is currently in-flight. */
    data object Loading : NetworkResult<Nothing>()
}
