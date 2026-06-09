package com.example.weatherapp.data.model

import org.json.JSONObject

/**
 * Wind readings from the "wind" block of the API response.
 *
 * Speed is in metres per second when the request includes units=metric.
 */
data class Wind(
    val speedMps: Double,
    /**
     * Wind direction in meteorological degrees (0 = North, 90 = East).
     * Nullable because the API omits this field for calm conditions.
     */
    val directionDeg: Int?
) {
    companion object {
        /** Parses the "wind" block from the API response JSON. */
        fun fromJson(json: JSONObject): Wind = Wind(
            speedMps = json.getDouble("speed"),
            directionDeg = if (json.has("deg")) json.getInt("deg") else null
        )
    }
}
