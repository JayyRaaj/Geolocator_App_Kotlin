package com.example.weatherapp.ui.screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.weatherapp.ui.viewmodel.WeatherUiState

/**
 * Search screen — stub replaced in Step 9.
 */
@Composable
fun SearchScreen(
    uiState: WeatherUiState,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onDismissError: () -> Unit
) {
    Text(text = "Search screen — coming in Step 9")
}
