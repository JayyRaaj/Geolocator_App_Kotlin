package com.example.weatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Entry point for the app. Will be fleshed out in Step 8.
 * For now it just confirms the project builds and Compose is wired up.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlaceholderScreen()
        }
    }
}

/** Temporary placeholder — replaced in Step 8 with the real UI. */
@Composable
private fun PlaceholderScreen() {
    Text(text = "Weather App — coming soon")
}
