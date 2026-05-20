package com.watchapp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.watchapp.flight.FlightMath
import com.watchapp.sensor.BarometricAltitude
import com.watchapp.watchface.WatchFaceInvalidate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

private val Context.flightDataStore by preferencesDataStore("flight_mode")

data class FlightSession(
    val tracking: Boolean = false,
    val baseAltitudeFeet: Int = 0,
    val startLat: Double? = null,
    val startLon: Double? = null,
    val startTimeMs: Long = 0L,
)

object FlightModeStore {
    private val session = AtomicReference(FlightSession())
    private val trackingKey = booleanPreferencesKey("tracking")
    private val baseKey = intPreferencesKey("base_alt_ft")
    private val startLatKey = doublePreferencesKey("start_lat")
    private val startLonKey = doublePreferencesKey("start_lon")
    private val startTimeKey = longPreferencesKey("start_time_ms")

    fun get(): FlightSession = session.get()

    fun hydrate(context: Context) {
        runBlocking {
            val prefs = context.flightDataStore.data.first()
            session.set(
                FlightSession(
                    tracking = prefs[trackingKey] ?: false,
                    baseAltitudeFeet = prefs[baseKey] ?: 0,
                    startLat = prefs[startLatKey],
                    startLon = prefs[startLonKey],
                    startTimeMs = prefs[startTimeKey] ?: 0L,
                ),
            )
        }
    }

    fun start(context: Context) {
        val (lat, lon) = FlightLocationStore.get()
        val base = BarometricAltitude.currentRawAltitudeFeet()
        val next = FlightSession(
            tracking = true,
            baseAltitudeFeet = base,
            startLat = lat,
            startLon = lon,
            startTimeMs = System.currentTimeMillis(),
        )
        session.set(next)
        runBlocking { persist(context, next) }
        WatchFaceInvalidate.request()
    }

    fun stop(context: Context) {
        val next = session.get().copy(tracking = false)
        session.set(next)
        runBlocking { persist(context, next) }
        WatchFaceInvalidate.request()
    }

    fun toggle(context: Context) {
        if (session.get().tracking) stop(context) else start(context)
    }

    fun flightHeightFeet(): Int {
        val s = session.get()
        if (!s.tracking) return 0
        val current = BarometricAltitude.currentRawAltitudeFeet()
        return (current - s.baseAltitudeFeet).coerceAtLeast(0)
    }

    fun distanceMeters(): Double {
        val s = session.get()
        val startLat = s.startLat ?: return 0.0
        val startLon = s.startLon ?: return 0.0
        val (lat, lon) = FlightLocationStore.get()
        return FlightMath.haversineMeters(startLat, startLon, lat, lon)
    }

    fun formattedDistance(): String {
        if (!session.get().tracking) return "0"
        val feet = (distanceMeters() * 3.28084).toInt()
        return feet.toString()
    }

    fun elapsedMs(): Long {
        val s = session.get()
        if (!s.tracking || s.startTimeMs <= 0L) return 0L
        return (System.currentTimeMillis() - s.startTimeMs).coerceAtLeast(0L)
    }

    fun formattedFlightTime(): String {
        val totalSec = (elapsedMs() / 1000).toInt()
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val sec = totalSec % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, sec)
    }

    private suspend fun persist(context: Context, s: FlightSession) {
        context.flightDataStore.edit { prefs ->
            prefs[trackingKey] = s.tracking
            prefs[baseKey] = s.baseAltitudeFeet
            if (s.startLat != null) prefs[startLatKey] = s.startLat else prefs.remove(startLatKey)
            if (s.startLon != null) prefs[startLonKey] = s.startLon else prefs.remove(startLonKey)
            prefs[startTimeKey] = s.startTimeMs
        }
    }
}