package com.example.weatherapp.ui.screen

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.weatherapp.ui.viewmodel.WeatherUiState

/**
 * Search screen — the first screen the user sees.
 *
 * Handles three visible states:
 *  - [WeatherUiState.Idle]    — empty, just the search form.
 *  - [WeatherUiState.Loading] — form disabled, spinner inside the search button.
 *  - [WeatherUiState.Error]   — form re-enabled, inline error card below the buttons.
 *
 * [WeatherUiState.Success] is never passed here — [com.example.weatherapp.WeatherApp]
 * routes that state to [WeatherDetailScreen] instead.
 *
 * @param uiState                     Current state from the ViewModel.
 * @param searchQuery                 Current text in the search field.
 * @param onQueryChange               Called on every keystroke.
 * @param onSearch                    Called when the user submits a search.
 * @param onRequestLocationPermission Triggers the system location permission dialog.
 * @param onDismissError              Resets state to Idle after an error is shown.
 * @param onRetry                     Replays the last search — shown as a button in the error card.
 */
@Composable
fun SearchScreen(
    uiState: WeatherUiState,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onDismissError: () -> Unit,
    onRetry: () -> Unit
) {
    val isLoading = uiState is WeatherUiState.Loading
    val errorMessage = (uiState as? WeatherUiState.Error)?.message
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            // Scrollable so the error card is accessible on small screens / with keyboard open
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Push content down to roughly the upper-third of the screen
        Spacer(modifier = Modifier.height(96.dp))

        // ── App title ──────────────────────────────────────────────────────
        Text(
            text = "Weather",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Search a city to see current conditions",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // ── Search field ───────────────────────────────────────────────────
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("City name") },
            placeholder = { Text("e.g. London, Tokyo, New York") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    onSearch()
                }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Search button ──────────────────────────────────────────────────
        Button(
            onClick = {
                keyboardController?.hide()
                onSearch()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            // Disabled while loading, or if the field is blank (nothing to search)
            enabled = !isLoading && searchQuery.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Location button ────────────────────────────────────────────────
        OutlinedButton(
            onClick = onRequestLocationPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !isLoading
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Use my location",
                style = MaterialTheme.typography.labelLarge
            )
        }

        // ── Error card (only shown in Error state) ─────────────────────────
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(24.dp))
            SearchErrorCard(
                message = errorMessage,
                onDismiss = onDismissError,
                onRetry = onRetry
            )
        }

        // Bottom padding so content isn't flush against the navigation bar
        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ---------------------------------------------------------------------------
// Private composables
// ---------------------------------------------------------------------------

/**
 * Inline error card shown below the search buttons.
 *
 * Chose an inline card over a Snackbar because:
 *  - It stays visible until explicitly dismissed (Snackbar auto-hides).
 *  - It's easier to notice on screens where the keyboard is closed.
 *
 * The Retry button replays the last search so the user doesn't have to retype.
 * The Dismiss button clears the error and returns to Idle.
 */
@Composable
private fun SearchErrorCard(
    message: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        // Error message row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss error",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Action buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Dismiss",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onRetry) {
                Text(
                    text = "Retry",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
