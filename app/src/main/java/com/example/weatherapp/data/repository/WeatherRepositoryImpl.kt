package com.example.weatherapp.data.repository

import android.content.Context
import com.example.weatherapp.BuildConfig
import com.example.weatherapp.data.model.NetworkResult
import com.example.weatherapp.data.model.WeatherResponse
import com.example.weatherapp.data.remote.WeatherApiException
import com.example.weatherapp.data.remote.WeatherApiService
import com.example.weatherapp.util.NetworkMonitor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.IOException

/**
 * Production implementation of [WeatherRepository].
 *
 * Responsibilities:
 *  - Checks connectivity upfront via [NetworkMonitor] to give immediate offline feedback.
 *  - Dispatches blocking OkHttp calls onto [ioDispatcher] (default: [Dispatchers.IO]).
 *  - Delegates JSON parsing to [WeatherResponse.fromJson].
 *  - Catches every exception layer and maps it to a [NetworkResult.Error] with a
 *    user-displayable message so the ViewModel never needs to handle raw exceptions.
 *
 * @param apiService     OkHttp wrapper. Injected so it can be swapped in tests.
 * @param networkMonitor Connectivity checker. Injected for the same reason.
 * @param ioDispatcher   Dispatcher for network I/O. Inject [kotlinx.coroutines.test.UnconfinedTestDispatcher]
 *                       in unit tests to skip real thread-switching.
 *
 * TODO: ideally all params would be provided via a DI framework like Hilt, but keeping it simple for now.
 */
class WeatherRepositoryImpl(
    private val apiService: WeatherApiService,
    private val networkMonitor: NetworkMonitor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : WeatherRepository {

    override suspend fun getWeatherByCity(city: String): NetworkResult<WeatherResponse> =
        withContext(ioDispatcher) {
            fetchAndParse { apiService.fetchByCity(city) }
        }

    override suspend fun getWeatherByCoordinates(
        lat: Double,
        lon: Double
    ): NetworkResult<WeatherResponse> =
        withContext(ioDispatcher) {
            fetchAndParse { apiService.fetchByCoordinates(lat, lon) }
        }

    /**
     * Checks connectivity, invokes [block] to get raw JSON, then parses the result.
     *
     * Error priority:
     *  1. Offline check — immediate feedback, no waiting for a timeout.
     *  2. [WeatherApiException] — HTTP-level errors with pre-mapped messages.
     *  3. [IOException] — network failures that slipped past the offline check (racy window).
     *  4. [JSONException] — malformed API response.
     *  5. [Exception] — safety net for anything unexpected.
     */
    private fun fetchAndParse(block: () -> String): NetworkResult<WeatherResponse> {
        if (!networkMonitor.isConnected()) {
            return NetworkResult.Error(
                "No internet connection. Check your network and try again."
            )
        }

        return try {
            val json = block()
            val response = WeatherResponse.fromJson(json)
            NetworkResult.Success(response)
        } catch (e: WeatherApiException) {
            NetworkResult.Error(e.message ?: "Unknown API error.", e)
        } catch (e: IOException) {
            // Covers the racy window between the connectivity check and the actual call.
            NetworkResult.Error(
                "Could not reach the weather service. Check your connection and try again.",
                e
            )
        } catch (e: JSONException) {
            NetworkResult.Error(
                "Received unexpected data from the weather service. Please try again.",
                e
            )
        } catch (e: Exception) {
            NetworkResult.Error("Something went wrong. Please try again.", e)
        }
    }

    companion object {
        /**
         * Creates a production-wired [WeatherRepositoryImpl].
         *
         * Uses [Context.applicationContext] internally so the repository never holds
         * a reference to a short-lived Activity or Fragment.
         *
         * TODO: replace with Hilt @Provides / @Singleton when DI is added.
         */
        fun create(context: Context): WeatherRepositoryImpl {
            val appContext = context.applicationContext
            return WeatherRepositoryImpl(
                apiService = WeatherApiService.create(BuildConfig.OPENWEATHER_API_KEY),
                networkMonitor = NetworkMonitor(appContext)
            )
        }
    }
}
