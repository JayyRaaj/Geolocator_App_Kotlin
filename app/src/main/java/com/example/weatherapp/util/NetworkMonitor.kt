package com.example.weatherapp.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Checks whether the device currently has a working internet connection.
 *
 * Used by the repository to give the user an immediate "you're offline" message
 * rather than waiting for OkHttp's 10-second timeout to expire.
 *
 * Note: connectivity checks are inherently racy — the network can drop between
 * [isConnected] returning true and the actual HTTP call. The repository's
 * IOException handler remains the authoritative safety net for that window.
 *
 * Requires the ACCESS_NETWORK_STATE permission (declared in AndroidManifest).
 *
 * @param context Application context — not stored beyond constructor use.
 *
 * TODO: ideally this would be injected via a DI framework like Hilt, but keeping it simple for now.
 */
class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Returns `true` if the active network has validated internet access.
     *
     * [NetworkCapabilities.NET_CAPABILITY_VALIDATED] means Android has confirmed
     * the network can actually reach the internet — not just that WiFi is connected
     * (a hotspot with no data would pass the WiFi check but fail VALIDATED).
     */
    fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
