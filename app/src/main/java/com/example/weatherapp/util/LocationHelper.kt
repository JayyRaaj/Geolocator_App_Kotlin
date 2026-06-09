package com.example.weatherapp.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Wraps [com.google.android.gms.location.FusedLocationProviderClient] and converts its
 * callback-based API into suspend functions for use with coroutines.
 *
 * This class only reads location — it never requests permissions.
 * Permission prompts are the Activity's responsibility.
 *
 * @param context Used to create the Fused Location client and check permissions.
 *                Pass the Application context to avoid Activity leaks if this
 *                helper outlives the screen.
 *
 * TODO: ideally this would be injected via a DI framework like Hilt, but keeping it simple for now.
 */
class LocationHelper(private val context: Context) {

    // FusedLocationProviderClient is internally thread-safe.
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    /**
     * Returns `true` if the app currently holds either fine or coarse location permission.
     *
     * The Activity should call this before [getCurrentLocation] and show a rationale
     * or launch the permission request if it returns `false`.
     */
    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Returns the best available device location, or `null` if location is unavailable.
     *
     * Strategy (two-step to balance speed vs. freshness):
     *  1. Try [FusedLocationProviderClient.lastLocation] — instant, zero battery cost.
     *  2. If that returns null (device just booted, or GPS was off), request a fresh
     *     single-shot fix via [FusedLocationProviderClient.getCurrentLocation].
     *
     * Returns `null` when:
     *  - Location permission is not granted.
     *  - Location services are disabled on the device.
     *  - A fresh fix could not be obtained (e.g. indoors with no GPS signal).
     *
     * Never throws — all failure paths return null so callers can treat it as
     * "location unavailable" without needing try/catch blocks.
     */
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) return null

        // Fast path: use the cached last-known fix if the provider has one.
        val cached = fetchLastKnownLocation()
        if (cached != null) return cached

        // Slow path: ask for a fresh single-shot fix.
        return fetchFreshLocation()
    }

    /**
     * Reads the most recent cached location from the Fused Location provider.
     * Typically returns immediately. May return null if the device has just
     * rebooted or if location services were recently re-enabled.
     */
    private suspend fun fetchLastKnownLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            try {
                fusedClient.lastLocation
                    .addOnSuccessListener { location ->
                        continuation.resume(location)
                    }
                    .addOnFailureListener {
                        // Failure here is non-fatal; the slow path will try next.
                        continuation.resume(null)
                    }
            } catch (e: SecurityException) {
                // Permission was revoked between the hasLocationPermission() check and this call.
                continuation.resume(null)
            }
        }

    /**
     * Requests a fresh single-shot location fix.
     *
     * Uses [Priority.PRIORITY_BALANCED_POWER_ACCURACY] — accurate enough for
     * city-level weather lookups without draining the battery with full GPS.
     *
     * Cancels the underlying Fused Location request if the coroutine is cancelled
     * (e.g. the user navigates away before a fix arrives), preventing background
     * location work from continuing unnecessarily.
     */
    private suspend fun fetchFreshLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            // Wire coroutine cancellation into the Fused Location cancellation token
            // so we don't leave an orphaned location request running.
            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }

            try {
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token
                )
                    .addOnSuccessListener { location ->
                        continuation.resume(location)
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }
            } catch (e: SecurityException) {
                // Defensive guard — same rationale as fetchLastKnownLocation.
                continuation.resume(null)
            }
        }
}
