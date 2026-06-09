package com.example.weatherapp.data.repository

import com.example.weatherapp.BuildConfig
import com.example.weatherapp.data.model.NetworkResult
import com.example.weatherapp.data.model.WeatherResponse
import com.example.weatherapp.data.remote.WeatherApiException
import com.example.weatherapp.data.remote.WeatherApiService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.IOException

/**
 * Production implementation of [WeatherRepository].
 *
 * Responsibilities:
 *  - Dispatches blocking OkHttp calls onto [ioDispatcher] (default: [Dispatchers.IO]).
 *  - Delegates JSON parsing to [WeatherResponse.fromJson].
 *  - Catches every exception layer and maps it to a [NetworkResult.Error] with a
 *    user-displayable message so the ViewModel never needs to handle raw exceptions.
 *
 * @param apiService    OkHttp wrapper. Injected so it can be replaced in tests.
 * @param ioDispatcher  Dispatcher for network I/O. Injected so unit tests can use
 *                      [kotlinx.coroutines.test.UnconfinedTestDispatcher] without real threads.
 *
 * TODO: ideally both params would be provided via a DI framework like Hilt, but keeping it simple for now.
 */
class WeatherRepositoryImpl(
    private val apiService: WeatherApiService,
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
     * Runs [block] to obtain a raw JSON string, then parses it.
     * All exception types are caught here so neither callers above nor the ViewModel
     * need separate try/catch blocks.
     */
    private fun fetchAndParse(block: () -> String): NetworkResult<WeatherResponse> {
        return try {
            val json = block()
            val response = WeatherResponse.fromJson(json)
            NetworkResult.Success(response)
        } catch (e: WeatherApiException) {
            // HTTP-level errors (404, 401, etc.) — message already user-friendly
            NetworkResult.Error(e.message ?: "Unknown API error.", e)
        } catch (e: IOException) {
            // Network unreachable, timeout, SSL handshake failure, etc.
            NetworkResult.Error(
                "No internet connection. Check your network and try again.",
                e
            )
        } catch (e: JSONException) {
            // The API returned something we couldn't parse — shouldn't happen in normal operation
            NetworkResult.Error(
                "Received unexpected data from the weather service. Please try again.",
                e
            )
        } catch (e: Exception) {
            // Safety net for anything else (e.g. OOM, unexpected runtime errors)
            NetworkResult.Error("Something went wrong. Please try again.", e)
        }
    }

    companion object {
        /**
         * Creates a ready-to-use [WeatherRepositoryImpl] wired up to the live API.
         *
         * Call this from [com.example.weatherapp.MainActivity] or wherever the ViewModel
         * is constructed. The API key is read from [BuildConfig] so it never appears as
         * a plain string in business-logic code.
         *
         * TODO: ideally this factory would be replaced by Hilt's @Provides / @Singleton,
         * but a companion object works fine for a single-module app without DI.
         */
        fun create(): WeatherRepositoryImpl {
            val apiService = WeatherApiService.create(BuildConfig.OPENWEATHER_API_KEY)
            return WeatherRepositoryImpl(apiService)
        }
    }
}
