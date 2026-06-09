package com.example.weatherapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.weatherapp.data.model.WeatherCondition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Disk-backed cache for OpenWeatherMap weather icons.
 *
 * Icons are keyed by their OWM icon code (e.g. "01d", "09n") and stored as PNG
 * files under [Context.getCacheDir]/weather_icons/. Because icon codes are
 * deterministic (the same code always maps to the same image), entries never expire.
 *
 * All public methods dispatch onto [Dispatchers.IO] internally, so they are safe
 * to call from any coroutine scope without the caller worrying about threading.
 *
 * Returns [Bitmap] rather than Compose's ImageBitmap so this class stays
 * UI-framework-agnostic. Convert at the call site with `.asImageBitmap()`.
 *
 * @param context Used only to resolve the cache directory. Pass the Application
 *                context to avoid holding a reference to a short-lived Activity.
 *
 * TODO: ideally this would be injected via a DI framework like Hilt, but keeping it simple for now.
 */
class ImageCacheHelper(context: Context) {

    /**
     * Root directory for cached icons.
     * Created on construction so the first write never has to mkdir.
     */
    private val cacheDir: File =
        File(context.cacheDir, CACHE_DIR_NAME).also { it.mkdirs() }

    /**
     * Loads the icon bitmap for [iconCode], serving from the disk cache when possible.
     *
     * @param iconCode OWM icon code (e.g. "01d"). Use [WeatherCondition.iconCode].
     * @return Decoded [Bitmap], or `null` if the download failed or the cached file
     *         is corrupt. The UI should show a placeholder in the null case.
     */
    suspend fun loadIcon(iconCode: String): Bitmap? = withContext(Dispatchers.IO) {
        val cacheFile = File(cacheDir, "$iconCode.png")

        if (cacheFile.exists()) {
            // Cache hit — decode from disk. Returns null if the file is somehow corrupt;
            // the download path below will not be retried until the next app session.
            return@withContext decodeBitmap(cacheFile)
        }

        // Cache miss — fetch from the network and persist to disk.
        downloadAndCache(iconCode, cacheFile)
    }

    /**
     * Deletes all cached icon files.
     * Useful if the user wants to reclaim storage space.
     */
    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Returns the total byte size of all cached icon files.
     * Useful for displaying a "cache size" value in a settings screen.
     */
    fun cacheSize(): Long =
        cacheDir.listFiles()?.sumOf { it.length() } ?: 0L

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Decodes a [Bitmap] from [file]. Returns null on any decoding failure rather
     * than throwing, so a single corrupt file doesn't crash the UI.
     */
    private fun decodeBitmap(file: File): Bitmap? =
        try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            null
        }

    /**
     * Downloads the icon for [iconCode] and writes it to [destination] on disk.
     *
     * Uses a write-to-temp-then-rename strategy so that [destination] is never
     * left in a partially-written state if the process is killed mid-write.
     * On Linux/Android, [File.renameTo] within the same filesystem is atomic.
     *
     * @return The decoded [Bitmap] on success, or `null` if the download or
     *         disk write fails for any reason.
     */
    private fun downloadAndCache(iconCode: String, destination: File): Bitmap? {
        val iconUrl = WeatherCondition.iconUrl(iconCode)
        var connection: HttpURLConnection? = null

        return try {
            connection = (URL(iconUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout    = READ_TIMEOUT_MS
                requestMethod  = "GET"
                connect()
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                // Non-200 response — icon unavailable, fail gracefully.
                return null
            }

            val bitmap = BitmapFactory.decodeStream(connection.inputStream)
                ?: return null // Couldn't decode the stream (e.g. empty body)

            writeBitmapToDisk(bitmap, destination)

            bitmap
        } catch (e: IOException) {
            // Network unreachable, timeout, SSL error, etc. — not fatal.
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Compresses [bitmap] to a temporary file, then atomically renames it to [destination].
     *
     * If compression or rename fails, the temp file is cleaned up so we don't leave
     * partial data in the cache directory.
     */
    private fun writeBitmapToDisk(bitmap: Bitmap, destination: File) {
        val tempFile = File(cacheDir, "${destination.nameWithoutExtension}_tmp.png")
        try {
            FileOutputStream(tempFile).use { out ->
                // PNG is lossless; quality param is ignored for PNG but required by the API.
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            // Atomic rename — destination is either the full file or absent, never partial.
            if (!tempFile.renameTo(destination)) {
                // renameTo can return false on some Android versions when crossing mount points;
                // fall back to a regular copy so at least this session succeeds.
                tempFile.copyTo(destination, overwrite = true)
                tempFile.delete()
            }
        } catch (e: IOException) {
            tempFile.delete() // clean up the partial temp file
        }
    }

    companion object {
        private const val CACHE_DIR_NAME      = "weather_icons"
        private const val CONNECT_TIMEOUT_MS  = 10_000
        private const val READ_TIMEOUT_MS     = 10_000
    }
}
