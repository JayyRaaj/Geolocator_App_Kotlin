package com.example.weatherapp

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.weatherapp.ui.screen.SearchScreen
import com.example.weatherapp.ui.screen.WeatherDetailScreen
import com.example.weatherapp.ui.viewmodel.WeatherUiState
import com.example.weatherapp.ui.viewmodel.WeatherViewModel
import com.example.weatherapp.util.ImageCacheHelper
import com.example.weatherapp.util.LocationHelper
import kotlinx.coroutines.launch

/**
 * Single activity for the app. Owns:
 *  - The [WeatherViewModel] (via [viewModels]).
 *  - The location permission launcher (must be registered before [onCreate]).
 *  - [LocationHelper] and [ImageCacheHelper] as lazy singletons tied to the Application context.
 *
 * The activity itself contains minimal logic — it delegates everything to the ViewModel
 * and the Compose UI tree.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: WeatherViewModel by viewModels { WeatherViewModel.Factory }

    // Lazy so applicationContext is ready, and we avoid leaking the Activity into helpers
    // that may outlive the screen.
    private val locationHelper by lazy { LocationHelper(applicationContext) }
    private val imageCacheHelper by lazy { ImageCacheHelper(applicationContext) }

    /**
     * Must be registered before [onCreate] per the Activity Result API contract.
     * Receives the user's response to the location permission dialog.
     */
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Either fine or coarse location being granted is enough for a city-level lookup.
        val granted = permissions.values.any { it }
        if (granted) {
            loadWeatherForCurrentLocation()
        }
        // If denied: do nothing. The user can still search by city name.
        // We do not re-prompt — respecting the user's explicit decision.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                WeatherApp(
                    viewModel = viewModel,
                    imageCacheHelper = imageCacheHelper,
                    onRequestLocationPermission = ::requestLocationPermission
                )
            }
        }

        // GPS auto-load on first launch is wired up in Step 14.
        // Last-city auto-load on launch is wired up in Step 13.
    }

    /**
     * Launches the system permission dialog requesting fine and coarse location.
     * Requesting both lets the system grant at least coarse if the user declines fine.
     */
    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    /**
     * Fetches the device's current location and triggers a weather search.
     *
     * Called either after the permission grant result (Step 14), or directly if
     * permission is already held when the app launches.
     *
     * Internal visibility so it can also be called from [WeatherApp] callbacks
     * in tests without going through the permission launcher.
     */
    internal fun loadWeatherForCurrentLocation() {
        lifecycleScope.launch {
            val location = locationHelper.getCurrentLocation()
            if (location != null) {
                viewModel.searchByCoordinates(location.latitude, location.longitude)
            }
            // null means location services are off or no fix was available.
            // Fail silently — the search screen is still usable.
        }
    }
}

// ---------------------------------------------------------------------------
// Root composable
// ---------------------------------------------------------------------------

/**
 * Root of the Compose UI tree. Decides which screen to display based on [WeatherViewModel.uiState].
 *
 * Navigation strategy: [WeatherUiState.Success] → detail screen; everything else → search screen.
 * We avoid Jetpack Navigation to keep dependencies minimal for a two-screen app.
 *
 * @param viewModel              Shared ViewModel instance from the Activity.
 * @param imageCacheHelper       Passed down to the detail screen for icon loading.
 * @param onRequestLocationPermission Callback that triggers the system permission dialog.
 */
@Composable
fun WeatherApp(
    viewModel: WeatherViewModel,
    imageCacheHelper: ImageCacheHelper,
    onRequestLocationPermission: () -> Unit
) {
    val uiState    by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    // Intercept the system back button while the detail screen is showing,
    // so the user goes back to the search screen instead of exiting the app.
    if (uiState is WeatherUiState.Success) {
        BackHandler { viewModel.resetToSearch() }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = uiState) {
            is WeatherUiState.Success -> {
                WeatherDetailScreen(
                    weather = state.weather,
                    imageCacheHelper = imageCacheHelper,
                    onSearchAgain = { viewModel.resetToSearch() }
                )
            }
            else -> {
                SearchScreen(
                    uiState = uiState,
                    searchQuery = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onSearch = { viewModel.searchByCity() },
                    onRequestLocationPermission = onRequestLocationPermission,
                    onDismissError = { viewModel.clearError() }
                )
            }
        }
    }
}
