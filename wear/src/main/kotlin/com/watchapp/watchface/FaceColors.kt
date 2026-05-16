package com.watchapp.watchface

import android.graphics.Color
import kotlin.math.roundToInt

/** Colors sampled from [faces/preview.jpg] and weather-driven tints. */
object FaceColors {
    val background = Color.WHITE
    val divider = Color.parseColor("#ADD8E6")
    val tempCold = Color.parseColor("#2B4C9E")
    val tempHot = Color.parseColor("#C62828")
    val wind = Color.parseColor("#2B4C9E")
    val text = Color.BLACK
    val sunrise = Color.parseColor("#8B0000")
    val sunset = Color.parseColor("#003366")
    val ambientBackground = Color.BLACK
    val ambientTime = Color.parseColor("#FA8072")
    val ambientSub = Color.parseColor("#F0F0F0")
    val ambientTemp = Color.parseColor("#F5E6A3")
    val ambientWindAlert = Color.parseColor("#E53935")

    private const val WIND_ALERT_MPH = 15f
    private const val TEMP_COLD_F = 32f
    private const val TEMP_HOT_F = 95f

    fun activeTemperatureColor(temperature: String): Int {
        val f = parseTemperatureFahrenheit(temperature) ?: return tempCold
        val t = ((f - TEMP_COLD_F) / (TEMP_HOT_F - TEMP_COLD_F)).coerceIn(0f, 1f)
        return lerpArgb(tempCold, tempHot, t)
    }

    fun ambientWindColor(wind: String): Int {
        val mph = parseWindSpeedMph(wind)
        return if (mph != null && mph > WIND_ALERT_MPH) ambientWindAlert else ambientSub
    }

    private fun parseWindSpeedMph(wind: String): Float? {
        val normalized = wind.trim().lowercase()
        val value = Regex("([0-9.]+)").find(normalized)?.groupValues?.get(1)?.toFloatOrNull()
            ?: return null
        return if (normalized.contains("kph") || normalized.contains("km/h")) {
            value * 0.621371f
        } else {
            value
        }
    }

    private fun parseTemperatureFahrenheit(temperature: String): Float? {
        val normalized = temperature.trim()
        val match = Regex("(-?[0-9.]+)\\s*([FCfc])").find(normalized.replace("°", ""))
            ?: return null
        val value = match.groupValues[1].toFloatOrNull() ?: return null
        return when (match.groupValues[2].uppercase()) {
            "C" -> value * 9f / 5f + 32f
            "F" -> value
            else -> null
        }
    }

    private fun lerpArgb(from: Int, to: Int, fraction: Float): Int {
        val t = fraction.coerceIn(0f, 1f)
        return Color.argb(
            lerpChannel(Color.alpha(from), Color.alpha(to), t),
            lerpChannel(Color.red(from), Color.red(to), t),
            lerpChannel(Color.green(from), Color.green(to), t),
            lerpChannel(Color.blue(from), Color.blue(to), t),
        )
    }

    private fun lerpChannel(from: Int, to: Int, fraction: Float): Int =
        (from + (to - from) * fraction).roundToInt()
}
