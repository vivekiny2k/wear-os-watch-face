package com.watchapp.data

import android.content.Context
import com.watchapp.watchface.WatchFaceInvalidate
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference

/** Live weather/location/sun fields. Date and alt clock are drawn from device time in the renderer. */
data class WatchFaceData(
    val temperature: String = "--",
    val wind: String = "--",
    /** Meteorological degrees (0° = north, direction wind comes from). */
    val windDirectionDegrees: Int? = null,
    val conditions: String = "--",
    val sunrise: String = "--:--",
    val sunset: String = "--:--",
    val location: String = "--",
    val altitude: String = "-- MSL",
    val ambientTz1: String = "--",
    val ambientTz2: String = "--",
)

object WatchDataStore {
    private val data = AtomicReference(WatchFaceData())

    fun get(): WatchFaceData = data.get()

    fun hydrate(context: Context) {
        runBlocking {
            WatchFaceCache(context).load()?.let { data.set(it) }
        }
    }

    suspend fun replace(context: Context, newData: WatchFaceData) {
        data.set(newData)
        WatchFaceCache(context).save(newData)
        WatchFaceInvalidate.request()
    }

    fun updateAltitude(label: String) {
        data.updateAndGet { it.copy(altitude = label) }
        WatchFaceInvalidate.request()
    }
}
