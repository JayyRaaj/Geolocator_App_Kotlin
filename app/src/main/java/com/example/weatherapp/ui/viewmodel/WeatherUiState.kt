package com.example.weatherapp.ui.viewmodel

import com.example.weatherapp.data.model.WeatherResponse

/**
 * Represents every state the weather screen can be in.
 *
 * The ViewModel exposes this as a [kotlinx.coroutines.flow.StateFlow] so Compose
 * can collect it and recompose only when the state actually changes.
 */
sealed class WeatherUiState {

    /** Initial state — nothing has been searched yet. */
    data object Idle : WeatherUiState()

    /** A network request is in-flight. Show a loading indicator. */
    data object Loading : WeatherUiState()

    /**
     * The request succeeded.
     *
     * @param weather Parsed API response ready for display.
     */
    data class Success(val weather: WeatherResponse) : WeatherUiState()

    /**
     * The request failed.
     *
     * @param message User-displayable description of what went wrong.
     *                Already mapped from HTTP codes / IOExceptions by the repository.
     */
    data class Error(val message: String) : WeatherUiState()
}
