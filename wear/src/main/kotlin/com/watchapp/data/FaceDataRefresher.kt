package com.watchapp.data

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.watchapp.sensor.BarometricAltitude
import com.watchapp.util.OpenMeteoClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.shredzone.commons.suncalc.SunTimes
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object FaceDataRefresher {
    private const val TAG = "FaceDataRefresher"
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    suspend fun refresh(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val config = ConfigRepository(context).getConfig()
            val (lat, lon) = LocationResolver.getCurrent(context)
            FlightLocationStore.update(lat, lon)

            val weather = runCatching {
                OpenMeteoClient().fetch(lat, lon, config.units)
            }.onFailure { Log.w(TAG, "Weather failed", it) }
                .getOrNull()

            BarometricAltitude.seaLevelPressureHpa = weather?.pressureMslHpa
            BarometricAltitude.baseAltitudeFeet = config.units.barometricBaseAltitudeFeet
            val altitudeMsl = BarometricAltitude.formatMsl()
                ?: weather?.elevationMeters?.let { BarometricAltitude.formatElevationMeters(it) }
                ?: resolveGpsAltitudeMsl(context)

            val locationName = resolvePlaceName(context, lat, lon)
            val times = runCatching {
                SunTimes.compute().on(LocalDate.now()).at(lat, lon).execute()
            }.onFailure { Log.w(TAG, "Sun calc failed", it) }
                .getOrNull()

            val sunrise = times?.rise?.toLocalTime()?.format(timeFmt) ?: "--:--"
            val sunset = times?.set?.toLocalTime()?.format(timeFmt) ?: "--:--"

            val now = ZonedDateTime.now()
            val tz1 = runCatching { ZoneId.of(config.timezone.secondaryTimezone) }.getOrElse { ZoneId.systemDefault() }
            val tz2 = runCatching { ZoneId.of(config.timezone.ambientSecondTimezone) }.getOrElse { ZoneId.systemDefault() }

            WatchDataStore.replace(
                context,
                WatchFaceData(
                    temperature = weather?.temperature ?: "--",
                    wind = weather?.wind ?: "--",
                    windDirectionDegrees = weather?.windDirectionDegrees,
                    conditions = weather?.conditions ?: "--",
                    sunrise = sunrise,
                    sunset = sunset,
                    location = locationName,
                    altitude = altitudeMsl,
                    ambientTz1 = "${config.timezone.secondaryLabel} ${now.withZoneSameInstant(tz1).format(timeFmt)}",
                    ambientTz2 = "${config.timezone.ambientSecondLabel} ${now.withZoneSameInstant(tz2).format(timeFmt)}",
                ),
            )
            Log.i(TAG, "Refresh complete: ${WatchDataStore.get()}")
            weather != null
        } catch (e: Exception) {
            Log.e(TAG, "Refresh failed", e)
            false
        }
    }

    private suspend fun resolveGpsAltitudeMsl(context: Context): String {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return "-- MSL"
        val fused = LocationServices.getFusedLocationProviderClient(context)
        val location = fused.lastLocation.await() ?: return "-- MSL"
        val meters = location.altitude
        if (meters.isNaN() || meters == 0.0) return "-- MSL"
        val feet = (meters * 3.28084).toInt()
        return "$feet MSL"
    }

    private fun resolvePlaceName(context: Context, lat: Double, lon: Double): String {
        if (!Geocoder.isPresent()) {
            return shortCoordLabel(lat, lon)
        }
        return runCatching {
            val geocoder = Geocoder(context, Locale.US)
            @Suppress("DEPRECATION")
            val place = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
            place?.locality
                ?: place?.subAdminArea
                ?: place?.adminArea
                ?: shortCoordLabel(lat, lon)
        }.getOrElse {
            shortCoordLabel(lat, lon)
        }.uppercase(Locale.US)
    }

    private fun shortCoordLabel(lat: Double, lon: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"
        return "${"%.1f".format(abs(lat))}$latDir ${"%.1f".format(abs(lon))}$lonDir"
    }
}
