package com.watchapp.util

import android.util.Log
import com.watchapp.shared.UnitsConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
data class OpenMeteoResponse(
    val elevation: Double? = null,
    val current: CurrentWeather? = null,
)

@Serializable
data class CurrentWeather(
    @SerialName("temperature_2m") val temperature: Double? = null,
    @SerialName("wind_speed_10m") val windSpeed: Double? = null,
    @SerialName("wind_direction_10m") val windDirection: Int? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    @SerialName("pressure_msl") val pressureMsl: Double? = null,
)

data class WeatherSnapshot(
    val temperature: String,
    val wind: String,
    val windDirectionDegrees: Int?,
    val conditions: String,
    /** Sea-level pressure (hPa) for barometric altitude — matches weather apps' QNH. */
    val pressureMslHpa: Float?,
    val elevationMeters: Double?,
)

class OpenMeteoClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(lat: Double, lon: Double, units: UnitsConfig): WeatherSnapshot = withContext(Dispatchers.IO) {
        val tempUnit = if (units.temperatureUnit == "celsius") "celsius" else "fahrenheit"
        val windUnit = if (units.windSpeedUnit == "kph") "kmh" else "mph"
        val url =
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,wind_speed_10m,wind_direction_10m,weather_code,pressure_msl" +
                "&temperature_unit=$tempUnit&wind_speed_unit=$windUnit&timezone=auto"

        Log.d(TAG, "Fetching $url")
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "WatchApp/1.0")
            .build()

        http.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: error("Empty weather response")
            if (!response.isSuccessful) {
                Log.e(TAG, "HTTP ${response.code}: $body")
                error("Weather API failed: ${response.code}")
            }
            val parsed = json.decodeFromString(OpenMeteoResponse.serializer(), body)
            val current = parsed.current ?: error("No current weather in response: $body")
            val tempSuffix = if (tempUnit == "celsius") "°C" else "°F"
            val windSuffix = if (windUnit == "kmh") " kph" else " mph"
            val temp = current.temperature?.let { "${it.toInt()}$tempSuffix" } ?: "--"
            val wind = current.windSpeed?.let { "${"%.1f".format(it)}$windSuffix" } ?: "--"
            val conditions = weatherCodeLabel(current.weatherCode)
            WeatherSnapshot(
                temperature = temp,
                wind = wind,
                windDirectionDegrees = current.windDirection,
                conditions = conditions,
                pressureMslHpa = current.pressureMsl?.toFloat(),
                elevationMeters = parsed.elevation,
            )
        }
    }

    private fun weatherCodeLabel(code: Int?): String = when (code) {
        0 -> "CLEAR"
        1, 2, 3 -> "CLOUDS"
        45, 48 -> "FOG"
        51, 53, 55, 56, 57 -> "DRIZZLE"
        61, 63, 65, 66, 67 -> "RAIN"
        71, 73, 75, 77 -> "SNOW"
        80, 81, 82 -> "SHOWERS"
        95, 96, 99 -> "STORM"
        null -> "--"
        else -> "UNKNOWN"
    }

    companion object {
        private const val TAG = "OpenMeteoClient"
    }
}
