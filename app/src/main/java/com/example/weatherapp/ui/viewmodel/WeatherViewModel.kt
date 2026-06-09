package com.example.weatherapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
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

    // --- Last search (for retry) --------------------------------------------------

    /**
     * Tracks the most recent search so [retry] can replay it exactly.
     * Sealed so we know whether to re-search by city or by coordinates.
     */
    private sealed class LastSearch {
        data class ByCity(val city: String) : LastSearch()
        data class ByCoordinates(val lat: Double, val lon: Double) : LastSearch()
    }

    private var lastSearch: LastSearch? = null

    // --- Public actions -----------------------------------------------------------

    /**
     * Updates the search field text. Called on every keystroke from the UI.
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
     * @param city City name (e.g. "Tokyo"). Defaults to the current [searchQuery] value
     *             so callers that auto-load a saved city don't have to touch the field.
     */
    fun searchByCity(city: String = _searchQuery.value) {
        val trimmed = city.trim()
        if (trimmed.isEmpty()) return

        _searchQuery.value = trimmed
        lastSearch = LastSearch.ByCity(trimmed)

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
        lastSearch = LastSearch.ByCoordinates(lat, lon)

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
     * Replays the most recent search — useful after a network error.
     * Does nothing if no search has been made yet in this session.
     */
    fun retry() {
        when (val last = lastSearch) {
            is LastSearch.ByCity        -> searchByCity(last.city)
            is LastSearch.ByCoordinates -> searchByCoordinates(last.lat, last.lon)
            null                        -> { /* no previous search to replay */ }
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

    /**
     * Navigates back to the search screen from any state.
     * Called when the user presses back or taps "Search again" on the detail screen.
     */
    fun resetToSearch() {
        _uiState.value = WeatherUiState.Idle
    }

    // --- Factory ------------------------------------------------------------------

    companion object {
        /**
         * [ViewModelProvider.Factory] that constructs a [WeatherViewModel] wired to the
         * live repository.
         *
         * Uses [CreationExtras] + [APPLICATION_KEY] to get the Application context without
         * making WeatherViewModel extend AndroidViewModel — keeps the ViewModel pure and
         * easier to test.
         *
         * TODO: replace with @HiltViewModel + hiltViewModel() when DI is added.
         */
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY]) {
                    "WeatherViewModel.Factory requires APPLICATION_KEY in CreationExtras"
                }
                if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
                    return WeatherViewModel(WeatherRepositoryImpl.create(application)) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
