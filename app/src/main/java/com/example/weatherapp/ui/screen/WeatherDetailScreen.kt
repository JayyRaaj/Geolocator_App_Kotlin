package com.example.weatherapp.ui.screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.weatherapp.data.model.WeatherResponse
import com.example.weatherapp.util.ImageCacheHelper

/**
 * Weather detail screen — stub replaced in Step 10.
 */
@Composable
fun WeatherDetailScreen(
    weather: WeatherResponse,
    imageCacheHelper: ImageCacheHelper,
    onSearchAgain: () -> Unit
) {
    Text(text = "Detail screen — coming in Step 10 (${weather.cityName})")
}
