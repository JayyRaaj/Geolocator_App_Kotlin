package com.example.weatherapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.data.model.NetworkResult
import com.example.weatherapp.data.repository.WeatherRepository
import com.example.weatherapp.data.repository.WeatherRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the weather feature.
 *
 * Survives configuration changes and owns all UI-related state.
 * The UI layer never talks to the repository directly — only through here.
 *
 * @param repository Data source. Injected so the ViewModel can be tested with a fake.
 *
 * TODO: ideally the repository would be injected via Hilt's @HiltViewModel, but keeping it simple for now.
 */
class WeatherViewModel(
    private val repository: WeatherRepository
) : ViewModel() {

    // --- UI state -----------------------------------------------------------------

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)

    /** The current state of the weather screen. Collected by Compose. */
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    // --- Search query -------------------------------------------------------------

    private val _searchQuery = MutableStateFlow("")

    /**
     * The text currently typed in the search field.
     * Stored in the ViewModel so it survives screen rotation.
     */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // --- Public actions -----------------------------------------------------------

    /**
     * Updates the search field text. Called on every keystroke from the UI.
     *
     * Does not trigger a network call — the user must submit explicitly.
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * Kicks off a city-name weather search.
     *
     * Trims whitespace and silently ignores blank input so the UI doesn't
     * need to guard against accidental empty submissions.
     *
     * @param city City name (e.g. "Tokyo"). Uses [searchQuery] if blank.
     */
    fun searchByCity(city: String = _searchQuery.value) {
        val trimmed = city.trim()
        if (trimmed.isEmpty()) return

        // Keep the query field in sync in case the caller passed a city directly
        // (e.g. when auto-loading the last searched city on app launch).
        _searchQuery.value = trimmed

        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            _uiState.value = when (val result = repository.getWeatherByCity(trimmed)) {
                is NetworkResult.Success -> WeatherUiState.Success(result.data)
                is NetworkResult.Error   -> WeatherUiState.Error(result.message)
                is NetworkResult.Loading -> WeatherUiState.Loading // repository never emits this
            }
        }
    }

    /**
     * Kicks off a coordinate-based weather lookup.
     * Called after a successful GPS fix from [com.example.weatherapp.util.LocationHelper].
     *
     * @param lat Latitude in decimal degrees.
     * @param lon Longitude in decimal degrees.
     */
    fun searchByCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            _uiState.value = when (val result = repository.getWeatherByCoordinates(lat, lon)) {
                is NetworkResult.Success -> WeatherUiState.Success(result.data)
                is NetworkResult.Error   -> WeatherUiState.Error(result.message)
                is NetworkResult.Loading -> WeatherUiState.Loading
            }
        }
    }

    /**
     * Resets the screen back to [WeatherUiState.Idle].
     * Called after the user dismisses an error message.
     */
    fun clearError() {
        if (_uiState.value is WeatherUiState.Error) {
            _uiState.value = WeatherUiState.Idle
        }
    }

    // --- Factory ------------------------------------------------------------------

    /**
     * [ViewModelProvider.Factory] that constructs a [WeatherViewModel] wired to the
     * live repository.
     *
     * Usage in an Activity/Fragment:
     * ```
     * val viewModel: WeatherViewModel by viewModels { WeatherViewModel.Factory }
     * ```
     *
     * TODO: replace with @HiltViewModel + hiltViewModel() when DI is added.
     */
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
                    return WeatherViewModel(WeatherRepositoryImpl.create()) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
