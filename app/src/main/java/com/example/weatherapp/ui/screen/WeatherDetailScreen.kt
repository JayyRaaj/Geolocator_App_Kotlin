package com.example.weatherapp.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.weatherapp.data.model.WeatherResponse
import com.example.weatherapp.data.model.Wind
import com.example.weatherapp.util.ImageCacheHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Shows current weather for a single location.
 *
 * Laid out as:
 *  - Top bar with back / "search again" action
 *  - Hero: city name, date, animated weather icon, temperature, description, feels-like
 *  - High/Low temperature chips
 *  - 2×2 stat grid: humidity, wind, sunrise, sunset
 *
 * @param weather          Parsed API response to display.
 * @param imageCacheHelper Used to load the weather icon from cache or network.
 * @param onSearchAgain    Navigates back to the search screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailScreen(
    weather: WeatherResponse,
    imageCacheHelper: ImageCacheHelper,
    onSearchAgain: () -> Unit
) {
    // Load the weather icon when the icon code changes (e.g. after a new search while already
    // on this screen). ImageCacheHelper is already dispatched on IO, so this is safe here.
    var iconBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(weather.condition.iconCode) {
        iconBitmap = imageCacheHelper.loadIcon(weather.condition.iconCode)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* intentionally empty — city name is in the hero section */ },
                navigationIcon = {
                    IconButton(onClick = onSearchAgain) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to search"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onSearchAgain) {
                        Text("Search again")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Hero ───────────────────────────────────────────────────────
            Text(
                text = "${weather.cityName}, ${weather.countryCode}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = formatDate(weather.timestamp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            WeatherIconImage(
                bitmap = iconBitmap,
                contentDescription = weather.condition.description
            )

            Text(
                text = "${weather.main.tempCelsius.roundToInt()}°C",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Light
            )

            Text(
                text = weather.condition.description.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Feels like ${weather.main.feelsLikeCelsius.roundToInt()}°C",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── High / Low chips ───────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TemperatureChip(label = "H", temp = weather.main.tempMaxCelsius.roundToInt())
                Spacer(modifier = Modifier.width(32.dp))
                TemperatureChip(label = "L", temp = weather.main.tempMinCelsius.roundToInt())
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Stats grid ─────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.WaterDrop,
                    label = "Humidity",
                    value = "${weather.main.humidityPercent}%"
                )
                Spacer(modifier = Modifier.width(12.dp))
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Air,
                    label = "Wind",
                    value = formatWind(weather.wind)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.WbSunny,
                    label = "Sunrise",
                    value = formatTime(weather.sunriseTimestamp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Nightlight,
                    label = "Sunset",
                    value = formatTime(weather.sunsetTimestamp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Private composables
// ---------------------------------------------------------------------------

/**
 * Displays the weather icon bitmap.
 * Shows a neutral placeholder box while the image is loading or if it failed to load.
 */
@Composable
private fun WeatherIconImage(
    bitmap: Bitmap?,
    contentDescription: String
) {
    Box(
        modifier = Modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Placeholder shown while the icon is being loaded from cache or network
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
            )
        }
    }
}

/**
 * Small pill showing a labelled temperature value (e.g. "H  28°C").
 */
@Composable
private fun TemperatureChip(label: String, temp: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$temp°C",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Card for a single weather stat (icon + label + value).
 * Used in the 2×2 grid for humidity, wind, sunrise, and sunset.
 */
@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Formatting helpers
// ---------------------------------------------------------------------------

/** Formats a Unix timestamp (seconds) as a full weekday date, e.g. "Tuesday, 3 Jun". */
private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("EEEE, d MMM", Locale.getDefault())
        .format(Date(timestamp * 1000L))

/** Formats a Unix timestamp (seconds) as a 24-hour time string, e.g. "06:14". */
private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault())
        .format(Date(timestamp * 1000L))

/**
 * Formats wind speed and optional direction as a single string.
 * Example outputs: "4.2 m/s NE", "1.5 m/s" (when direction is absent).
 */
private fun formatWind(wind: Wind): String {
    // Locale.ROOT ensures "4.2 m/s" regardless of the device's regional settings
    // (some locales use a comma as the decimal separator).
    val speed = "%.1f m/s".format(Locale.ROOT, wind.speedMps)
    val cardinal = wind.directionDeg?.let { " ${degreesToCardinal(it)}" }.orEmpty()
    return "$speed$cardinal"
}

/**
 * Converts meteorological wind degrees to an 8-point cardinal direction label.
 * 0° / 360° = North, 90° = East, 180° = South, 270° = West.
 */
private fun degreesToCardinal(degrees: Int): String {
    val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    // +22 offsets the boundaries so that e.g. 0–22° and 338–360° both map to "N"
    return directions[((degrees + 22) / 45) % 8]
}
